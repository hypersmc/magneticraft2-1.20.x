package com.magneticraft2.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author JumpWatch on 04-07-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiBlockModelLoader implements IGeometryLoader<MultiBlockGeometry> {
    public static final Logger LOGGER = LogManager.getLogger("MultiBlockModelLoader");

    @Override
    public MultiBlockGeometry read(JsonObject json, JsonDeserializationContext ctx) throws JsonParseException {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        String baseModelPath = json.get("baseModel").getAsString();
        BlockModel baseModel = loadBlockModel(baseModelPath, manager);

        JsonObject namedModelsJson = json.getAsJsonObject("namedModels");
        Map<String, BlockModel> namedModels = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : namedModelsJson.entrySet()) {
            String modelName = entry.getKey();
            String modelPath = entry.getValue().getAsString();
            BlockModel model = loadBlockModel(modelPath, manager);
            if (model != null) {
                namedModels.put(modelName, model);
            } else {
                LOGGER.warn("Failed to load model for key: {}", modelName);
            }
            LOGGER.info("Model key: {} - Model path: {}", modelName, modelPath);

        }


        return new MultiBlockGeometry(baseModel, namedModels);
    }

    private BlockModel loadBlockModel(String modelPath, ResourceManager manager) {
        try {
            ResourceLocation modelResourceLocation = new ResourceLocation(modelPath);
            InputStream inputStream = manager.getResource(modelResourceLocation).get().open();
            BlockModel model = BlockModel.fromStream(new InputStreamReader(inputStream));
            inputStream.close();  // Ensure stream is closed after use
            LOGGER.info("Successfully loaded BlockModel for path: {}", modelPath);
            return model;
        } catch (IOException e) {
            LOGGER.error("Failed to load BlockModel from path: {}", modelPath, e);
            return null;
        }
    }

}