package com.magneticraft2.common.systems.Multiblocking.core;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Static Steam I model palette selected once on multiblock formation. */
public final class SteamEngineModelParts {
    private SteamEngineModelParts() {}

    public static String key(Direction facing,int dx,int dy,int dz,BlockState original){
        if(!facing.getAxis().isHorizontal())
            throw new IllegalArgumentException("Steam Engine facing must be horizontal");

        // The formed engine is one coherent machine split into block-local baked
        // fragments. Construction materials do not each become a repeated visual
        // post/slab; their cells select the fragment occupying that exact position.
        // Missing fragment ids intentionally render empty through MultiBlockModel.
        return "steam_engine/" + facing.getName()
                + "/part_" + dx + "_" + dy + "_" + dz;
    }
}
