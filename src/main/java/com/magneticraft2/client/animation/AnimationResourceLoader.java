package com.magneticraft2.client.animation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
     * "animations" is a shared resource-pack directory, also used by
     * GeckoLib and other mods. Never treat an unrelated file as a broken
     * Magneticraft2 animation. Still validate all files in our own namespace
     * (even malformed ones), and accept explicit schema markers from any
     * other namespace for third-party authored animations.
     *
     * The legacy structural check allows older correctly formed third-party
     * packs written before the explicit "format" marker existed.
     */
    private static boolean isOurAnimation(ResourceLocation location, JsonElement source) {
        if ("magneticraft2".equals(location.getNamespace())) {
            return true;
        }
        if (source == null || !source.isJsonObject()) {
            return false;
        }
        JsonObject root = source.getAsJsonObject();
        JsonElement explicitFormat = root.get("format");
        if (explicitFormat != null) {
            return explicitFormat.isJsonPrimitive()
                    && explicitFormat.getAsJsonPrimitive().isString()
                    && "magneticraft2:visual_animation".equals(explicitFormat.getAsString());
        }
        return root.has("coordinate_system")
                && root.has("components")
                && root.has("inputs")
                && root.has("animations");
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
                JsonElement source = JsonParser.parseReader(reader);
                if (!isOurAnimation(entry.getKey(), source)) {
                    continue;
                }
                AnimationDefinition definition = AnimationDefinition.parse(source);
                for (AnimationDefinition.Component component : definition.components()) {
                    // A transform-only hierarchy node intentionally has no
                    // model to bake; its children provide the geometry.
                    if (component.model() != null) {
                        event.register(component.model());
                    }
                    AnimationDefinition.ModelVariants variants = component.modelVariants();
                    if (variants != null) {
                        for (ResourceLocation variantModel : variants.models().values()) {
                            event.register(variantModel);
                        }
                    }
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
            if (!isOurAnimation(entry.getKey(), entry.getValue())) {
                continue;
            }
            try {
                AnimationDefinition definition = AnimationDefinition.parse(entry.getValue());
                // Dynamic belt texture IDs are NOT block models and therefore
                // are not registered in the ModelEvent bake pass. Validate
                // their real client resources on reload, so missing textures
                // reject the entire scene before the renderer emits geometry.
                for (AnimationDefinition.BeltLoop loop : definition.beltLoops().values()) {
                    if (resourceManager.getResource(loop.texture()).isEmpty()) {
                        throw new JsonParseException("Missing belt texture " + loop.texture());
                    }
                }
                compiled.put(entry.getKey(), definition);
            } catch (JsonParseException | IllegalArgumentException e) {
                LOGGER.error("Rejected animation {}: {}", entry.getKey(), e.getMessage());
            }
        }
        definitions = Map.copyOf(compiled);
        missingModelWarnings.clear();
        LOGGER.info("Loaded {} Magneticraft2 JSON animation definitions", definitions.size());
    }
}
