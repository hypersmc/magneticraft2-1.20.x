package com.magneticraft2.common.registry;

import net.minecraft.world.food.FoodProperties;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class ModFoods {
    public static final FoodProperties rice = (new FoodProperties.Builder())
            .fast()
            .nutrition(1)
            .saturationMod(0.2F)
            .build();
}
