package com.magneticraft2.common.blockentity.stage.iron;

import com.magneticraft2.common.blockentity.stage.copper.MediumGearBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 8-tooth iron gear in the existing Gear V2 graph. A stronger node is
 * not a source: 16 is a capacity ceiling and weak neighbors still limit it.
 */
public final class WroughtIronMediumGearBlockEntity extends MediumGearBlockEntity_wood {
    public WroughtIronMediumGearBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GEAR_MEDIUM_BE_IRON.get(), pos, state);
    }

    @Override
    public float getGearMaxTorque() {
        return 16.0F;
    }
}
