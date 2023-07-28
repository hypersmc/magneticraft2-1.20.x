package com.magneticraft2.common.systems.Multiblocking.json;

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
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockDataCodec implements JsonSerializer<MultiblockData>, JsonDeserializer<MultiblockData> {
    private static final Logger LOGGER = LogManager.getLogger("Magneticraft2 MultiblockDataCodec");

    @Override
    public JsonElement serialize(MultiblockData multiblockData, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", multiblockData.getName());
        jsonObject.add("structure", context.serialize(multiblockData.getStructure()));
        jsonObject.add("blocks", context.serialize(multiblockData.getBlocks()));
        jsonObject.add("settings", context.serialize(multiblockData.getSettings()));
        return jsonObject;
    }

    @Override
    public MultiblockData deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            String name = jsonObject.get("name").getAsString();
            JsonObject structureObject = jsonObject.getAsJsonObject("structure");
            MultiblockStructure structure = context.deserialize(structureObject, MultiblockStructure.class);

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

            MultiblockSettings settings = context.deserialize(jsonObject.get("settings"), MultiblockSettings.class);
            return new MultiblockData(name, structure, blocks, settings);
        } catch (JsonParseException e) {
            LOGGER.info("Error during MultiblockData deserialize: " + e.getMessage());
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

    private static class MultiblockInputAdapter implements JsonSerializer<MultiblockInput>, JsonDeserializer<MultiblockInput> {

        @Override
        public JsonElement serialize(MultiblockInput multiblockInput, Type type, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("slot", multiblockInput.getSlot());
            jsonObject.addProperty("type", multiblockInput.getType());
            jsonObject.add("allowedItems", context.serialize(multiblockInput.getAllowedItems()));
            jsonObject.addProperty("size", multiblockInput.getSize());
            return jsonObject;
        }

        @Override
        public MultiblockInput deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject jsonObject = json.getAsJsonObject();
                int slot = jsonObject.get("slot").getAsInt();
                String typeString = jsonObject.get("type").getAsString();
                List<String> allowedItems = context.deserialize(jsonObject.get("allowedItems"), List.class);
                int size = jsonObject.get("size").getAsInt();
                return new MultiblockInput(slot, typeString, allowedItems, size);
            }catch (JsonParseException e) {
                LOGGER.info("Error during MultiblockInput deserialize: " + e.getMessage());
                throw e;
            }
        }
    }

    private static class MultiblockOutputAdapter implements JsonSerializer<MultiblockOutput>, JsonDeserializer<MultiblockOutput> {

        @Override
        public JsonElement serialize(MultiblockOutput multiblockOutput, Type type, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("slot", multiblockOutput.getSlot());
            jsonObject.addProperty("type", multiblockOutput.getType());
            jsonObject.add("allowedItems", context.serialize(multiblockOutput.getAllowedItems()));
            jsonObject.addProperty("size", multiblockOutput.getSize());
            return jsonObject;
        }

        @Override
        public MultiblockOutput deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject jsonObject = json.getAsJsonObject();
                int slot = jsonObject.get("slot").getAsInt();
                String typeString = jsonObject.get("type").getAsString();
                List<String> allowedItems = context.deserialize(jsonObject.get("allowedItems"), List.class);
                int size = jsonObject.get("size").getAsInt();
                return new MultiblockOutput(slot, typeString, allowedItems, size);
            }catch (JsonParseException e) {
                LOGGER.info("Error during MultiblockOutput deserialize: " + e.getMessage());
                throw e;
            }
        }
    }

    private static class MultiblockSettingsAdapter implements JsonSerializer<MultiblockSettings>, JsonDeserializer<MultiblockSettings> {

        @Override
        public JsonElement serialize(MultiblockSettings multiblockSettings, Type type, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("replaceWhenFormed", multiblockSettings.getReplaceWhenFormed());
            jsonObject.addProperty("hasInventory", multiblockSettings.hasInventory());
            jsonObject.addProperty("hasPower", multiblockSettings.hasPower());
            jsonObject.addProperty("hasFuel", multiblockSettings.hasFuel());
            jsonObject.addProperty("hasFluid", multiblockSettings.hasFluid());
            jsonObject.add("inputs", context.serialize(multiblockSettings.getInputs()));
            jsonObject.add("outputs", context.serialize(multiblockSettings.getOutputs()));
            return jsonObject;
        }

        @Override
        public MultiblockSettings deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject jsonObject = json.getAsJsonObject();
                String replaceWhenFormed = jsonObject.get("replaceWhenFormed").getAsString();
                boolean hasInventory = jsonObject.get("hasInventory").getAsBoolean();
                boolean hasPower = jsonObject.get("hasPower").getAsBoolean();
                boolean hasFuel = jsonObject.get("hasFuel").getAsBoolean();
                boolean hasFluid = jsonObject.get("hasFluid").getAsBoolean();
                List<MultiblockInput> inputs = context.deserialize(jsonObject.get("inputs"), List.class);
                List<MultiblockOutput> outputs = context.deserialize(jsonObject.get("outputs"), List.class);
                return new MultiblockSettings(replaceWhenFormed, hasInventory, hasPower, hasFuel, hasFluid, inputs, outputs);
            }catch (JsonParseException e) {
                LOGGER.info("Error during MultiblockSettings deserialize: " + e.getMessage());
                throw e;
            }
        }
    }

    private static class MultiblockStructureAdapter implements JsonSerializer<MultiblockStructure>, JsonDeserializer<MultiblockStructure> {

        @Override
        public JsonElement serialize(MultiblockStructure multiblockStructure, Type type, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("dimensions", context.serialize(multiblockStructure.getDimensions()));
            jsonObject.add("layout", context.serialize(multiblockStructure.getLayout()));
            jsonObject.add("blocks", context.serialize(multiblockStructure.getBlocks()));
            return jsonObject;
        }

        @Override
        public MultiblockStructure deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
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

                return new MultiblockStructure(dimensions, layout, blocks);
            } catch (JsonParseException e) {
                LOGGER.info("Error during MultiblockStructure deserialize: " + e.getMessage());
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
        gsonBuilder.registerTypeAdapter(MultiblockData.class, new MultiblockDataCodec());
        gsonBuilder.registerTypeAdapter(MultiblockInput.class, new MultiblockInputAdapter());
        gsonBuilder.registerTypeAdapter(MultiblockOutput.class, new MultiblockOutputAdapter());
        gsonBuilder.registerTypeAdapter(MultiblockSettings.class, new MultiblockSettingsAdapter());
        gsonBuilder.registerTypeAdapter(MultiblockStructure.class, new MultiblockStructureAdapter());
        return gsonBuilder.create();
    }
}
