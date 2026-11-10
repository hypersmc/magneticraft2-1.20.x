package com.magneticraft2.common.systems.Multiblocking.core;

import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Static Steam I model palette selected once on multiblock formation. */
public final class SteamEngineModelParts {
    private SteamEngineModelParts() {}

    public static String key(Direction facing,int dx,int dy,int dz,BlockState original){
        if(!facing.getAxis().isHorizontal())
            throw new IllegalArgumentException("Steam Engine facing must be horizontal");
        if(original.isAir())return "steam_engine/empty";
        if(original.is(BlockRegistry.IRON_MACHINE_FOUNDATION.get()))
            return "steam_engine/foundation";
        if(original.is(BlockRegistry.IRON_STRUCTURAL_BEAM.get()))
            return "steam_engine/beam";

        int cx=switch(facing){
            case NORTH->-dx;case EAST->-dz;case WEST->dz;default->dx;
        };
        int cz=switch(facing){
            case NORTH->-dz;case EAST->dx;case WEST->-dx;default->dz;
        };
        if(original.is(BlockRegistry.IRON_PLATE_PANEL.get())
                && cx>=-1&&cx<=1&&dy>=1&&dy<=2&&cz>=4&&cz<=6)
            return "steam_engine/cylinder_"+facing.getName()+"_"+(cx+1)+"_"+dy+"_"+(cz-4);
        throw new IllegalStateException("Unexpected Steam Engine formed part at "
                +dx+","+dy+","+dz+" from "+original);
    }
}
