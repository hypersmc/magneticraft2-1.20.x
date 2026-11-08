package com.magneticraft2.common.blockentity.stage.steel;

import com.magneticraft2.common.blockentity.stage.copper.LargeGearBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Steel III gear_large: rated 48 game-torque, not a power source. */
public final class SteelLargeGearBlockEntity extends LargeGearBlockEntity_wood {
    public SteelLargeGearBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.STEEL_GEAR_LARGE_BE.get(), pos, state);
    }

    @Override public float getGearMaxTorque() { return 48.0F; }
}
