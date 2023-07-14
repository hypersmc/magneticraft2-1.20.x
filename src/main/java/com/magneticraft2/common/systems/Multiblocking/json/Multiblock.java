package com.magneticraft2.common.systems.Multiblocking.json;

import net.minecraft.world.level.block.Block;

import java.util.Map;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Multiblock {
    private final String name;
    private final MultiblockStructure structure;
    private final Map<String, Block> blocks;
    private final MultiblockSettings settings;

    public Multiblock(String name, MultiblockStructure structure, Map<String, Block> blocks, MultiblockSettings settings) {
        this.name = name;
        this.structure = structure;
        this.blocks = blocks;
        this.settings = settings;
    }

    public String getName() {
        return name;
    }

    public MultiblockStructure getStructure() {
        return structure;
    }

    public Map<String, Block> getBlocks() {
        return blocks;
    }

    public MultiblockSettings getSettings() {
        return settings;
    }
}
