package com.magneticraft2.common.block.stage.steel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Steel IV-C inert vessel sections with real supported geometry and no BE or server tick. */
public final class SteelBoilerStructuralBlock extends HorizontalDirectionalBlock {
    private final boolean shell;
    private static final VoxelShape SHELL_NS = Shapes.or(
            Block.box(0, 0, 2, 16, 5, 4),
            Block.box(0, 5, 1, 16, 11, 3),
            Block.box(0, 11, 2, 16, 16, 4)).optimize();
    private static final VoxelShape SHELL_EW = Shapes.or(
            Block.box(2, 0, 0, 4, 5, 16),
            Block.box(1, 5, 0, 3, 11, 16),
            Block.box(2, 11, 0, 4, 16, 16)).optimize();
    private static final VoxelShape END_NS = Shapes.or(
            Block.box(2, 2, 5, 14, 14, 8), Block.box(0, 0, 4, 16, 2, 9),
            Block.box(0, 14, 4, 16, 16, 9), Block.box(0, 2, 4, 2, 14, 9),
            Block.box(14, 2, 4, 16, 14, 9), Block.box(6, 6, 2, 10, 10, 5)).optimize();
    private static final VoxelShape END_EW = Shapes.or(
            Block.box(5, 2, 2, 8, 14, 14), Block.box(4, 0, 0, 9, 2, 16),
            Block.box(4, 14, 0, 9, 16, 16), Block.box(4, 2, 0, 9, 14, 2),
            Block.box(4, 2, 14, 9, 14, 16), Block.box(2, 6, 6, 5, 10, 10)).optimize();

    public SteelBoilerStructuralBlock(boolean shell) {
        super(BlockBehaviour.Properties.of().strength(5F, 8F).sound(SoundType.METAL)
                .requiresCorrectToolForDrops().noOcclusion());
        this.shell = shell;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        boolean x = state.getValue(FACING).getAxis() == Direction.Axis.X;
        return shell ? (x ? SHELL_EW : SHELL_NS) : (x ? END_EW : END_NS);
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
