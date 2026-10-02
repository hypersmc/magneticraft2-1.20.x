package com.magneticraft2.common.systems.GEAR;

import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runtime registry for physical pulley-belt connections.
 *
 * Weak Level keys keep client/server worlds isolated and allow old worlds to be collected
 * after disconnect. Connections themselves contain no Level reference.
 */
public final class BeltConnectionManager {
    private static final Map<Level, Map<BeltKey, BeltConnection>> CONNECTIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private BeltConnectionManager() {
    }

    public static void ensureRegistered(PulleyBlockEntity_wood pulley) {
        Level level = pulley.getLevel();
        BlockPos partnerPos = pulley.getBeltPartner();
        if (level == null || partnerPos == null) {
            return;
        }

        if (!(level.getBlockEntity(partnerPos) instanceof PulleyBlockEntity_wood partner)
                || !partner.isLinkedTo(pulley.getBlockPos())
                || partner.getGearAxis() != pulley.getGearAxis()) {
            return;
        }

        BeltKey key = BeltKey.of(pulley.getBlockPos(), partnerPos);
        Map<BeltKey, BeltConnection> levelConnections =
                CONNECTIONS.computeIfAbsent(level, ignored -> new HashMap<>());

        PulleyBlockEntity_wood start = key.start().equals(pulley.getBlockPos()) ? pulley : partner;
        PulleyBlockEntity_wood end = start == pulley ? partner : pulley;

        BeltConnection existing = levelConnections.get(key);
        if (existing != null
                && existing.axis() == start.getGearAxis()
                && Math.abs(existing.startRadius() - start.getPulleyRadius()) < 0.00001D
                && Math.abs(existing.endRadius() - end.getPulleyRadius()) < 0.00001D) {
            return;
        }

        BeltPath path = BeltPath.create(
                start.getBlockPos(),
                end.getBlockPos(),
                start.getGearAxis(),
                start.getPulleyRadius(),
                end.getPulleyRadius()
        );

        if (path == null) {
            levelConnections.remove(key);
            return;
        }

        levelConnections.put(
                key,
                new BeltConnection(
                        key,
                        start.getGearAxis(),
                        start.getPulleyRadius(),
                        end.getPulleyRadius(),
                        path
                )
        );
    }

    public static boolean isRegistered(Level level, BlockPos first, BlockPos second) {
        if (level == null || first == null || second == null) {
            return false;
        }
        Map<BeltKey, BeltConnection> map = CONNECTIONS.get(level);
        return map != null && map.containsKey(BeltKey.of(first, second));
    }

    @Nullable
    public static BeltPath getPath(Level level, BlockPos first, BlockPos second) {
        if (level == null || first == null || second == null) {
            return null;
        }
        Map<BeltKey, BeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return null;
        }
        BeltConnection connection = map.get(BeltKey.of(first, second));
        return connection == null ? null : connection.path();
    }

    public static void remove(Level level, BlockPos first, BlockPos second) {
        if (level == null || first == null || second == null) {
            return;
        }

        Map<BeltKey, BeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return;
        }

        map.remove(BeltKey.of(first, second));
        if (map.isEmpty()) {
            CONNECTIONS.remove(level);
        }
    }

    public static void removeFor(Level level, BlockPos pulleyPos) {
        if (level == null || pulleyPos == null) {
            return;
        }

        Map<BeltKey, BeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return;
        }

        map.entrySet().removeIf(entry -> entry.getKey().contains(pulleyPos));
        if (map.isEmpty()) {
            CONNECTIONS.remove(level);
        }
    }

    /**
     * Shapes are queried with the entity's swept movement box, so vanilla can resolve the
     * impending collision before the entity enters the belt.
     */
    public static List<VoxelShape> getCollisionShapes(Level level,
                                                      @Nullable Entity entity,
                                                      AABB sweptBounds) {
        if (level == null || sweptBounds == null || (entity != null && entity.isSpectator())) {
            return List.of();
        }

        Map<BeltKey, BeltConnection> map = CONNECTIONS.get(level);
        if (map == null || map.isEmpty()) {
            return List.of();
        }

        List<VoxelShape> result = new ArrayList<>();

        for (BeltConnection connection : map.values()) {
            BeltPath path = connection.path();
            if (path.bounds().intersects(sweptBounds)) {
                result.addAll(path.collisionShapes(sweptBounds));
            }
        }

        return result;
    }

    /**
     * Item-transport preparation: locate the nearest registered belt path to a point.
     */
    @Nullable
    public static BeltHit findNearest(Level level, net.minecraft.world.phys.Vec3 point, double maxDistance) {
        Map<BeltKey, BeltConnection> map = CONNECTIONS.get(level);
        if (map == null || map.isEmpty()) {
            return null;
        }

        double maxDistanceSquared = maxDistance * maxDistance;
        BeltHit best = null;

        for (BeltConnection connection : map.values()) {
            BeltPath.Projection projection = connection.path().project(point);
            if (projection.distanceSquared() <= maxDistanceSquared
                    && (best == null || projection.distanceSquared() < best.projection().distanceSquared())) {
                best = new BeltHit(connection.path(), projection);
            }
        }

        return best;
    }

    private record BeltConnection(BeltKey key,
                                  Direction.Axis axis,
                                  double startRadius,
                                  double endRadius,
                                  BeltPath path) {
    }

    private record BeltKey(BlockPos start, BlockPos end) {
        private static BeltKey of(BlockPos first, BlockPos second) {
            BlockPos a = first.immutable();
            BlockPos b = second.immutable();
            return a.asLong() <= b.asLong() ? new BeltKey(a, b) : new BeltKey(b, a);
        }

        private boolean contains(BlockPos pos) {
            return start.equals(pos) || end.equals(pos);
        }
    }

    public record BeltHit(BeltPath path, BeltPath.Projection projection) {
    }
}
