package com.magneticraft2.common.block.general.ores.deepslate;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * @author JumpWatch on 16-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Galena_deepslate extends Block {
    public Galena_deepslate() {
        super(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(1.5F, 6F));
    }
}
