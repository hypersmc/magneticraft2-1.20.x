package com.magneticraft2.common.block.stage.steel;

import com.magneticraft2.common.block.stage.iron.WroughtIronGearboxBlock;
import com.magneticraft2.common.blockentity.stage.steel.SteelGearboxBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.GearboxBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Steel material variant reusing proven Iron placement and Gear V2 geometry. */
public final class SteelGearboxBlock extends WroughtIronGearboxBlock {
    public SteelGearboxBlock() { super(); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteelGearboxBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, BlockEntityRegistry.STEEL_GEARBOX_BE.get(),
                        GearboxBlockEntity_wood::serverTick);
    }
}
