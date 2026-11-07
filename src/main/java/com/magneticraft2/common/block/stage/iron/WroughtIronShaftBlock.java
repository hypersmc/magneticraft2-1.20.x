package com.magneticraft2.common.block.stage.iron;

import com.magneticraft2.common.block.stage.copper.ShaftBlock_wood;
import com.magneticraft2.common.blockentity.stage.copper.ShaftBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.iron.WroughtIronShaftBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Existing Gear V2 axial 1:1 shaft connection, forged iron material. */
public final class WroughtIronShaftBlock extends ShaftBlock_wood {
    public WroughtIronShaftBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 6.0F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WroughtIronShaftBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
            BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null :
                createTickerHelper(type, BlockEntityRegistry.SHAFT_BE_IRON.get(),
                        ShaftBlockEntity_wood::serverTick);
    }
}
