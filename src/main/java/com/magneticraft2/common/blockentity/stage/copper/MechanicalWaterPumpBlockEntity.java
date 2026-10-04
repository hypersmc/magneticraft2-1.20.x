package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalWaterPumpBlock;
import com.magneticraft2.common.block.stage.copper.WaterPipeBlock;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MechanicalWaterPumpBlockEntity
        extends GearBlockEntity {

    public static final float MIN_SPEED = 15.0F;
    public static final float TORQUE_DEMAND = 2.0F;
    public static final int MAX_FLOW_PER_TICK = 200;
    public static final int MAX_PIPE_BLOCKS = 256;

    public MechanicalWaterPumpBlockEntity(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .MECHANICAL_WATER_PUMP_BE
                        .get(),
                pos,
                state
        );
    }

    public static <E extends BlockEntity> void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            E blockEntity) {
        if (level.isClientSide
                || !(blockEntity
                instanceof MechanicalWaterPumpBlockEntity pump)) {
            return;
        }

        pump.serverTickGear();
        pump.tickPump();
    }

    private void tickPump() {
        if (level == null) {
            return;
        }

        GearNode node =
                getOrCreateGearNode();

        float speed =
                Math.abs(
                        node.getEffectiveSpeed()
                );

        boolean sourceWater =
                hasSourceWater();

        List<IFluidHandler> targets =
                sourceWater
                        ? findTargets()
                        : List.of();

        boolean hasDestination =
                targets.stream()
                        .anyMatch(
                                this::canAcceptWater
                        );

        boolean shouldLoad =
                sourceWater
                        && hasDestination;

        GearNetworkManager.getInstance()
                .setMechanicalLoad(
                        level,
                        worldPosition,
                        worldPosition,
                        TORQUE_DEMAND,
                        shouldLoad
                );

        GearNetworkManager.MechanicalLoadState load =
                GearNetworkManager.getInstance()
                        .getMechanicalLoadState(
                                level,
                                worldPosition
                        );

        boolean canPump =
                shouldLoad
                        && load.supplied()
                        && speed >= MIN_SPEED
                        && !node.isOverloaded();

        int moved =
                canPump
                        ? pushWater(
                                targets,
                                flowFor(speed)
                        )
                        : 0;

        setActiveState(moved > 0);
    }

    private boolean hasSourceWater() {
        if (level == null) {
            return false;
        }

        var fluid =
                level.getFluidState(
                        worldPosition.below()
                );

        return fluid.isSource()
                && fluid.is(FluidTags.WATER);
    }

    private int flowFor(float speed) {
        return Math.min(
                MAX_FLOW_PER_TICK,
                Math.max(
                        1,
                        (int) Math.floor(
                                speed * 2.0F
                        )
                )
        );
    }

    private boolean canAcceptWater(
            IFluidHandler handler) {
        return handler.fill(
                new FluidStack(
                        Fluids.WATER,
                        1
                ),
                IFluidHandler.FluidAction.SIMULATE
        ) > 0;
    }

    private int pushWater(
            List<IFluidHandler> targets,
            int amount) {
        int remaining = amount;

        for (IFluidHandler target : targets) {
            if (remaining <= 0) {
                break;
            }

            int accepted =
                    target.fill(
                            new FluidStack(
                                    Fluids.WATER,
                                    remaining
                            ),
                            IFluidHandler.FluidAction.EXECUTE
                    );

            remaining -=
                    Math.max(
                            0,
                            accepted
                    );
        }

        return amount - remaining;
    }

    private List<IFluidHandler> findTargets() {
        if (level == null) {
            return List.of();
        }

        List<IFluidHandler> result =
                new ArrayList<>();
        Set<BlockPos> targetPositions =
                new HashSet<>();

        BlockPos outputPos =
                worldPosition.above();

        BlockState outputState =
                level.getBlockState(
                        outputPos
                );

        if (outputState.is(
                BlockRegistry.WATER_PIPE.get()
        )) {
            collectPipeTargets(
                    outputPos,
                    result,
                    targetPositions
            );
        } else {
            addFluidTarget(
                    outputPos,
                    Direction.DOWN,
                    result,
                    targetPositions
            );
        }

        return result;
    }

    private void collectPipeTargets(
            BlockPos firstPipe,
            List<IFluidHandler> targets,
            Set<BlockPos> targetPositions) {
        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();
        Set<BlockPos> visited =
                new HashSet<>();

        queue.add(
                firstPipe.immutable()
        );

        while (!queue.isEmpty()
                && visited.size()
                < MAX_PIPE_BLOCKS) {
            BlockPos pipePos =
                    queue.removeFirst();

            if (!visited.add(pipePos)) {
                continue;
            }

            for (Direction direction :
                    Direction.values()) {
                BlockPos neighbour =
                        pipePos.relative(direction);

                if (neighbour.equals(worldPosition)) {
                    continue;
                }

                BlockState neighbourState =
                        level.getBlockState(
                                neighbour
                        );

                if (neighbourState.is(
                        BlockRegistry.WATER_PIPE.get()
                )) {
                    if (!visited.contains(neighbour)) {
                        queue.addLast(
                                neighbour.immutable()
                        );
                    }
                    continue;
                }

                addFluidTarget(
                        neighbour,
                        direction.getOpposite(),
                        targets,
                        targetPositions
                );
            }
        }
    }

    private void addFluidTarget(
            BlockPos pos,
            Direction side,
            List<IFluidHandler> targets,
            Set<BlockPos> targetPositions) {
        if (!targetPositions.add(
                pos.immutable()
        )) {
            return;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (blockEntity == null
                || blockEntity == this) {
            return;
        }

        blockEntity
                .getCapability(
                        ForgeCapabilities.FLUID_HANDLER,
                        side
                )
                .resolve()
                .ifPresent(targets::add);
    }

    private void setActiveState(
            boolean active) {
        BlockState state =
                level.getBlockState(
                        worldPosition
                );

        if (state.hasProperty(
                MechanicalWaterPumpBlock.ACTIVE
        )
                && state.getValue(
                        MechanicalWaterPumpBlock.ACTIVE
                ) != active) {
            level.setBlock(
                    worldPosition,
                    state.setValue(
                            MechanicalWaterPumpBlock.ACTIVE,
                            active
                    ),
                    Block.UPDATE_CLIENTS
            );
        }
    }

    @Override
    public int getGearTeeth() {
        return 1;
    }

    @Override
    public float getGearMaxTorque() {
        return 16.0F;
    }

    @Override
    public boolean isShaftLike() {
        return true;
    }

    @Override
    public Direction.Axis getGearAxis() {
        BlockState state =
                getBlockState();

        return state.hasProperty(
                DirectionalBlock.FACING
        )
                ? state.getValue(
                        DirectionalBlock.FACING
                ).getAxis()
                : Direction.Axis.X;
    }
}
