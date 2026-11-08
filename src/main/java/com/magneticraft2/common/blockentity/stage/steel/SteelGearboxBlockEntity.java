package com.magneticraft2.common.blockentity.stage.steel;

import com.magneticraft2.common.blockentity.stage.copper.GearboxBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Steel III gearbox: rated 32 game-torque, not a power source. */
public final class SteelGearboxBlockEntity extends GearboxBlockEntity_wood {
    public SteelGearboxBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.STEEL_GEARBOX_BE.get(), pos, state);
    }

    @Override public float getGearMaxTorque() { return 32.0F; }
}
