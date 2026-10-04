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

    private static final double CRANK_RADIUS = 0.16D;
    private static final double CONNECTING_ROD_LENGTH = 0.34D;

    /**
     * 0.0 = rod fully retracted, 1.0 = rod fully extended.
     *
     * Keep this derived from the exact same slider-crank geometry used by the
     * renderer. The previous cosine approximation was 180 degrees out of phase:
     * the rendered crosshead was physically extended while machine logic reported
     * a fully retracted stroke, making attached Bellows move opposite the rod.
     */
    public float getStrokeProgress(float partialTicks) {
        return strokeProgressFromDegrees(
                getVisualRotationDegrees(
                        partialTicks
                )
        );
    }

    /**
     * Server-side stroke phase for machine logic. Kept separate from the smooth
     * client interpolation used by the renderer.
     */
    public float getServerStrokeProgress() {
        return strokeProgressFromDegrees(
                getOrCreateGearNode()
                        .getRotationDegrees()
        );
    }

    private float strokeProgressFromDegrees(
            float rotationDegrees) {
        double radians =
                Math.toRadians(rotationDegrees);

        double pinY =
                Math.cos(radians)
                        * CRANK_RADIUS;

        double pinZ =
                Math.sin(radians)
                        * CRANK_RADIUS;

        double sliderY =
                pinY
                        + Math.sqrt(
                                Math.max(
                                        0.0D,
                                        CONNECTING_ROD_LENGTH
                                                * CONNECTING_ROD_LENGTH
                                                - pinZ * pinZ
                                )
                        );

        double minimum =
                CONNECTING_ROD_LENGTH
                        - CRANK_RADIUS;
        double maximum =
                CONNECTING_ROD_LENGTH
                        + CRANK_RADIUS;

        return (float) Math.max(
                0.0D,
                Math.min(
                        1.0D,
                        (sliderY - minimum)
                                / (maximum - minimum)
                )
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
