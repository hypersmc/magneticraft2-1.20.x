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
        public final ForgeConfigSpec.ConfigValue<Integer> MaxWireLenght;
        public final ForgeConfigSpec.ConfigValue<Integer> PitKilnTime;
        public final ForgeConfigSpec.ConfigValue<Boolean> DevMode;
        General(ForgeConfigSpec.Builder builder) {
            builder.push("Configuration for Magneticraft2");
            MaxWireLenght = builder
                    .comment("The max length Wires can go")
                    .translation("magneticraft2.wirelenght.config")
                    .defineInRange("Wire Length", 64, 0, Integer.MAX_VALUE);
            PitKilnTime = builder
                    .comment("Burntime for the pitklin (In ticks). Default value is 2400.")
                    .translation("magneticraft2.pitklinburntime.config")
                    .defineInRange("pitklin burntime", 2400, 0, Integer.MAX_VALUE);
            DevMode = builder
                    .comment("Dev mode for the mod. This enables functions that are not intended for the normal end user like debug output from most things in the mod.")
                    .translation("magneticraft2.devmode.config")
                    .define("Dev mode", false);
            builder.pop();
        }


    }
    public static final ForgeConfigSpec SPEC = BUILDER.build();
}