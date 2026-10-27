package com.magneticraft2.client.hologram;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.magneticraft2.client.animation.AnimationResourceLoader;
import com.magneticraft2.common.hologram.RecordingTimeline;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Validates recording resources on resource reload, never on the render thread.
 * This is a client-only resource-pack format; no gameplay blocks are created.
 *
 * Register this listener alongside AnimationResourceLoader during the client
 * resource-reload lifecycle before wiring a projector renderer to it.
 */
public final class HologramRecordingResourceLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Map<ResourceLocation, Recording> recordings = Map.of();

    /** Client-side resource-pack controlled projection material and timings. */
    public record Appearance(float red, float green, float blue,
                             float opacity, int fadeInTicks, int fadeOutTicks,
                             float flicker, boolean preserveSourceTexture) {}

    private static final Appearance DEFAULT_APPEARANCE =
            new Appearance(0.38F, 0.90F, 1.0F, 0.32F, 24, 36, 0.28F, true);

    public record Recording(String archive, String title,
                            ResourceLocation animation, int durationTicks,
                            int offsetX, int offsetY, int offsetZ,
                            Map<String, RecordingTimeline.Track> channels,
                            Appearance appearance) {
        public Recording {
            channels = Map.copyOf(channels);
        }

        public RecordingTimeline.Frame sample(double elapsedTicks) {
            return RecordingTimeline.sample(channels, elapsedTicks);
        }
    }

    public HologramRecordingResourceLoader() {
        super(new Gson(), "holograms");
    }

    public static Recording get(ResourceLocation id) {
        return recordings.get(id);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Recording> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                Recording recording = parse(entry.getValue());
                if (AnimationResourceLoader.get(recording.animation()) == null) {
                    // Reload listener ordering may not yet have published animation
                    // definitions; the playback renderer must resolve at draw time.
                    LOGGER.debug("Recording {} references animation {} (resolved at playback)",
                            entry.getKey(), recording.animation());
                }
                loaded.put(entry.getKey(), recording);
            } catch (RuntimeException ex) {
                LOGGER.warn("Rejected hologram recording {}: {}",
                        entry.getKey(), ex.getMessage());
            }
        }
        recordings = Map.copyOf(loaded);
        LOGGER.info("Loaded {} hologram recordings", recordings.size());
    }

    public static Recording parse(JsonElement element) {
        JsonObject root = object(element, "recording");
        if (!"magneticraft2:hologram_recording".equals(text(root, "format"))
                || number(root, "format_version") != 1) {
            throw new JsonParseException("Unsupported hologram format/version");
        }
        String archive = text(root, "archive");
        String title = text(root, "title");
        if (archive.length() > 32 || title.length() > 128) {
            throw new JsonParseException("Recording metadata too long");
        }
        ResourceLocation animation = ResourceLocation.tryParse(text(root, "animation"));
        if (animation == null) throw new JsonParseException("Invalid animation ID");
        int duration = number(root, "duration_ticks");
        if (duration < 1 || duration > 12000) {
            throw new JsonParseException("Duration must be 1..12000 ticks");
        }
        JsonObject placement = object(root.get("placement"), "placement");
        if (!"south".equals(text(placement, "reference_facing"))) {
            throw new JsonParseException("Recording must use canonical south coordinates");
        }
        JsonArray offset = array(placement.get("offset"), "offset");
        if (offset.size() != 3) throw new JsonParseException("offset requires three integers");
        int[] xyz = new int[3];
        for (int i = 0; i < 3; i++) {
            xyz[i] = integer(offset.get(i), "offset");
            if (Math.abs(xyz[i]) > 64) throw new JsonParseException("offset outside +/-64 blocks");
        }

        JsonObject inputs = object(root.get("channels"), "channels");
        if (inputs.size() < 1 || inputs.size() > 32) {
            throw new JsonParseException("Expected 1..32 animation channels");
        }
        Map<String, RecordingTimeline.Track> channels = new HashMap<>();
        for (Map.Entry<String, JsonElement> input : inputs.entrySet()) {
            if (!input.getKey().matches("[a-z][a-z0-9_.]{0,127}")) {
                throw new JsonParseException("Invalid channel name " + input.getKey());
            }
            JsonArray keyframes = array(input.getValue(), input.getKey());
            List<RecordingTimeline.Keyframe> frames = new ArrayList<>();
            for (JsonElement keyframe : keyframes) {
                JsonObject key = object(keyframe, "keyframe");
                float angle = finiteFloat(key, "angle_degrees");
                frames.add(new RecordingTimeline.Keyframe(
                        number(key, "tick"), angle,
                        bool(key, "running"), bool(key, "overloaded")));
            }
            channels.put(input.getKey(), new RecordingTimeline.Track(duration, frames));
        }
        return new Recording(archive, title, animation, duration,
                xyz[0], xyz[1], xyz[2], channels, parseAppearance(root));
    }

    private static Appearance parseAppearance(JsonObject root) {
        if (!root.has("appearance")) return DEFAULT_APPEARANCE;
        JsonObject json = object(root.get("appearance"), "appearance");
        float red = DEFAULT_APPEARANCE.red();
        float green = DEFAULT_APPEARANCE.green();
        float blue = DEFAULT_APPEARANCE.blue();
        if (json.has("tint")) {
            JsonArray tint = array(json.get("tint"), "appearance.tint");
            if (tint.size() != 3) {
                throw new JsonParseException("appearance.tint requires RGB values");
            }
            red = normalized(tint.get(0), "appearance.tint[0]");
            green = normalized(tint.get(1), "appearance.tint[1]");
            blue = normalized(tint.get(2), "appearance.tint[2]");
        }

        float opacity = json.has("opacity")
                ? normalized(json.get("opacity"), "appearance.opacity")
                : DEFAULT_APPEARANCE.opacity();
        int fadeIn = json.has("fade_in_ticks")
                ? number(json, "fade_in_ticks") : DEFAULT_APPEARANCE.fadeInTicks();
        int fadeOut = json.has("fade_out_ticks")
                ? number(json, "fade_out_ticks") : DEFAULT_APPEARANCE.fadeOutTicks();
        if (fadeIn < 0 || fadeIn > 200 || fadeOut < 0 || fadeOut > 200) {
            throw new JsonParseException("appearance fade timings must be 0..200 ticks");
        }
        float flicker = json.has("flicker")
                ? normalized(json.get("flicker"), "appearance.flicker")
                : DEFAULT_APPEARANCE.flicker();
        if (flicker > 0.40F) {
            throw new JsonParseException("appearance.flicker must be <= 0.40");
        }
        boolean preserveSourceTexture = json.has("preserve_texture")
                ? bool(json, "preserve_texture")
                : DEFAULT_APPEARANCE.preserveSourceTexture();
        return new Appearance(red, green, blue, opacity,
                fadeIn, fadeOut, flicker, preserveSourceTexture);
    }

    private static float normalized(JsonElement value, String field) {
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(field + " must be numeric");
        }
        double n = value.getAsDouble();
        if (!Double.isFinite(n) || n < 0D || n > 1D) {
            throw new JsonParseException(field + " must be a finite value from 0 to 1");
        }
        return (float) n;
    }

    private static JsonObject object(JsonElement value, String name) {
        if (value == null || !value.isJsonObject()) {
            throw new JsonParseException(name + " must be an object");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray array(JsonElement value, String name) {
        if (value == null || !value.isJsonArray()) {
            throw new JsonParseException(name + " must be an array");
        }
        return value.getAsJsonArray();
    }

    private static String text(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new JsonParseException(field + " must be a string");
        }
        return value.getAsString();
    }

    private static int number(JsonObject object, String field) {
        return integer(object.get(field), field);
    }

    private static int integer(JsonElement value, String field) {
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(field + " must be an integer");
        }
        double n = value.getAsDouble();
        if (!Double.isFinite(n) || n != Math.rint(n) || n < Integer.MIN_VALUE
                || n > Integer.MAX_VALUE) {
            throw new JsonParseException(field + " must be a finite integer");
        }
        return (int) n;
    }

    private static float finiteFloat(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(field + " must be numeric");
        }
        float n = value.getAsFloat();
        if (!Float.isFinite(n) || Math.abs(n) > 1000000F) {
            throw new JsonParseException(field + " outside finite angle range");
        }
        return n;
    }

    private static boolean bool(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException(field + " must be boolean");
        }
        return value.getAsBoolean();
    }
}
