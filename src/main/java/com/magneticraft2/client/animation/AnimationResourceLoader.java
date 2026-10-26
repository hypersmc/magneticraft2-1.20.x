package com.magneticraft2.client.animation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
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
 * Client resource-pack definitions: assets/<namespace>/animations/<name>.json.
 * Definitions are compiled once per reload and published as an immutable snapshot.
 * Existing Java renderers remain the fallback for absent or rejected resources.
 */
public final class AnimationResourceLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Map<ResourceLocation, AnimationDefinition> definitions = Map.of();

    public AnimationResourceLoader() {
        super(new Gson(), "animations");
    }

    public static AnimationDefinition get(ResourceLocation id) {
        return definitions.get(id);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, AnimationDefinition> compiled = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                compiled.put(entry.getKey(), AnimationDefinition.parse(entry.getValue()));
            } catch (JsonParseException | IllegalArgumentException e) {
                LOGGER.error("Rejected animation {}: {}", entry.getKey(), e.getMessage());
            }
        }
        definitions = Map.copyOf(compiled);
        LOGGER.info("Loaded {} Magneticraft2 JSON animation definitions", definitions.size());
    }
}
