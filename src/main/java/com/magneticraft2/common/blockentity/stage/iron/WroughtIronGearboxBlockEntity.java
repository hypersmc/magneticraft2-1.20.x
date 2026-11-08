package com.magneticraft2.common.blockentity.stage.iron;

import com.magneticraft2.common.blockentity.stage.copper.GearboxBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Inherits all Gear V2 right-angle port, rotation and overload behavior. */
public final class WroughtIronGearboxBlockEntity extends GearboxBlockEntity_wood {
    public WroughtIronGearboxBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GEARBOX_BE_IRON.get(), pos, state);
    }

    @Override
    public float getGearMaxTorque() {
        return 16.0F;
    }
}
