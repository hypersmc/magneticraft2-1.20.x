package com.magneticraft2.common.hologram;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-authoritative recording catalog. Resource packs own the appearance,
 * datapacks own the playback duration and identity/availability.
 *
 * <p>Loaded at server startup and /reload. Existing projectors latch a copy of
 * durationTicks when placed; edits to a datapack never modify a recording
 * already playing or break a saved world.
 */
public final class HologramRecordingCatalog extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TICKS = 12000;

    public static final ResourceLocation DEFAULT_RECORDING =
            new ResourceLocation("magneticraft2", "mechanical_transmission");
    private static final RecordingInfo LEGACY_DEFAULT =
            new RecordingInfo("017", "Mechanical Transmission", 900);
    private static volatile Map<ResourceLocation, RecordingInfo> recordings = Map.of();

    public record RecordingInfo(String archive, String title, int durationTicks) {
        public RecordingInfo {
            if (archive == null || archive.isBlank() || archive.length() > 32
                    || title == null || title.isBlank() || title.length() > 128
                    || durationTicks < 1 || durationTicks > MAX_TICKS) {
                throw new IllegalArgumentException("Invalid hologram recording metadata");
            }
        }
    }

    public HologramRecordingCatalog() {
        super(new Gson(), "hologram_recordings");
    }

    public static RecordingInfo get(ResourceLocation id) {
        return recordings.get(id);
    }

    public static RecordingInfo getOrFallback(ResourceLocation id) {
        RecordingInfo configured = recordings.get(id);
        if (configured != null) return configured;
        // Old creative items / worlds from the first prototype still work
        // even if the datapack was removed. Unknown archive IDs remain intact.
        return LEGACY_DEFAULT;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, RecordingInfo> parsed = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            if (!"magneticraft2".equals(entry.getKey().getNamespace())) {
                if (!entry.getValue().isJsonObject()
                        || !entry.getValue().getAsJsonObject().has("format")) continue;
            }
            try {
                parsed.put(entry.getKey(), parse(entry.getValue()));
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping invalid hologram recording metadata {}: {}",
                        entry.getKey(), e.getMessage());
            }
        }
        recordings = Map.copyOf(parsed);
        LOGGER.info("Loaded {} server hologram recording definitions", recordings.size());
    }

    public static RecordingInfo parse(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new JsonParseException("Recording must be an object");
        }
        JsonObject json = element.getAsJsonObject();
        if (!string(json, "format").equals("magneticraft2:hologram_recording")
                || integer(json, "format_version") != 1) {
            throw new JsonParseException("Invalid hologram recording format/version");
        }
        return new RecordingInfo(string(json, "archive"), string(json, "title"),
                integer(json, "duration_ticks"));
    }

    private static String string(JsonObject json, String name) {
        JsonElement value = json.get(name);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new JsonParseException(name + " must be a string");
        }
        return value.getAsString();
    }

    private static int integer(JsonObject json, String name) {
        JsonElement value = json.get(name);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(name + " must be an integer");
        }
        double result = value.getAsDouble();
        if (!Double.isFinite(result) || result != Math.rint(result)
                || result < Integer.MIN_VALUE || result > Integer.MAX_VALUE) {
            throw new JsonParseException(name + " must be a finite integer");
        }
        return (int) result;
    }
}
