package com.magneticraft2.common.systems.GEAR;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.ClutchBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.CustomGearboxBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.GearboxBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.FlywheelBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.OverloadDisconnectBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.WaterWheelBlockEntity;
import com.magneticraft2.common.systems.networking.GearSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

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
    private static final int VISUAL_SYNC_INTERVAL_TICKS = 4;

    private static GearNetworkManager instance;

    private final Map<ResourceKey<Level>, Map<BlockPos, GearNode>> gearsByLevel = new HashMap<>();
    private final Map<ResourceKey<Level>, Map<BlockPos, MechanicalLoad>> loadsByLevel = new HashMap<>();
    private final Map<ResourceKey<Level>, Map<BlockPos, List<GearConnection>>> connectionsByLevel = new HashMap<>();
    private final Set<ResourceKey<Level>> topologyDirtyLevels = new HashSet<>();
    private final Map<ResourceKey<Level>, Long> lastDecayTickByLevel = new HashMap<>();
    private final Map<ResourceKey<Level>, Long> lastRotationTickByLevel = new HashMap<>();
    private final Map<ResourceKey<Level>, Long> lastNetworkTickByLevel = new HashMap<>();
    private final Map<ResourceKey<Level>, Long> lastSyncTickByLevel = new HashMap<>();

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
        markTopologyDirty(level);
        updateNetwork(level);
    }

    public void addOrUpdateGear(GearBlockEntity gearBlockEntity) {
        if (gearBlockEntity == null
                || gearBlockEntity.getLevel() == null
                || gearBlockEntity.getLevel().isClientSide) {
            return;
        }

        registerGearMetadata(gearBlockEntity);
        markTopologyDirty(gearBlockEntity.getLevel());
        // Explicit calls mean something meaningful changed (placement, source state,
        // topology, etc.), so refresh immediately. Normal per-BE ticking uses tickGear()
        // and is guarded to one whole-network evaluation per level per game tick.
        updateNetwork(gearBlockEntity.getLevel());
    }

    private boolean registerGearMetadata(GearBlockEntity gearBlockEntity) {
        Level level = gearBlockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }

        GearNode node = gearBlockEntity.getOrCreateGearNode();
        node.setTeeth(gearBlockEntity.getGearTeeth());
        node.setAxis(gearBlockEntity.getGearAxis());
        node.setMaxTorque(gearBlockEntity.getGearMaxTorque());
        node.setShaftLike(gearBlockEntity.isShaftLike());

        Map<BlockPos, GearNode> gears = getGearMap(level);
        boolean newlyRegistered = !gears.containsKey(node.getPosition());
        gears.put(node.getPosition(), node);
        return newlyRegistered;
    }

    public void removeGear(BlockPos position, Level level) {
        if (position == null || level == null || level.isClientSide) {
            return;
        }
        if (getGearMap(level).remove(position) != null) {
            markTopologyDirty(level);
        }
        updateNetwork(level);
    }

    public void tickGear(GearBlockEntity gearBlockEntity) {
        if (gearBlockEntity == null
                || gearBlockEntity.getLevel() == null
                || gearBlockEntity.getLevel().isClientSide) {
            return;
        }

        Level level = gearBlockEntity.getLevel();
        if (registerGearMetadata(gearBlockEntity)) {
            markTopologyDirty(level);
        }

        ResourceKey<Level> dimension = level.dimension();
        long gameTime = level.getGameTime();
        Long lastTick = lastNetworkTickByLevel.get(dimension);

        // Every GearBlockEntity ticks independently. Previously each one rebuilt the
        // complete Gear V2 graph, scanned all possible mesh neighbors, evaluated all
        // loads and synchronized every node. With N nodes that turned one network tick
        // into roughly N full network rebuilds.
        if (lastTick != null && lastTick == gameTime) {
            return;
        }

        lastNetworkTickByLevel.put(dimension, gameTime);
        updateNetwork(level);
    }

    public void updateNetwork(Level level) {
        if (level == null || level.isClientSide) {
            return;
        }

        Map<BlockPos, GearNode> gears = getGearMap(level);
        ResourceKey<Level> dimension = level.dimension();

        boolean topologyDirty = topologyDirtyLevels.remove(dimension);
        if (removeMissingBlockEntities(level, gears)) {
            topologyDirty = true;
        }

        if (topologyDirty) {
            refreshGearMetadata(level, gears);
            getConnectionCache(level).clear();
        }

        // Flywheels are passive while a real source is present. If a connected
        // component loses all non-flywheel sources, one charged flywheel becomes
        // the temporary inertial source for that component.
        prepareFlywheelSources(level, gears);

        Queue<GearNode> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> activelyDriven = new HashSet<>();

        for (GearNode gear : gears.values()) {
            if (gear.isSource() && gear.getSpeed() > STOP_EPSILON) {
                gear.setSourcePos(gear.getPosition());

                // A source owns its rotation direction. Do not overwrite it here:
                // continuous sources such as Water Wheels derive clockwise vs
                // counter-clockwise rotation from their physical input. Downstream
                // shafts/belts preserve it and external gears reverse exactly once.
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

                float ratio = connection.kind() == ConnectionKind.SHAFT
                        || connection.kind() == ConnectionKind.RIGHT_ANGLE
                        || connection.kind() == ConnectionKind.CUSTOM_GEARBOX
                        ? 1.0F
                        : (float) current.getTeeth() / (float) neighbor.getTeeth();
                float newSpeed = current.getSpeed() * ratio;

                // Torque on the network is available capacity, not a load that is being
                // continuously applied. A 16T shaft connected to an 8T wooden gear does
                // not magically put 16T through that unloaded gear and stall it; the gear
                // can simply pass at most 8T. Actual overload is evaluated later from
                // MechanicalLoad demand.
                float availableTorque =
                        current.getTorque() / Math.max(TORQUE_EPSILON, ratio);
                float transmittedTorque =
                        Math.min(availableTorque, neighbor.getMaxTorque());

                neighbor.setTorque(transmittedTorque);
                neighbor.setOverloaded(false);

                if (connection.kind() == ConnectionKind.SHAFT) {
                    neighbor.setMeshPhaseDegrees(current.getMeshPhaseDegrees());
                    neighbor.setRotationDegrees(current.getRotationDegrees());
                    neighbor.setDirectionMultiplier(current.getDirectionMultiplier());
                } else if (connection.kind() == ConnectionKind.RIGHT_ANGLE) {
                    int turnSign = getRightAngleDirectionSign(
                            level,
                            current.getPosition(),
                            neighbor.getPosition()
                    );
                    neighbor.setMeshPhaseDegrees(0.0F);
                    neighbor.setRotationDegrees(
                            current.getRotationDegrees() * turnSign
                    );
                    neighbor.setDirectionMultiplier(
                            current.getDirectionMultiplier() * turnSign
                    );
                } else if (connection.kind() == ConnectionKind.CUSTOM_GEARBOX) {
                    int customSign = getCustomGearboxDirectionSign(
                            level,
                            current.getPosition(),
                            neighbor.getPosition()
                    );
                    neighbor.setMeshPhaseDegrees(0.0F);
                    neighbor.setRotationDegrees(
                            current.getRotationDegrees() * customSign
                    );
                    neighbor.setDirectionMultiplier(
                            current.getDirectionMultiplier() * customSign
                    );
                } else if (connection.kind() == ConnectionKind.BELT) {
                    neighbor.setMeshPhaseDegrees(0.0F);
                    neighbor.setRotationDegrees(current.getRotationDegrees() * ratio);
                    neighbor.setDirectionMultiplier(current.getDirectionMultiplier());
                } else {
                    neighbor.setMeshPhaseDegrees(calculateMeshedPhaseDegrees(current, neighbor));
                    neighbor.setDirectionMultiplier(-current.getDirectionMultiplier());
                }
                neighbor.setSourcePos(current.getSourcePos() == null ? current.getPosition() : current.getSourcePos());

                neighbor.setSpeed(newSpeed);
                queue.add(neighbor);

                visited.add(connection.neighborPos());
                activelyDriven.add(connection.neighborPos());
            }
        }

        // A topology split (manual clutch, overload disconnect, removed belt,
        // etc.) must sever source ownership immediately. Detached nodes may keep
        // some residual RPM/torque while they visually coast down, but that motion
        // is no longer mechanically connected to the old source and therefore must
        // not keep contributing load to it.
        for (GearNode gear : gears.values()) {
            if (!gear.isSource()
                    && !activelyDriven.contains(gear.getPosition())) {
                gear.setSourcePos(null);
                gear.setOverloaded(false);
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

        applyMechanicalLoads(level, gears);
        updateFlywheelStorage(level, gears);

        // Advance visual/mechanical rotation only after the final overload state for this
        // network tick is known. Advancing before load evaluation made stalled networks
        // creep a few degrees every tick because refreshGearMetadata temporarily clears
        // non-source overload flags before the load pass reapplies them.
        advanceGearRotations(level, gears);
        lockDrivenGearRotations(level, gears);

        alignPassiveGearPhases(level, gears, activelyDriven);
        syncAll(level, gears.values());
    }

    public void setMechanicalLoad(Level level,
                                  BlockPos loadPos,
                                  BlockPos inputPos,
                                  float torqueDemand,
                                  boolean active) {
        if (level == null || level.isClientSide || loadPos == null || inputPos == null) {
            return;
        }

        Map<BlockPos, MechanicalLoad> loads = getLoadMap(level);
        float clampedDemand = Math.max(0.0F, torqueDemand);
        MechanicalLoad existing = loads.get(loadPos);
        boolean changed = existing == null
                || !existing.inputPos.equals(inputPos)
                || Math.abs(existing.localTorqueDemand - clampedDemand) > TORQUE_EPSILON
                || existing.active != active;

        if (existing == null) {
            existing = new MechanicalLoad(loadPos, inputPos);
            loads.put(loadPos, existing);
        }

        existing.inputPos = inputPos;
        existing.localTorqueDemand = clampedDemand;
        existing.active = active;

        if (changed) {
            updateNetwork(level);
        }
    }

    public void removeMechanicalLoad(Level level, BlockPos loadPos) {
        if (level == null || level.isClientSide || loadPos == null) {
            return;
        }

        if (getLoadMap(level).remove(loadPos) != null) {
            updateNetwork(level);
        }
    }

    public MechanicalLoadState getMechanicalLoadState(Level level, BlockPos loadPos) {
        if (level == null || loadPos == null) {
            return MechanicalLoadState.inactive();
        }

        MechanicalLoad load = getLoadMap(level).get(loadPos);
        return load == null ? MechanicalLoadState.inactive() : load.snapshot();
    }

    /**
     * Decide which flywheel, if any, is allowed to act as a source before the
     * normal source BFS begins.
     *
     * A component with an active Water Wheel/hand source always wins over stored
     * inertia. When no real source exists, the charged flywheel with the highest
     * stored RPM becomes the temporary source. Other flywheels remain passive,
     * which avoids two inertial sources fighting each other in the same network.
     */
    private void prepareFlywheelSources(
            Level level,
            Map<BlockPos, GearNode> gears) {
        Set<BlockPos> handled =
                new HashSet<>();

        for (Map.Entry<BlockPos, GearNode> entry :
                gears.entrySet()) {
            BlockPos start = entry.getKey();

            if (handled.contains(start)
                    || !(level.getBlockEntity(start)
                    instanceof FlywheelBlockEntity_wood)) {
                continue;
            }

            Set<BlockPos> component =
                    collectMechanicalComponent(
                            level,
                            gears,
                            start
                    );

            handled.addAll(component);

            boolean hasRealSource = false;
            List<FlywheelBlockEntity_wood> flywheels =
                    new ArrayList<>();

            for (BlockPos pos : component) {
                BlockEntity blockEntity =
                        level.getBlockEntity(pos);

                if (blockEntity
                        instanceof FlywheelBlockEntity_wood flywheel) {
                    flywheels.add(flywheel);
                    continue;
                }

                GearNode node = gears.get(pos);

                if (node != null
                        && node.isSource()
                        && node.getSpeed() > STOP_EPSILON
                        && !node.isOverloaded()) {
                    hasRealSource = true;
                }
            }

            if (hasRealSource) {
                for (FlywheelBlockEntity_wood flywheel :
                        flywheels) {
                    flywheel.setPassiveForExternalDrive();
                }
                continue;
            }

            FlywheelBlockEntity_wood leader = null;

            for (FlywheelBlockEntity_wood flywheel :
                    flywheels) {
                if (!flywheel.hasStoredInertia()) {
                    flywheel.setPassiveForExternalDrive();
                    continue;
                }

                if (leader == null
                        || flywheel.getStoredSpeed()
                        > leader.getStoredSpeed()
                        + STOP_EPSILON
                        || (Math.abs(
                                flywheel.getStoredSpeed()
                                        - leader.getStoredSpeed()
                        ) <= STOP_EPSILON
                        && flywheel.getBlockPos().asLong()
                        < leader.getBlockPos().asLong())) {
                    leader = flywheel;
                }
            }

            for (FlywheelBlockEntity_wood flywheel :
                    flywheels) {
                if (flywheel == leader) {
                    flywheel.prepareAsInertialSource();
                } else {
                    flywheel.setPassiveForExternalDrive();
                }
            }
        }
    }

    private Set<BlockPos> collectMechanicalComponent(
            Level level,
            Map<BlockPos, GearNode> gears,
            BlockPos start) {
        Set<BlockPos> component =
                new HashSet<>();
        Queue<BlockPos> queue =
                new ArrayDeque<>();

        component.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current =
                    queue.poll();

            for (GearConnection connection :
                    getConnectedGears(
                            current,
                            level
                    )) {
                BlockPos neighbor =
                        connection.neighborPos();

                if (!gears.containsKey(neighbor)
                        || !component.add(neighbor)) {
                    continue;
                }

                queue.add(neighbor);
            }
        }

        return component;
    }

    /**
     * Charge flywheels from real sources and drain the one currently carrying a
     * component on stored inertia. Drain rate increases with real mechanical
     * demand, so a flywheel can bridge a light interruption much longer than it
     * can run a heavily loaded machine.
     */
    private void updateFlywheelStorage(
            Level level,
            Map<BlockPos, GearNode> gears) {
        Map<BlockPos, Float> demandBySource =
                new HashMap<>();

        for (MechanicalLoad load :
                getLoadMap(level).values()) {
            if (!load.active
                    || load.sourcePos == null
                    || load.sourceEquivalentDemand
                    <= TORQUE_EPSILON) {
                continue;
            }

            demandBySource.merge(
                    load.sourcePos,
                    load.sourceEquivalentDemand,
                    Float::sum
            );
        }

        for (Map.Entry<BlockPos, GearNode> entry :
                gears.entrySet()) {
            BlockPos pos = entry.getKey();

            if (!(level.getBlockEntity(pos)
                    instanceof FlywheelBlockEntity_wood flywheel)) {
                continue;
            }

            GearNode node = entry.getValue();

            if (node.isSource()
                    && pos.equals(node.getSourcePos())) {
                flywheel.coastTick(
                        demandBySource.getOrDefault(
                                pos,
                                0.0F
                        ),
                        node.isOverloaded()
                );
                continue;
            }

            BlockPos sourcePos =
                    node.getSourcePos();

            if (sourcePos == null
                    || sourcePos.equals(pos)
                    || node.getSpeed() <= STOP_EPSILON) {
                continue;
            }

            BlockEntity sourceBlockEntity =
                    level.getBlockEntity(sourcePos);

            // Never recharge one flywheel from another. That would create a
            // perpetual hand-off of stored energy. Only a real source replenishes
            // the stored inertia.
            if (sourceBlockEntity
                    instanceof FlywheelBlockEntity_wood) {
                continue;
            }

            GearNode sourceNode =
                    gears.get(sourcePos);

            if (sourceNode != null
                    && sourceNode.isSource()
                    && sourceNode.getSpeed() > STOP_EPSILON
                    && !sourceNode.isOverloaded()) {
                flywheel.captureExternalDrive(
                        node.getSpeed(),
                        node.getDirectionMultiplier()
                );
            }
        }
    }

    private void applyMechanicalLoads(Level level,
                                      Map<BlockPos, GearNode> gears) {
        Map<BlockPos, MechanicalLoad> loads = getLoadMap(level);

        loads.entrySet().removeIf(entry ->
                level.hasChunkAt(entry.getKey())
                        && level.getBlockEntity(entry.getKey()) == null);

        Map<BlockPos, Float> totalSourceDemand = new HashMap<>();

        for (MechanicalLoad load : loads.values()) {
            load.resetEvaluation();
            if (!load.active
                    || load.localTorqueDemand <= TORQUE_EPSILON) {
                continue;
            }

            GearNode input = gears.get(load.inputPos);
            if (input == null
                    || input.getSpeed() <= STOP_EPSILON) {
                continue;
            }

            BlockPos sourcePos = input.getSourcePos();
            GearNode source = sourcePos == null
                    ? null
                    : gears.get(sourcePos);

            if (source == null
                    || source.getSpeed() <= STOP_EPSILON) {
                continue;
            }

            load.sourcePos = sourcePos;
            load.sourceTorqueCapacity = source.getTorque();

            // Preserve mechanical power through gearing:
            // T_source * RPM_source = T_load * RPM_load.
            float speedRatio =
                    input.getSpeed()
                            / Math.max(
                                    STOP_EPSILON,
                                    source.getSpeed()
                            );

            load.sourceEquivalentDemand =
                    load.localTorqueDemand * speedRatio;

            totalSourceDemand.merge(
                    sourcePos,
                    load.sourceEquivalentDemand,
                    Float::sum
            );
        }

        Set<BlockPos> overloadedSources = new HashSet<>();
        Set<BlockPos> protectedSources = new HashSet<>();

        // Prefer shedding the heaviest branch first when aggregate source load
        // is too high. This makes automatic clutch behavior deterministic and
        // avoids opening every clutch on a source at once.
        List<MechanicalLoad> evaluatedLoads =
                new ArrayList<>(loads.values());

        evaluatedLoads.sort(
                (first, second) -> Float.compare(
                        second.sourceEquivalentDemand,
                        first.sourceEquivalentDemand
                )
        );

        for (MechanicalLoad load : evaluatedLoads) {
            if (!load.active || load.sourcePos == null) {
                continue;
            }

            GearNode input = gears.get(load.inputPos);
            GearNode source = gears.get(load.sourcePos);
            if (input == null || source == null) {
                continue;
            }

            load.totalSourceDemand =
                    totalSourceDemand.getOrDefault(
                            load.sourcePos,
                            0.0F
                    );
            load.sourceTorqueCapacity =
                    source.getTorque();

            boolean localOverload =
                    input.isOverloaded()
                            || load.localTorqueDemand
                            > input.getTorque()
                            + TORQUE_EPSILON;

            boolean sourceOverload =
                    load.totalSourceDemand
                            > load.sourceTorqueCapacity
                            + TORQUE_EPSILON;

            if (!localOverload && !sourceOverload) {
                continue;
            }

            if (protectedSources.contains(load.sourcePos)) {
                continue;
            }

            // Before stalling the source, look for an engaged overload disconnect
            // between that source and the overloaded consumer. If one exists,
            // trip it and let the source keep rotating unloaded.
            if (tripProtectiveDisconnectOnPath(
                    level,
                    gears,
                    load.sourcePos,
                    load.inputPos
            )) {
                protectedSources.add(load.sourcePos);
                overloadedSources.remove(load.sourcePos);
                load.supplied = false;
                continue;
            }

            overloadedSources.add(load.sourcePos);
        }

        if (!overloadedSources.isEmpty()) {
            for (GearNode gear : gears.values()) {
                BlockPos sourcePos =
                        gear.isSource()
                                ? gear.getPosition()
                                : gear.getSourcePos();

                if (sourcePos != null
                        && overloadedSources.contains(sourcePos)) {
                    gear.setOverloaded(true);

                    // Keep the raw RPM relationship intact for load/ratio
                    // evaluation. Effective speed becomes zero while stalled.
                }
            }
        }

        for (MechanicalLoad load : loads.values()) {
            if (!load.active || load.sourcePos == null) {
                load.supplied = false;
                continue;
            }

            GearNode input = gears.get(load.inputPos);
            load.supplied =
                    input != null
                            && input.getSpeed() > STOP_EPSILON
                            && !input.isOverloaded()
                            && load.localTorqueDemand
                            <= input.getTorque()
                            + TORQUE_EPSILON
                            && !overloadedSources.contains(
                                    load.sourcePos
                            );
        }
    }

    /**
     * Finds the nearest engaged overload disconnect to the overloaded consumer
     * on a real Gear V2 path from the source. The path is reconstructed backwards
     * from the consumer so the first protector found isolates the smallest branch.
     */
    private boolean tripProtectiveDisconnectOnPath(
            Level level,
            Map<BlockPos, GearNode> gears,
            BlockPos sourcePos,
            BlockPos inputPos) {
        if (sourcePos == null
                || inputPos == null
                || sourcePos.equals(inputPos)) {
            return false;
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(sourcePos);
        visited.add(sourcePos);

        boolean found = false;

        while (!queue.isEmpty() && !found) {
            BlockPos current = queue.poll();

            for (GearConnection connection :
                    getConnectedGears(current, level)) {
                BlockPos neighbor =
                        connection.neighborPos();

                if (!gears.containsKey(neighbor)
                        || !visited.add(neighbor)) {
                    continue;
                }

                parent.put(neighbor, current);

                if (neighbor.equals(inputPos)) {
                    found = true;
                    break;
                }

                queue.add(neighbor);
            }
        }

        if (!found) {
            return false;
        }

        BlockPos cursor = inputPos;

        while (cursor != null
                && !cursor.equals(sourcePos)) {
            BlockEntity blockEntity =
                    level.getBlockEntity(cursor);

            if (blockEntity
                    instanceof OverloadDisconnectBlockEntity_wood disconnect
                    && disconnect.tripFromOverload()) {
                // The protector opened during network evaluation. Do not recurse
                // into updateNetwork(); invalidate the topology so the next tick
                // rebuilds around the isolated downstream branch.
                markTopologyDirty(level);
                return true;
            }

            cursor = parent.get(cursor);
        }

        return false;
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

    /**
     * Re-lock every actively driven gear to the exact angular relationship of its source.
     *
     * RPM ratios alone are not enough: letting every node integrate its own angle causes
     * floating-point and client interpolation drift over time. For an external mesh the
     * contact constraint is:
     *
     *   currentTeeth * currentVisualAngle
     *       + neighborTeeth * neighborVisualAngle = 180 degrees (mod 360)
     *
     * Shafts remain 1:1 and therefore copy the same rotation/phase directly.
     */
    private void lockDrivenGearRotations(Level level, Map<BlockPos, GearNode> gears) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<GearNode> queue = new ArrayDeque<>();

        for (GearNode gear : gears.values()) {
            if (!gear.isSource() || gear.getSpeed() <= STOP_EPSILON) {
                continue;
            }

            visited.add(gear.getPosition());
            queue.add(gear);
        }

        while (!queue.isEmpty()) {
            GearNode current = queue.poll();

            for (GearConnection connection : getConnectedGears(current.getPosition(), level)) {
                GearNode neighbor = gears.get(connection.neighborPos());
                if (neighbor == null || visited.contains(neighbor.getPosition())) {
                    continue;
                }

                BlockPos expectedSource = current.isSource()
                        ? current.getPosition()
                        : current.getSourcePos();
                if (expectedSource == null || !expectedSource.equals(neighbor.getSourcePos())) {
                    continue;
                }

                if (connection.kind() == ConnectionKind.SHAFT) {
                    neighbor.setMeshPhaseDegrees(current.getMeshPhaseDegrees());
                    neighbor.setRotationDegrees(current.getRotationDegrees());
                } else if (connection.kind() == ConnectionKind.RIGHT_ANGLE) {
                    int turnSign = getRightAngleDirectionSign(
                            level,
                            current.getPosition(),
                            neighbor.getPosition()
                    );
                    neighbor.setMeshPhaseDegrees(0.0F);
                    neighbor.setRotationDegrees(
                            current.getRotationDegrees() * turnSign
                    );
                } else if (connection.kind() == ConnectionKind.CUSTOM_GEARBOX) {
                    int customSign = getCustomGearboxDirectionSign(
                            level,
                            current.getPosition(),
                            neighbor.getPosition()
                    );
                    neighbor.setMeshPhaseDegrees(0.0F);
                    neighbor.setRotationDegrees(
                            current.getRotationDegrees() * customSign
                    );
                } else if (connection.kind() == ConnectionKind.BELT) {
                    float ratio = (float) current.getTeeth() / (float) Math.max(1, neighbor.getTeeth());
                    neighbor.setMeshPhaseDegrees(0.0F);
                    neighbor.setRotationDegrees(current.getRotationDegrees() * ratio);
                } else {
                    float neighborPhase = calculateMeshedPhaseDegrees(current, neighbor);
                    neighbor.setMeshPhaseDegrees(neighborPhase);

                    float currentVisualAngle =
                            current.getRotationDegrees() + current.getMeshPhaseDegrees();
                    float neighborVisualAngle =
                            (180.0F - current.getTeeth() * currentVisualAngle)
                                    / Math.max(1, neighbor.getTeeth());

                    neighbor.setRotationDegrees(neighborVisualAngle - neighborPhase);
                }

                visited.add(neighbor.getPosition());
                queue.add(neighbor);
            }
        }
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

                    if (connection.kind() == ConnectionKind.SHAFT) {
                        neighbor.setMeshPhaseDegrees(current.getMeshPhaseDegrees());
                        neighbor.setRotationDegrees(current.getRotationDegrees());
                        neighbor.setDirectionMultiplier(current.getDirectionMultiplier());
                    } else if (connection.kind() == ConnectionKind.RIGHT_ANGLE) {
                        int turnSign = getRightAngleDirectionSign(
                                level,
                                current.getPosition(),
                                neighbor.getPosition()
                        );
                        neighbor.setMeshPhaseDegrees(0.0F);
                        neighbor.setRotationDegrees(
                                current.getRotationDegrees() * turnSign
                        );
                        neighbor.setDirectionMultiplier(
                                current.getDirectionMultiplier() * turnSign
                        );
                    } else if (connection.kind() == ConnectionKind.CUSTOM_GEARBOX) {
                        int customSign = getCustomGearboxDirectionSign(
                                level,
                                current.getPosition(),
                                neighbor.getPosition()
                        );
                        neighbor.setMeshPhaseDegrees(0.0F);
                        neighbor.setRotationDegrees(
                                current.getRotationDegrees() * customSign
                        );
                        neighbor.setDirectionMultiplier(
                                current.getDirectionMultiplier() * customSign
                        );
                    } else if (connection.kind() == ConnectionKind.BELT) {
                        float ratio = (float) current.getTeeth() / (float) Math.max(1, neighbor.getTeeth());
                        neighbor.setMeshPhaseDegrees(0.0F);
                        neighbor.setRotationDegrees(current.getRotationDegrees() * ratio);
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

    private boolean removeMissingBlockEntities(Level level, Map<BlockPos, GearNode> gears) {
        int before = gears.size();
        gears.entrySet().removeIf(entry ->
                !(level.getBlockEntity(entry.getKey()) instanceof GearBlockEntity));
        return gears.size() != before;
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
        // External gear meshing is a tooth-phase constraint, not a cumulative angle offset.
        //
        // Treat one full tooth pitch as 360 degrees of tooth phase. At the contact point,
        // a tooth on one gear must meet a gap on the other, so the two tooth phases must
        // add up to 180 degrees:
        //
        //   currentTeeth * currentPhase + neighborTeeth * neighborPhase = 180 (mod 360)
        //
        // Solving for the neighbor gives a stable phase that does not accumulate through
        // a gear train. For an 8-tooth chain this produces:
        //
        //   0.0 -> 22.5 -> 0.0 -> 22.5 ...
        //
        // instead of the old incorrect:
        //
        //   0.0 -> 22.5 -> 45.0 -> 67.5 ...
        int currentTeeth = Math.max(1, current.getTeeth());
        int neighborTeeth = Math.max(1, neighbor.getTeeth());

        float currentToothPhase = currentTeeth * current.getMeshPhaseDegrees();
        float neighborPhase = (180.0F - currentToothPhase) / neighborTeeth;

        // Gear models are rotationally identical after one tooth pitch, so normalize to
        // that smaller period rather than a full 360 degrees.
        float toothPitch = 360.0F / neighborTeeth;
        float normalized = neighborPhase % toothPitch;
        if (normalized < 0.0F) {
            normalized += toothPitch;
        }
        return normalized;
    }

    private float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0F;
        if (normalized < 0.0F) {
            normalized += 360.0F;
        }
        return normalized;
    }

    private List<GearConnection> getConnectedGears(BlockPos pos, Level level) {
        Map<BlockPos, List<GearConnection>> cache = getConnectionCache(level);
        List<GearConnection> cached = cache.get(pos);
        if (cached != null) {
            return cached;
        }

        List<GearConnection> calculated = calculateConnectedGears(pos, level);
        List<GearConnection> immutable = List.copyOf(calculated);
        cache.put(pos.immutable(), immutable);
        return immutable;
    }

    private List<GearConnection> calculateConnectedGears(BlockPos pos, Level level) {
        List<GearConnection> connected = new ArrayList<>();
        Map<BlockPos, GearNode> gears = getGearMap(level);
        GearNode gear = gears.get(pos);
        if (gear == null) {
            return connected;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        // The wooden gearbox is a hybrid two-port node. Its GearNode reference axis is
        // the INPUT port; OUTPUT is a 1:1 right-angle edge into a perpendicular shaft axis.
        if (blockEntity instanceof GearboxBlockEntity_wood gearbox) {
            addGearboxPortConnection(
                    connected,
                    gears,
                    level,
                    pos,
                    gearbox.getInputDirection(),
                    ConnectionKind.SHAFT
            );
            addGearboxPortConnection(
                    connected,
                    gears,
                    level,
                    pos,
                    gearbox.getOutputDirection(),
                    ConnectionKind.RIGHT_ANGLE
            );
            return connected;
        }

        if (blockEntity instanceof CustomGearboxBlockEntity_wood customGearbox) {
            for (Direction port : customGearbox.getActivePorts()) {
                addCustomGearboxPortConnection(
                        connected,
                        gears,
                        level,
                        pos,
                        port
                );
            }
            return connected;
        }

        // An open inline disconnect is an actual topology break, not merely a
        // shaft whose displayed RPM has been forced to zero.
        if (isOpenInlineDisconnect(blockEntity)) {
            return connected;
        }

        // Shaft/axle style transmission is only along the spin axis and remains adjacent.
        // When the adjacent node is a gearbox, only its explicitly exposed face may connect.
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != gear.getAxis()) {
                continue;
            }

            BlockPos neighborPos = pos.relative(direction);
            GearNode neighbor = gears.get(neighborPos);
            if (neighbor == null) {
                continue;
            }

            BlockEntity neighborBlockEntity = level.getBlockEntity(neighborPos);

            // A shaft beside an open inline disconnect must not connect into it
            // from the neighbor side either.
            if (isOpenInlineDisconnect(neighborBlockEntity)) {
                continue;
            }

            if (neighborBlockEntity instanceof CustomGearboxBlockEntity_wood customGearbox) {
                Direction customPort = direction.getOpposite();
                if (!customGearbox.isPortActive(customPort)) {
                    continue;
                }

                connected.add(new GearConnection(
                        neighborPos,
                        ConnectionKind.CUSTOM_GEARBOX
                ));
                continue;
            }

            if (neighborBlockEntity instanceof GearboxBlockEntity_wood gearbox) {
                Direction gearboxPort = direction.getOpposite();
                if (!gearbox.acceptsPort(gearboxPort)) {
                    continue;
                }

                connected.add(new GearConnection(
                        neighborPos,
                        gearbox.isPrimaryPort(gearboxPort)
                                ? ConnectionKind.SHAFT
                                : ConnectionKind.RIGHT_ANGLE
                ));
                continue;
            }

            if (neighbor.getAxis() == gear.getAxis()) {
                connected.add(new GearConnection(
                        neighborPos,
                        ConnectionKind.SHAFT
                ));
            }
        }

        // Pulley-to-pulley belts are remote mechanical connections. They are checked
        // before the shaft-like early return because pulleys deliberately behave as shafts
        // locally while still exposing one remote belt edge.
        if (blockEntity instanceof PulleyBlockEntity_wood pulley) {
            BlockPos partnerPos = pulley.getBeltPartner();
            if (partnerPos != null
                    && level.getBlockEntity(partnerPos) instanceof PulleyBlockEntity_wood partner
                    && partner.isLinkedTo(pos)
                    && partner.getGearAxis() == gear.getAxis()) {
                GearNode partnerNode = gears.get(partnerPos);
                if (partnerNode != null) {
                    connected.add(new GearConnection(partnerPos, ConnectionKind.BELT));
                }
            }
        }

        // Wide item belts are also remote open-belt mechanical connections. The two
        // rollers therefore stay phase/speed locked while the continuous span between
        // them carries items.
        if (blockEntity instanceof ConveyorRollerBlockEntity roller) {
            BlockPos partnerPos = roller.getItemBeltPartner();
            if (partnerPos != null
                    && level.getBlockEntity(partnerPos) instanceof ConveyorRollerBlockEntity partner
                    && partner.isItemBeltLinkedTo(pos)
                    && partner.getGearAxis() == gear.getAxis()) {
                GearNode partnerNode = gears.get(partnerPos);
                if (partnerNode != null) {
                    connected.add(new GearConnection(partnerPos, ConnectionKind.BELT));
                }
            }
        }

        // External gear/rim meshing. Ordinary shaft-like nodes stop here, but hybrid
        // components such as Water Wheels expose both an axial shaft port and a visible
        // rim that can drive gears directly.
        GearBlockEntity currentBlockEntity =
                blockEntity instanceof GearBlockEntity gearBlockEntity
                        ? gearBlockEntity
                        : null;
        if (currentBlockEntity == null || !currentBlockEntity.supportsExternalGearMesh()) {
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
            if (neighbor == null || neighbor.getAxis() != gear.getAxis()) {
                continue;
            }

            BlockEntity neighborBlockEntity = level.getBlockEntity(scanPos);
            if (!(neighborBlockEntity instanceof GearBlockEntity neighborGearBlockEntity)
                    || !neighborGearBlockEntity.supportsExternalGearMesh()) {
                continue;
            }

            if (!GearPlacementValidator.isOffsetInGearPlane(pos, scanPos, gear.getAxis())) {
                continue;
            }

            boolean validMesh = isValidExternalMeshBetween(
                    pos,
                    currentBlockEntity,
                    scanPos,
                    neighborGearBlockEntity,
                    gear.getAxis(),
                    gear.getTeeth(),
                    neighbor.getTeeth()
            );

            if (validMesh
                    && hasRequiredExternalMeshClearanceBetween(
                    level,
                    pos,
                    currentBlockEntity,
                    scanPos,
                    neighborGearBlockEntity,
                    gear.getAxis(),
                    gear.getTeeth(),
                    neighbor.getTeeth())) {
                connected.add(new GearConnection(scanPos, ConnectionKind.GEAR_MESH));
            }
        }

        return connected;
    }

    private void addCustomGearboxPortConnection(List<GearConnection> connected,
                                                    Map<BlockPos, GearNode> gears,
                                                    Level level,
                                                    BlockPos gearboxPos,
                                                    Direction portDirection) {
        BlockPos neighborPos = gearboxPos.relative(portDirection);
        GearNode neighbor = gears.get(neighborPos);
        if (neighbor == null
                || neighbor.getAxis() != portDirection.getAxis()
                || isOpenInlineDisconnect(level, neighborPos)) {
            return;
        }

        connected.add(new GearConnection(
                neighborPos,
                ConnectionKind.CUSTOM_GEARBOX
        ));
    }

    private int getCustomGearboxDirectionSign(Level level,
                                              BlockPos firstPos,
                                              BlockPos secondPos) {
        BlockEntity first = level.getBlockEntity(firstPos);
        if (first instanceof CustomGearboxBlockEntity_wood gearbox) {
            Direction port = directionBetween(firstPos, secondPos);
            int sign = port == null ? 0 : gearbox.getPortDirectionSign(port);
            return sign == 0 ? 1 : sign;
        }

        BlockEntity second = level.getBlockEntity(secondPos);
        if (second instanceof CustomGearboxBlockEntity_wood gearbox) {
            Direction port = directionBetween(secondPos, firstPos);
            int sign = port == null ? 0 : gearbox.getPortDirectionSign(port);
            return sign == 0 ? 1 : sign;
        }

        return 1;
    }

    @Nullable
    private Direction directionBetween(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();

        if (dx == 1 && dy == 0 && dz == 0) {
            return Direction.EAST;
        }
        if (dx == -1 && dy == 0 && dz == 0) {
            return Direction.WEST;
        }
        if (dy == 1 && dx == 0 && dz == 0) {
            return Direction.UP;
        }
        if (dy == -1 && dx == 0 && dz == 0) {
            return Direction.DOWN;
        }
        if (dz == 1 && dx == 0 && dy == 0) {
            return Direction.SOUTH;
        }
        if (dz == -1 && dx == 0 && dy == 0) {
            return Direction.NORTH;
        }

        return null;
    }

    private void addGearboxPortConnection(List<GearConnection> connected,
                                              Map<BlockPos, GearNode> gears,
                                              Level level,
                                              BlockPos gearboxPos,
                                              Direction portDirection,
                                              ConnectionKind kind) {
        BlockPos neighborPos = gearboxPos.relative(portDirection);
        GearNode neighbor = gears.get(neighborPos);
        if (neighbor == null
                || neighbor.getAxis() != portDirection.getAxis()
                || isOpenInlineDisconnect(level, neighborPos)) {
            return;
        }

        connected.add(new GearConnection(neighborPos, kind));
    }

    private boolean isOpenInlineDisconnect(Level level,
                                           BlockPos pos) {
        return isOpenInlineDisconnect(
                level.getBlockEntity(pos)
        );
    }

    private boolean isOpenInlineDisconnect(
            BlockEntity blockEntity) {
        if (blockEntity
                instanceof ClutchBlockEntity_wood clutch) {
            return !clutch.isEngaged();
        }

        if (blockEntity
                instanceof OverloadDisconnectBlockEntity_wood disconnect) {
            return !disconnect.isEngaged();
        }

        return false;
    }

    private int getRightAngleDirectionSign(Level level,
                                           BlockPos firstPos,
                                           BlockPos secondPos) {
        BlockEntity first = level.getBlockEntity(firstPos);
        if (first instanceof GearboxBlockEntity_wood gearbox) {
            return gearbox.getRightAngleDirectionSign();
        }

        BlockEntity second = level.getBlockEntity(secondPos);
        if (second instanceof GearboxBlockEntity_wood gearbox) {
            return gearbox.getRightAngleDirectionSign();
        }

        return -1;
    }

    private boolean hasRequiredExternalMeshClearanceBetween(Level level,
                                                              BlockPos firstPos,
                                                              GearBlockEntity first,
                                                              BlockPos secondPos,
                                                              GearBlockEntity second,
                                                              Direction.Axis axis,
                                                              int firstTeeth,
                                                              int secondTeeth) {
        boolean largeWaterWheel =
                (first instanceof WaterWheelBlockEntity firstWheel && firstWheel.isLarge())
                        || (second instanceof WaterWheelBlockEntity secondWheel && secondWheel.isLarge());

        if (largeWaterWheel) {
            // The special distance-2 rim connection is already outside the wheel's 3x3
            // occupied cells. Generic mixed medium/large corner-clearance rules describe
            // two ordinary gears and do not apply to the water-wheel rim.
            return true;
        }

        return GearPlacementValidator.hasRequiredExternalMeshClearance(
                level,
                firstPos,
                secondPos,
                axis,
                firstTeeth,
                secondTeeth
        );
    }

    private boolean isValidExternalMeshBetween(BlockPos firstPos,
                                               GearBlockEntity first,
                                               BlockPos secondPos,
                                               GearBlockEntity second,
                                               Direction.Axis axis,
                                               int firstTeeth,
                                               int secondTeeth) {
        boolean firstWaterWheel = first instanceof WaterWheelBlockEntity;
        boolean secondWaterWheel = second instanceof WaterWheelBlockEntity;

        if (!firstWaterWheel && !secondWaterWheel) {
            return GearPlacementValidator.isValidExternalMeshOffset(
                    firstPos,
                    secondPos,
                    axis,
                    firstTeeth,
                    secondTeeth
            );
        }

        // Water wheels may drive ordinary gears directly, but two water wheels are
        // not treated as a tooth mesh.
        if (firstWaterWheel && secondWaterWheel) {
            return false;
        }

        WaterWheelBlockEntity wheel = firstWaterWheel
                ? (WaterWheelBlockEntity) first
                : (WaterWheelBlockEntity) second;
        BlockPos wheelPos = firstWaterWheel ? firstPos : secondPos;
        BlockPos gearPos = firstWaterWheel ? secondPos : firstPos;
        int otherTeeth = firstWaterWheel ? secondTeeth : firstTeeth;

        int[] offset = getPlanarOffsetComponents(wheelPos, gearPos, axis);
        int a = offset[0];
        int b = offset[1];

        if (wheel.isLarge()) {
            if (otherTeeth <= 8) {
                // 24-tooth water-wheel rim (~1.5 block radius) + 8-tooth gear
                // (~0.5 block radius) lands cleanly at a cardinal distance of 2.
                return (a == 2 && b == 0) || (a == 0 && b == 2);
            }

            // A 16-tooth large gear needs a little more center distance from the 3x3
            // wheel. The 2-by-1 grid offset is sqrt(5) ~= 2.24 blocks, which is a much
            // better visual contact than overlapping it at straight distance 2.
            return (a == 2 && b == 1) || (a == 1 && b == 2);
        }

        // The 1x1 wheel uses the same pitch relationships as the corresponding wooden
        // 8-tooth node.
        return GearPlacementValidator.isValidExternalMeshOffset(
                firstPos,
                secondPos,
                axis,
                firstTeeth,
                secondTeeth
        );
    }

    private int[] getPlanarOffsetComponents(BlockPos first,
                                            BlockPos second,
                                            Direction.Axis axis) {
        int dx = Math.abs(second.getX() - first.getX());
        int dy = Math.abs(second.getY() - first.getY());
        int dz = Math.abs(second.getZ() - first.getZ());

        if (axis == Direction.Axis.X) {
            return new int[]{dy, dz};
        }
        if (axis == Direction.Axis.Y) {
            return new int[]{dx, dz};
        }
        return new int[]{dx, dy};
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

    private void markTopologyDirty(Level level) {
        if (level == null) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        topologyDirtyLevels.add(dimension);
        Map<BlockPos, List<GearConnection>> cache = connectionsByLevel.get(dimension);
        if (cache != null) {
            cache.clear();
        }
    }

    private Map<BlockPos, List<GearConnection>> getConnectionCache(Level level) {
        return connectionsByLevel.computeIfAbsent(
                level.dimension(),
                ignored -> new HashMap<>()
        );
    }

    private Map<BlockPos, GearNode> getGearMap(Level level) {
        return gearsByLevel.computeIfAbsent(level.dimension(), dimension -> new HashMap<>());
    }

    private Map<BlockPos, MechanicalLoad> getLoadMap(Level level) {
        return loadsByLevel.computeIfAbsent(level.dimension(), dimension -> new HashMap<>());
    }

    private void syncAll(Level level, Iterable<GearNode> gears) {
        ResourceKey<Level> dimension = level.dimension();
        long gameTime = level.getGameTime();

        // Client animation now integrates RPM continuously and only needs occasional
        // authoritative phase correction. Sending the whole graph at 20 Hz wastes both
        // server time and bandwidth; 5 Hz keeps visual drift corrected without packet spam.
        Long lastSyncTick = lastSyncTickByLevel.get(dimension);
        if (lastSyncTick != null
                && gameTime - lastSyncTick < VISUAL_SYNC_INTERVAL_TICKS) {
            return;
        }
        lastSyncTickByLevel.put(dimension, gameTime);

        for (GearNode gear : gears) {
            float transmittedSpeed = gear.getEffectiveSpeed();
            CHANNEL.send(PacketDistributor.ALL.noArg(), new GearSyncPacket(
                    gear.getPosition(),
                    transmittedSpeed,
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

    public record MechanicalLoadState(boolean active,
                                      boolean supplied,
                                      float localTorqueDemand,
                                      float sourceEquivalentDemand,
                                      float totalSourceDemand,
                                      float sourceTorqueCapacity,
                                      BlockPos sourcePos) {
        public static MechanicalLoadState inactive() {
            return new MechanicalLoadState(false, false, 0.0F, 0.0F, 0.0F, 0.0F, null);
        }
    }

    private static final class MechanicalLoad {
        private final BlockPos loadPos;
        private BlockPos inputPos;
        private float localTorqueDemand;
        private boolean active;

        private boolean supplied;
        private float sourceEquivalentDemand;
        private float totalSourceDemand;
        private float sourceTorqueCapacity;
        private BlockPos sourcePos;

        private MechanicalLoad(BlockPos loadPos, BlockPos inputPos) {
            this.loadPos = loadPos;
            this.inputPos = inputPos;
        }

        private void resetEvaluation() {
            supplied = false;
            sourceEquivalentDemand = 0.0F;
            totalSourceDemand = 0.0F;
            sourceTorqueCapacity = 0.0F;
            sourcePos = null;
        }

        private MechanicalLoadState snapshot() {
            return new MechanicalLoadState(
                    active,
                    supplied,
                    localTorqueDemand,
                    sourceEquivalentDemand,
                    totalSourceDemand,
                    sourceTorqueCapacity,
                    sourcePos
            );
        }
    }

    private enum ConnectionKind {
        SHAFT,
        RIGHT_ANGLE,
        CUSTOM_GEARBOX,
        GEAR_MESH,
        BELT
    }

    private record GearConnection(BlockPos neighborPos, ConnectionKind kind) {
    }
}
