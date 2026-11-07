package com.magneticraft2.common.block.stage.iron;

import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Simple hand-operated wrought-iron maintenance access hatch.
 * Delegates hinge, facing, redstone, waterlogging, and collision to the
 * vanilla Forge 1.20.1 TrapDoorBlock. No BE/ticker/custom packet needed.
 */
public final class IronServiceHatchBlock extends TrapDoorBlock {
    private static final BlockSetType IRON_SERVICE = BlockSetType.register(
            new BlockSetType(
                    "magneticraft2:iron_service_hatch",
                    true,
                    BlockSetType.IRON.soundType(),
                    BlockSetType.IRON.doorClose(),
                    BlockSetType.IRON.doorOpen(),
                    BlockSetType.IRON.trapdoorClose(),
                    BlockSetType.IRON.trapdoorOpen(),
                    BlockSetType.IRON.pressurePlateClickOff(),
                    BlockSetType.IRON.pressurePlateClickOn(),
                    BlockSetType.IRON.buttonClickOff(),
                    BlockSetType.IRON.buttonClickOn()));

    public IronServiceHatchBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(4.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(), IRON_SERVICE);
    }
}
