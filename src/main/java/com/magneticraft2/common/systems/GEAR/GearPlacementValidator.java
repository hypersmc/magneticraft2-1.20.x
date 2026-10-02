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

            int[] offset = getPlanarOffsetComponents(pos, scanPos, newAxis);
            int first = offset[0];
            int second = offset[1];

            boolean newLarge = newTeeth > 8;
            boolean neighborLarge = neighborTeeth > 8;

            // Keep placement and rendering centered on the actual axle. The wooden models are
            // sized for these grid relationships:
            //
            // - 8 <-> 8: adjacent straight mesh
            // - 8 <-> 16: one-by-one diagonal mesh
            // - 16 <-> 16: two-block straight mesh
            //
            // Other non-overlapping placements are allowed; they simply do not transmit.
            if (newLarge && neighborLarge) {
                if (Math.max(first, second) <= 1) {
                    return "Large gears are too close together.";
                }
            } else if (newLarge != neighborLarge) {
                // A straight adjacent medium/large pair physically overlaps. The intended
                // mixed-size mesh is diagonal (1,1), where their pitch circles meet naturally.
                if ((first == 1 && second == 0) || (first == 0 && second == 1)) {
                    return "Medium and large gears mesh diagonally.";
                }
            }
        }

        if (!newShaftLike) {
            String clearanceConflict = getMixedMeshClearanceConflict(level, pos, newState, newTeeth, newAxis);
            if (clearanceConflict != null) {
                return clearanceConflict;
            }
        }

        return null;
    }

    @Nullable
    private static String getMixedMeshClearanceConflict(Level level,
                                                        BlockPos newPos,
                                                        BlockState newState,
                                                        int newTeeth,
                                                        Direction.Axis newAxis) {
        GearBlock newGear = (GearBlock) newState.getBlock();

        // Case 1: the newly placed gear forms a diagonal medium<->large mesh.
        // The two orthogonal cells inside that corner must remain free of other
        // visible gears, otherwise the large gear body/spokes visually overlap them.
        for (BlockPos scanPos : BlockPos.betweenClosed(
                newPos.offset(-1, -1, -1),
                newPos.offset(1, 1, 1))) {

            if (scanPos.equals(newPos)) {
                continue;
            }

            BlockState neighborState = level.getBlockState(scanPos);
            if (!(neighborState.getBlock() instanceof GearBlock neighborGear)
                    || neighborGear.isShaftLikeForPlacement(neighborState)) {
                continue;
            }

            Direction.Axis neighborAxis = neighborGear.getPlacementGearAxis(neighborState);
            int neighborTeeth = neighborGear.getPlacementGearTeeth(neighborState);

            if (neighborAxis != newAxis
                    || !isMixedSizePair(newTeeth, neighborTeeth)
                    || !isValidExternalMeshOffset(newPos, scanPos, newAxis, newTeeth, neighborTeeth)) {
                continue;
            }

            for (BlockPos cornerPos : getMixedMeshReservedCornerPositions(newPos, scanPos, newAxis)) {
                if (hasBlockingVisibleGear(level, cornerPos, newAxis)) {
                    return "Large gear mesh needs clearance around the diagonal connection.";
                }
            }
        }

        // Case 2: an existing diagonal medium<->large mesh already reserves this
        // position. This makes the rule independent of placement order.
        for (BlockPos firstPos : BlockPos.betweenClosed(
                newPos.offset(-2, -2, -2),
                newPos.offset(2, 2, 2))) {

            BlockState firstState = level.getBlockState(firstPos);
            if (!(firstState.getBlock() instanceof GearBlock firstGear)
                    || firstGear.isShaftLikeForPlacement(firstState)) {
                continue;
            }

            Direction.Axis firstAxis = firstGear.getPlacementGearAxis(firstState);
            if (firstAxis != newAxis) {
                continue;
            }

            int firstTeeth = firstGear.getPlacementGearTeeth(firstState);

            for (BlockPos secondPos : getDiagonalPlanarNeighbors(firstPos, firstAxis)) {
                // Visit each existing pair once.
                if (secondPos.asLong() <= firstPos.asLong()) {
                    continue;
                }

                BlockState secondState = level.getBlockState(secondPos);
                if (!(secondState.getBlock() instanceof GearBlock secondGear)
                        || secondGear.isShaftLikeForPlacement(secondState)) {
                    continue;
                }

                Direction.Axis secondAxis = secondGear.getPlacementGearAxis(secondState);
                int secondTeeth = secondGear.getPlacementGearTeeth(secondState);

                if (secondAxis != firstAxis
                        || !isMixedSizePair(firstTeeth, secondTeeth)
                        || !isValidExternalMeshOffset(firstPos, secondPos, firstAxis, firstTeeth, secondTeeth)) {
                    continue;
                }

                for (BlockPos reservedPos : getMixedMeshReservedCornerPositions(firstPos, secondPos, firstAxis)) {
                    if (reservedPos.equals(newPos)) {
                        return "This space is reserved for clearance around a large gear mesh.";
                    }
                }
            }
        }

        return null;
    }

    private static boolean isMixedSizePair(int teethA, int teethB) {
        return (teethA > 8) != (teethB > 8);
    }

    private static boolean hasBlockingVisibleGear(Level level, BlockPos pos, Direction.Axis axis) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GearBlock gear) || gear.isShaftLikeForPlacement(state)) {
            return false;
        }
        return gear.getPlacementGearAxis(state) == axis;
    }

    private static BlockPos[] getMixedMeshReservedCornerPositions(BlockPos first,
                                                                  BlockPos second,
                                                                  Direction.Axis axis) {
        if (axis == Direction.Axis.X) {
            return new BlockPos[]{
                    new BlockPos(first.getX(), first.getY(), second.getZ()),
                    new BlockPos(first.getX(), second.getY(), first.getZ())
            };
        }

        if (axis == Direction.Axis.Y) {
            return new BlockPos[]{
                    new BlockPos(first.getX(), first.getY(), second.getZ()),
                    new BlockPos(second.getX(), first.getY(), first.getZ())
            };
        }

        return new BlockPos[]{
                new BlockPos(first.getX(), second.getY(), first.getZ()),
                new BlockPos(second.getX(), first.getY(), first.getZ())
        };
    }

    private static BlockPos[] getDiagonalPlanarNeighbors(BlockPos pos, Direction.Axis axis) {
        if (axis == Direction.Axis.X) {
            return new BlockPos[]{
                    pos.offset(0, 1, 1),
                    pos.offset(0, 1, -1),
                    pos.offset(0, -1, 1),
                    pos.offset(0, -1, -1)
            };
        }

        if (axis == Direction.Axis.Y) {
            return new BlockPos[]{
                    pos.offset(1, 0, 1),
                    pos.offset(1, 0, -1),
                    pos.offset(-1, 0, 1),
                    pos.offset(-1, 0, -1)
            };
        }

        return new BlockPos[]{
                pos.offset(1, 1, 0),
                pos.offset(1, -1, 0),
                pos.offset(-1, 1, 0),
                pos.offset(-1, -1, 0)
        };
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

    /**
     * Returns whether two visible gears occupy one of Gear V2's supported external-mesh
     * relationships. Axles stay on their block centers; no render-only position nudging is
     * involved.
     */
    public static boolean isValidExternalMeshOffset(BlockPos first,
                                                    BlockPos second,
                                                    Direction.Axis axis,
                                                    int teethA,
                                                    int teethB) {
        if (!isOffsetInGearPlane(first, second, axis)) {
            return false;
        }

        int[] offset = getPlanarOffsetComponents(first, second, axis);
        int a = offset[0];
        int b = offset[1];

        boolean largeA = teethA > 8;
        boolean largeB = teethB > 8;

        if (!largeA && !largeB) {
            return (a == 1 && b == 0) || (a == 0 && b == 1);
        }

        if (largeA && largeB) {
            return (a == 2 && b == 0) || (a == 0 && b == 2);
        }

        // A medium + large pair has a combined pitch radius of roughly 1.5 blocks.
        // Minecraft's one-by-one diagonal is sqrt(2) ~= 1.414 blocks, which matches the
        // current wooden models much better than the old straight two-block workaround.
        return a == 1 && b == 1;
    }

    private static int[] getPlanarOffsetComponents(BlockPos first, BlockPos second, Direction.Axis axis) {
        int dx = Math.abs(second.getX() - first.getX());
        int dy = Math.abs(second.getY() - first.getY());
        int dz = Math.abs(second.getZ() - first.getZ());

        if (axis == Direction.Axis.X) {
            return new int[]{dy, dz};
        }
        if (axis == Direction.Axis.Y) {
            return new int[]{dx, dz};
        }
        return new int[]{dx, dy};
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
