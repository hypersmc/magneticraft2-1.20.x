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
    private static final VoxelShape SHELL_NORTH = Shapes.or(
            Block.box(0, 0, 2, 16, 5, 4),
            Block.box(0, 5, 1, 16, 11, 3),
            Block.box(0, 11, 2, 16, 16, 4)).optimize();
    private static final VoxelShape SHELL_EAST = Shapes.or(
            Block.box(12, 0, 0, 14, 5, 16),
            Block.box(13, 5, 0, 15, 11, 16),
            Block.box(12, 11, 0, 14, 16, 16)).optimize();
    private static final VoxelShape SHELL_SOUTH = Shapes.or(
            Block.box(0, 0, 12, 16, 5, 14),
            Block.box(0, 5, 13, 16, 11, 15),
            Block.box(0, 11, 12, 16, 16, 14)).optimize();
    private static final VoxelShape SHELL_WEST = Shapes.or(
            Block.box(2, 0, 0, 4, 5, 16),
            Block.box(1, 5, 0, 3, 11, 16),
            Block.box(2, 11, 0, 4, 16, 16)).optimize();
    private static final VoxelShape END_NORTH = Shapes.or(
            Block.box(2, 2, 5, 14, 14, 8),
            Block.box(0, 0, 4, 16, 2, 9),
            Block.box(0, 14, 4, 16, 16, 9),
            Block.box(0, 2, 4, 2, 14, 9),
            Block.box(14, 2, 4, 16, 14, 9),
            Block.box(6, 6, 2, 10, 10, 5)).optimize();
    private static final VoxelShape END_EAST = Shapes.or(
            Block.box(8, 2, 2, 11, 14, 14),
            Block.box(7, 0, 0, 12, 2, 16),
            Block.box(7, 14, 0, 12, 16, 16),
            Block.box(7, 2, 0, 12, 14, 2),
            Block.box(7, 2, 14, 12, 14, 16),
            Block.box(11, 6, 6, 14, 10, 10)).optimize();
    private static final VoxelShape END_SOUTH = Shapes.or(
            Block.box(2, 2, 8, 14, 14, 11),
            Block.box(0, 0, 7, 16, 2, 12),
            Block.box(0, 14, 7, 16, 16, 12),
            Block.box(14, 2, 7, 16, 14, 12),
            Block.box(0, 2, 7, 2, 14, 12),
            Block.box(6, 6, 11, 10, 10, 14)).optimize();
    private static final VoxelShape END_WEST = Shapes.or(
            Block.box(5, 2, 2, 8, 14, 14),
            Block.box(4, 0, 0, 9, 2, 16),
            Block.box(4, 14, 0, 9, 16, 16),
            Block.box(4, 2, 14, 9, 14, 16),
            Block.box(4, 2, 0, 9, 14, 2),
            Block.box(2, 6, 6, 5, 10, 10)).optimize();

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
        Direction facing = state.getValue(FACING);
        if (shell) {
            return switch (facing) {
                case NORTH -> SHELL_NORTH;
                case EAST -> SHELL_EAST;
                case SOUTH -> SHELL_SOUTH;
                case WEST -> SHELL_WEST;
                default -> SHELL_NORTH;
            };
        }
        return switch (facing) {
            case NORTH -> END_NORTH;
            case EAST -> END_EAST;
            case SOUTH -> END_SOUTH;
            case WEST -> END_WEST;
            default -> END_NORTH;
        };
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
