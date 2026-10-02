package com.magneticraft2.common.systems.GEAR;

import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import com.magneticraft2.common.entity.mechanical.BeltCollisionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Runtime registry for pulley-belt connections.
 *
 * BeltPath owns the geometry. On the logical server, each exposed straight run is backed by
 * lightweight invisible BeltCollisionEntity proxies. Vanilla automatically includes those
 * entity AABBs in Entity.collide(...), so belt collision requires neither hidden blocks nor
 * movement mixins.
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
                && existing.axis == start.getGearAxis()
                && Math.abs(existing.startRadius - start.getPulleyRadius()) < 0.00001D
                && Math.abs(existing.endRadius - end.getPulleyRadius()) < 0.00001D) {
            ensureCollisionProxies(level, existing);
            return;
        }

        if (existing != null) {
            removeCollisionProxies(level, existing);
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

        BeltConnection connection = new BeltConnection(
                key,
                start.getGearAxis(),
                start.getPulleyRadius(),
                end.getPulleyRadius(),
                path
        );

        levelConnections.put(key, connection);
        ensureCollisionProxies(level, connection);
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
        return connection == null ? null : connection.path;
    }

    public static void remove(Level level, BlockPos first, BlockPos second) {
        if (level == null || first == null || second == null) {
            return;
        }

        Map<BeltKey, BeltConnection> map = CONNECTIONS.get(level);
        if (map == null) {
            return;
        }

        BeltConnection removed = map.remove(BeltKey.of(first, second));
        if (removed != null) {
            removeCollisionProxies(level, removed);
        }

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

        List<BeltKey> removeKeys = new ArrayList<>();
        for (Map.Entry<BeltKey, BeltConnection> entry : map.entrySet()) {
            if (entry.getKey().contains(pulleyPos)) {
                removeCollisionProxies(level, entry.getValue());
                removeKeys.add(entry.getKey());
            }
        }

        for (BeltKey key : removeKeys) {
            map.remove(key);
        }

        if (map.isEmpty()) {
            CONNECTIONS.remove(level);
        }
    }

    private static void ensureCollisionProxies(Level level, BeltConnection connection) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!connection.proxyIds.isEmpty()) {
            boolean complete = true;
            for (UUID proxyId : connection.proxyIds) {
                Entity entity = serverLevel.getEntity(proxyId);
                if (!(entity instanceof BeltCollisionEntity) || entity.isRemoved()) {
                    complete = false;
                    break;
                }
            }

            if (complete) {
                return;
            }

            removeCollisionProxies(level, connection);
        }

        for (AABB collisionBox : connection.path.collisionBoxes()) {
            BeltCollisionEntity proxy = new BeltCollisionEntity(serverLevel, collisionBox);
            if (serverLevel.addFreshEntity(proxy)) {
                connection.proxyIds.add(proxy.getUUID());
            } else {
                proxy.discard();
            }
        }
    }

    private static void removeCollisionProxies(Level level, BeltConnection connection) {
        if (!(level instanceof ServerLevel serverLevel)) {
            connection.proxyIds.clear();
            return;
        }

        for (UUID proxyId : connection.proxyIds) {
            Entity entity = serverLevel.getEntity(proxyId);
            if (entity instanceof BeltCollisionEntity) {
                entity.discard();
            }
        }

        connection.proxyIds.clear();
    }

    /**
     * Item-transport preparation: locate the nearest registered belt path to a point.
     */
    @Nullable
    public static BeltHit findNearest(Level level,
                                      net.minecraft.world.phys.Vec3 point,
                                      double maxDistance) {
        Map<BeltKey, BeltConnection> map = CONNECTIONS.get(level);
        if (map == null || map.isEmpty()) {
            return null;
        }

        double maxDistanceSquared = maxDistance * maxDistance;
        BeltHit best = null;

        for (BeltConnection connection : map.values()) {
            BeltPath.Projection projection = connection.path.project(point);
            if (projection.distanceSquared() <= maxDistanceSquared
                    && (best == null
                    || projection.distanceSquared() < best.projection().distanceSquared())) {
                best = new BeltHit(connection.path, projection);
            }
        }

        return best;
    }

    private static final class BeltConnection {
        private final BeltKey key;
        private final Direction.Axis axis;
        private final double startRadius;
        private final double endRadius;
        private final BeltPath path;
        private final List<UUID> proxyIds = new ArrayList<>();

        private BeltConnection(BeltKey key,
                               Direction.Axis axis,
                               double startRadius,
                               double endRadius,
                               BeltPath path) {
            this.key = key;
            this.axis = axis;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.path = path;
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

    public record BeltHit(BeltPath path, BeltPath.Projection projection) {
    }
}
