package com.magneticraft2.common.utils;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * @author JumpWatch on 30-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */

public class Magneticraft2ConfigCommon {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final General GENERAL = new General(BUILDER);




    public static class General {
        public  final ForgeConfigSpec.ConfigValue<Integer> MaxWireLenght;
        public  final ForgeConfigSpec.ConfigValue<Integer> PitKilnTime;
        //        public  final ForgeConfigSpec.ConfigValue<Double> EnderDamage;
        General(ForgeConfigSpec.Builder builder) {
            builder.push("Configuration for Magneticraft2");
            MaxWireLenght = builder
                    .comment("The max length Wires can go")
                    .translation("magneticraft2.wirelenght.config")
                    .defineInRange("Wire Length", 64, 0, Integer.MAX_VALUE);
            PitKilnTime = builder
                    .comment("The Cooldown before Stable Ender Pearl can be used again (In ticks). Default value is 240000.")
                    .translation("cuprum.endercooldown.config")
                    .defineInRange("Ender Cooldown", 240000, 0, Integer.MAX_VALUE);
//            EnderDamage = builder
//                    .comment("The damage the player takes when landing. Default value is 0.5.")
//                    .translation("cuprum.enderdamage.config")
//                    .defineInRange("Ender Damage", 0.5, 0.0, 20.0);
            builder.pop();
        }


    }
    public static final ForgeConfigSpec SPEC = BUILDER.build();
}