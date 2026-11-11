package com.magneticraft2.common.block.stage.steel;

import com.magneticraft2.common.block.stage.iron.WroughtIronMediumGearBlock;
import com.magneticraft2.common.blockentity.stage.steel.SteelMediumGearBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.MediumGearBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Steel material variant reusing proven Iron placement and Gear V2 geometry. */
public final class SteelMediumGearBlock extends WroughtIronMediumGearBlock {
    public SteelMediumGearBlock() { super(); }

    @Override protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SteelMediumGearBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, BlockEntityRegistry.STEEL_GEAR_MEDIUM_BE.get(),
                        MediumGearBlockEntity_wood::serverTick);
    }
}
