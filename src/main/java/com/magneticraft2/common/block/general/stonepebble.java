package com.magneticraft2.common.block.general;

import com.magneticraft2.common.blockentity.general.stonepebbleBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * @author JumpWatch on 18-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class stonepebble extends BaseBlockMagneticraft2 {
    public static final IntegerProperty TYPE = IntegerProperty.create("type", 0, 4);
    private static final VoxelShape EMPTY = Block.box(0, 0, 0, 16, 0, 16);
    private static final VoxelShape TINY = Stream.of(Block.box(0.5, 0, 6.8, 1.5, 1, 7.8), Block.box(7, 0, 7, 10, 2, 9), Block.box(7, 0, 0, 9, 1, 1), Block.box(6, 0, 13, 7, 1, 14), Block.box(-7, 0, 12, -6, 1, 13), Block.box(13, 0, 12, 15, 1, 14), Block.box(2, 0, 5, 4, 2, 6), Block.box(9, 0, 8, 12, 3, 10)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape SMALL = Stream.of(Block.box(9, 0, 7, 13, 3, 10), Block.box(6, 0, 9, 10, 2, 11), Block.box(6, 0, 2, 9, 2, 4), Block.box(13, 0, 1, 14, 1, 2), Block.box(2, 0, 9, 3, 1, 10), Block.box(8, 0, 16, 9, 1, 17)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape MEDIUM = Stream.of(Block.box(8, 0, 6, 11, 4, 10), Block.box(6, 0, 4, 9, 2, 8), Block.box(2.8, 0, 8, 4.8, 1, 10), Block.box(0, 0, 3, 2, 1, 5), Block.box(5, 0, 12, 6, 1, 14), Block.box(1, 0, 8, 3, 1, 9), Block.box(-5, 0, 7.7, -4, 2, 8.7), Block.box(13, 0, 2, 15, 1, 5), Block.box(11, 0, 11, 13, 1, 13), Block.box(12, 0, 12, 14, 2, 14), Block.box(7.6, 0, 16.1, 8.6, 1, 18.1)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape BIG = Stream.of(Block.box(11, 0, 12, 12, 1, 13), Block.box(0, 0, 4, 8, 5, 11), Block.box(11, 0, 3, 18, 4, 8), Block.box(2, 0, 9, 8, 8, 13), Block.box(5, 0, 10, 7, 1, 12), Block.box(-2, 0, 15, -1, 1, 16)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public stonepebble() {
        super(BlockBehaviour.Properties.of().noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, 0));
    }


    @Override
    public boolean canSurvive(BlockState state, LevelReader reader, BlockPos pos) {
        return reader.getBlockState(pos.below()).isSolid();
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        int type = pState.getValue(TYPE);
        if (type == 1){
            return TINY;
        }else if (type == 2) {
            return SMALL;
        }else if (type == 3) {
            return MEDIUM;
        }else if (type == 4) {
            return BIG;
        }else {
            return EMPTY;
        }
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack itemStack = new ItemStack(ItemRegistry.item_pebble.get(), 1);
        pPlayer.getInventory().add(itemStack);
        pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(TYPE);
    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : createTickerHelper(pBlockEntityType, BlockEntityRegistry.stonepebbleBlockEntity.get(), stonepebbleBlockEntity::serverTick);
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new stonepebbleBlockEntity(pPos, pState);
    }
}
