package com.magneticraft2.common.block.stage.stone;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class PrimitiveGrinderTop extends BaseBlockMagneticraft2 {

    public PrimitiveGrinderTop() {
        super(Properties.of().noOcclusion().strength(3.5F).requiresCorrectToolForDrops());
    }

    @Override
    protected void interactableNoGui(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {

    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return null;
    }
}
