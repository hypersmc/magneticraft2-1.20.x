package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.systems.GEAR.ItemBeltConnectionManager;
import com.magneticraft2.common.systems.GEAR.ItemBeltGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Internal physical cell for a continuous Item Belt.
 *
 * Players never place these one at a time. ItemBeltItem generates them between two
 * Wooden Belt Rollers so Minecraft gets normal block collision, selection and chunk
 * ownership while the endpoint renderer still draws one visually continuous moving belt.
 */
public class ItemBeltBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<ItemBeltGeometry.BeltSlope> SLOPE =
            EnumProperty.create("slope", ItemBeltGeometry.BeltSlope.class);

    private static final VoxelShape HORIZONTAL_NORTH_SOUTH =
            Block.box(2.0D, 7.0D, 0.0D, 14.0D, 9.0D, 16.0D);
    private static final VoxelShape HORIZONTAL_EAST_WEST =
            Block.box(0.0D, 7.0D, 2.0D, 16.0D, 9.0D, 14.0D);

    private static final VoxelShape VERTICAL_NORTH_SOUTH =
            Block.box(2.0D, 0.0D, 7.0D, 14.0D, 16.0D, 9.0D);
    private static final VoxelShape VERTICAL_EAST_WEST =
            Block.box(7.0D, 0.0D, 2.0D, 9.0D, 16.0D, 14.0D);

    public ItemBeltBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(0.6F)
                .noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(SLOPE, ItemBeltGeometry.BeltSlope.HORIZONTAL));
    }

    public BlockState stateFor(ItemBeltGeometry.Layout layout) {
        return defaultBlockState()
                .setValue(FACING, layout.facing())
                .setValue(SLOPE, layout.slope());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SLOPE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The canonical endpoint renderer draws the complete animated belt loop.
        // These cells exist for physics/interaction, not for per-block visual seams.
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state,
                               BlockGetter level,
                               BlockPos pos,
                               CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state,
                                        BlockGetter level,
                                        BlockPos pos,
                                        CollisionContext context) {
        return shapeFor(state);
    }

    private VoxelShape shapeFor(BlockState state) {
        Direction facing = state.getValue(FACING);
        ItemBeltGeometry.BeltSlope slope = state.getValue(SLOPE);

        if (slope == ItemBeltGeometry.BeltSlope.HORIZONTAL) {
            return facing.getAxis() == Direction.Axis.Z
                    ? HORIZONTAL_NORTH_SOUTH
                    : HORIZONTAL_EAST_WEST;
        }

        if (slope == ItemBeltGeometry.BeltSlope.VERTICAL) {
            return facing.getAxis() == Direction.Axis.Z
                    ? VERTICAL_NORTH_SOUTH
                    : VERTICAL_EAST_WEST;
        }

        return makeSlopeShape(facing, slope == ItemBeltGeometry.BeltSlope.UPWARD);
    }

    private VoxelShape makeSlopeShape(Direction facing, boolean risesAlongFacing) {
        VoxelShape result = Shapes.empty();
        final int slices = 8;
        final double sliceSize = 16.0D / slices;

        for (int coordinateIndex = 0; coordinateIndex < slices; coordinateIndex++) {
            boolean positiveFacing = facing == Direction.EAST || facing == Direction.SOUTH;
            int travelIndex = positiveFacing
                    ? coordinateIndex
                    : slices - 1 - coordinateIndex;

            int heightIndex = risesAlongFacing
                    ? travelIndex
                    : slices - 1 - travelIndex;

            double centerY = (heightIndex + 0.5D) * sliceSize;
            double minY = Math.max(0.0D, centerY - 1.5D);
            double maxY = Math.min(16.0D, centerY + 1.5D);

            double from = coordinateIndex * sliceSize;
            double to = (coordinateIndex + 1) * sliceSize;

            VoxelShape slice;
            if (facing.getAxis() == Direction.Axis.X) {
                slice = Block.box(from, minY, 2.0D, to, maxY, 14.0D);
            } else {
                slice = Block.box(2.0D, minY, from, 14.0D, maxY, to);
            }

            result = Shapes.or(result, slice);
        }

        return result;
    }

    @Override
    public void onRemove(BlockState state,
                         Level level,
                         BlockPos pos,
                         BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            ItemBeltConnectionManager.onPhysicalBeltBlockRemoved(level, pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
