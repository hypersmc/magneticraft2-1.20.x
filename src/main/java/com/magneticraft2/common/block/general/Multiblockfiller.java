package com.magneticraft2.common.block.general;

import com.magneticraft2.common.blockentity.general.Multiblockfiller_tile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
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
