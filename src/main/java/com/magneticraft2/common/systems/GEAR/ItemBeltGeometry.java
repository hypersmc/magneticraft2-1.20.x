package com.magneticraft2.common.systems.GEAR;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grid geometry for Create-style physical item belts.
 *
 * A belt is only valid when it follows one of the discrete shapes Minecraft can represent
 * cleanly with real blocks: horizontal, 45-degree ascending/descending, or vertical.
 * The roller axis remains horizontal and is perpendicular to horizontal belt travel.
 */
public final class ItemBeltGeometry {
    private ItemBeltGeometry() {
    }

    @Nullable
    public static Layout create(BlockPos first,
                                BlockPos second,
                                Direction.Axis rollerAxis,
                                int maxSpan) {
        if (first == null
                || second == null
                || rollerAxis == null
                || rollerAxis == Direction.Axis.Y
                || first.equals(second)) {
            return null;
        }

        int dx = second.getX() - first.getX();
        int dy = second.getY() - first.getY();
        int dz = second.getZ() - first.getZ();

        // Belt movement must remain in the plane perpendicular to the roller axle.
        if ((rollerAxis == Direction.Axis.X && dx != 0)
                || (rollerAxis == Direction.Axis.Z && dz != 0)) {
            return null;
        }

        int horizontalDelta = rollerAxis == Direction.Axis.X ? dz : dx;
        int horizontalSteps = Math.abs(horizontalDelta);
        int verticalSteps = Math.abs(dy);

        BeltSlope slope;
        int steps;

        if (horizontalSteps == 0) {
            if (verticalSteps < 2) {
                return null;
            }
            slope = BeltSlope.VERTICAL;
            steps = verticalSteps;
        } else if (dy == 0) {
            if (horizontalSteps < 2) {
                return null;
            }
            slope = BeltSlope.HORIZONTAL;
            steps = horizontalSteps;
        } else {
            if (horizontalSteps != verticalSteps || horizontalSteps < 2) {
                return null;
            }
            slope = dy > 0 ? BeltSlope.UPWARD : BeltSlope.DOWNWARD;
            steps = horizontalSteps;
        }

        if (steps > maxSpan) {
            return null;
        }

        Direction facing;
        if (horizontalSteps > 0) {
            if (rollerAxis == Direction.Axis.X) {
                facing = horizontalDelta > 0 ? Direction.SOUTH : Direction.NORTH;
            } else {
                facing = horizontalDelta > 0 ? Direction.EAST : Direction.WEST;
            }
        } else {
            // Vertical belts still need a horizontal orientation for width/collision.
            // Choose the direction perpendicular to the axle.
            facing = rollerAxis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        }

        int stepX = Integer.signum(dx);
        int stepY = Integer.signum(dy);
        int stepZ = Integer.signum(dz);

        List<BlockPos> beltBlocks = new ArrayList<>();
        for (int i = 1; i < steps; i++) {
            beltBlocks.add(first.offset(stepX * i, stepY * i, stepZ * i).immutable());
        }

        if (beltBlocks.isEmpty()) {
            return null;
        }

        return new Layout(
                first.immutable(),
                second.immutable(),
                rollerAxis,
                facing,
                slope,
                steps,
                Collections.unmodifiableList(beltBlocks)
        );
    }

    public enum BeltSlope {
        HORIZONTAL,
        UPWARD,
        DOWNWARD,
        VERTICAL
    }

    public record Layout(BlockPos start,
                         BlockPos end,
                         Direction.Axis rollerAxis,
                         Direction facing,
                         BeltSlope slope,
                         int steps,
                         List<BlockPos> beltBlocks) {
        public int requiredSegments() {
            return beltBlocks.size();
        }
    }
}
