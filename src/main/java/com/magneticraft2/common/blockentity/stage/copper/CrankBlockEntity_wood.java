package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.CrankBlock_wood;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gear V2 crank node.
 *
 * The mechanical network still sees this as a normal 1:1 wooden shaft. Machines
 * on the linear side consume the exposed stroke phase instead of pretending
 * reciprocating machinery accepts raw shaft rotation directly.
 */
public class CrankBlockEntity_wood extends GearBlockEntity {
    public CrankBlockEntity_wood(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry.CRANK_BE_WOOD.get(),
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
                instanceof CrankBlockEntity_wood crank)) {
            return;
        }

        crank.serverTickGear();
        crank.markHasEverRotatedIfMoving(
                crank.getServerSpeed()
        );
    }

    public Direction getRodDirection() {
        BlockState state = getBlockState();

        if (state.hasProperty(
                CrankBlock_wood.ROD_DIRECTION
        )) {
            return state.getValue(
                    CrankBlock_wood.ROD_DIRECTION
            );
        }

        return Direction.UP;
    }

    /**
     * 0.0 = rod fully retracted, 1.0 = rod fully extended.
     */
    public float getStrokeProgress(float partialTicks) {
        double radians = Math.toRadians(
                getVisualRotationDegrees(
                        partialTicks
                )
        );

        return (float) (
                0.5D
                        - 0.5D
                        * Math.cos(radians)
        );
    }

    /**
     * Server-side stroke phase for machine logic. Kept separate from the smooth
     * client interpolation used by the renderer.
     */
    public float getServerStrokeProgress() {
        double radians = Math.toRadians(
                getOrCreateGearNode()
                        .getRotationDegrees()
        );

        return (float) (
                0.5D
                        - 0.5D
                        * Math.cos(radians)
        );
    }

    public BlockPos getRodOutputPos() {
        return worldPosition.relative(
                getRodDirection()
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

        return state.hasProperty(
                DirectionalBlock.FACING
        )
                ? state.getValue(
                        DirectionalBlock.FACING
                ).getAxis()
                : Direction.Axis.X;
    }
}
