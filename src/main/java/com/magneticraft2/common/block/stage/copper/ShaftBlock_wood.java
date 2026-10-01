package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.stage.copper.ShaftBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Simple wooden shaft/axle node for the Gear V2 network.
 *
 * Shafts transfer speed/torque along their own axis at a 1:1 ratio and do not
 * side-mesh with gears. Machines can later attach to this same shaft node type.
 */
public class ShaftBlock_wood extends GearBlock {
    private static final VoxelShape Y_AXIS_SHAPE = Shapes.box(0.3125, 0.0, 0.3125, 0.6875, 1.0, 0.6875);
    private static final VoxelShape X_AXIS_SHAPE = Shapes.box(0.0, 0.3125, 0.3125, 1.0, 0.6875, 0.6875);
    private static final VoxelShape Z_AXIS_SHAPE = Shapes.box(0.3125, 0.3125, 0.0, 0.6875, 0.6875, 1.0);

    public ShaftBlock_wood() {
        super(BlockBehaviour.Properties.of().strength(3.5F).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ShaftBlockEntity_wood(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placementState = this.defaultBlockState().setValue(FACING, context.getClickedFace());
        return validateGearPlacement(context, placementState);
    }

    @Override
    public int getPlacementGearTeeth(BlockState state) {
        return 1;
    }

    @Override
    public boolean isShaftLikeForPlacement(BlockState state) {
        return true;
    }

    @Override
    public Direction.Axis getPlacementGearAxis(BlockState state) {
        return state.hasProperty(FACING) ? state.getValue(FACING).getAxis() : Direction.Axis.Y;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis axis = state.getValue(FACING).getAxis();
        if (axis == Direction.Axis.X) {
            return X_AXIS_SHAPE;
        }
        if (axis == Direction.Axis.Z) {
            return Z_AXIS_SHAPE;
        }
        return Y_AXIS_SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, BlockEntityRegistry.SHAFT_BE_WOOD.get(), ShaftBlockEntity_wood::serverTick);
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> serverType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == serverType ? (BlockEntityTicker<A>) ticker : null;
    }
}
