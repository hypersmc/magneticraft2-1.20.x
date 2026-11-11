package com.magneticraft2.common.block.stage.steel;

import com.magneticraft2.common.block.stage.iron.WroughtIronShaftBlock;
import com.magneticraft2.common.blockentity.stage.steel.SteelShaftBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.ShaftBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Steel material variant reusing proven Iron placement and Gear V2 geometry. */
public final class SteelShaftBlock extends WroughtIronShaftBlock {
    public SteelShaftBlock() { super(); }

    @Override protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SteelShaftBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, BlockEntityRegistry.STEEL_SHAFT_BE.get(),
                        ShaftBlockEntity_wood::serverTick);
    }
}
