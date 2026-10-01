package com.magneticraft2.common.systems.GEAR;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.systems.networking.GearSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static com.magneticraft2.common.systems.mgc2Network.CHANNEL;

/**
 * Gear V2 foundation manager.
 *
 * This still keeps the old getInstance() API so existing gear classes do not need a full rewrite,
 * but the actual gear maps are now stored per dimension instead of in one global position map.
 */
public class GearNetworkManager {
    private static final float DECAY_FACTOR = 0.92F;
    private static final float STOP_EPSILON = 0.05F;
    private static final float TORQUE_EPSILON = 0.001F;
    private static final int GEAR_MESH_SCAN_RADIUS = 3;

    private static GearNetworkManager instance;

    private final Map<ResourceKey<Level>, Map<BlockPos, GearNode>> gearsByLevel = new HashMap<>();
    private final Map<ResourceKey<Level>, Long> lastDecayTickByLevel = new HashMap<>();
    private final Map<ResourceKey<Level>, Long> lastRotationTickByLevel = new HashMap<>();

    private GearNetworkManager() {
    }

    public static GearNetworkManager getInstance() {
        if (instance == null) {
            instance = new GearNetworkManager();
        }
        return instance;
    }

    public void addGear(GearNode gear, Level level) {
        if (gear == null || level == null || level.isClientSide) {
            return;
        }
        getGearMap(level).put(gear.getPosition(), gear);
        updateNetwork(level);
    }

    public void addOrUpdateGear(GearBlockEntity gearBlockEntity) {
        if (gearBlockEntity == null || gearBlockEntity.getLevel() == null || gearBlockEntity.getLevel().isClientSide) {
            return;
        }

        GearNode node = gearBlockEntity.getOrCreateGearNode();
        node.setTeeth(gearBlockEntity.getGearTeeth());
        node.setAxis(gearBlockEntity.getGearAxis());
        node.setMaxTorque(gearBlockEntity.getGearMaxTorque());
        node.setShaftLike(gearBlockEntity.isShaftLike());
        getGearMap(gearBlockEntity.getLevel()).put(node.getPosition(), node);
        updateNetwork(gearBlockEntity.getLevel());
    }

    public void removeGear(BlockPos position, Level level) {
        if (position == null || level == null || level.isClientSide) {
            return;
        }
        getGearMap(level).remove(position);
        updateNetwork(level);
    }

    public void tickGear(GearBlockEntity gearBlockEntity) {
        if (gearBlockEntity == null || gearBlockEntity.getLevel() == null || gearBlockEntity.getLevel().isClientSide) {
            return;
        }
        addOrUpdateGear(gearBlockEntity);
    }

    public void updateNetwork(Level level) {
        if (level == null || level.isClientSide) {
            return;
        }

        Map<BlockPos, GearNode> gears = getGearMap(level);
        removeMissingBlockEntities(level, gears);
        refreshGearMetadata(level, gears);
        advanceGearRotations(level, gears);

        Queue<GearNode> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> activelyDriven = new HashSet<>();

        for (GearNode gear : gears.values()) {
            if (gear.isSource() && gear.getSpeed() > STOP_EPSILON) {
                gear.setSourcePos(gear.getPosition());
                gear.setDirectionMultiplier(1);
                gear.setMeshPhaseDegrees(0.0F);
                gear.setOverloaded(gear.getTorque() > gear.getMaxTorque() + TORQUE_EPSILON);
                visited.add(gear.getPosition());
                activelyDriven.add(gear.getPosition());
                if (!gear.isOverloaded()) {
                    queue.add(gear);
                } else {
                    gear.setSpeed(0.0F);
                }
            }
        }

        while (!queue.isEmpty()) {
            GearNode current = queue.poll();
            for (GearConnection connection : getConnectedGears(current.getPosition(), level)) {
                GearNode neighbor = gears.get(connection.neighborPos());
                if (neighbor == null || visited.contains(connection.neighborPos())) {
                    continue;
                }

                float ratio = connection.shaftConnection()
                        ? 1.0F
                        : (float) current.getTeeth() / (float) neighbor.getTeeth();
                float newSpeed = current.getSpeed() * ratio;
                float newTorque = current.getTorque() / Math.max(TORQUE_EPSILON, ratio);
                boolean overloaded = newTorque > neighbor.getMaxTorque() + TORQUE_EPSILON;

                neighbor.setTorque(newTorque);
                neighbor.setOverloaded(overloaded);
                if (connection.shaftConnection()) {
                    neighbor.setMeshPhaseDegrees(current.getMeshPhaseDegrees());
                    neighbor.setRotationDegrees(current.getRotationDegrees());
                    neighbor.setDirectionMultiplier(current.getDirectionMultiplier());
                } else {
                    neighbor.setMeshPhaseDegrees(calculateMeshedPhaseDegrees(current, neighbor));
                    neighbor.setDirectionMultiplier(-current.getDirectionMultiplier());
                }
                neighbor.setSourcePos(current.getSourcePos() == null ? current.getPosition() : current.getSourcePos());

                if (overloaded) {
                    neighbor.setSpeed(0.0F);
                } else {
                    neighbor.setSpeed(newSpeed);
                    queue.add(neighbor);
                }

                visited.add(connection.neighborPos());
                activelyDriven.add(connection.neighborPos());
            }
        }

        boolean shouldDecayThisTick = shouldDecayThisTick(level);
        if (shouldDecayThisTick) {
            for (GearNode gear : gears.values()) {
                if (!activelyDriven.contains(gear.getPosition())) {
                    gear.decayMotion(DECAY_FACTOR, STOP_EPSILON);
                }
            }
        }

        alignPassiveGearPhases(level, gears, activelyDriven);
        syncAll(level, gears.values());
    }

    private void refreshGearMetadata(Level level, Map<BlockPos, GearNode> gears) {
        for (GearNode gear : gears.values()) {
            gear.setTeeth(getTeethFor(level, gear.getPosition(), gear.getTeeth()));
            gear.setAxis(getAxisFor(level, gear.getPosition(), gear.getAxis()));
            gear.setMaxTorque(getMaxTorqueFor(level, gear.getPosition(), gear.getMaxTorque()));
            gear.setShaftLike(isShaftLikeFor(level, gear.getPosition(), gear.isShaftLike()));
            if (!gear.isSource()) {
                gear.setOverloaded(false);
            }
        }
    }


    private void advanceGearRotations(Level level, Map<BlockPos, GearNode> gears) {
        ResourceKey<Level> dimension = level.dimension();
        long gameTime = level.getGameTime();
        Long lastRotationTick = lastRotationTickByLevel.get(dimension);
        if (lastRotationTick != null && lastRotationTick == gameTime) {
            return;
        }

        float deltaTicks = lastRotationTick == null ? 1.0F : Math.max(0.0F, gameTime - lastRotationTick);
        if (deltaTicks > 20.0F) {
            deltaTicks = 20.0F;
        }

        for (GearNode gear : gears.values()) {
            gear.advanceRotation(deltaTicks);
        }

        lastRotationTickByLevel.put(dimension, gameTime);
    }

    private boolean shouldDecayThisTick(Level level) {
        ResourceKey<Level> dimension = level.dimension();
        long gameTime = level.getGameTime();
        Long lastDecayTick = lastDecayTickByLevel.get(dimension);
        if (lastDecayTick != null && lastDecayTick == gameTime) {
            return false;
        }
        lastDecayTickByLevel.put(dimension, gameTime);
        return true;
    }

    /**
     * Keep gear teeth aligned even when the network is not actively powered.
     *
     * The powered BFS above assigns phase while RPM is flowing. Without this passive pass,
     * newly placed idle gears all sit at phase 0 until they run once, and stopped gears can
     * lose their tooth/gap alignment after decay reaches zero.
     */
    private void alignPassiveGearPhases(Level level, Map<BlockPos, GearNode> gears, Set<BlockPos> activelyDriven) {
        Set<BlockPos> visited = new HashSet<>(activelyDriven);

        for (GearNode root : gears.values()) {
            if (visited.contains(root.getPosition())) {
                continue;
            }

            Queue<GearNode> queue = new ArrayDeque<>();
            visited.add(root.getPosition());
            queue.add(root);

            while (!queue.isEmpty()) {
                GearNode current = queue.poll();

                for (GearConnection connection : getConnectedGears(current.getPosition(), level)) {
                    GearNode neighbor = gears.get(connection.neighborPos());
                    if (neighbor == null || visited.contains(connection.neighborPos())) {
                        continue;
                    }

                    if (connection.shaftConnection()) {
                        neighbor.setMeshPhaseDegrees(current.getMeshPhaseDegrees());
                        neighbor.setRotationDegrees(current.getRotationDegrees());
                        neighbor.setDirectionMultiplier(current.getDirectionMultiplier());
                    } else {
                        neighbor.setMeshPhaseDegrees(calculateMeshedPhaseDegrees(current, neighbor));
                        neighbor.setDirectionMultiplier(-current.getDirectionMultiplier());
                    }

                    visited.add(connection.neighborPos());
                    queue.add(neighbor);
                }
            }
        }
    }

    private void removeMissingBlockEntities(Level level, Map<BlockPos, GearNode> gears) {
        gears.entrySet().removeIf(entry -> !(level.getBlockEntity(entry.getKey()) instanceof GearBlockEntity));
    }

    private int getTeethFor(Level level, BlockPos pos, int fallback) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof GearBlockEntity gearBlockEntity) {
            return gearBlockEntity.getGearTeeth();
        }
        return fallback;
    }

    private Direction.Axis getAxisFor(Level level, BlockPos pos, Direction.Axis fallback) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof GearBlockEntity gearBlockEntity) {
            return gearBlockEntity.getGearAxis();
        }
        return fallback == null ? Direction.Axis.Y : fallback;
    }

    private float getMaxTorqueFor(Level level, BlockPos pos, float fallback) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof GearBlockEntity gearBlockEntity) {
            return gearBlockEntity.getGearMaxTorque();
        }
        return fallback;
    }

    private boolean isShaftLikeFor(Level level, BlockPos pos, boolean fallback) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof GearBlockEntity gearBlockEntity) {
            return gearBlockEntity.isShaftLike();
        }
        return fallback;
    }

    private float calculateMeshedPhaseDegrees(GearNode current, GearNode neighbor) {
        // External gears need one gear's teeth to sit in the other gear's gaps.
        // This simple phase offset is half of the driven gear's tooth pitch.
        // Example: an 8 tooth gear has 45 degrees per tooth, so its gap offset is 22.5 degrees.
        return normalizeDegrees(current.getMeshPhaseDegrees() + (180.0F / Math.max(1, neighbor.getTeeth())));
    }

    private float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0F;
        if (normalized < 0.0F) {
            normalized += 360.0F;
        }
        return normalized;
    }

    private List<GearConnection> getConnectedGears(BlockPos pos, Level level) {
        List<GearConnection> connected = new ArrayList<>();
        Map<BlockPos, GearNode> gears = getGearMap(level);
        GearNode gear = gears.get(pos);
        if (gear == null) {
            return connected;
        }

        // Shaft/axle style transmission is only along the spin axis and remains adjacent.
        // This is how a shaft line keeps the exact same speed, direction, phase, and rotation.
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != gear.getAxis()) {
                continue;
            }

            BlockPos neighborPos = pos.relative(direction);
            GearNode neighbor = gears.get(neighborPos);
            if (neighbor != null && neighbor.getAxis() == gear.getAxis()) {
                connected.add(new GearConnection(neighborPos, true));
            }
        }

        // External gear tooth meshing can happen at larger grid distances depending on gear size.
        // Medium-medium meshes at distance 1, medium-large at distance 2, and large-large at
        // distance 2 for the current wooden models. This keeps small gears out of the visual
        // footprint of large gears while avoiding invisible long-distance large-large meshing.
        if (gear.isShaftLike()) {
            return connected;
        }

        for (BlockPos scanPosMutable : BlockPos.betweenClosed(
                pos.offset(-GEAR_MESH_SCAN_RADIUS, -GEAR_MESH_SCAN_RADIUS, -GEAR_MESH_SCAN_RADIUS),
                pos.offset(GEAR_MESH_SCAN_RADIUS, GEAR_MESH_SCAN_RADIUS, GEAR_MESH_SCAN_RADIUS))) {

            BlockPos scanPos = scanPosMutable.immutable();
            if (scanPos.equals(pos)) {
                continue;
            }

            GearNode neighbor = gears.get(scanPos);
            if (neighbor == null || neighbor.isShaftLike() || neighbor.getAxis() != gear.getAxis()) {
                continue;
            }

            if (!GearPlacementValidator.isOffsetInGearPlane(pos, scanPos, gear.getAxis())) {
                continue;
            }

            if (!GearPlacementValidator.isStraightPlanarOffset(pos, scanPos, gear.getAxis())) {
                continue;
            }

            int requiredDistance = GearPlacementValidator.getRequiredMeshDistance(gear.getTeeth(), neighbor.getTeeth());
            int planarDistance = GearPlacementValidator.getPlanarChebyshevDistance(pos, scanPos, gear.getAxis());
            if (planarDistance == requiredDistance) {
                connected.add(new GearConnection(scanPos, false));
            }
        }

        return connected;
    }

    public GearNode getGear(BlockPos position) {
        if (position == null) {
            return null;
        }
        for (Map<BlockPos, GearNode> gears : gearsByLevel.values()) {
            GearNode gear = gears.get(position);
            if (gear != null) {
                return gear;
            }
        }
        return null;
    }

    public GearNode getGear(BlockPos position, Level level) {
        if (position == null || level == null) {
            return null;
        }
        return getGearMap(level).get(position);
    }

    public void printNetworkState() {
        System.out.println("Gear Network State:");
        for (Map.Entry<ResourceKey<Level>, Map<BlockPos, GearNode>> levelEntry : gearsByLevel.entrySet()) {
            System.out.println("Dimension: " + levelEntry.getKey().location());
            for (GearNode gear : levelEntry.getValue().values()) {
                System.out.println("Gear at " + gear.getPosition() +
                        " | Speed: " + gear.getSpeed() +
                        " | Torque: " + gear.getTorque() +
                        " / " + gear.getMaxTorque() +
                        " | Overloaded: " + gear.isOverloaded() +
                        " | Direction Multiplier: " + gear.getDirectionMultiplier() +
                        " | Mesh Phase: " + gear.getMeshPhaseDegrees() +
                        " | Rotation: " + gear.getRotationDegrees() +
                        " | Source: " + gear.getSourcePos() +
                        " | Teeth: " + gear.getTeeth() +
                        " | Axis: " + gear.getAxis());
            }
        }
    }

    private Map<BlockPos, GearNode> getGearMap(Level level) {
        return gearsByLevel.computeIfAbsent(level.dimension(), dimension -> new HashMap<>());
    }

    private void syncAll(Level level, Iterable<GearNode> gears) {
        for (GearNode gear : gears) {
            CHANNEL.send(PacketDistributor.ALL.noArg(), new GearSyncPacket(
                    gear.getPosition(),
                    gear.getSpeed(),
                    gear.getTorque(),
                    gear.getMaxTorque(),
                    gear.isOverloaded(),
                    gear.getMeshPhaseDegrees(),
                    gear.getRotationDegrees(),
                    gear.getDirectionMultiplier(),
                    gear.getSourcePos()
            ));
        }
    }

    private record GearConnection(BlockPos neighborPos, boolean shaftConnection) {
    }
}
