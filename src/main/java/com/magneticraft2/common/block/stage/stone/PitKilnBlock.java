package com.magneticraft2.common.block.stage.stone;

import com.magneticraft2.common.blockentity.stage.stone.PitKilnBlockEntity;
import com.magneticraft2.common.registry.FinalRegistry;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.stream.Stream;

/**
 * @author JumpWatch on 01-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class PitKilnBlock  extends BaseEntityBlock {
    private static final Logger LOGGER = LogManager.getLogger("Pitkilnblock");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 0.0001, 16);

    private static final VoxelShape WHEAT1 = Block.box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape WHEAT2 = Block.box(0, 0, 0, 16, 4, 16);
    private static final VoxelShape WHEAT3 = Block.box(0, 0, 0, 16, 6, 16);
    private static final VoxelShape WHEAT4 = Block.box(0, 0, 0, 16, 8, 16);

    private static final VoxelShape LOG1 = Stream.of(Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 8, 0, 4, 12, 16)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape LOG2 = Stream.of(Block.box(4, 8, 0, 8, 12, 16), Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 8, 0, 4, 12, 16)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape LOG3 = Stream.of(Block.box(4, 8, 0, 8, 12, 16), Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 8, 0, 4, 12, 16), Block.box(8, 8, 0, 12, 12, 16)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape LOG4 = Stream.of(Block.box(12, 8, 0, 16, 12, 16), Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 8, 0, 4, 12, 16), Block.box(8, 8, 0, 12, 12, 16), Block.box(4, 8, 0, 8, 12, 16)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape LOG5 = Stream.of(Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 8, 0, 4, 12, 16), Block.box(0, 12, 0, 4, 16, 16), Block.box(8, 8, 0, 12, 12, 16), Block.box(4, 8, 0, 8, 12, 16), Block.box(12, 8, 0, 16, 12, 16)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape LOG6 = Stream.of(Block.box(0, 8, 0, 4, 12, 16), Block.box(0, 0, 0, 16, 8, 16), Block.box(12, 8, 0, 16, 12, 16), Block.box(4, 8, 0, 8, 12, 16), Block.box(8, 8, 0, 12, 12, 16), Block.box(4, 12, 0, 8, 16, 16), Block.box(0, 12, 0, 4, 16, 16)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape LOG7 = Stream.of(Block.box(0, 8, 0, 4, 12, 16), Block.box(0, 0, 0, 16, 8, 16), Block.box(12, 8, 0, 16, 12, 16), Block.box(8, 12, 0, 12, 16, 16), Block.box(4, 8, 0, 8, 12, 16), Block.box(8, 8, 0, 12, 12, 16), Block.box(4, 12, 0, 8, 16, 16), Block.box(0, 12, 0, 4, 16, 16)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape LOG8 = Block.box(0, 0, 0, 16, 16, 16);


    public static final IntegerProperty WHEAT_COUNT = IntegerProperty.create("wheat_count", 0, 4);
    public static final IntegerProperty LOG_COUNT = IntegerProperty.create("log_count", 0, 8);
    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");


    public PitKilnBlock() {
        super(BlockBehaviour.Properties.of().strength(3.5F).noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(WHEAT_COUNT, 0).setValue(LOG_COUNT, 0).setValue(ACTIVATED, false));

    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }


    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof PitKilnBlockEntity) {
                PitKilnBlockEntity pitKilnBlockEntity = (PitKilnBlockEntity)blockEntity;
                ItemStack heldStack = player.getItemInHand(hand);
                // Get the block entity and item handler for the pit kiln
                IItemHandler itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
                // Check if the item has the "clay_items" tag and if so, try to insert it into the next available slot
                if (heldStack.is(FinalRegistry.clayitem)) {
                    for (int i = 2; i <= 5; i++) {
                        if (itemHandler.getStackInSlot(i).isEmpty()) {
                            itemHandler.insertItem(i, heldStack.copy().split(1), false);
                            heldStack.shrink(1);
                            break;
                        }
                    }
                }
                // Add wheat and oak to the pit kiln's inventory
                if (heldStack.getItem() == Items.WHEAT && itemHandler.getStackInSlot(1).getCount() < 4) {
                    itemHandler.insertItem(1, heldStack.getItem().getDefaultInstance(), false);
                    heldStack.setCount(heldStack.getCount() - 1);

                } else if (heldStack.getItem() == Items.OAK_LOG && itemHandler.getStackInSlot(0).getCount() < 8 && itemHandler.getStackInSlot(1).getCount() == 4) {
                    itemHandler.insertItem(0, heldStack.getItem().getDefaultInstance(), false);
                    heldStack.setCount(heldStack.getCount() - 1);
                }


                // Check if the player is holding a fire starter item
                if (heldStack.getItem() == Items.FLINT_AND_STEEL) {
                    // Start the firing process
                    pitKilnBlockEntity.activate(state, world, pos);
                    return InteractionResult.PASS;
                }

            }
        }
        return super.use(state, world, pos, player, hand, hit);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        int wheatCount = pState.getValue(WHEAT_COUNT);
        int logCount = pState.getValue(LOG_COUNT);
        boolean activated = pState.getValue(ACTIVATED);
        if (wheatCount == 1){
            return WHEAT1;
        }else if (wheatCount == 2){
            return WHEAT2;
        }else if (wheatCount == 3){
            return WHEAT3;
        }else if (wheatCount == 4 && logCount == 0){
            return WHEAT4;
        }else if (wheatCount == 4 && logCount == 1){
            return LOG1;
        }else if (wheatCount == 4 && logCount == 2){
            return LOG2;
        }else if (wheatCount == 4 && logCount == 3){
            return LOG3;
        }else if (wheatCount == 4 && logCount == 4){
            return LOG4;
        }else if (wheatCount == 4 && logCount == 5){
            return LOG5;
        }else if (wheatCount == 4 && logCount == 6){
            return LOG6;
        }else if (wheatCount == 4 && logCount == 7){
            return LOG7;
        }else if (wheatCount == 4 && logCount == 8){
            return LOG8;
        }else if (activated){
            return LOG8;
        }else {
            return SHAPE;
        }
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(LOG_COUNT).add(WHEAT_COUNT).add(ACTIVATED);
    }




    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new PitKilnBlockEntity(pPos,pState);
    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : createTickerHelper(pBlockEntityType, BlockEntityRegistry.PitKilnblockEntity.get(), PitKilnBlockEntity::serverTick);
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pLevel.isClientSide) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof PitKilnBlockEntity) {
                // Update the kiln's state to the client
                ((PitKilnBlockEntity)blockEntity).setChanged();
            }
        }
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
    }
    //    @Override
//    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
//        if (state.getBlock() != newState.getBlock()) {
//            BlockEntity blockEntity = world.getBlockEntity(pos);
//            if (blockEntity instanceof Inventory) {
//                // Drop the kiln's inventory items as items in the world
//                ItemScatterer.spawn(world, pos, (Inventory)blockEntity);
//            }
//            super.onStateReplaced(state, world, pos, newState, moved);
//        }
//    }
//
//    @Override
//    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
//        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
//    }
}
