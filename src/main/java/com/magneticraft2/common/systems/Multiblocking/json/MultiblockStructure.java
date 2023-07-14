package com.magneticraft2.common.systems.Multiblocking.json;

import com.magneticraft2.common.systems.Multiblocking.core.MultiblockModule;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockStructure {
    private final int[] dimensions;
    private final Map<String, List<List<String>>> layout;
    private final Map<String, Block> blocks;
    private final Map<String, MultiblockModule> modules;


    public MultiblockStructure(int[] dimensions, Map<String, List<List<String>>> layout, Map<String, Block> blocks) {
        this.dimensions = dimensions;
        this.layout = layout;
        this.blocks = blocks;
        this.modules = new HashMap<>();
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
    public Collection<MultiblockModule> getModules() {
        return modules.values();
    }
}