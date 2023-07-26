package com.magneticraft2.common.block.general;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.stream.Stream;

/**
 * @author JumpWatch on 18-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class stick extends Block {
    private static final VoxelShape STICK = Stream.of(Block.box(10, 0.4, 10, 13, 1.4, 11), Block.box(4, 0, 4, 6, 2, 16), Block.box(6.7, -1.7, 5, 7.7, 1.3, 6)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public stick() {
        super(BlockBehaviour.Properties.of().noOcclusion().requiresCorrectToolForDrops());
    }
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader reader, BlockPos pos) {
        return reader.getBlockState(pos.below()).isSolid();
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return STICK;
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack itemStack = new ItemStack(Items.STICK, 1);
        pPlayer.getInventory().add(itemStack);
        pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }
}
