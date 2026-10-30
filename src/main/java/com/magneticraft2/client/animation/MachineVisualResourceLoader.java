package com.magneticraft2.client.animation;

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-owned machine-to-animation binding manifests.
 *
 * assets/<namespace>/machine_visuals/<machine>.json
 *
 * The manifest and the animation scene can be independently replaced by
 * resource packs. Compiled immutable snapshots are swapped on resource reload,
 * never loaded or parsed from JSON on the render thread.
 */
public final class MachineVisualResourceLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Map<ResourceLocation, MachineVisualDefinition> bindings = Map.of();
    private static final Set<ResourceLocation> reportedBindings =
            ConcurrentHashMap.newKeySet();

    public MachineVisualResourceLoader() {
        super(new Gson(), "machine_visuals");
    }

    public static MachineVisualDefinition get(ResourceLocation machine) {
        return bindings.get(machine);
    }

    /** Log an invalid physical input binding only once per machine/reload. */
    public static void warnUnavailableInput(ResourceLocation machine, String source) {
        if (reportedBindings.add(machine)) {
            LOGGER.warn("Machine visual {} references unavailable Java input {}. "
                    + "Using the original Java machine rendering.", machine, source);
        }
    }

    private static boolean isOurManifest(ResourceLocation id, JsonElement element) {
        if ("magneticraft2".equals(id.getNamespace())) {
            return true;
        }
        if (element == null || !element.isJsonObject()) {
            return false;
        }
        JsonObject root = element.getAsJsonObject();
        JsonElement format = root.get("format");
        return format != null && format.isJsonPrimitive()
                && format.getAsJsonPrimitive().isString()
                && "magneticraft2:machine_visual".equals(format.getAsString());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, MachineVisualDefinition> compiled = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            if (!isOurManifest(entry.getKey(), entry.getValue())) {
                continue;
            }
            try {
                compiled.put(entry.getKey(), MachineVisualDefinition.parse(entry.getValue()));
            } catch (JsonParseException | IllegalArgumentException exception) {
                LOGGER.error("Rejected machine visual {}: {}",
                        entry.getKey(), exception.getMessage());
            }
        }
        bindings = Map.copyOf(compiled);
        reportedBindings.clear();
        LOGGER.info("Loaded {} Magneticraft2 machine visual bindings", bindings.size());
    }
}
