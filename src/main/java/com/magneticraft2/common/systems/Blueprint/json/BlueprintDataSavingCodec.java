package com.magneticraft2.common.systems.Blueprint.json;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

/**
 * @author JumpWatch on 26-08-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BlueprintDataSavingCodec implements JsonSerializer<BlueprintData> {
    @Override
    public JsonElement serialize(BlueprintData src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", src.getName());
        jsonObject.addProperty("owner", src.getOwner());
        JsonObject structureObject = new JsonObject();
        structureObject.add("dimensions", context.serialize(src.getStructure().getDimensions()));
        structureObject.add("layout", context.serialize(src.getStructure().getLayout()));

        // Move the blocks serialization into the structureObject
        JsonObject blocksObject = new JsonObject();
        for (Map.Entry<String, Block> entry : src.getBlocks().entrySet()) {
            String blockKey = entry.getKey();
            String blockIdentifier = entry.getValue().toString();
            String blockName = validateAndGetBlockName(new ResourceLocation(blockIdentifier.replace("Block{", "").replace("}", "")));
            blocksObject.addProperty(blockKey, blockName);
        }
        structureObject.add("blocks", blocksObject);

        jsonObject.add("structure", structureObject);

        return jsonObject;
    }

    private String validateAndGetBlockName(ResourceLocation blockId) {
        Block block = ForgeRegistries.BLOCKS.getValue(blockId);

        if (block != null) {
            String blockString = block.toString();
            if (!blockId.toString().equals(blockString.replace("Block{", "").replace("}", ""))) {
                throw new IllegalArgumentException("Block not found for identifier: " + blockId);
            }
            return blockId.toString();
        } else {
            throw new IllegalArgumentException("Block not found for identifier: " + blockId);
        }
    }
    public static Gson createGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(BlueprintData.class, new BlueprintDataSavingCodec());
        return gsonBuilder.create();
    }
}
