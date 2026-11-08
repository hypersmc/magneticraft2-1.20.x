package com.magneticraft2.common.blockentity.stage.iron;

import com.magneticraft2.common.blockentity.stage.copper.LargeGearBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 16-tooth Gear V2 node with a 24-torque structural ceiling; not a generator. */
public final class WroughtIronLargeGearBlockEntity extends LargeGearBlockEntity_wood {
    public WroughtIronLargeGearBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GEAR_LARGE_BE_IRON.get(), pos, state);
    }

    @Override public float getGearMaxTorque() { return 24.0F; }
}
