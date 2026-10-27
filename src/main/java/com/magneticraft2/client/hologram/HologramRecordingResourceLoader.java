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
                             float flicker, boolean preserveSourceTexture,
                             boolean shaderEffect) {}

    private static final Appearance DEFAULT_APPEARANCE =
            new Appearance(0.38F, 0.90F, 1.0F, 0.32F, 24, 36, 0.28F, true, true);

    /**
     * Scene reconstruction moment: fade to zero at tick, swap the model variant
     * at that same tick, then fade the replacement back in. Never changes the
     * actual time or any physical block in the world.
     */
    public record Transition(int tick, int fadeTicks) {
        public float visibility(double elapsedTicks) {
            double distance = Math.abs(elapsedTicks - tick);
            if (distance >= fadeTicks) return 1F;
            double x = distance / fadeTicks;
            return (float) (x * x * (3D - 2D * x));
        }
    }

    /**
     * Audio belongs to the recorded film, not the mechanical simulation.
     * Each cue fires at most once per continuous client observation.
     */
    public record AudioCue(int tick, ResourceLocation sound, float volume, float pitch) {}

    /**
     * Bounded visual cue in scene-local block coordinates (canonical south).
     * Effects are CLIENT-ONLY and never produce actual electric power, damage
     * or other world changes. Duration is an inclusive visual time span.
     */
    public record VisualEvent(int tick, int durationTicks, int intervalTicks,
                              String kind, double x, double y, double z,
                              int count, float spread, float red, float green, float blue) {
        public int lastTick() {
            return tick + durationTicks;
        }
    }

    public record Recording(String archive, String title,
                            ResourceLocation animation, int durationTicks,
                            int offsetX, int offsetY, int offsetZ,
                            Map<String, RecordingTimeline.Track> channels,
                            Map<String, RecordingTimeline.DiscreteTrack> visualChannels,
                            List<Transition> transitions,
                            List<AudioCue> audioCues,
                            List<VisualEvent> visualEvents,
                            Appearance appearance) {
        public Recording {
            channels = Map.copyOf(channels);
            visualChannels = Map.copyOf(visualChannels);
            transitions = List.copyOf(transitions);
            audioCues = List.copyOf(audioCues);
            visualEvents = List.copyOf(visualEvents);
        }

        public RecordingTimeline.Frame sample(double elapsedTicks) {
            return RecordingTimeline.sample(channels, elapsedTicks);
        }

        public float visibility(double elapsedTicks) {
            float opacity = 1F;
            for (Transition transition : transitions) {
                opacity = Math.min(opacity, transition.visibility(elapsedTicks));
            }
            return opacity;
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
        // A resource pack may replace any animation/model/variant. Never
        // retain dimensions computed from the previous baked model set.
        HologramSceneBounds.clearCache();
        HologramRecordingValidator.clearCache();
        HologramModelDiagnostics.clearCache();
        HologramBufferSource.resetMissingSpriteWarning();
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
        Map<String, RecordingTimeline.DiscreteTrack> visualChannels =
                parseVisualChannels(root, duration);
        List<Transition> transitions = parseTransitions(root, duration);
        return new Recording(archive, title, animation, duration,
                xyz[0], xyz[1], xyz[2], channels, visualChannels,
                transitions, parseAudioCues(root, duration),
                parseVisualEvents(root, duration), parseAppearance(root));
    }

    /**
     * Discrete scene cues are NOT gameplay inputs: they only select whitelisted
     * model variants through PR #12's already-baked visual scene graph.
     */
    private static Map<String, RecordingTimeline.DiscreteTrack> parseVisualChannels(
            JsonObject root, int duration) {
        if (!root.has("visual_channels")) return Map.of();
        JsonObject inputs = object(root.get("visual_channels"), "visual_channels");
        if (inputs.size() > 16) {
            throw new JsonParseException("Too many visual_channels (max 16)");
        }
        Map<String, RecordingTimeline.DiscreteTrack> tracks = new HashMap<>();
        for (Map.Entry<String, JsonElement> input : inputs.entrySet()) {
            if (!input.getKey().matches("[a-z][a-z0-9_.]{0,127}")) {
                throw new JsonParseException("Invalid visual channel " + input.getKey());
            }
            JsonArray keyframes = array(input.getValue(), input.getKey());
            List<RecordingTimeline.DiscreteKeyframe> frames = new ArrayList<>();
            for (JsonElement element : keyframes) {
                JsonObject key = object(element, "visual keyframe");
                frames.add(new RecordingTimeline.DiscreteKeyframe(
                        number(key, "tick"), number(key, "value")));
            }
            tracks.put(input.getKey(), new RecordingTimeline.DiscreteTrack(duration, frames));
        }
        return Map.copyOf(tracks);
    }

    private static List<Transition> parseTransitions(JsonObject root, int duration) {
        if (!root.has("transitions")) return List.of();
        JsonArray input = array(root.get("transitions"), "transitions");
        if (input.size() > 32) {
            throw new JsonParseException("Too many scene transitions (max 32)");
        }
        List<Transition> transitions = new ArrayList<>();
        for (JsonElement element : input) {
            JsonObject t = object(element, "transition");
            int tick = number(t, "tick");
            int fadeTicks = number(t, "fade_ticks");
            if (fadeTicks < 1 || fadeTicks > 40 || tick < fadeTicks
                    || tick + fadeTicks >= duration) {
                throw new JsonParseException(
                        "Transition must have a 1..40 tick half-fade inside recording duration");
            }
            transitions.add(new Transition(tick, fadeTicks));
        }
        transitions.sort(java.util.Comparator.comparingInt(Transition::tick));
        for (int i = 1; i < transitions.size(); i++) {
            Transition last = transitions.get(i - 1);
            Transition current = transitions.get(i);
            if (current.tick() - current.fadeTicks() <= last.tick() + last.fadeTicks()) {
                throw new JsonParseException("Reconstruction fade windows cannot overlap");
            }
        }
        return List.copyOf(transitions);
    }

    /**
     * Strictly bounded sound events, evaluated on the synchronized
     * recording timeline. Resource packs may swap their sound IDs, but
     * they cannot trigger arbitrary code, music streaming or world events.
     */
    private static List<AudioCue> parseAudioCues(JsonObject root, int duration) {
        if (!root.has("audio_cues")) return List.of();
        JsonArray source = array(root.get("audio_cues"), "audio_cues");
        if (source.size() > 32) {
            throw new JsonParseException("audio_cues supports at most 32 entries");
        }
        List<AudioCue> result = new ArrayList<>();
        for (JsonElement entry : source) {
            JsonObject item = object(entry, "audio cue");
            int tick = number(item, "tick");
            if (tick < 0 || tick >= duration) {
                throw new JsonParseException("Audio cue tick outside recording duration");
            }
            ResourceLocation sound = ResourceLocation.tryParse(text(item, "sound"));
            if (sound == null) {
                throw new JsonParseException("Invalid audio cue sound ID");
            }
            float volume = item.has("volume")
                    ? normalized(item.get("volume"), "audio cue volume") : 0.25F;
            if (volume <= 0F || volume > 0.65F) {
                throw new JsonParseException("Audio cue volume must be (0, 0.65]");
            }
            float pitch = 1F;
            if (item.has("pitch")) {
                JsonElement value = item.get("pitch");
                if (value == null || !value.isJsonPrimitive()
                        || !value.getAsJsonPrimitive().isNumber()) {
                    throw new JsonParseException("Audio cue pitch must be numeric");
                }
                pitch = value.getAsFloat();
                if (!Float.isFinite(pitch) || pitch < 0.5F || pitch > 2F) {
                    throw new JsonParseException("Audio cue pitch must be within 0.5..2");
                }
            }
            result.add(new AudioCue(tick, sound, volume, pitch));
        }
        result.sort(java.util.Comparator.comparingInt(AudioCue::tick));
        for (int i = 1; i < result.size(); i++) {
            if (result.get(i).tick() == result.get(i - 1).tick()) {
                throw new JsonParseException("Multiple audio cues at one tick are not allowed");
            }
        }
        return List.copyOf(result);
    }

    /**
     * Visual events are deliberately small, local and bounded. A resource pack
     * may place a few sparks near a recorded machine component, but cannot
     * define arbitrary particle identifiers or unbounded repeated emissions.
     */
    private static List<VisualEvent> parseVisualEvents(JsonObject root, int duration) {
        if (!root.has("visual_events")) return List.of();
        JsonArray entries = array(root.get("visual_events"), "visual_events");
        if (entries.size() > 24) {
            throw new JsonParseException("visual_events supports at most 24 entries");
        }
        List<VisualEvent> events = new ArrayList<>();
        for (JsonElement element : entries) {
            JsonObject cue = object(element, "visual event");
            int tick = number(cue, "tick");
            int life = cue.has("duration_ticks") ? number(cue, "duration_ticks") : 0;
            int interval = cue.has("interval_ticks") ? number(cue, "interval_ticks") : 6;
            int count = cue.has("count") ? number(cue, "count") : 5;
            if (tick < 1 || tick >= duration || life < 0 || life > 120
                    || (long) tick + life >= duration
                    || interval < 4 || interval > 40 || count < 1 || count > 10) {
                throw new JsonParseException(
                        "Invalid visual event time, repeat interval or particle count");
            }

            String kind = text(cue, "kind");
            if (!"spark".equals(kind) && !"warning".equals(kind)
                    && !"reconstruct".equals(kind)) {
                throw new JsonParseException("Unknown hologram visual kind " + kind);
            }
            JsonArray pos = array(cue.get("position"), "visual event position");
            if (pos.size() != 3) {
                throw new JsonParseException("visual event position needs three coordinates");
            }
            double[] xyz = new double[3];
            for (int i = 0; i < 3; i++) {
                JsonElement val = pos.get(i);
                if (!val.isJsonPrimitive() || !val.getAsJsonPrimitive().isNumber()) {
                    throw new JsonParseException("Visual position must be numeric");
                }
                xyz[i] = val.getAsDouble();
                if (!Double.isFinite(xyz[i]) || Math.abs(xyz[i]) > 64D) {
                    throw new JsonParseException("Visual position outside +/-64 blocks");
                }
            }
            float spread = 0.35F;
            if (cue.has("spread")) {
                spread = boundedFloat(cue.get("spread"), "visual event spread", 0F, 1.5F);
            }
            float[] rgb = switch (kind) {
                case "warning" -> new float[]{1.0F, 0.42F, 0.16F};
                case "reconstruct" -> new float[]{0.26F, 0.93F, 1.0F};
                default -> new float[]{0.48F, 0.84F, 1.0F};
            };
            if (cue.has("color")) {
                JsonArray color = array(cue.get("color"), "visual event color");
                if (color.size() != 3) {
                    throw new JsonParseException("visual event color requires RGB");
                }
                for (int i = 0; i < 3; i++) {
                    rgb[i] = normalized(color.get(i), "visual event RGB");
                }
            }
            events.add(new VisualEvent(tick, life, interval, kind,
                    xyz[0], xyz[1], xyz[2], count, spread,
                    rgb[0], rgb[1], rgb[2]));
        }
        events.sort(java.util.Comparator.comparingInt(VisualEvent::tick));
        return List.copyOf(events);
    }

    private static float boundedFloat(JsonElement value, String name,
                                      float min, float max) {
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(name + " must be numeric");
        }
        double n = value.getAsDouble();
        if (!Double.isFinite(n) || n < min || n > max) {
            throw new JsonParseException(name + " out of range");
        }
        return (float) n;
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
        boolean shaderEffect = json.has("shader_effect")
                ? bool(json, "shader_effect")
                : DEFAULT_APPEARANCE.shaderEffect();
        return new Appearance(red, green, blue, opacity,
                fadeIn, fadeOut, flicker, preserveSourceTexture, shaderEffect);
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
