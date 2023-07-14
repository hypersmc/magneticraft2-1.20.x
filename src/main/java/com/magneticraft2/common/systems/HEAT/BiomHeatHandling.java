package com.magneticraft2.common.systems.HEAT;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BiomHeatHandling {
    public static int getHeatManagment(Level world, BlockPos pos, String purpose) {
        if (world.getBiomeManager().getBiome(pos).is(new ResourceLocation("desert"))){
            switch (purpose) {
                case "start":
                    return 35;
                case "lose":
                    return 25;
                case "losetick":
                    return 1;
                case "gain":
                    return 4;
                case "min":
                    return 35;
            }
        }
        if (world.getBiomeManager().getBiome(pos).is(new ResourceLocation("desert_lakes"))){
            switch (purpose) {
                case "start":
                    return 35;
                case "lose":
                    return 25;
                case "losetick":
                    return 1;
                case "gain":
                    return 4;
                case "min":
                    return 35;
            }
        }
        if (world.getBiomeManager().getBiome(pos).is(new ResourceLocation("desert_hills"))){
            switch (purpose) {
                case "start":
                    return 35;
                case "lose":
                    return 25;
                case "losetick":
                    return 1;
                case "gain":
                    return 4;
                case "min":
                    return 35;
            }
        }
        if (world.getBiomeManager().getBiome(pos).is(new ResourceLocation("cold_ocean"))){
            switch (purpose) {
                case "start":
                    return 0;
                case "lose":
                    return 40;
                case "losetick":
                    return 5;
                case "gain":
                    return 2;
                case "min":
                    return 1;
            }
        }
        if (world.getBiomeManager().getBiome(pos).is(new ResourceLocation("frozen_ocean"))){
            switch (purpose) {
                case "start":
                    return 0;
                case "lose":
                    return 40;
                case "losetick":
                    return 5;
                case "gain":
                    return 2;
                case "min":
                    return 1;
            }
        }
        switch (purpose) {
            case "start":
                return 0;
            case "lose":
                return 5;
            case "losetick":
                return 1;
            case "gain":
                return 1;
            case "min":
                return 0;
        }
        return 0;
    }
}
