package com.magneticraft2.common.block.general;

import com.google.common.graph.Network;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.general.blueprintmakerBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 20-08-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class blueprintmaker extends BaseBlockMagneticraft2{
    public blueprintmaker() {
        super(BlockBehaviour.Properties.of().noOcclusion().requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new blueprintmakerBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
                return pLevel.isClientSide() ? null : createTickerHelper(pBlockEntityType, BlockEntityRegistry.blueprintmakerBlockEntity.get(), blueprintmakerBlockEntity::serverTick);
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
        super.onPlace(pState, pLevel, pPos, pOldState, pMovedByPiston);
        if (!pLevel.isClientSide){
            blueprintmakerBlockEntity blockEntity = (blueprintmakerBlockEntity) pLevel.getBlockEntity(pPos);
            assert blockEntity != null;
            blockEntity.setJustPlaced(false);
            blockEntity.setInitialGameTime(pLevel.getGameTime()); // Store initial game time

        }
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide){
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof BaseBlockEntityMagneticraft2 blueprintmaker){
                NetworkHooks.openScreen((ServerPlayer) pPlayer, (blueprintmaker).menuProvider, blockEntity.getBlockPos());
            }
        }
        return InteractionResult.SUCCESS;
    }
}
