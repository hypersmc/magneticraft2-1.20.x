package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalBellowsBlock;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressure;
import com.magneticraft2.common.systems.PRESSURE.IPressureStorage;
import com.magneticraft2.common.utils.PressureStorages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Crank-driven air pump.
 *
 * The crank must sit directly behind the bellows and its rod output must point
 * into this block. One compression stroke creates one air unit. Buffered air is
 * pushed through the copper nozzle into an adjacent Pressure-capability receiver,
 * which means it can automatically refill the Primitive Furnace's existing
 * Bellows module without changing the Stone Age multiblock.
 */
public class MechanicalBellowsBlockEntity
        extends BlockEntity {

    public static final int MAX_AIR = 5;
    public static final float TORQUE_DEMAND = 3.0F;
    private static final float PUMP_THRESHOLD = 0.82F;
    private static final float MOVING_EPSILON = 0.05F;

    private final PressureStorages pressureStorage;
    private final LazyOptional<IPressureStorage> pressure;

    private float previousStroke = -1.0F;

    public MechanicalBellowsBlockEntity(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .MECHANICAL_BELLOWS_BE
                        .get(),
                pos,
                state
        );

        pressureStorage =
                new PressureStorages(MAX_AIR, 1) {
                    @Override
                    protected void onPressureChanged() {
                        setChanged();
                    }
                };

        pressure = LazyOptional.of(
                () -> pressureStorage
        );
    }

    public static <E extends BlockEntity> void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            E blockEntity) {
        if (level.isClientSide
                || !(blockEntity
                instanceof MechanicalBellowsBlockEntity bellows)) {
            return;
        }

        bellows.tickPump();
    }

    private void tickPump() {
        CrankBlockEntity_wood crank =
                getConnectedCrank();

        // Always try to empty the local one-stroke buffer first.
        transferBufferedAir();

        boolean connected =
                crank != null;
        boolean moving =
                connected
                        && crank.getOrCreateGearNode()
                        .getEffectiveSpeed()
                        > MOVING_EPSILON;

        boolean canWork =
                moving
                        && pressureStorage
                        .getPressureStored()
                        < pressureStorage
                        .getMaxPressureStored();

        GearNetworkManager.getInstance()
                .setMechanicalLoad(
                        level,
                        worldPosition,
                        connected
                                ? crank.getBlockPos()
                                : worldPosition,
                        TORQUE_DEMAND,
                        canWork
                );

        setActiveState(moving);

        if (!moving) {
            previousStroke = -1.0F;
            return;
        }

        float stroke =
                crank.getServerStrokeProgress();

        if (previousStroke < 0.0F) {
            previousStroke = stroke;
            return;
        }

        boolean compressionPulse =
                stroke >= PUMP_THRESHOLD
                        && previousStroke
                        < PUMP_THRESHOLD;

        previousStroke = stroke;

        if (!compressionPulse
                || pressureStorage
                .getPressureStored()
                >= pressureStorage
                .getMaxPressureStored()) {
            return;
        }

        int received =
                pressureStorage.receivePressure(
                        1,
                        false
                );

        if (received > 0) {
            setChanged();
            transferBufferedAir();
        }
    }

    private void transferBufferedAir() {
        if (level == null
                || pressureStorage
                .getPressureStored() <= 0) {
            return;
        }

        Direction facing = getFacing();
        BlockEntity target =
                level.getBlockEntity(
                        worldPosition.relative(facing)
                );

        if (target == null) {
            return;
        }

        target.getCapability(
                        CapabilityPressure.PRESSURE,
                        facing.getOpposite()
                )
                .ifPresent(handler -> {
                    if (!handler.canReceive()) {
                        return;
                    }

                    int available =
                            pressureStorage
                                    .getPressureStored();

                    int accepted =
                            handler.receivePressure(
                                    Math.min(1, available),
                                    false
                            );

                    if (accepted > 0) {
                        pressureStorage
                                .extractPressure(
                                        accepted,
                                        false
                                );
                        setChanged();
                    }
                });
    }

    public boolean hasValidCrank() {
        return getConnectedCrank() != null;
    }

    @Nullable
    public CrankBlockEntity_wood getConnectedCrank() {
        if (level == null) {
            return null;
        }

        Direction facing = getFacing();
        BlockPos crankPos =
                worldPosition.relative(
                        facing.getOpposite()
                );

        BlockEntity blockEntity =
                level.getBlockEntity(crankPos);

        if (blockEntity
                instanceof CrankBlockEntity_wood crank
                && crank.getRodOutputPos()
                .equals(worldPosition)) {
            return crank;
        }

        return null;
    }

    public float getCompressionProgress(
            float partialTicks) {
        CrankBlockEntity_wood crank =
                getConnectedCrank();

        return crank == null
                ? 0.0F
                : crank.getStrokeProgress(
                        partialTicks
                );
    }

    public Direction getFacing() {
        BlockState state = getBlockState();

        return state.hasProperty(
                MechanicalBellowsBlock.FACING
        )
                ? state.getValue(
                        MechanicalBellowsBlock.FACING
                )
                : Direction.NORTH;
    }

    public int getStoredAir() {
        return pressureStorage
                .getPressureStored();
    }

    public int getMaxStoredAir() {
        return pressureStorage
                .getMaxPressureStored();
    }

    private void setActiveState(boolean active) {
        if (level == null) {
            return;
        }

        BlockState state =
                level.getBlockState(
                        worldPosition
                );

        if (state.hasProperty(
                MechanicalBellowsBlock.ACTIVE
        ) && state.getValue(
                MechanicalBellowsBlock.ACTIVE
        ) != active) {
            level.setBlock(
                    worldPosition,
                    state.setValue(
                            MechanicalBellowsBlock.ACTIVE,
                            active
                    ),
                    Block.UPDATE_CLIENTS
            );
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T>
    getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side) {
        if (cap
                == CapabilityPressure.PRESSURE) {
            return pressure.cast();
        }

        return super.getCapability(
                cap,
                side
        );
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(
                "Pressure",
                pressureStorage.serializeNBT()
        );
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("Pressure")) {
            pressureStorage.deserializeNBT(
                    tag.getCompound("Pressure")
            );
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        pressure.invalidate();
    }
}
