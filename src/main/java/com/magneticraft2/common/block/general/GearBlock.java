package com.magneticraft2.common.block.general;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public abstract class GearBlock extends DirectionalBlock implements EntityBlock {
    public GearBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return createBlockEntity(pos, state);
    }

    protected abstract BlockEntity createBlockEntity(BlockPos pos, BlockState state);

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
        super.onPlace(pState, pLevel, pPos, pOldState, pMovedByPiston);
        GearBlockEntity blockEntity = (GearBlockEntity) pLevel.getBlockEntity(pPos);
        if (blockEntity != null) {
            blockEntity.updateGearNetwork();
        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level pLevel, BlockPos pPos, Player player, boolean willHarvest, FluidState fluid) {
        GearBlockEntity blockEntity = (GearBlockEntity) pLevel.getBlockEntity(pPos);
        if (blockEntity != null) {
            System.out.println("Trigger");
            GearNetworkManager.getInstance().removeGear(pPos);
        }
        return super.onDestroyedByPlayer(state, pLevel, pPos, player, willHarvest, fluid);

    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
        GearBlockEntity blockEntity = (GearBlockEntity) pLevel.getBlockEntity(pPos);
        if (blockEntity != null) {
            System.out.println("Trigger");
            GearNetworkManager.getInstance().removeGear(pPos);
        }
    }
}
