package com.magneticraft2.common.systems.Multiblocking.core;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable baked part palette, keyed in canonical SOUTH coordinates. */
public final class SteelBoilerModelParts {
    private SteelBoilerModelParts(){}
    public static String key(Direction facing,int dx,int dy,int dz,BlockState original){
        int cx=switch(facing){case NORTH->-dx;case EAST->-dz;case WEST->dz;
            case SOUTH->dx;default->throw new IllegalArgumentException("Horizontal facing required");};
        int cz=switch(facing){case NORTH->-dz;case EAST->dx;case WEST->-dx;default->dz;};
        int x=cx+1,z=cz+4;
        if(original.is(BlockRegistry.IRON_MACHINE_FOUNDATION.get()))return "boiler_vessel/foundation";
        if(dy<1||dy>3||x<0||x>2||z<0||z>4)
            throw new IllegalStateException("Unexpected boiler part");
        if(original.is(BlockRegistry.BOILER_END_JOINT.get())){
            if(x!=1||dy!=2||(z!=0&&z!=4))throw new IllegalStateException("Misplaced end closure");
            return "boiler_vessel/cap_"+(z==4?"front_":"back_")+facing.getName();
        }
        String role=switch((dy-1)*3+x){
            case 0->"bottom_left";case 1->"bottom";case 2->"bottom_right";
            case 3->"left";case 5->"right";
            case 6->"top_left";case 7->"top";case 8->"top_right";
            default->throw new IllegalStateException("Boiler center must be hollow");};
        if(original.is(BlockRegistry.BOILER_SHELL_PANEL.get())&&z>=1&&z<=3)
            return "boiler_vessel/shell_"+role+"_"+facing.getName();
        if(original.is(BlockRegistry.IRON_PLATE_PANEL.get())&&(z==0||z==4))
            return "boiler_vessel/rim_"+(z==4?"front_":"back_")+role+"_"+facing.getName();
        throw new IllegalStateException("Unexpected boiler wall material");
    }
}
