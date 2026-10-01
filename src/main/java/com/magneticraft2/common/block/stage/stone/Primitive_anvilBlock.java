package com.magneticraft2.common.block.stage.stone;

import com.magneticraft2.common.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class Primitive_anvilBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING;
    private static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");
    private static final BooleanProperty HALFBROKEN = BooleanProperty.create("halfbroken");
    private static final VoxelShape HALF_BROKEN = Stream.of(Block.box(4, 0, 2, 12, 4, 14), Block.box(4, 4, 3, 12, 5, 13), Block.box(4, 5, 4, 12, 9, 12), Block.box(4, 9, 3, 12, 10, 13), Stream.of(Block.box(7, 10, 0, 12, 12, 15), Block.box(10, 14, 0, 12, 16, 15), Block.box(9, 12, 11, 12, 14, 16), Block.box(6, 14, 0, 10, 16, 6), Block.box(6, 14, 6, 10, 15, 10), Block.box(4, 14, 0, 6, 16, 10), Block.box(4, 12, 0, 12, 14, 11), Block.box(4, 10, 0, 7, 12, 13), Block.box(6, 10, 13, 7, 11, 14), Block.box(6, 12, 11, 9, 13, 12), Block.box(7.25, 15, 6, 10.25, 16, 7)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape NEW = Stream.of(Block.box(4, 10, 0, 12, 16, 16), Block.box(4, 0, 2, 12, 4, 14), Block.box(4, 4, 3, 12, 5, 13), Block.box(4, 5, 4, 12, 9, 12), Block.box(4, 9, 3, 12, 10, 13)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public Primitive_anvilBlock() {
        super(BlockBehaviour.Properties.of().strength(3.5F).noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVATED, false).setValue(HALFBROKEN, false));

    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction direction = pState.getValue(FACING);
        boolean halfbroken = pState.getValue(HALFBROKEN);
        if (halfbroken) {
            switch (direction) {
                case NORTH:
                    return VoxelShapeUtils.rotateHorizontal(HALF_BROKEN, Direction.SOUTH);
                case SOUTH:
                    return VoxelShapeUtils.rotateHorizontal(HALF_BROKEN, Direction.NORTH);
                case EAST:
                    return VoxelShapeUtils.rotateHorizontal(HALF_BROKEN, Direction.WEST);
                case WEST:
                    return VoxelShapeUtils.rotateHorizontal(HALF_BROKEN, Direction.EAST);
            }
        }else {
            switch (direction) {
                case NORTH:
                    return VoxelShapeUtils.rotateHorizontal(NEW, Direction.SOUTH);
                case SOUTH:
                    return VoxelShapeUtils.rotateHorizontal(NEW, Direction.NORTH);
                case EAST:
                    return VoxelShapeUtils.rotateHorizontal(NEW, Direction.WEST);
                case WEST:
                    return VoxelShapeUtils.rotateHorizontal(NEW, Direction.EAST);
            }
        }
        return super.getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return (BlockState) this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getClockWise()).setValue(ACTIVATED, false).setValue(HALFBROKEN, false);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return (BlockState) state.setValue(FACING, direction.rotate(state.getValue(FACING))).setValue(ACTIVATED, false).setValue(HALFBROKEN, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(new Property[]{FACING, ACTIVATED, HALFBROKEN});
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return null;
    }


    static {
        FACING = BlockStateProperties.FACING;
    }
}
