package com.magneticraft2.client.animation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.ModelEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource-pack animations in assets/<namespace>/animations/<name>.json.
 * The model-baking registration and the runtime reload listener use the SAME
 * validator, so replacement model ids can come from higher-priority packs.
 */
public final class AnimationResourceLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Map<ResourceLocation, AnimationDefinition> definitions = Map.of();
    private static final Set<ResourceLocation> missingModelWarnings =
            ConcurrentHashMap.newKeySet();

    public AnimationResourceLoader() {
        super(new Gson(), "animations");
    }

    public static AnimationDefinition get(ResourceLocation id) {
        return definitions.get(id);
    }

    /**
     * Called during ModelEvent.RegisterAdditional, BEFORE models are baked.
     * Loading only the already-compiled definitions here would lag one reload
     * behind; instead read the current pack resources and register their model
     * dependencies on every bake, including the very first one.
     */
    public static void registerModels(ModelEvent.RegisterAdditional event,
                                      ResourceManager resourceManager) {
        Map<ResourceLocation, Resource> files =
                resourceManager.listResources("animations",
                        location -> location.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                AnimationDefinition definition =
                        AnimationDefinition.parse(JsonParser.parseReader(reader));
                for (AnimationDefinition.Component component : definition.components()) {
                    event.register(component.model());
                }
            } catch (IOException | JsonParseException | IllegalArgumentException ex) {
                LOGGER.warn("Cannot register animated models from {}: {}",
                        entry.getKey(), ex.getMessage());
            }
        }
    }

    /** Avoid spamming the render thread when a resource-pack model is absent. */
    public static void warnMissingModel(ResourceLocation id) {
        if (missingModelWarnings.add(id)) {
            LOGGER.warn("Animated component model {} was not baked or is missing; "
                    + "using the machine's Java fallback", id);
        }
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
        missingModelWarnings.clear();
        LOGGER.info("Loaded {} Magneticraft2 JSON animation definitions", definitions.size());
    }
}
