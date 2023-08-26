package com.magneticraft2.common.block.general;

import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.general.projectortestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class projectortest extends BaseBlockMagneticraft2{
    public projectortest() {
        super(BlockBehaviour.Properties.of().noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.EAST));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
    return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getClockWise());
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return new projectortestBlockEntity(p_153215_, p_153216_);
    }

    @Override
    public InteractionResult use(BlockState p_60503_, Level p_60504_, BlockPos p_60505_, Player p_60506_, InteractionHand p_60507_, BlockHitResult p_60508_) {
        if (!p_60504_.isClientSide) {
            BlockEntity blockEntity = p_60504_.getBlockEntity(p_60505_);
            if (blockEntity instanceof BaseBlockEntityMagneticraft2 projector) {
                NetworkHooks.openScreen((ServerPlayer) p_60506_, (projector).menuProvider, blockEntity.getBlockPos());
            }
        }
        return InteractionResult.SUCCESS;
    }
}
