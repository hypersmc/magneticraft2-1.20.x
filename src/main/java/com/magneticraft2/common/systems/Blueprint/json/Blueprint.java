package com.magneticraft2.common.systems.Blueprint.json;

import net.minecraft.world.level.block.Block;

import java.util.Map;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */

public class Blueprint {
    private final String name;
    private final BlueprintStructure structure;
    private final Map<String, Block> blocks;

    public Blueprint(String name, BlueprintStructure structure, Map<String, Block> blocks){
        this.name = name;
        this.structure = structure;
        this.blocks = blocks;
    }
    public String getName(){
        return name;
    }
    public BlueprintStructure getStructure() {
        return structure;
    }
    public Map<String, Block> getBlocks() {
        return blocks;
    }

}
