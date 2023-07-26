package com.magneticraft2.common.block.general.ores.normal;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * @author JumpWatch on 16-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Osmiridium extends Block {
    public Osmiridium() {
        super(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(1.5F, 6F));
    }
}
