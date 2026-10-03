package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.CustomGearboxBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Open wooden housing for the player-configurable 3x3x3 internal gearbox.
 */
public class CustomGearboxBlock_wood extends Block implements EntityBlock {
    public CustomGearboxBlock_wood() {
        super(BlockBehaviour.Properties.of()
                .strength(2.5F)
                .noOcclusion());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CustomGearboxBlockEntity_wood(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                type,
                BlockEntityRegistry.CUSTOM_GEARBOX_BE_WOOD.get(),
                CustomGearboxBlockEntity_wood::serverTick
        );
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual,
            BlockEntityType<E> expected,
            BlockEntityTicker<? super E> ticker) {
        return actual == expected
                ? (BlockEntityTicker<A>) ticker
                : null;
    }

    @Override
    public void onPlace(BlockState state,
                        Level level,
                        BlockPos pos,
                        BlockState oldState,
                        boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide
                && level.getBlockEntity(pos)
                instanceof CustomGearboxBlockEntity_wood gearbox) {
            gearbox.updateGearNetwork();
        }
    }

    @Override
    public void playerWillDestroy(Level level,
                                  BlockPos pos,
                                  BlockState state,
                                  Player player) {
        if (!level.isClientSide
                && !player.getAbilities().instabuild
                && level.getBlockEntity(pos)
                instanceof CustomGearboxBlockEntity_wood gearbox) {
            for (var stack : gearbox.extractInstalledComponents()) {
                ItemEntity dropped = new ItemEntity(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.65D,
                        pos.getZ() + 0.5D,
                        stack
                );
                dropped.setDefaultPickUpDelay();
                level.addFreshEntity(dropped);
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state,
                         Level level,
                         BlockPos pos,
                         BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            GearNetworkManager.getInstance().removeGear(pos, level);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(BlockState state,
                                 Level level,
                                 BlockPos pos,
                                 Player player,
                                 InteractionHand hand,
                                 BlockHitResult hit) {
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos)
                instanceof CustomGearboxBlockEntity_wood gearbox) {
            NetworkHooks.openScreen(
                    serverPlayer,
                    gearbox,
                    pos
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
