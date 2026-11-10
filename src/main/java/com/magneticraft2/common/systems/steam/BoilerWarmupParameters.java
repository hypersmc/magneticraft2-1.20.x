package com.magneticraft2.common.systems.steam;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.magneticraft2.common.magneticraft2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;
import java.util.Map;

/**
 * Data-pack boiler balance. Format 1 keeps old warm-up working with Steam
 * disabled; only format 2 explicitly opts into phase conversion/output.
 */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoilerWarmupParameters extends SimpleJsonResourceReloadListener {
    private static final ResourceLocation ID = new ResourceLocation(magneticraft2.MOD_ID, "default");
    @Nullable private static volatile Rules current;
    public record Rules(int waterCapacity, int heatCapacity, int heatPerBurnTick,
            int passiveLoss, boolean steamEnabled, int boilThresholdHeat,
            int steamWaterPerUnit, int steamHeatPerUnit, int steamBufferCapacity,
            int maxPhaseUnitsPerTick, int maxOutputUnitsPerTick,
            double steamTemperatureK, double outputCutoffKPa) { }

    public BoilerWarmupParameters() { super(new Gson(), "boiler_warmup"); }
    @Nullable public static Rules rules() { return current; }
    @SubscribeEvent public static void register(AddReloadListenerEvent event) {
        event.addListener(new BoilerWarmupParameters());
    }

    @Override protected void apply(Map<ResourceLocation, JsonElement> entries,
            ResourceManager resources, ProfilerFiller profiler) {
        current = null;
        try {
            JsonElement entry = entries.get(ID);
            if (entry == null) throw new IllegalArgumentException("Missing magneticraft2:default");
            JsonObject j = entry.getAsJsonObject();
            int format = integer(j, "format", 1, 2);
            int water = integer(j, "water_capacity_mb", 1, BoilerWarmupState.MAX_AMOUNT);
            int heat = integer(j, "heat_capacity", 1, BoilerWarmupState.MAX_AMOUNT);
            int gain = integer(j, "heat_per_burn_tick", 1, BoilerWarmupState.MAX_HEAT_PER_TICK);
            int loss = integer(j, "passive_heat_loss", 0, BoilerWarmupState.MAX_HEAT_PER_TICK);
            if (format == 1) {
                current = new Rules(water, heat, gain, loss, false,
                        0, 0, 0, 0, 0, 0, 453.15, 0);
                return;
            }
            int threshold = integer(j, "boil_threshold_heat", 1, heat);
            int waterPer = integer(j, "steam_water_per_unit_mb", 1, water);
            int heatPer = integer(j, "steam_heat_per_unit", 1, heat);
            int buffer = integer(j, "steam_buffer_capacity_units", 1, BoilerSteamState.MAX_UNITS);
            int phase = integer(j, "max_phase_units_per_tick", 1, 64);
            int output = integer(j, "max_output_units_per_tick", 1, 64);
            double temperature = number(j, "steam_temperature_k", 373.15, 2000.0);
            double cutoff = number(j, "output_cutoff_kpa", 1.0, 1_000_000.0);
            current = new Rules(water, heat, gain, loss, true, threshold,
                    waterPer, heatPer, buffer, phase, output, temperature, cutoff);
        } catch (RuntimeException invalid) {
            LogManager.getLogger("Magneticraft2Boiler").error(
                    "Boiler warm-up disabled: invalid boiler_warmup/default.json: {}",
                    invalid.getMessage());
        }
    }

    private static int integer(JsonObject j, String key, int min, int max) {
        JsonElement e = j.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Expected integer " + key);
        int value = e.getAsBigDecimal().intValueExact();
        if (value < min || value > max)
            throw new IllegalArgumentException("Out-of-range " + key);
        return value;
    }
    private static double number(JsonObject j, String key, double min, double max) {
        JsonElement e = j.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Expected number " + key);
        double value = e.getAsDouble();
        if (!Double.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException("Out-of-range " + key);
        return value;
    }
}
