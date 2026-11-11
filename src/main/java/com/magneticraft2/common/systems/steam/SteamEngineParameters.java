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

@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SteamEngineParameters extends SimpleJsonResourceReloadListener {
    private static final ResourceLocation ID =
            new ResourceLocation(magneticraft2.MOD_ID, "default");
    @Nullable private static volatile Rules current;

    public record Rules(double minimumPressureKPa, double ratedPressureKPa,
            float minimumRpm, float ratedRpm, float ratedTorque,
            int idleMilliSteamPerTick, int loadMilliSteamPerTorqueTick,
            int maxDrawUnitsPerTick) {}

    public SteamEngineParameters() { super(new Gson(), "steam_engine"); }
    @Nullable public static Rules rules() { return current; }

    @SubscribeEvent public static void register(AddReloadListenerEvent event) {
        event.addListener(new SteamEngineParameters());
    }

    @Override protected void apply(Map<ResourceLocation, JsonElement> entries,
            ResourceManager resources, ProfilerFiller profiler) {
        current = null;
        try {
            JsonElement entry = entries.get(ID);
            if (entry == null) throw new IllegalArgumentException("Missing magneticraft2:default");
            JsonObject j = entry.getAsJsonObject();
            int format = integer(j, "format", 1, 1);
            double minimum = number(j, "minimum_pressure_kpa", 1, 1_000_000);
            double rated = number(j, "rated_pressure_kpa", minimum + 0.001, 1_000_000);
            float minimumRpm = (float) number(j, "minimum_rpm", 0.1, 1000);
            float ratedRpm = (float) number(j, "rated_rpm", minimumRpm, 1000);
            float ratedTorque = (float) number(j, "rated_torque", 0.1, 1000);
            int idle = integer(j, "idle_millisteam_per_tick", 0, 1000);
            int load = integer(j, "load_millisteam_per_torque_tick", 0, 1000);
            int draw = integer(j, "max_draw_units_per_tick", 1, 64);
            current = new Rules(minimum, rated, minimumRpm, ratedRpm,
                    ratedTorque, idle, load, draw);
        } catch (RuntimeException invalid) {
            LogManager.getLogger("Magneticraft2SteamEngine").error(
                    "Steam Engine disabled: invalid steam_engine/default.json: {}",
                    invalid.getMessage());
        }
    }

    private static int integer(JsonObject j,String key,int min,int max) {
        JsonElement e=j.get(key);
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Expected integer "+key);
        int v=e.getAsBigDecimal().intValueExact();
        if(v<min||v>max)throw new IllegalArgumentException("Out-of-range "+key);
        return v;
    }
    private static double number(JsonObject j,String key,double min,double max) {
        JsonElement e=j.get(key);
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Expected number "+key);
        double v=e.getAsDouble();
        if(!Double.isFinite(v)||v<min||v>max)
            throw new IllegalArgumentException("Out-of-range "+key);
        return v;
    }
}
