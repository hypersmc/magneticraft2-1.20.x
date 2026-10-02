package com.magneticraft2.common.systems.GEAR;

import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runtime manager for continuous wide item belts between two Conveyor Rollers.
 *
 * Unlike the thin transmission belt, this system does not fill the world with proxy
 * collision entities. Items are projected onto the shared BeltPath top run and moved
 * deterministically along that continuous surface.
 */
public final class ItemBeltConnectionManager {
    public static final double BELT_HALF_WIDTH = 0.375D;
    public static final double BELT_HALF_THICKNESS = 0.035D;

    private static final float MIN_TRANSPORT_RPM = 5.0F;
    private static final double MAX_BLOCKS_PER_TICK = 0.18D;
    private static final double ITEM_SURFACE_OFFSET = 0.15D;
    private static final double ITEM_CAPTURE_HALF_WIDTH = 0.52D;
    private static final double ITEM_CAPTURE_BELOW = 0.22D;
    private static final double ITEM_CAPTURE_ABOVE = 0.70D;
    private static final String ITEM_TICK_TAG = "MGC2ItemBeltTick";
    private static final String ITEM_COOLDOWN_TAG = "MGC2ItemBeltCooldown";

    private static final Map<Level, Map<BeltKey, ItemBeltConnection>> CONNECTIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ItemBeltConnectionManager() {
    }

    public static void ensureRegistered(ConveyorRollerBlockEntity roller) {
        Level level = roller.getLevel();
        BlockPos partnerPos = roller.getItemBeltPartner();
        if (level == null || partnerPos == null) {
            return;
        }

        if (!(level.getBlockEntity(partnerPos) instanceof ConveyorRollerBlockEntity partner)
                || !partner.isItemBeltLinkedTo(roller.getBlockPos())
                || partner.getGearAxis() != roller.getGearAxis()) {
            return;
        }

        BeltKey key = BeltKey.of(roller.getBlockPos(), partnerPos);
        Map<BeltKey, ItemBeltConnection> levelConnections =
                CONNECTIONS.computeIfAbsent(level, ignored -> new HashMap<>());

        ConveyorRollerBlockEntity start = key.start().equals(roller.getBlockPos())
                ? roller
                : partner;
        ConveyorRollerBlockEntity end = start == roller ? partner : roller;

        ItemBeltConnection existing = levelConnections.get(key);
        if (existing != null
                && existing.axis == start.getGearAxis()
                && Math.abs(existing.radius - start.getRollerRadius()) < 0.00001D) {
            return;
        }

        BeltPath path = BeltPath.create(
                start.getBlockPos(),
                end.getBlockPos(),
                start.getGearAxis(),
                start.getRollerRadius(),
                end.getRollerRadius()
        );

        if (path == null) {
            levelConnections.remove(key);
            return;
        }

        TransportRun transportRun = findUpperRun(path, start.getBlockPos(), end.getBlockPos());
        if (transportRun == null) {
            levelConnections.remove(key);
            return;
        }

        levelConnections.put(
                key,
                new ItemBeltConnection(
                        key,
                        start.getGearAxis(),
                        start.getRollerRadius(),
                        path,
                        transportRun
                )
        );
    }

    public static boolean isRegistered(Level level, BlockPos first, BlockPos second) {
        if (level == null || first == null || second == null) {
            return false;
        }

        Map<BeltKey, ItemBeltConnection> map = CONNECTIONS.get(level);
        return map != null && map.containsKey(BeltKey.of(first, second));
    }

    @Nullable
    public static BeltPath getPath(Level level, BlockPos first, BlockPos second) {
        if (level == null || first == null || second == null) {
            return null;
        }

        Map<BeltKey, ItemBeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return null;
        }

        ItemBeltConnection connection = map.get(BeltKey.of(first, second));
        return connection == null ? null : connection.path;
    }

    public static void tickConnection(ConveyorRollerBlockEntity roller) {
        Level level = roller.getLevel();
        BlockPos partnerPos = roller.getItemBeltPartner();
        if (!(level instanceof ServerLevel) || partnerPos == null) {
            return;
        }

        ensureRegistered(roller);

        Map<BeltKey, ItemBeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return;
        }

        ItemBeltConnection connection = map.get(BeltKey.of(roller.getBlockPos(), partnerPos));
        if (connection == null) {
            return;
        }

        long gameTick = level.getGameTime();
        if (connection.lastProcessedTick == gameTick) {
            return;
        }
        connection.lastProcessedTick = gameTick;

        tickConnection(level, connection);
    }

    private static void tickConnection(Level level, ItemBeltConnection connection) {
        if (!(level.getBlockEntity(connection.key.start()) instanceof ConveyorRollerBlockEntity start)
                || !(level.getBlockEntity(connection.key.end()) instanceof ConveyorRollerBlockEntity end)
                || !start.isItemBeltLinkedTo(end.getBlockPos())
                || !end.isItemBeltLinkedTo(start.getBlockPos())) {
            remove(level, connection.key.start(), connection.key.end());
            return;
        }

        GearNetworkManager network = GearNetworkManager.getInstance();

        double spanLength = connection.transportRun.length();
        float torqueDemand = (float) Math.min(6.0D, 0.50D + spanLength * 0.15D);

        network.setMechanicalLoad(
                level,
                connection.key.start(),
                connection.key.start(),
                torqueDemand,
                true
        );

        GearNode startNode = start.getOrCreateGearNode();
        GearNetworkManager.MechanicalLoadState loadState =
                network.getMechanicalLoadState(level, connection.key.start());

        float rpm = startNode.getEffectiveSpeed();
        boolean running = rpm >= MIN_TRANSPORT_RPM
                && !startNode.isOverloaded()
                && loadState.supplied();

        double blocksPerTick = running
                ? Math.min(
                        MAX_BLOCKS_PER_TICK,
                        (rpm / 1200.0D) * (Math.PI * 2.0D * start.getRollerRadius())
                )
                : 0.0D;

        int direction = start.getDirectionMultiplier() < 0 ? -1 : 1;
        moveItems(level, connection, blocksPerTick, direction, running);
    }

    private static void moveItems(Level level,
                                  ItemBeltConnection connection,
                                  double blocksPerTick,
                                  int direction,
                                  boolean running) {
        TransportRun run = connection.transportRun;
        long gameTick = level.getGameTime();

        for (ItemEntity item : level.getEntitiesOfClass(
                ItemEntity.class,
                run.captureBounds(),
                entity -> entity.isAlive() && !entity.getItem().isEmpty())) {

            if (item.getPersistentData().getLong(ITEM_COOLDOWN_TAG) > gameTick
                    || item.getPersistentData().getLong(ITEM_TICK_TAG) == gameTick) {
                continue;
            }

            Projection projection = run.project(item.position());
            if (!projection.onSurface()) {
                continue;
            }

            item.getPersistentData().putLong(ITEM_TICK_TAG, gameTick);

            double nextDistance = projection.distance();
            if (running) {
                nextDistance += blocksPerTick * direction;
            }

            if (nextDistance < 0.0D || nextDistance > run.length()) {
                double exitDistance = nextDistance < 0.0D ? 0.0D : run.length();
                Vec3 exitPoint = run.pointAt(exitDistance)
                        .add(run.surfaceNormal().scale(ITEM_SURFACE_OFFSET));
                Vec3 travelDirection = run.tangent().scale(direction);

                item.setPos(exitPoint.x, exitPoint.y, exitPoint.z);
                item.setDeltaMovement(travelDirection.scale(Math.max(0.04D, blocksPerTick)));
                item.getPersistentData().putLong(ITEM_COOLDOWN_TAG, gameTick + 4L);
                item.hurtMarked = true;
                continue;
            }

            Vec3 target = run.pointAt(nextDistance)
                    .add(run.surfaceNormal().scale(ITEM_SURFACE_OFFSET));

            item.setPos(target.x, target.y, target.z);
            item.setDeltaMovement(Vec3.ZERO);
            item.fallDistance = 0.0F;
            item.hurtMarked = true;
        }
    }

    public static void remove(Level level, BlockPos first, BlockPos second) {
        if (level == null || first == null || second == null) {
            return;
        }

        Map<BeltKey, ItemBeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return;
        }

        BeltKey key = BeltKey.of(first, second);
        if (map.remove(key) != null && !level.isClientSide) {
            GearNetworkManager.getInstance().removeMechanicalLoad(level, key.start());
        }

        if (map.isEmpty()) {
            CONNECTIONS.remove(level);
        }
    }

    public static void removeFor(Level level, BlockPos rollerPos) {
        if (level == null || rollerPos == null) {
            return;
        }

        Map<BeltKey, ItemBeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return;
        }

        List<BeltKey> remove = new ArrayList<>();
        for (BeltKey key : map.keySet()) {
            if (key.contains(rollerPos)) {
                remove.add(key);
            }
        }

        for (BeltKey key : remove) {
            map.remove(key);
            if (!level.isClientSide) {
                GearNetworkManager.getInstance().removeMechanicalLoad(level, key.start());
            }
        }

        if (map.isEmpty()) {
            CONNECTIONS.remove(level);
        }
    }

    @Nullable
    private static TransportRun findUpperRun(BeltPath path,
                                             BlockPos startRoller,
                                             BlockPos endRoller) {
        List<BeltPath.Segment> straightSegments = path.segments().stream()
                .filter(segment -> segment.type() == BeltPath.SegmentType.STRAIGHT)
                .toList();

        if (straightSegments.isEmpty()) {
            return null;
        }

        BeltPath.Segment upper = straightSegments.stream()
                .max(Comparator.comparingDouble(segment ->
                        (segment.from().y + segment.to().y) * 0.5D))
                .orElse(null);

        if (upper == null) {
            return null;
        }

        Vec3 startCenter = Vec3.atCenterOf(startRoller);
        Vec3 from = upper.from();
        Vec3 to = upper.to();

        // Orient the transport coordinate so distance 0 is always the canonical start roller,
        // independent of which direction BeltPath happened to emit the physical upper run.
        if (to.distanceToSqr(startCenter) < from.distanceToSqr(startCenter)) {
            Vec3 swap = from;
            from = to;
            to = swap;
        }

        Vec3 normal = upper.thicknessDirection().normalize();
        if (normal.y < 0.0D) {
            normal = normal.scale(-1.0D);
        }

        return new TransportRun(
                from,
                to,
                upper.widthDirection().normalize(),
                normal
        );
    }

    private static final class ItemBeltConnection {
        private final BeltKey key;
        private final net.minecraft.core.Direction.Axis axis;
        private final double radius;
        private final BeltPath path;
        private final TransportRun transportRun;
        private long lastProcessedTick = Long.MIN_VALUE;

        private ItemBeltConnection(BeltKey key,
                                   net.minecraft.core.Direction.Axis axis,
                                   double radius,
                                   BeltPath path,
                                   TransportRun transportRun) {
            this.key = key;
            this.axis = axis;
            this.radius = radius;
            this.path = path;
            this.transportRun = transportRun;
        }
    }

    private record BeltKey(BlockPos start, BlockPos end) {
        private static BeltKey of(BlockPos first, BlockPos second) {
            BlockPos a = first.immutable();
            BlockPos b = second.immutable();
            return a.asLong() <= b.asLong()
                    ? new BeltKey(a, b)
                    : new BeltKey(b, a);
        }

        private boolean contains(BlockPos pos) {
            return start.equals(pos) || end.equals(pos);
        }
    }

    private record Projection(double distance, boolean onSurface) {
    }

    private record TransportRun(Vec3 start,
                                Vec3 end,
                                Vec3 widthDirection,
                                Vec3 surfaceNormal) {
        private double length() {
            return start.distanceTo(end);
        }

        private Vec3 tangent() {
            Vec3 delta = end.subtract(start);
            return delta.lengthSqr() < 0.000001D ? Vec3.ZERO : delta.normalize();
        }

        private Vec3 pointAt(double distance) {
            double length = length();
            if (length <= 0.000001D) {
                return start;
            }

            double clamped = Math.max(0.0D, Math.min(length, distance));
            return start.add(tangent().scale(clamped));
        }

        private Projection project(Vec3 worldPoint) {
            Vec3 tangent = tangent();
            double length = length();
            if (length <= 0.000001D) {
                return new Projection(0.0D, false);
            }

            Vec3 relative = worldPoint.subtract(start);
            double along = relative.dot(tangent);
            if (along < -0.20D || along > length + 0.20D) {
                return new Projection(along, false);
            }

            double clampedAlong = Math.max(0.0D, Math.min(length, along));
            Vec3 center = pointAt(clampedAlong);
            Vec3 offset = worldPoint.subtract(center);

            double sideways = Math.abs(offset.dot(widthDirection));
            double normalDistance = offset.dot(surfaceNormal);

            return new Projection(
                    clampedAlong,
                    sideways <= ITEM_CAPTURE_HALF_WIDTH
                            && normalDistance >= -ITEM_CAPTURE_BELOW
                            && normalDistance <= ITEM_CAPTURE_ABOVE
            );
        }

        private AABB captureBounds() {
            Vec3 width = widthDirection.scale(ITEM_CAPTURE_HALF_WIDTH + 0.15D);
            Vec3 normal = surfaceNormal.scale(ITEM_CAPTURE_ABOVE);

            double extentX = Math.abs(width.x) + Math.abs(normal.x) + 0.30D;
            double extentY = Math.abs(width.y) + Math.abs(normal.y) + ITEM_CAPTURE_BELOW + 0.30D;
            double extentZ = Math.abs(width.z) + Math.abs(normal.z) + 0.30D;

            return new AABB(
                    Math.min(start.x, end.x) - extentX,
                    Math.min(start.y, end.y) - extentY,
                    Math.min(start.z, end.z) - extentZ,
                    Math.max(start.x, end.x) + extentX,
                    Math.max(start.y, end.y) + extentY,
                    Math.max(start.z, end.z) + extentZ
            );
        }
    }
}
