package com.magneticraft2.client.hologram;

import com.magneticraft2.client.animation.AnimationDefinition;
import com.magneticraft2.client.animation.AnimationResourceLoader;
import com.magneticraft2.common.hologram.RecordingTimeline;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks the contract between a hologram's recording JSON and its PR #12
 * animation scene. The individual files are syntactically validated at reload;
 * this class verifies their CROSS-FILE references when both are available.
 *
 * No I/O, JSON parsing, model baking or per-frame validation occurs here.
 * Successful/failed checks are cached by immutable definition identity and
 * refreshed automatically after resource-pack reloads.
 *
 * The same contract can later be exposed by the authoring/builder tool.
 */
public final class HologramRecordingValidator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private record Validation(HologramRecordingResourceLoader.Recording recording,
                              AnimationDefinition animation, boolean valid) {}
    private static final Map<ResourceLocation, Validation> CACHE =
            new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> REPORTED_MISSING_SCENES =
            ConcurrentHashMap.newKeySet();

    private HologramRecordingValidator() {}

    public static void clearCache() {
        CACHE.clear();
        REPORTED_MISSING_SCENES.clear();
    }

    public static boolean isUsable(ResourceLocation id,
                                   HologramRecordingResourceLoader.Recording film) {
        if (film == null || id == null) return false;
        AnimationDefinition scene = AnimationResourceLoader.get(film.animation());
        if (scene == null) {
            if (REPORTED_MISSING_SCENES.add(id)) {
                LOGGER.warn("Hologram recording {} refers to unavailable animation {}",
                        id, film.animation());
            }
            return false;
        }

        Validation previous = CACHE.get(id);
        if (previous != null && previous.recording() == film
                && previous.animation() == scene) {
            return previous.valid();
        }

        String error = validate(film, scene);
        boolean valid = error == null;
        if (!valid) {
            LOGGER.warn("Hologram recording {} cannot render: {}", id, error);
        }
        CACHE.put(id, new Validation(film, scene, valid));
        REPORTED_MISSING_SCENES.remove(id);
        return valid;
    }

    /**
     * Pure cross-file verification for the JSON authoring contract.
     *
     * @return null if valid, or a targeted error identifying the source/key.
     */
    public static String validate(HologramRecordingResourceLoader.Recording film,
                                  AnimationDefinition scene) {
        if (film == null || scene == null) {
            return "missing recording or animation scene";
        }

        for (AnimationDefinition.Input input : scene.inputs()) {
            if ("mechanical.visual_angle_degrees".equals(input.source())
                    && !film.channels().containsKey(input.module())) {
                return "animation requests missing mechanical channel '"
                        + input.module() + "'";
            }
        }

        Map<String, Set<Integer>> expectedValues = new HashMap<>();
        for (String name : scene.componentNames()) {
            AnimationDefinition.Component component = scene.component(name);
            AnimationDefinition.ModelVariants variants = component.modelVariants();
            if (variants == null) continue;
            String source = variants.source();
            RecordingTimeline.DiscreteTrack track = film.visualChannels().get(source);
            if (track == null) {
                return "component '" + name + "' uses missing visual channel '"
                        + source + "'";
            }
            Set<Integer> accepted = expectedValues.computeIfAbsent(
                    source, unused -> new HashSet<>(variants.models().keySet()));
            // A shared channel may legitimately control several different
            // components. Every keyed stage must be handled by EACH one.
            accepted.retainAll(variants.models().keySet());
            if (accepted.isEmpty()) {
                return "visual channel '" + source
                        + "' has no model stages shared by its components";
            }
        }

        for (Map.Entry<String, RecordingTimeline.DiscreteTrack> entry
                : film.visualChannels().entrySet()) {
            Set<Integer> accepted = expectedValues.get(entry.getKey());
            if (accepted == null) {
                return "unused visual channel '" + entry.getKey()
                        + "' (no component uses this model-variant source)";
            }
            for (RecordingTimeline.DiscreteKeyframe key : entry.getValue().keyframes()) {
                if (!accepted.contains(key.value())) {
                    return "visual channel '" + entry.getKey() + "' selects stage "
                            + key.value() + " at tick " + key.tick()
                            + ", but not every component defines that model";
                }
            }
        }
        return null;
    }
}
