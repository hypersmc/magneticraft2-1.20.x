package com.magneticraft2.common.systems.Multiblocking.core;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Temporary Steam I prototype integration. V2 fillers still own collision,
 * selection and teardown state; the reviewed free-mesh body is rendered by
 * the controller BER until the accepted fixed body is split back into V2.
 */
public final class SteamEngineModelParts {
    private SteamEngineModelParts() {}

    public static String key(Direction facing,int dx,int dy,int dz,BlockState original){
        if(!facing.getAxis().isHorizontal())
            throw new IllegalArgumentException("Steam Engine facing must be horizontal");
        return "steam_engine/empty";
    }
}
