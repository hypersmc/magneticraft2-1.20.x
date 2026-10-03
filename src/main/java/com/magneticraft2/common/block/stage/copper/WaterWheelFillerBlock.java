package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Internal occupancy + visual segment for the 3x3 large Water Wheel.
 *
 * While idle these cells render their own block-sized wheel segment. While the wheel is
 * active the static segments become invisible and the controller BER renders the same
 * eight pieces under one common rotation transform.
 */
public class WaterWheelFillerBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction.Axis> AXIS =
            BlockStateProperties.HORIZONTAL_AXIS;
    public static final EnumProperty<Part> PART =
            EnumProperty.create("part", Part.class);
    public static final BooleanProperty ACTIVE =
            BooleanProperty.create("active");
    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    private static final VoxelShape X_SHAPE =
            Block.box(3.0D, 0.0D, 0.0D, 13.0D, 16.0D, 16.0D);
    private static final VoxelShape Z_SHAPE =
            Block.box(0.0D, 0.0D, 3.0D, 16.0D, 16.0D, 13.0D);

    public WaterWheelFillerBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(0.6F)
                .noOcclusion());

        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.X)
                .setValue(PART, Part.TOP)
                .setValue(ACTIVE, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, PART, ACTIVE, WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The controller BER draws the complete 3x3 wheel, including these occupied
        // cells, both stopped and running.
        return RenderShape.INVISIBLE;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state,
                                  Direction direction,
                                  BlockState neighborState,
                                  LevelAccessor level,
                                  BlockPos pos,
                                  BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(
                    pos,
                    Fluids.WATER,
                    Fluids.WATER.getTickDelay(level)
            );
        }

        return super.updateShape(
                state,
                direction,
                neighborState,
                level,
                pos,
                neighborPos
        );
    }

    @Override
    public VoxelShape getShape(BlockState state,
                               BlockGetter level,
                               BlockPos pos,
                               CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X
                ? X_SHAPE
                : Z_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state,
                                        BlockGetter level,
                                        BlockPos pos,
                                        CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public void onRemove(BlockState state,
                         Level level,
                         BlockPos pos,
                         BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            BlockPos controller = findController(
                    level,
                    pos,
                    state.getValue(AXIS)
            );

            if (controller != null) {
                level.destroyBlock(controller, true);
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private BlockPos findController(Level level,
                                    BlockPos fillerPos,
                                    Direction.Axis axis) {
        for (int horizontal = -1; horizontal <= 1; horizontal++) {
            for (int vertical = -1; vertical <= 1; vertical++) {
                BlockPos candidate = axis == Direction.Axis.X
                        ? fillerPos.offset(0, vertical, horizontal)
                        : fillerPos.offset(horizontal, vertical, 0);

                if (level.getBlockState(candidate).is(BlockRegistry.WATER_WHEEL_LARGE.get())) {
                    return candidate;
                }
            }
        }

        return null;
    }

    public enum Part implements StringRepresentable {
        TOP("top"),
        TOP_RIGHT("top_right"),
        RIGHT("right"),
        BOTTOM_RIGHT("bottom_right"),
        BOTTOM("bottom"),
        BOTTOM_LEFT("bottom_left"),
        LEFT("left"),
        TOP_LEFT("top_left");

        private final String serializedName;

        Part(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
