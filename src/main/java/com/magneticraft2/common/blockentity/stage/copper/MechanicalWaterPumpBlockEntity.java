package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalWaterPumpBlock;
import com.magneticraft2.common.block.stage.copper.WaterPipeBlock;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.fluid.FluidPipeNetwork;
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

import java.util.ArrayList;
import java.util.List;

public class MechanicalWaterPumpBlockEntity
        extends GearBlockEntity {

    public static final float MIN_SPEED = 15.0F;
    public static final float TORQUE_DEMAND = 2.0F;
    public static final int MAX_FLOW_PER_TICK = 200;
    public static final int MAX_PIPE_BLOCKS =
            FluidPipeNetwork.DEFAULT_MAX_PIPE_BLOCKS;

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
        BlockPos outputPos =
                worldPosition.above();

        if (level.getBlockState(outputPos).is(
                BlockRegistry.WATER_PIPE.get()
        )) {
            for (FluidPipeNetwork.Endpoint endpoint :
                    FluidPipeNetwork.findEndpoints(
                            level,
                            outputPos,
                            worldPosition,
                            MAX_PIPE_BLOCKS
                    )) {
                result.add(endpoint.handler());
            }

            return result;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(outputPos);

        if (blockEntity != null) {
            blockEntity
                    .getCapability(
                            ForgeCapabilities.FLUID_HANDLER,
                            Direction.DOWN
                    )
                    .resolve()
                    .ifPresent(result::add);
        }

        return result;
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

    public Direction getMechanicalInputSide() {
        BlockState state =
                getBlockState();

        return state.hasProperty(
                DirectionalBlock.FACING
        )
                ? state.getValue(
                        DirectionalBlock.FACING
                )
                : Direction.WEST;
    }

    @Override
    public boolean acceptsShaftConnection(
            Direction side) {
        return side == getMechanicalInputSide();
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
