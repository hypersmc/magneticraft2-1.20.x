package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.OverloadDisconnectBlock_wood;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Self-resetting overload disconnect.
 *
 * It is normally a 1:1 shaft node. GearNetworkManager trips it before applying
 * overload to the source, which cleanly separates the downstream branch while
 * the source keeps turning on the input side.
 */
public class OverloadDisconnectBlockEntity_wood extends GearBlockEntity {
    private static final int[] RESET_SECONDS =
            new int[]{1, 2, 5, 10, 30};

    private int resetOptionIndex = 2;
    private boolean tripped = false;
    private int resetTicksRemaining = 0;

    public OverloadDisconnectBlockEntity_wood(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry.OVERLOAD_DISCONNECT_BE_WOOD.get(),
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
                instanceof OverloadDisconnectBlockEntity_wood disconnect)) {
            return;
        }

        disconnect.tickResetTimer();
        disconnect.serverTickGear();
        disconnect.markHasEverRotatedIfMoving(
                disconnect.getServerSpeed()
        );
        disconnect.markNeighborMotionForRendering();
        disconnect.updateRotatingState();
    }

    public boolean isEngaged() {
        BlockState state = getBlockState();
        return state.hasProperty(
                OverloadDisconnectBlock_wood.ENGAGED
        ) && state.getValue(
                OverloadDisconnectBlock_wood.ENGAGED
        );
    }

    public boolean isTripped() {
        return tripped;
    }

    public int getResetSeconds() {
        return RESET_SECONDS[
                Math.max(
                        0,
                        Math.min(
                                RESET_SECONDS.length - 1,
                                resetOptionIndex
                        )
                )
        ];
    }

    public int getResetTicksRemaining() {
        return resetTicksRemaining;
    }

    public int cycleResetDelay(int direction) {
        int step = direction < 0 ? -1 : 1;
        resetOptionIndex =
                (resetOptionIndex
                        + step
                        + RESET_SECONDS.length)
                        % RESET_SECONDS.length;

        if (tripped) {
            resetTicksRemaining =
                    getResetSeconds() * 20;
        }

        setChanged();
        return getResetSeconds();
    }

    /**
     * Called from GearNetworkManager while it is evaluating overload.
     * The manager invalidates topology after a successful trip.
     */
    public boolean tripFromOverload() {
        if (tripped || !isEngaged()) {
            return false;
        }

        tripped = true;
        resetTicksRemaining =
                getResetSeconds() * 20;

        applyEngagement(false);
        setChanged();
        return true;
    }

    private void tickResetTimer() {
        if (!tripped) {
            return;
        }

        if (resetTicksRemaining > 0) {
            resetTicksRemaining--;
        }

        if (resetTicksRemaining > 0) {
            if (resetTicksRemaining % 20 == 0) {
                setChanged();
            }
            return;
        }

        tripped = false;
        resetTicksRemaining = 0;

        boolean changed = applyEngagement(true);
        setChanged();

        if (changed) {
            updateGearNetwork();
        }
    }

    private boolean applyEngagement(boolean engaged) {
        if (level == null || level.isClientSide) {
            return false;
        }

        BlockState state =
                level.getBlockState(worldPosition);

        if (!state.hasProperty(
                OverloadDisconnectBlock_wood.ENGAGED
        ) || state.getValue(
                OverloadDisconnectBlock_wood.ENGAGED
        ) == engaged) {
            return false;
        }

        level.setBlock(
                worldPosition,
                state.setValue(
                        OverloadDisconnectBlock_wood.ENGAGED,
                        engaged
                ),
                2
        );

        return true;
    }

    private void markNeighborMotionForRendering() {
        if (level == null) {
            return;
        }

        Direction.Axis axis = getGearAxis();

        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis) {
                continue;
            }

            if (level.getBlockEntity(
                    worldPosition.relative(direction)
            ) instanceof GearBlockEntity neighbor
                    && neighbor.getGearAxis() == axis) {
                markHasEverRotatedIfMoving(
                        neighbor.getServerSpeed()
                );
            }
        }
    }

    private void updateRotatingState() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean dynamic =
                shouldRenderGearWithBlockEntity();

        BlockState state =
                level.getBlockState(worldPosition);

        if (state.hasProperty(
                OverloadDisconnectBlock_wood.ROTATING
        ) && state.getValue(
                OverloadDisconnectBlock_wood.ROTATING
        ) != dynamic) {
            level.setBlock(
                    worldPosition,
                    state.setValue(
                            OverloadDisconnectBlock_wood.ROTATING,
                            dynamic
                    ),
                    2
            );
        }
    }

    @Override
    public int getGearTeeth() {
        return 1;
    }

    @Override
    public float getGearMaxTorque() {
        // The protector should not itself halve a wooden shaft network's
        // capacity. It only opens when the actual downstream demand would
        // overload the source/network.
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
                ? state.getValue(DirectionalBlock.FACING).getAxis()
                : Direction.Axis.X;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putInt(
                "ResetOptionIndex",
                resetOptionIndex
        );
        tag.putBoolean(
                "Tripped",
                tripped
        );
        tag.putInt(
                "ResetTicksRemaining",
                resetTicksRemaining
        );
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        resetOptionIndex = Math.max(
                0,
                Math.min(
                        RESET_SECONDS.length - 1,
                        tag.contains("ResetOptionIndex")
                                ? tag.getInt("ResetOptionIndex")
                                : 2
                )
        );

        tripped = tag.getBoolean("Tripped");
        resetTicksRemaining = Math.max(
                0,
                tag.getInt("ResetTicksRemaining")
        );
    }
}
