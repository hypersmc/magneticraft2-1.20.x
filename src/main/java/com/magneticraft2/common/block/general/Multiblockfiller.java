package com.magneticraft2.common.block.general;

import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.general.Multiblockfiller_tile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 01-07-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Multiblockfiller extends BaseEntityBlock {
    private static final Logger LOGGER = LogManager.getLogger("MGC2MultiblockFiller");

    public Multiblockfiller() {
        super(BlockBehaviour.Properties.of().noOcclusion().requiresCorrectToolForDrops());
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide) {
            // Get the filler block's BlockEntity and read its NBT data
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;

            if (tag != null && tag.contains("controller_x") && tag.contains("controller_y") && tag.contains("controller_z")) {
                // Retrieve the controller position from the NBT data
                BlockPos controllerPos = new BlockPos(tag.getInt("controller_x"), tag.getInt("controller_y"), tag.getInt("controller_z"));
                BlockEntity controllerEntity = pLevel.getBlockEntity(controllerPos);

                // Check if the BlockEntity at the controller position is an instance of BaseBlockEntityMagneticraft2
                if (controllerEntity instanceof BaseBlockEntityMagneticraft2 multiblockController) {
                    NetworkHooks.openScreen((ServerPlayer) pPlayer, (multiblockController).menuProvider, controllerPos);
                }
            }
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide) {
            // Get the filler block's BlockEntity and read its NBT data
            BlockEntity blockEntity = level.getBlockEntity(pos);
            CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;

            if (tag != null && tag.contains("controller_x") && tag.contains("controller_y") && tag.contains("controller_z")) {
                // Retrieve the controller position from the NBT data
                BlockPos controllerPos = new BlockPos(tag.getInt("controller_x"), tag.getInt("controller_y"), tag.getInt("controller_z"));
                BlockEntity controllerEntity = level.getBlockEntity(controllerPos);

                // Check if the BlockEntity at the controller position is an instance of BaseBlockEntityMagneticraft2
                if (controllerEntity instanceof BaseBlockEntityMagneticraft2 multiblockController) {
                    multiblockController.onDestroy(level); // Call onDestroy on the controller
                }
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new Multiblockfiller_tile(blockPos, blockState);
    }
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F;
    }
}
