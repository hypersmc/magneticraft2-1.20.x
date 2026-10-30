package com.magneticraft2.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Existing selector loader, hardened against missing optional model files. */
public class MultiBlockModelLoader implements IGeometryLoader<MultiBlockGeometry> {
    public static final Logger LOGGER = LogManager.getLogger("MultiBlockModelLoader");

    @Override
    public MultiBlockGeometry read(JsonObject json, JsonDeserializationContext ctx) throws JsonParseException {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (!json.has("baseModel")) {
            throw new JsonParseException("magneticraft2:multiblock loader requires baseModel");
        }
        String baseName = json.get("baseModel").getAsString();
        BlockModel baseModel = loadBlockModel(baseName, manager);
        if (baseModel == null) {
            throw new JsonParseException("Missing required multiblock base model: " + baseName);
        }

        Map<String, BlockModel> namedModels = new HashMap<>();
        JsonObject named = json.has("namedModels") ? json.getAsJsonObject("namedModels") : new JsonObject();
        for (Map.Entry<String, JsonElement> entry : named.entrySet()) {
            String name = entry.getKey();
            String path = entry.getValue().getAsString();
            BlockModel optional = loadBlockModel(path, manager);
            if (optional != null) {
                namedModels.put(name, optional);
            } else {
                LOGGER.warn("Ignoring missing optional multiblock model {} at {}", name, path);
            }
        }
        return new MultiBlockGeometry(baseModel, namedModels);
    }

    private BlockModel loadBlockModel(String modelPath, ResourceManager manager) {
        try {
            ResourceLocation resourceId = new ResourceLocation(modelPath);
            var resource = manager.getResource(resourceId);
            if (resource.isEmpty()) return null;
            try (InputStreamReader reader =
                         new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                return BlockModel.fromStream(reader);
            }
        } catch (Exception error) {
            LOGGER.warn("Cannot read multiblock model {}", modelPath, error);
            return null;
        }
    }
}
