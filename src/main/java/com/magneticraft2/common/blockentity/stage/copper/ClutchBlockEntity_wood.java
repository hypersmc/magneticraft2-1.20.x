package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.ClutchBlock_wood;
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
 * Gear V2 node for the wooden clutch.
 *
 * The GearNode remains registered while open, but GearNetworkManager exposes no
 * mechanical edges through it until ENGAGED becomes true. That actually splits
 * the network rather than merely stopping the visual shaft.
 */
public class ClutchBlockEntity_wood extends GearBlockEntity {
    private boolean manualEngaged = true;

    public ClutchBlockEntity_wood(BlockPos pos,
                                  BlockState state) {
        super(
                BlockEntityRegistry.CLUTCH_BE_WOOD.get(),
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
                instanceof ClutchBlockEntity_wood clutch)) {
            return;
        }

        clutch.refreshEffectiveEngagement();
        clutch.serverTickGear();
        clutch.markHasEverRotatedIfMoving(
                clutch.getServerSpeed()
        );
        clutch.markNeighborMotionForRendering();
        clutch.updateRotatingState();
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

        BlockState state = level.getBlockState(
                worldPosition
        );

        if (state.hasProperty(
                ClutchBlock_wood.ROTATING
        ) && state.getValue(
                ClutchBlock_wood.ROTATING
        ) != dynamic) {
            level.setBlock(
                    worldPosition,
                    state.setValue(
                            ClutchBlock_wood.ROTATING,
                            dynamic
                    ),
                    2
            );
        }
    }

    public boolean toggleManualEngagement() {
        manualEngaged = !manualEngaged;
        setChanged();
        refreshEffectiveEngagement();
        return manualEngaged;
    }

    public boolean isManuallyEngaged() {
        return manualEngaged;
    }

    public boolean isEngaged() {
        BlockState state = getBlockState();
        return state.hasProperty(ClutchBlock_wood.ENGAGED)
                && state.getValue(ClutchBlock_wood.ENGAGED);
    }

    /**
     * Redstone acts as an "open clutch" signal. When the signal disappears the
     * previous manual latch choice is restored.
     */
    public void refreshEffectiveEngagement() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean effectiveEngaged =
                manualEngaged
                        && !level.hasNeighborSignal(worldPosition);

        BlockState state = getBlockState();
        if (!state.hasProperty(ClutchBlock_wood.ENGAGED)
                || state.getValue(ClutchBlock_wood.ENGAGED)
                == effectiveEngaged) {
            return;
        }

        level.setBlock(
                worldPosition,
                state.setValue(
                        ClutchBlock_wood.ENGAGED,
                        effectiveEngaged
                ),
                2
        );

        setChanged();

        // Engagement changes graph topology immediately.
        updateGearNetwork();
    }

    @Override
    public int getGearTeeth() {
        return 1;
    }

    @Override
    public float getGearMaxTorque() {
        return 8.0F;
    }

    @Override
    public boolean isShaftLike() {
        return true;
    }

    @Override
    public Direction.Axis getGearAxis() {
        BlockState state = getBlockState();
        return state.hasProperty(DirectionalBlock.FACING)
                ? state.getValue(DirectionalBlock.FACING)
                .getAxis()
                : Direction.Axis.X;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(
                "ManualEngaged",
                manualEngaged
        );
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        manualEngaged = !tag.contains("ManualEngaged")
                || tag.getBoolean("ManualEngaged");
    }
}
