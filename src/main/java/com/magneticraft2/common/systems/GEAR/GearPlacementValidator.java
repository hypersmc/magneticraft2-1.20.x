package com.magneticraft2.common.systems.GEAR;

import com.magneticraft2.common.block.general.GearBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Placement rules for the Gear V2 foundation.
 *
 * Gears are visually larger than one Minecraft block. This validator prevents placements
 * where gear models would occupy the same visual space. The rules are intentionally grid-based
 * rather than perfect model-geometry checks so they stay predictable while the mechanical system
 * is still being developed.
 */
public final class GearPlacementValidator {
    private static final int SCAN_RADIUS = 3;

    private GearPlacementValidator() {
    }

    @Nullable
    public static String getInvalidPlacementReason(Level level, BlockPos pos, BlockState newState) {
        if (level == null || pos == null || !(newState.getBlock() instanceof GearBlock newGear)) {
            return null;
        }

        boolean newShaftLike = newGear.isShaftLikeForPlacement(newState);
        int newTeeth = newGear.getPlacementGearTeeth(newState);
        Direction.Axis newAxis = newGear.getPlacementGearAxis(newState);

        for (BlockPos scanPos : BlockPos.betweenClosed(
                pos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                pos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))) {

            if (scanPos.equals(pos)) {
                continue;
            }

            BlockState neighborState = level.getBlockState(scanPos);
            if (!(neighborState.getBlock() instanceof GearBlock neighborGear)) {
                continue;
            }

            boolean neighborShaftLike = neighborGear.isShaftLikeForPlacement(neighborState);
            int neighborTeeth = neighborGear.getPlacementGearTeeth(neighborState);
            Direction.Axis neighborAxis = neighborGear.getPlacementGearAxis(neighborState);

            // Shaft/axle blocks are thin and only transmit along their own axis. They do not
            // reserve a large side-meshing footprint like visible gears do.
            if (newShaftLike || neighborShaftLike) {
                continue;
            }

            // Different spin axes are different gear planes. Bevel/cross-axis gears are not
            // supported yet, so they are ignored here instead of being treated as side meshes.
            if (newAxis != neighborAxis) {
                continue;
            }

            if (!isOffsetInGearPlane(pos, scanPos, newAxis)) {
                continue;
            }

            int requiredDistance = getRequiredMeshDistance(newTeeth, neighborTeeth);
            int planarDistance = getPlanarChebyshevDistance(pos, scanPos, newAxis);

            // If the centers are closer than the smallest valid meshing distance, the models
            // will clip. This catches medium gears placed inside large gears too.
            if (planarDistance > 0 && planarDistance < requiredDistance) {
                return "Gear is too close to another gear.";
            }
        }

        return null;
    }

    public static boolean canPlace(Level level, BlockPos pos, BlockState newState) {
        return getInvalidPlacementReason(level, pos, newState) == null;
    }

    /**
     * Grid distance where two gears are allowed to externally mesh.
     *
     * The early V2.2 rule used {@code sizeA + sizeB - 1}. That made two large
     * gears require distance 3, which placed them too far apart visually while
     * still allowing the network to treat that gap as a valid mesh. For the
     * current wooden models, the larger gear size is a better center distance:
     *
     * - 8 teeth  + 8 teeth  -> distance 1
     * - 8 teeth  + 16 teeth -> distance 2
     * - 16 teeth + 16 teeth -> distance 2
     */
    public static int getRequiredMeshDistance(int teethA, int teethB) {
        int sizeA = getGearSizeUnits(teethA);
        int sizeB = getGearSizeUnits(teethB);
        return Math.max(1, Math.max(sizeA, sizeB));
    }

    public static int getGearSizeUnits(int teeth) {
        return Math.max(1, Math.round(teeth / 8.0F));
    }

    public static boolean isOffsetInGearPlane(BlockPos first, BlockPos second, Direction.Axis axis) {
        int dx = second.getX() - first.getX();
        int dy = second.getY() - first.getY();
        int dz = second.getZ() - first.getZ();

        if (axis == Direction.Axis.X) {
            return dx == 0;
        }
        if (axis == Direction.Axis.Y) {
            return dy == 0;
        }
        return dz == 0;
    }

    public static int getPlanarChebyshevDistance(BlockPos first, BlockPos second, Direction.Axis axis) {
        int dx = second.getX() - first.getX();
        int dy = second.getY() - first.getY();
        int dz = second.getZ() - first.getZ();

        if (axis == Direction.Axis.X) {
            return Math.max(Math.abs(dy), Math.abs(dz));
        }
        if (axis == Direction.Axis.Y) {
            return Math.max(Math.abs(dx), Math.abs(dz));
        }
        return Math.max(Math.abs(dx), Math.abs(dy));
    }

    public static boolean isStraightPlanarOffset(BlockPos first, BlockPos second, Direction.Axis axis) {
        int dx = second.getX() - first.getX();
        int dy = second.getY() - first.getY();
        int dz = second.getZ() - first.getZ();

        if (axis == Direction.Axis.X) {
            return dx == 0 && ((dy == 0) != (dz == 0));
        }
        if (axis == Direction.Axis.Y) {
            return dy == 0 && ((dx == 0) != (dz == 0));
        }
        return dz == 0 && ((dx == 0) != (dy == 0));
    }
}
