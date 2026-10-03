package com.magneticraft2.common.systems.GEAR;

import com.magneticraft2.common.block.stage.copper.ItemBeltBlock;
import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Runtime controller for Create-style continuous Item Belts.
 *
 * The connection is visually rendered as one continuous loop by the canonical roller,
 * while every intermediate grid position is backed by a real ItemBeltBlock. That gives
 * Minecraft normal collision, selection and chunk ownership without collision entities.
 */
public final class ItemBeltConnectionManager {
    public static final int MAX_ITEM_BELT_SPAN = 16;
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
    private static final Map<Level, Set<BlockPos>> REMOVING_PHYSICAL_BLOCKS =
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

        ItemBeltGeometry.Layout layout = ItemBeltGeometry.create(
                start.getBlockPos(),
                end.getBlockPos(),
                start.getGearAxis(),
                MAX_ITEM_BELT_SPAN
        );

        if (layout == null) {
            levelConnections.remove(key);
            return;
        }

        ItemBeltConnection existing = levelConnections.get(key);
        if (existing != null
                && existing.axis == start.getGearAxis()
                && existing.layout.equals(layout)
                && Math.abs(existing.radius - start.getRollerRadius()) < 0.00001D) {
            if (!level.isClientSide) {
                ensurePhysicalBlocks(level, existing);
            }
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

        TransportRun transportRun = findCarryingRun(path, start.getBlockPos(), layout);
        if (transportRun == null) {
            levelConnections.remove(key);
            return;
        }

        ItemBeltConnection connection = new ItemBeltConnection(
                key,
                start.getGearAxis(),
                start.getRollerRadius(),
                path,
                layout,
                transportRun
        );

        levelConnections.put(key, connection);

        if (!level.isClientSide) {
            ensurePhysicalBlocks(level, connection);
        }
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
            unregister(level, connection.key);
            return;
        }

        if (!hasCompletePhysicalBelt(level, connection)) {
            // Do not silently overwrite a newly placed obstruction. A valid saved belt will
            // recreate missing replaceable cells, but an occupied route remains stopped until
            // the player clears/reconnects it.
            if (!ensurePhysicalBlocks(level, connection)) {
                GearNetworkManager.getInstance().removeMechanicalLoad(level, connection.key.start());
                return;
            }
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

    private static boolean ensurePhysicalBlocks(Level level, ItemBeltConnection connection) {
        BlockState desired = BlockRegistry.ITEM_BELT_BLOCK.get().stateFor(connection.layout);

        for (BlockPos pos : connection.layout.beltBlocks()) {
            BlockState state = level.getBlockState(pos);

            if (state.is(BlockRegistry.ITEM_BELT_BLOCK.get())) {
                if (!state.equals(desired)) {
                    level.setBlock(pos, desired, Block.UPDATE_ALL);
                }
                continue;
            }

            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
            }
        }

        for (BlockPos pos : connection.layout.beltBlocks()) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockRegistry.ITEM_BELT_BLOCK.get()) || !state.equals(desired)) {
                level.setBlock(pos, desired, Block.UPDATE_ALL);
            }
        }

        return true;
    }

    private static boolean hasCompletePhysicalBelt(Level level, ItemBeltConnection connection) {
        for (BlockPos pos : connection.layout.beltBlocks()) {
            if (!level.getBlockState(pos).is(BlockRegistry.ITEM_BELT_BLOCK.get())) {
                return false;
            }
        }
        return true;
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

    /**
     * Manual unlink path used by ItemBeltItem. The item itself performs the inventory refund.
     */
    public static void remove(Level level, BlockPos first, BlockPos second) {
        if (level == null || first == null || second == null) {
            return;
        }

        Map<BeltKey, ItemBeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return;
        }

        BeltKey key = BeltKey.of(first, second);
        ItemBeltConnection connection = map.remove(key);
        if (connection != null) {
            if (!level.isClientSide) {
                GearNetworkManager.getInstance().removeMechanicalLoad(level, key.start());
                removePhysicalBlocks(level, connection);
            }
        }

        if (map.isEmpty()) {
            CONNECTIONS.remove(level);
        }
    }

    /**
     * Runtime-only unregister used by BlockEntity#setRemoved during chunk unload.
     * Physical belt cells and saved endpoint links remain untouched.
     */
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

    public static void onPhysicalBeltBlockRemoved(Level level, BlockPos beltPos) {
        if (level == null
                || level.isClientSide
                || beltPos == null
                || isRemovingPhysicalBlock(level, beltPos)) {
            return;
        }

        ItemBeltConnection connection = findByPhysicalBlock(level, beltPos);
        if (connection != null) {
            destroyConnection(level, connection, true, beltPos);
        }
    }

    public static void breakAtRoller(Level level, BlockPos rollerPos) {
        if (level == null || level.isClientSide || rollerPos == null) {
            return;
        }

        Map<BeltKey, ItemBeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return;
        }

        ItemBeltConnection connection = null;
        for (Map.Entry<BeltKey, ItemBeltConnection> entry : map.entrySet()) {
            if (entry.getKey().contains(rollerPos)) {
                connection = entry.getValue();
                break;
            }
        }

        if (connection != null) {
            destroyConnection(level, connection, true, rollerPos);
        }
    }

    private static void destroyConnection(Level level,
                                          ItemBeltConnection connection,
                                          boolean refundSegments,
                                          BlockPos dropPos) {
        Map<BeltKey, ItemBeltConnection> map = CONNECTIONS.get(level);
        if (map != null) {
            map.remove(connection.key);
            if (map.isEmpty()) {
                CONNECTIONS.remove(level);
            }
        }

        GearNetworkManager.getInstance().removeMechanicalLoad(level, connection.key.start());
        removePhysicalBlocks(level, connection);

        if (level.getBlockEntity(connection.key.start()) instanceof ConveyorRollerBlockEntity start
                && start.isItemBeltLinkedTo(connection.key.end())) {
            start.disconnectItemBelt(false);
        }

        if (level.getBlockEntity(connection.key.end()) instanceof ConveyorRollerBlockEntity end
                && end.isItemBeltLinkedTo(connection.key.start())) {
            end.disconnectItemBelt(false);
        }

        if (refundSegments && dropPos != null && connection.layout.requiredSegments() > 0) {
            ItemStack refund = new ItemStack(
                    ItemRegistry.ITEM_ITEM_BELT.get(),
                    connection.layout.requiredSegments()
            );
            ItemEntity dropped = new ItemEntity(
                    level,
                    dropPos.getX() + 0.5D,
                    dropPos.getY() + 0.5D,
                    dropPos.getZ() + 0.5D,
                    refund
            );
            level.addFreshEntity(dropped);
        }
    }

    private static void removePhysicalBlocks(Level level, ItemBeltConnection connection) {
        Set<BlockPos> removing = REMOVING_PHYSICAL_BLOCKS.computeIfAbsent(
                level,
                ignored -> new HashSet<>()
        );

        try {
            for (BlockPos pos : connection.layout.beltBlocks()) {
                removing.add(pos);
            }

            for (BlockPos pos : connection.layout.beltBlocks()) {
                if (level.getBlockState(pos).is(BlockRegistry.ITEM_BELT_BLOCK.get())) {
                    level.removeBlock(pos, false);
                }
            }
        } finally {
            for (BlockPos pos : connection.layout.beltBlocks()) {
                removing.remove(pos);
            }
            if (removing.isEmpty()) {
                REMOVING_PHYSICAL_BLOCKS.remove(level);
            }
        }
    }

    private static boolean isRemovingPhysicalBlock(Level level, BlockPos pos) {
        Set<BlockPos> removing = REMOVING_PHYSICAL_BLOCKS.get(level);
        return removing != null && removing.contains(pos);
    }

    @Nullable
    private static ItemBeltConnection findByPhysicalBlock(Level level, BlockPos pos) {
        Map<BeltKey, ItemBeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return null;
        }

        for (ItemBeltConnection connection : map.values()) {
            if (connection.layout.beltBlocks().contains(pos)) {
                return connection;
            }
        }

        return null;
    }

    private static void unregister(Level level, BeltKey key) {
        Map<BeltKey, ItemBeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return;
        }

        if (map.remove(key) != null && !level.isClientSide) {
            GearNetworkManager.getInstance().removeMechanicalLoad(level, key.start());
        }

        if (map.isEmpty()) {
            CONNECTIONS.remove(level);
        }
    }

    public static PhysicalBeltState getPhysicalBeltState(Level level,
                                                        BlockPos first,
                                                        BlockPos second) {
        if (level == null || first == null || second == null) {
            return new PhysicalBeltState(0, 0);
        }

        Map<BeltKey, ItemBeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return new PhysicalBeltState(0, 0);
        }

        ItemBeltConnection connection = map.get(BeltKey.of(first, second));
        if (connection == null) {
            return new PhysicalBeltState(0, 0);
        }

        int present = 0;
        for (BlockPos pos : connection.layout.beltBlocks()) {
            if (level.getBlockState(pos).is(BlockRegistry.ITEM_BELT_BLOCK.get())) {
                present++;
            }
        }

        return new PhysicalBeltState(connection.layout.beltBlocks().size(), present);
    }

    @Nullable
    private static TransportRun findCarryingRun(BeltPath path,
                                                 BlockPos canonicalStart,
                                                 ItemBeltGeometry.Layout layout) {
        List<BeltPath.Segment> straightSegments = path.segments().stream()
                .filter(segment -> segment.type() == BeltPath.SegmentType.STRAIGHT)
                .toList();

        if (straightSegments.isEmpty()) {
            return null;
        }

        // For vertical belts both long runs have the same average Y. BeltPath emits the
        // carrying-side tangent first, and ItemBeltGeometry encodes that same side in
        // FACING. Horizontal/sloped belts can simply choose the physically upper run.
        BeltPath.Segment carrying = layout.slope() == ItemBeltGeometry.BeltSlope.VERTICAL
                ? straightSegments.get(0)
                : straightSegments.stream()
                .max(Comparator.comparingDouble(segment ->
                        (segment.from().y + segment.to().y) * 0.5D))
                .orElse(null);

        if (carrying == null) {
            return null;
        }

        Vec3 startCenter = Vec3.atCenterOf(canonicalStart);
        Vec3 from = carrying.from();
        Vec3 to = carrying.to();

        if (to.distanceToSqr(startCenter) < from.distanceToSqr(startCenter)) {
            Vec3 swap = from;
            from = to;
            to = swap;
        }

        Vec3 normal = carrying.thicknessDirection().normalize();
        if (normal.y < 0.0D) {
            normal = normal.scale(-1.0D);
        }

        return new TransportRun(
                from,
                to,
                carrying.widthDirection().normalize(),
                normal
        );
    }

    private static final class ItemBeltConnection {
        private final BeltKey key;
        private final Direction.Axis axis;
        private final double radius;
        private final BeltPath path;
        private final ItemBeltGeometry.Layout layout;
        private final TransportRun transportRun;
        private long lastProcessedTick = Long.MIN_VALUE;

        private ItemBeltConnection(BeltKey key,
                                   Direction.Axis axis,
                                   double radius,
                                   BeltPath path,
                                   ItemBeltGeometry.Layout layout,
                                   TransportRun transportRun) {
            this.key = key;
            this.axis = axis;
            this.radius = radius;
            this.path = path;
            this.layout = layout;
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

    public record PhysicalBeltState(int expectedCells, int presentCells) {
        public boolean complete() {
            return expectedCells > 0 && expectedCells == presentCells;
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
