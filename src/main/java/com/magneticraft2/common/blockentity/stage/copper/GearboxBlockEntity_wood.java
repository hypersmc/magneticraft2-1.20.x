package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.GearboxBlock_wood;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One-node representation of a 1:1 right-angle wooden gearbox.
 *
 * Gear V2 stores the INPUT axis as this node's reference rotation. The OUTPUT port is
 * connected through a special RIGHT_ANGLE edge which converts the scalar direction into
 * the other global axis convention.
 */
public class GearboxBlockEntity_wood extends GearBlockEntity {
    public GearboxBlockEntity_wood(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GEARBOX_BE_WOOD.get(), pos, state);
    }

    public static <E extends BlockEntity> void serverTick(Level level,
                                                           BlockPos pos,
                                                           BlockState state,
                                                           E blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (blockEntity instanceof GearboxBlockEntity_wood gearbox) {
            gearbox.serverTickGear();
        }
    }

    @Override
    public int getGearTeeth() {
        return 8;
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
    public boolean supportsExternalGearMesh() {
        return false;
    }

    @Override
    public Direction.Axis getGearAxis() {
        return getInputDirection().getAxis();
    }

    public Direction getInputDirection() {
        BlockState state = getBlockState();
        return state.hasProperty(GearboxBlock_wood.INPUT)
                ? state.getValue(GearboxBlock_wood.INPUT)
                : Direction.WEST;
    }

    public Direction getOutputDirection() {
        BlockState state = getBlockState();
        return state.hasProperty(GearboxBlock_wood.OUTPUT)
                ? state.getValue(GearboxBlock_wood.OUTPUT)
                : Direction.UP;
    }

    public boolean acceptsPort(Direction outwardDirection) {
        return outwardDirection == getInputDirection()
                || outwardDirection == getOutputDirection();
    }

    public boolean isPrimaryPort(Direction outwardDirection) {
        return outwardDirection == getInputDirection();
    }

    /**
     * Convert rotation expressed around INPUT's positive global axis into OUTPUT's
     * positive global axis.
     *
     * Two equal miter gears reverse around their signed outward shaft axes. Converting
     * back to Gear V2's +X/+Y/+Z scalar convention gives:
     *
     *     output = -sign(inputFace) * sign(outputFace) * input
     */
    public int getRightAngleDirectionSign() {
        return -axisDirectionSign(getInputDirection())
                * axisDirectionSign(getOutputDirection());
    }

    private int axisDirectionSign(Direction direction) {
        return switch (direction) {
            case EAST, UP, SOUTH -> 1;
            case WEST, DOWN, NORTH -> -1;
        };
    }

    public float getOutputVisualRotationDegrees(float partialTicks) {
        return getVisualRotationDegrees(partialTicks)
                * getRightAngleDirectionSign();
    }
}
