package com.magneticraft2.common.block.general;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 07-01-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BeltBlock extends Block {
    public static final BooleanProperty IS_STRAIGHT = BooleanProperty.create("is_straight");
    public static final BooleanProperty IS_45 = BooleanProperty.create("is_45");
    public static final BooleanProperty IS_90 = BooleanProperty.create("is_90");
    public BeltBlock() {
        super(BlockBehaviour.Properties.of().strength(3.5F).noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(IS_STRAIGHT, true).setValue(IS_45, false).setValue(IS_90, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        boolean is_straight = pState.getValue(IS_STRAIGHT);
        boolean is_45 = pState.getValue(IS_45);
        boolean is_90 = pState.getValue(IS_90);
        // TODO: Implement this function properly.
        // I'm not even sure what it's supposed to do at this point.
        // Should properly have a angle like from_straight_to_45 and from_straight_to_90
        return super.getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(IS_STRAIGHT).add(IS_45).add(IS_90);
    }

}
