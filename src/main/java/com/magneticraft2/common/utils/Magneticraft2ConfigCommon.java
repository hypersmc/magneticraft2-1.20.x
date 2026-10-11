package com.magneticraft2.common.utils;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * @author JumpWatch on 30-06-2023
 * @Project mgc2-1.20
* @version 1.0.0
 */

public class Magneticraft2ConfigCommon {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final General GENERAL = new General(BUILDER);




    public static class General {
        public final ForgeConfigSpec.ConfigValue<Integer> MaxWireLenght;
        public final ForgeConfigSpec.ConfigValue<Integer> PitKilnTime;
        public final ForgeConfigSpec.ConfigValue<Boolean> DevMode;
        public final ForgeConfigSpec.ConfigValue<Integer> WasherBeltLifetime;
        public final ForgeConfigSpec.ConfigValue<Integer> SifterMeshLifetime;
        // Foundry energy uses Magneticraft HEAT units, not real-world degrees.
        public final ForgeConfigSpec.ConfigValue<Integer> FoundryHeatCapacity;
        public final ForgeConfigSpec.ConfigValue<Integer> FoundryHeatPerFuelTick;
        public final ForgeConfigSpec.ConfigValue<Integer> FoundryPassiveHeatLoss;
        public final ForgeConfigSpec.ConfigValue<Integer> FoundryMeltThreshold;
        public final ForgeConfigSpec.ConfigValue<Integer> FoundrySolidificationThreshold;
        public final ForgeConfigSpec.ConfigValue<Integer> FoundryHeatCostPerIngot;
        public final ForgeConfigSpec.ConfigValue<Integer> FoundryCrucibleIngotCapacity;
        General(ForgeConfigSpec.Builder builder) {
            builder.push("Common configuration for Magneticraft2");
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
            // One wear cycle is one completed recipe, not one server tick.
            // The values are early balancing defaults and can be tuned per pack.
            WasherBeltLifetime = builder
                    .comment("Completed Ore Washer batches before its leather drive belt needs replacing")
                    .defineInRange("Washer belt batches", 320, 1, Integer.MAX_VALUE);
            SifterMeshLifetime = builder
                    .comment("Completed Mechanical Sifter batches before its screen mesh needs replacing")
                    .defineInRange("Sifter mesh batches", 256, 1, Integer.MAX_VALUE);
            builder.push("Bronze Foundry (early balance values, Magneticraft HEAT units)");
            FoundryHeatCapacity = builder
                    .comment("Maximum stored thermal energy in the brick mass; NOT degrees Celsius")
                    .defineInRange("heatCapacity", 32000, 1, 1000000);
            FoundryHeatPerFuelTick = builder
                    .comment("HEAT energy added per tick of burning vanilla solid fuel")
                    .defineInRange("heatPerBurnTick", 32, 1, 100000);
            FoundryPassiveHeatLoss = builder
                    .comment("HEAT energy dissipated every loaded server tick")
                    .defineInRange("heatLossPerTick", 2, 0, 100000);
            FoundryMeltThreshold = builder
                    .comment("Minimum stored HEAT energy to begin melting a bronze ingot; keep at or below heatCapacity")
                    .defineInRange("meltThreshold", 14000, 1, 1000000);
            FoundrySolidificationThreshold = builder
                    .comment("Below this HEAT amount stored molten bronze solidifies in place. Reheating requires the higher melt threshold.")
                    .defineInRange("solidificationThreshold", 7000, 0, 1000000);
            FoundryHeatCostPerIngot = builder
                    .comment("HEAT energy consumed when one bronze ingot changes from charge to crucible metal")
                    .defineInRange("heatPerIngot", 2500, 1, 1000000);
            FoundryCrucibleIngotCapacity = builder
                    .comment("Crucible capacity in bronze ingot-equivalents. Conversion to a fluid unit is not decided.")
                    .defineInRange("crucibleIngots", 8, 1, 64);
            builder.pop();
            builder.pop();
        }


    }
    public static final ForgeConfigSpec SPEC = BUILDER.build();
}