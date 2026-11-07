package com.magneticraft2.common.blockentity.stage.iron;

import com.magneticraft2.common.blockentity.stage.copper.ShaftBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stronger shaft capacity, not a source. The existing large water wheel
 * still supplies at most 16 torque, and weaker connected nodes still limit.
 */
public final class WroughtIronShaftBlockEntity extends ShaftBlockEntity_wood {
    public WroughtIronShaftBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.SHAFT_BE_IRON.get(), pos, state);
    }

    @Override
    public float getGearMaxTorque() {
        return 24.0F;
    }
}
