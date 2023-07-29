package com.magneticraft2.common.systems.Blueprint.json;

import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BlueprintStructure {
    private final int[] dimensions;
    private final Map<String, List<List<String>>> layout;
    private final Map<String, Block> blocks;

    public BlueprintStructure(int[] dimensions, Map<String, List<List<String>>> layout, Map<String, Block> blocks) {
        this.dimensions = dimensions;
        this.layout = layout;
        this.blocks = blocks;
    }

    public int[] getDimensions() {
        return dimensions;
    }

    public Map<String, List<List<String>>> getLayout() {
        return layout;
    }

    public Map<String, Block> getBlocks() {
        return blocks;
    }
}
