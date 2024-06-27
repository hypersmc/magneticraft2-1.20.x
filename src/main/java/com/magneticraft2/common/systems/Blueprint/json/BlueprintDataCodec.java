package com.magneticraft2.common.systems.Blueprint.json;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BlueprintDataCodec implements JsonSerializer<BlueprintData>, JsonDeserializer<BlueprintData> {
    private static final Logger LOGGER = LogManager.getLogger("Magneticraft2 BlueprintDataCodec");

    @Override
    public JsonElement serialize(BlueprintData src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", src.getName());
        jsonObject.addProperty("owner", src.getOwner());
        jsonObject.add("structure", context.serialize(src.getStructure()));
        jsonObject.add("blocks", context.serialize(src.getBlocks()));
        return jsonObject;
    }

    @Override
    public BlueprintData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            String name = jsonObject.get("name").getAsString();
            String owner = jsonObject.get("owner").getAsString();
            JsonObject structureObject = jsonObject.getAsJsonObject("structure");
            BlueprintStructure structure = context.deserialize(structureObject, BlueprintStructure.class);

            Map<String, Block> blocks = null;
            try {
                JsonObject blocksObject = structureObject.getAsJsonObject("blocks");
                blocks = new HashMap<>();

                for (Map.Entry<String, JsonElement> entry : blocksObject.entrySet()) {
                    String key = entry.getKey();
                    String blockIdentifier = entry.getValue().getAsString();

                    // Perform validation to ensure blockIdentifier is a valid block
                    Block value = validateAndGetBlock(blockIdentifier);

                    blocks.put(key, value);
                }
            } catch (JsonParseException e) {
                LOGGER.info("Error during blocks deserialization: " + e.getMessage());
                throw e;
            }
            return new BlueprintData(name, owner, structure, blocks);
        } catch (JsonParseException e) {
            LOGGER.info("Error during BlueprintData deserialize: " + e.getMessage());
            throw e;
        }
    }
    private Block validateAndGetBlock(String blockIdentifier) {
        ResourceLocation blockId = new ResourceLocation(blockIdentifier);
        Block block = ForgeRegistries.BLOCKS.getValue(blockId);
        String namespace = blockId.toString();
        String bl = block.toString();
        if (!blockIdentifier.equals(bl.replace("Block{", "").replace("}", ""))){
            throw new IllegalArgumentException("Block not found for identifier: " + blockIdentifier);
        }
        if (block != null && blockId != null) {
            return block;
        }else{
            throw new IllegalArgumentException("An unknown error has occurred!");
        }
    }

    private static class BlueprintStructureAdapter implements JsonSerializer<BlueprintStructure>, JsonDeserializer<BlueprintStructure> {

        @Override
        public JsonElement serialize(BlueprintStructure src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("dimensions", context.serialize(src.getDimensions()));
            jsonObject.add("layout", context.serialize(src.getLayout()));
            jsonObject.add("blocks", context.serialize(src.getBlocks()));
            return jsonObject;
        }

        @Override
        public BlueprintStructure deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject jsonObject = json.getAsJsonObject();

                int[] dimensions = null;
                try {
                    dimensions = context.deserialize(jsonObject.get("dimensions"), int[].class);
                } catch (JsonParseException e) {
                    LOGGER.info("Error during dimensions deserialization: " + e.getMessage());
                    throw e;
                }

                Map<String, List<List<String>>> layout = new HashMap<>();
                try {
                    JsonObject layoutObject = jsonObject.getAsJsonObject("layout");
                    for (Map.Entry<String, JsonElement> entry : layoutObject.entrySet()) {
                        String layerName = entry.getKey();
                        JsonArray layerArray = entry.getValue().getAsJsonArray();
                        List<List<String>> layer = new ArrayList<>();
                        for (JsonElement element : layerArray) {
                            JsonArray rowArray = element.getAsJsonArray();
                            List<String> row = new ArrayList<>();
                            for (JsonElement element2 : rowArray) {
                                String value = element2.getAsString();
                                row.add(value);
                            }
                            layer.add(row);
                        }
                        layout.put(layerName, layer);
                    }
                } catch (JsonParseException e) {
                    LOGGER.info("Error during layout deserialization: " + e.getMessage());
                    throw e;
                }

                Map<String, Block> blocks = null;
                try {
                    JsonObject blocksObject = jsonObject.getAsJsonObject("blocks");
                    blocks = new HashMap<>();

                    for (Map.Entry<String, JsonElement> entry : blocksObject.entrySet()) {
                        String key = entry.getKey();
                        String blockIdentifier = entry.getValue().getAsString();
                        // Perform validation to ensure blockIdentifier is a valid block
                        Block value = validateAndGetBlock(blockIdentifier);
                        blocks.put(key, value);
                    }
                } catch (JsonParseException e) {
                    LOGGER.info("Error during blocks deserialization: " + e.getMessage());
                    throw e;
                }

                return new BlueprintStructure(dimensions, layout, blocks);
            } catch (JsonParseException e) {
                LOGGER.info("Error during BlueprintStructure deserialize: " + e.getMessage());
                throw e;
            }
        }
        private Block validateAndGetBlock(String blockIdentifier) {
            ResourceLocation blockId = new ResourceLocation(blockIdentifier);
            Block block = ForgeRegistries.BLOCKS.getValue(blockId);
            String namespace = blockId.toString();
            String bl = block.toString();
            if (!blockIdentifier.equals(bl.replace("Block{", "").replace("}", ""))){
                throw new IllegalArgumentException("Block not found for identifier: " + blockIdentifier);
            }
            if (block != null && blockId != null) {
                return block;
            }else{
                throw new IllegalArgumentException("An unknown error has occurred!");
            }
        }


    }
    public static Gson createGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(BlueprintData.class, new BlueprintDataCodec());
        gsonBuilder.registerTypeAdapter(BlueprintStructure.class, new BlueprintStructureAdapter());
        return gsonBuilder.create();
    }


}
