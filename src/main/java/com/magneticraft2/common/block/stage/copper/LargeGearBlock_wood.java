package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.LargeGearBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.utils.VoxelShapeUtils;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class LargeGearBlock_wood extends GearBlock {
    public static final BooleanProperty VERTICAL_FACING_up = BooleanProperty.create("vertical_facing_up");
    public static final BooleanProperty VERTICAL_FACING_down = BooleanProperty.create("vertical_facing_down");
    private VoxelShape South = VoxelShapeUtils.rotate(downshape(), Direction.SOUTH);
    private VoxelShape East = VoxelShapeUtils.rotate(downshape(), Direction.WEST);
    private VoxelShape North = VoxelShapeUtils.rotate(downshape(), Direction.NORTH);
    private VoxelShape West = VoxelShapeUtils.rotate(downshape(), Direction.WEST);
    public LargeGearBlock_wood() {
        super(BlockBehaviour.Properties.of().strength(3.5F).noOcclusion());
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
        // Get the block entity at the given position
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);

        // If it's a GearBlockEntity, update the network
        if (blockEntity instanceof GearBlockEntity) {
            GearBlockEntity gearBlockEntity = (GearBlockEntity) blockEntity;

            // Create or get the network manager
            GearNetworkManager networkManager = GearNetworkManager.getInstance();

            // Call updateNetwork() to propagate the network and state changes
            gearBlockEntity.updateGearNetwork();
            networkManager.updateNetwork();
        }
        super.onPlace(pState, pLevel, pPos, pOldState, pMovedByPiston);
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LargeGearBlockEntity_wood(pos, state);
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getClickedFace(); // The face of the block that the player is clicking

        // Handle vertical placement (Up or Down)
        if (direction == Direction.UP) {
            return this.defaultBlockState().setValue(VERTICAL_FACING_up, false).setValue(VERTICAL_FACING_down, true);

        }
        if (direction == Direction.DOWN) {
            return this.defaultBlockState().setValue(VERTICAL_FACING_down, false).setValue(VERTICAL_FACING_up, true);
        }

        // Handle horizontal placement (North, South, East, West)
        else if (direction == Direction.NORTH || direction == Direction.SOUTH || direction == Direction.WEST || direction == Direction.EAST) {
            return this.defaultBlockState().setValue(FACING, direction).setValue(VERTICAL_FACING_up, false).setValue(VERTICAL_FACING_down, false);
        }

        // Default case, fallback direction (choose a default horizontal placement)
        return this.defaultBlockState().setValue(FACING, Direction.NORTH);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction direction = pState.getValue(FACING);
        boolean verticalFacingup = pState.getValue(VERTICAL_FACING_up);
        boolean verticalFacingDown = pState.getValue(VERTICAL_FACING_down);
        if (!verticalFacingup && verticalFacingDown && direction == Direction.NORTH) {
            return downshape();
        }
        if (verticalFacingup && !verticalFacingDown && direction == Direction.NORTH){
            return VoxelShapeUtils.rotate(downshape(), Direction.UP);
        }
        if (direction == Direction.NORTH) {
            return VoxelShapeUtils.rotate(downshape(), Direction.SOUTH);
        }
        if (direction == Direction.SOUTH) {
            return VoxelShapeUtils.rotate(downshape(), Direction.NORTH);
        }
        if (direction == Direction.WEST) {
            return VoxelShapeUtils.rotate(downshape(), Direction.EAST);
        }
        if (direction == Direction.EAST) {
            return VoxelShapeUtils.rotate(downshape(), Direction.WEST);
        }
        return super.getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING,VERTICAL_FACING_up,VERTICAL_FACING_down);
    }
    public VoxelShape downshape(){
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.3125, 0, 0.3125, 0.6875, 1, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0.375, 0, 1, 0.625, 1), BooleanOp.OR);
        return shape;
    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : createTickerHelper(pBlockEntityType, BlockEntityRegistry.GEAR_LARGE_BE_WOOD.get(), LargeGearBlockEntity_wood::serverTick);
    }
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> pServerType, BlockEntityType<E> pClientType, BlockEntityTicker<? super E> pTicker) {
        return pClientType == pServerType ? (BlockEntityTicker<A>)pTicker : null;
    }
}
