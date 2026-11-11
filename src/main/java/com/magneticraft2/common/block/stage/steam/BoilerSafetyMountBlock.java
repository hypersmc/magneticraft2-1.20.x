package com.magneticraft2.common.block.stage.steam;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Physical boiler gauge/relief assembly. State is updated server-side only
 * when the pressure band or relief state changes; no per-frame renderer.
 */
public final class BoilerSafetyMountBlock extends BoilerServiceFittingBlock {
    public static final IntegerProperty PRESSURE_STEP =
            IntegerProperty.create("pressure_step", 0, 5);
    public static final BooleanProperty RELIEVING =
            BooleanProperty.create("relieving");

    public BoilerSafetyMountBlock() {
        super(Role.SAFETY);
        registerDefaultState(defaultBlockState()
                .setValue(PRESSURE_STEP, 0)
                .setValue(RELIEVING, false));
    }

    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, net.minecraft.world.level.block.state.BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PRESSURE_STEP, RELIEVING);
    }
}
