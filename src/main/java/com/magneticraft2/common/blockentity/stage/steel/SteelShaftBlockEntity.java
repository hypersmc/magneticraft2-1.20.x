package com.magneticraft2.common.blockentity.stage.steel;

import com.magneticraft2.common.blockentity.stage.copper.ShaftBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Steel III shaft: rated 48 game-torque, not a power source. */
public final class SteelShaftBlockEntity extends ShaftBlockEntity_wood {
    public SteelShaftBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.STEEL_SHAFT_BE.get(), pos, state);
    }

    @Override public float getGearMaxTorque() { return 48.0F; }
}
