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

/** Data-pack settings for non-pressurized warm-up, not boiler pressure ratings. */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoilerWarmupParameters extends SimpleJsonResourceReloadListener {
    private static final ResourceLocation ID = new ResourceLocation(magneticraft2.MOD_ID, "default");
    @Nullable private static volatile Rules current;
    public record Rules(int waterCapacity, int heatCapacity, int heatPerBurnTick, int passiveLoss) { }

    public BoilerWarmupParameters() { super(new Gson(), "boiler_warmup"); }
    @Nullable public static Rules rules() { return current; }
    @SubscribeEvent public static void register(AddReloadListenerEvent event) {
        event.addListener(new BoilerWarmupParameters());
    }
    @Override protected void apply(Map<ResourceLocation, JsonElement> entries,
            ResourceManager resources, ProfilerFiller profiler) {
        current = null; // Broken/missing rules disable ignition rather than guessing values.
        try {
            JsonElement entry = entries.get(ID);
            if (entry == null) throw new IllegalArgumentException("Missing magneticraft2:default");
            JsonObject j = entry.getAsJsonObject();
            if (integer(j, "format", 1, 1) != 1) throw new IllegalArgumentException("Unknown format");
            current = new Rules(integer(j, "water_capacity_mb", 1, BoilerWarmupState.MAX_AMOUNT),
                    integer(j, "heat_capacity", 1, BoilerWarmupState.MAX_AMOUNT),
                    integer(j, "heat_per_burn_tick", 1, BoilerWarmupState.MAX_HEAT_PER_TICK),
                    integer(j, "passive_heat_loss", 0, BoilerWarmupState.MAX_HEAT_PER_TICK));
        } catch (RuntimeException invalid) {
            LogManager.getLogger("Magneticraft2Boiler").error(
                    "Boiler warm-up disabled: invalid boiler_warmup/default.json: {}", invalid.getMessage());
        }
    }
    private static int integer(JsonObject j, String key, int min, int max) {
        JsonElement e = j.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Expected integer " + key);
        int value = e.getAsBigDecimal().intValueExact();
        if (value < min || value > max) throw new IllegalArgumentException("Out-of-range " + key);
        return value;
    }
}
