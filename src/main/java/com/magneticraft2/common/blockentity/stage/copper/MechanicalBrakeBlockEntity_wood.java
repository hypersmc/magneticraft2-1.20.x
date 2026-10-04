package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalBrakeBlock_wood;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A hard mechanical brake load. It remains part of the shaft network while
 * active; the load system decides whether the source can carry that braking
 * demand or whether the network stalls / an upstream overload disconnect trips.
 */
public class MechanicalBrakeBlockEntity_wood
        extends GearBlockEntity {

    public static final float BRAKE_TORQUE_DEMAND = 32.0F;

    private boolean manualBrake = false;

    public MechanicalBrakeBlockEntity_wood(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .MECHANICAL_BRAKE_BE_WOOD
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
                instanceof MechanicalBrakeBlockEntity_wood brake)) {
            return;
        }

        brake.refreshBrakeState();
        brake.serverTickGear();
        brake.markHasEverRotatedIfMoving(
                brake.getServerSpeed()
        );
    }

    public boolean toggleManualBrake() {
        manualBrake = !manualBrake;
        setChanged();
        refreshBrakeState();
        return manualBrake;
    }

    public boolean isManuallyBraking() {
        return manualBrake;
    }

    public boolean isBraking() {
        BlockState state = getBlockState();
        return state.hasProperty(
                MechanicalBrakeBlock_wood.BRAKING
        ) && state.getValue(
                MechanicalBrakeBlock_wood.BRAKING
        );
    }

    public void refreshBrakeState() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean effective =
                manualBrake
                        || level.hasNeighborSignal(
                                worldPosition
                        );

        BlockState state =
                level.getBlockState(worldPosition);

        if (state.hasProperty(
                MechanicalBrakeBlock_wood.BRAKING
        ) && state.getValue(
                MechanicalBrakeBlock_wood.BRAKING
        ) != effective) {
            level.setBlock(
                    worldPosition,
                    state.setValue(
                            MechanicalBrakeBlock_wood.BRAKING,
                            effective
                    ),
                    2
            );
            setChanged();
        }

        GearNetworkManager.getInstance()
                .setMechanicalLoad(
                        level,
                        worldPosition,
                        worldPosition,
                        BRAKE_TORQUE_DEMAND,
                        effective
                );
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
        BlockState state = getBlockState();
        return state.hasProperty(DirectionalBlock.FACING)
                ? state.getValue(
                        DirectionalBlock.FACING
                ).getAxis()
                : Direction.Axis.X;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(
                "ManualBrake",
                manualBrake
        );
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        manualBrake = tag.getBoolean(
                "ManualBrake"
        );
    }
}
