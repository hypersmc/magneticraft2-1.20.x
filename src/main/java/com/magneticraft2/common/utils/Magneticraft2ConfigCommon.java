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
        public final ForgeConfigSpec.ConfigValue<Integer> FoundryLadleHotTicks;
        public final ForgeConfigSpec.ConfigValue<Integer> FoundryCastingCoolTicks;
        public final ForgeConfigSpec.ConfigValue<Integer> KilnHeatCapacity;
        public final ForgeConfigSpec.ConfigValue<Integer> KilnHeatPerFuelTick;
        public final ForgeConfigSpec.ConfigValue<Integer> KilnPassiveHeatLoss;
        public final ForgeConfigSpec.ConfigValue<Integer> KilnFiringThreshold;
        public final ForgeConfigSpec.ConfigValue<Integer> KilnFiringHeatPerTick;
        public final ForgeConfigSpec.ConfigValue<Integer> HearthHeatCapacity;
        public final ForgeConfigSpec.ConfigValue<Integer> HearthHeatPerFuelTick;
        public final ForgeConfigSpec.ConfigValue<Integer> HearthPassiveHeatLoss;
        public final ForgeConfigSpec.ConfigValue<Integer> HearthHeatingThreshold;
        public final ForgeConfigSpec.ConfigValue<Integer> HearthSteelHeatingThreshold;
        public final ForgeConfigSpec.ConfigValue<Integer> HearthHeatingHeatPerTick;
        // Provisional high-temperature ore-roasting balance in HEAT units.
        public final ForgeConfigSpec.ConfigValue<Integer> RoasterHeatCapacity;
        public final ForgeConfigSpec.ConfigValue<Integer> RoasterHeatPerFuelTick;
        public final ForgeConfigSpec.ConfigValue<Integer> RoasterPassiveHeatLoss;
        public final ForgeConfigSpec.ConfigValue<Integer> RoasterRoastingThreshold;
        public final ForgeConfigSpec.ConfigValue<Integer> RoasterHeatPerProcessingTick;
        public final ForgeConfigSpec.DoubleValue SteamPipeRuptureKPa;

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
            FoundryLadleHotTicks = builder
                    .comment("How many loaded-world ticks a ladle remains pourable; if it cools, sneak-use in the air to recover the original bronze ingot")
                    .defineInRange("ladleHotTicks", 1200, 20, 72000);
            FoundryCastingCoolTicks = builder
                    .comment("Loaded server ticks between pouring one bronze unit into a sand mold and obtaining a rough casting")
                    .defineInRange("castingCoolTicks", 200, 1, 72000);
            builder.pop();
            builder.push("Refractory Kiln (provisional K2, Magneticraft HEAT units)");
            KilnHeatCapacity = builder.comment("Kiln stored HEAT (not Celsius)")
                    .defineInRange("heatCapacity", 32000, 1, 1000000);
            KilnHeatPerFuelTick = builder.comment("HEAT generated per burning tick")
                    .defineInRange("heatPerFuelTick", 24, 1, 100000);
            KilnPassiveHeatLoss = builder.comment("HEAT lost per loaded tick")
                    .defineInRange("passiveHeatLoss", 1, 0, 100000);
            KilnFiringThreshold = builder.comment("Minimum HEAT to fire; <= heatCapacity")
                    .defineInRange("firingThreshold", 9000, 1, 1000000);
            KilnFiringHeatPerTick = builder.comment("HEAT consumed per firing tick")
                    .defineInRange("firingHeatPerTick", 6, 1, 100000);
            builder.pop();
            builder.push("Iron Forge Hearth (provisional batch reheating, HEAT units)");
            HearthHeatCapacity = builder.comment("Stored thermal HEAT; not degrees Celsius")
                    .defineInRange("heatCapacity", 36000, 1, 1000000);
            HearthHeatPerFuelTick = builder.comment("HEAT per solid fuel burn tick")
                    .defineInRange("heatPerFuelTick", 36, 1, 100000);
            HearthPassiveHeatLoss = builder.comment("Passive HEAT loss per loaded tick")
                    .defineInRange("passiveHeatLoss", 2, 0, 100000);
            HearthHeatingThreshold = builder.comment("HEAT threshold for wrought batches")
                    .defineInRange("heatingThreshold", 12000, 1, 1000000);
            HearthSteelHeatingThreshold = builder.comment("Minimum HEAT for Steel stock batches (must be <= Hearth capacity)")
                    .defineInRange("steelHeatingThreshold", 18000, 1, 1000000);
            HearthHeatingHeatPerTick = builder.comment("HEAT consumed by batch per tick")
                    .defineInRange("heatingHeatPerTick", 16, 1, 100000);
            builder.pop();
            builder.push("Ore Roasting Kiln (provisional K2, Magneticraft HEAT units)");
            RoasterHeatCapacity = builder.comment("Maximum brick thermal mass HEAT, not degrees Celsius")
                    .defineInRange("heatCapacity", 48000, 1, 1000000);
            RoasterHeatPerFuelTick = builder.comment("HEAT from one tick of solid-fuel burning")
                    .defineInRange("heatPerFuelTick", 32, 1, 100000);
            RoasterPassiveHeatLoss = builder.comment("HEAT lost per loaded tick")
                    .defineInRange("passiveHeatLoss", 2, 0, 100000);
            RoasterRoastingThreshold = builder.comment("Minimum HEAT for roasting; cannot exceed heatCapacity")
                    .defineInRange("roastingThreshold", 16000, 1, 1000000);
            RoasterHeatPerProcessingTick = builder.comment("HEAT consumed per active roasting tick")
                    .defineInRange("heatPerProcessingTick", 10, 1, 100000);
            builder.pop();
            builder.push("Steam pipe faults (provisional balance, steam stage not yet playable)");
            SteamPipeRuptureKPa = builder
                    .comment("A steam pipe ruptures only if its shared network pressure strictly exceeds this rating. No ordinary wear or random failures below it. Provisional gameplay kPa.")
                    .defineInRange("rupturePressureKPa", 600.0, 10.0, 1000000.0);
            builder.pop();
            builder.pop();
        }


    }
    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
