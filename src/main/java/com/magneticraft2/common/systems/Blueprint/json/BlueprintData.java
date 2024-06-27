package com.magneticraft2.common.systems.Blueprint.json;

import net.minecraft.world.level.block.Block;

import java.util.Map;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */

public class BlueprintData {
    public final String name;
    public final String owner;
    public final BlueprintStructure structure;
    public final Map<String, Block> blocks;

    public BlueprintData(String name, String owner, BlueprintStructure structure, Map<String, Block> blocks){
        this.name = name;
        this.owner = owner;
        this.structure = structure;
        this.blocks = blocks;
    }
    public String getName(){
        return name;
    }
    public String getOwner() {
        return owner;
    }
    public BlueprintStructure getStructure() {
        return structure;
    }
    public Map<String, Block> getBlocks() {
        return blocks;
    }

}
