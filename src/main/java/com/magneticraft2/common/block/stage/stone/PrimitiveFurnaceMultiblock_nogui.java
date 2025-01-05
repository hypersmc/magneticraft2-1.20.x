package com.magneticraft2.common.block.stage.stone;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;

import com.magneticraft2.common.blockentity.stage.stone.PrimitiveFurnaceMultiblockEntity_nogui;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 13-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class PrimitiveFurnaceMultiblock_nogui extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");
    public PrimitiveFurnaceMultiblock_nogui() {
        super(Properties.of().noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(IS_FORMED, Boolean.FALSE));
    }

    @Override
    public void animateTick(BlockState pState, Level level, BlockPos pos, RandomSource pRandom) {
        super.animateTick(pState, level, pos, pRandom);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                if (furnaceEntity.isCooking()) {
                    if (level.getGameTime() % 1 == 0) { // Checks if it’s every 10 ticks
                        double x = pos.getX() + 0.5;
                        double y = pos.getY() + 3.0; // Slightly above the block
                        double z = pos.getZ() + 0.5;

                        level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y, z, 0.0, 0.03, 0.0);
                    }
                    if (level.getGameTime() % 1 == 0) { // Checks if it’s every 5 ticks
                        RandomSource random = level.random;

                        // Number of flames to spawn
                        int flameCount = 5;

                        for (int i = 0; i < flameCount; i++) {
                            // Randomize position within the block bounds
                            double x = pos.getX() + 0.3 + random.nextDouble() * 0.4; // Between 0.3 and 0.7 on X
                            double y = pos.getY() + 0.1 + random.nextDouble() * 0.3; // Between 0.1 and 0.4 on Y
                            double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4; // Between 0.3 and 0.7 on Z

                            // Add flame particle with slight upward velocity
                            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.01, 0.0);
                        }
                    }
                }
            }


        }

    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(IS_FORMED);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            PrimitiveFurnaceMultiblockEntity_nogui multiblockEntity = (PrimitiveFurnaceMultiblockEntity_nogui) blockEntity;
            if (blockEntity instanceof BaseBlockEntityMagneticraft2 primitivefurnace) {
                if (multiblockEntity.isFormed()){
                    (primitivefurnace).interactable(pState, pLevel, pPos, pPlayer, pHand, pHit);
                }else{
                    multiblockEntity.onRightClick();
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
        return pLevel.isClientSide() ? null : createTickerHelper(pBlockEntityType, BlockEntityRegistry.primitivefurnacemultiblockentity_nogui.get(), PrimitiveFurnaceMultiblockEntity_nogui::serverTick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new PrimitiveFurnaceMultiblockEntity_nogui(pPos,pState);
    }
}
