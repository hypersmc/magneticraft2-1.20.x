package com.magneticraft2.common.block.general;

import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.general.BlueprintMultiblockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 12-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BlueprintMultiblock extends BaseBlockMagneticraft2{
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");
    public BlueprintMultiblock() {
        super(BlockBehaviour.Properties.of().noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(IS_FORMED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(IS_FORMED);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide){
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            BlueprintMultiblockEntity multiblockEntity = (BlueprintMultiblockEntity) blockEntity;
            if (blockEntity instanceof BaseBlockEntityMagneticraft2 blueprintmaker){
                if (multiblockEntity.isFormed()){
                    NetworkHooks.openScreen((ServerPlayer) pPlayer, (blueprintmaker).menuProvider, blockEntity.getBlockPos());
                }else {
                    multiblockEntity.onRightClick();
                    multiblockEntity.setJustPlaced(false);
                    multiblockEntity.setInitialGameTime(pLevel.getGameTime());
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide){
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BaseBlockEntityMagneticraft2 testmultiblock){
                testmultiblock.onDestroy(level);
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : createTickerHelper(pBlockEntityType, BlockEntityRegistry.blueprintmultiblockentity.get(), BlueprintMultiblockEntity::serverTick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BlueprintMultiblockEntity(pPos,pState);
    }
}
