package com.magneticraft2.common.block.stage.iron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Waterloggable open-grid metal decking. The 19 collision cuboids match
 * the baked model exactly: narrow real bars, not an invisible solid plate.
 * No block entity or tick-driven simulation is needed.
 */
public final class IronFloorGratingBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape GRATING = Shapes.or(
            Block.box(0, 13, 0, 16, 16, 2),
            Block.box(0, 13, 14, 16, 16, 16),
            Block.box(0, 13, 2, 2, 16, 14),
            Block.box(14, 13, 2, 16, 16, 14),
            Block.box(2, 13, 4, 14, 16, 5),
            Block.box(2, 13, 7, 14, 16, 8),
            Block.box(2, 13, 10, 14, 16, 11),
            Block.box(4, 13, 2, 5, 16, 4),
            Block.box(4, 13, 5, 5, 16, 7),
            Block.box(4, 13, 8, 5, 16, 10),
            Block.box(4, 13, 11, 5, 16, 14),
            Block.box(7, 13, 2, 8, 16, 4),
            Block.box(7, 13, 5, 8, 16, 7),
            Block.box(7, 13, 8, 8, 16, 10),
            Block.box(7, 13, 11, 8, 16, 14),
            Block.box(10, 13, 2, 11, 16, 4),
            Block.box(10, 13, 5, 11, 16, 7),
            Block.box(10, 13, 8, 11, 16, 10),
            Block.box(10, 13, 11, 11, 16, 14)
    ).optimize();

    public IronFloorGratingBlock() {
        super(BlockBehaviour.Properties.of().strength(4.0F, 6.0F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override public BlockState updateShape(BlockState state, Direction direction,
            BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return GRATING;
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return GRATING;
    }

    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }
}
