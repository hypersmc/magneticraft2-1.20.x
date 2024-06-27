package com.magneticraft2.common.utils;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * @author JumpWatch on 06-01-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Magneticraft2ConfigClient {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final General GENERAL = new General(BUILDER);
    public static class General {
        public final ForgeConfigSpec.ConfigValue<Boolean> DevMode;
        public final ForgeConfigSpec.ConfigValue<Boolean> blueprintmakerrenderer;
        General(ForgeConfigSpec.Builder builder){
            builder.push("Client configuration for Magneticraft2");
            builder.comment("We are mostly going to have settings for rendering of blocks and such.");
            DevMode = builder
                    .comment("Dev mode for the mod. This enables functions that are not intended for the normal end user like debug output from most things in the mod.")
                    .translation("magneticraft2.devmode.config")
                    .define("Dev mode", false);
            blueprintmakerrenderer = builder
                    .comment("This is where you can decide if you want to have the Blueprint Maker block renderer the 2 block positions on top of it. ")
                    .comment("Default: true")
                    .translation("magneticraft2.blueprintmaker.config")
                    .define("blueprintmaker", true);
            builder.pop();
        }
    }
    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
