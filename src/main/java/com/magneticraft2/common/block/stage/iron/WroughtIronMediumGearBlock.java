package com.magneticraft2.common.block.stage.iron;

import com.magneticraft2.common.block.stage.copper.MediumGearBlock_wood;
import com.magneticraft2.common.blockentity.stage.copper.MediumGearBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.iron.WroughtIronMediumGearBlockEntity;
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

/** The exact existing 8-tooth Gear V2 placement rules, with forged iron. */
public class WroughtIronMediumGearBlock extends MediumGearBlock_wood {
    public WroughtIronMediumGearBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 6.0F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WroughtIronMediumGearBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
            BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, BlockEntityRegistry.GEAR_MEDIUM_BE_IRON.get(),
                        MediumGearBlockEntity_wood::serverTick);
    }
}
