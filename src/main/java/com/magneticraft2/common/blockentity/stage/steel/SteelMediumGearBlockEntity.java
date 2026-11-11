package com.magneticraft2.common.blockentity.stage.steel;

import com.magneticraft2.common.blockentity.stage.copper.MediumGearBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Steel III gear_medium: rated 32 game-torque, not a power source. */
public final class SteelMediumGearBlockEntity extends MediumGearBlockEntity_wood {
    public SteelMediumGearBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.STEEL_GEAR_MEDIUM_BE.get(), pos, state);
    }

    @Override public float getGearMaxTorque() { return 32.0F; }
}
