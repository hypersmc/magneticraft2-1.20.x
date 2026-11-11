package com.magneticraft2.common.block.stage.steel;

import com.magneticraft2.common.block.stage.iron.WroughtIronLargeGearBlock;
import com.magneticraft2.common.blockentity.stage.steel.SteelLargeGearBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.LargeGearBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Steel material variant reusing proven Iron placement and Gear V2 geometry. */
public final class SteelLargeGearBlock extends WroughtIronLargeGearBlock {
    public SteelLargeGearBlock() { super(); }

    @Override protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SteelLargeGearBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, BlockEntityRegistry.STEEL_GEAR_LARGE_BE.get(),
                        LargeGearBlockEntity_wood::serverTick);
    }
}
