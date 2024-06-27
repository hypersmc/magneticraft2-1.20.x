package com.magneticraft2.common.utils;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * @author JumpWatch on 06-01-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Magneticraft2ConfigServer {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final General GENERAL = new General(BUILDER);
    public static class General {
        public final ForgeConfigSpec.ConfigValue<Boolean> DevMode;
        General(ForgeConfigSpec.Builder builder){
            builder.push("Server configuration for Magneticraft2");
            DevMode = builder
                    .comment("Dev mode for the mod. This enables functions that are not intended for the normal end user like debug output from most things in the mod.")
                    .translation("magneticraft2.devmode.config")
                    .define("Dev mode", false);
            builder.pop();
        }
    }
    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
