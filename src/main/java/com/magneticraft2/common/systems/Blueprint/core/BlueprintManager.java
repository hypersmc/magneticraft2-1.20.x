package com.magneticraft2.common.systems.Blueprint.core;

import com.google.gson.*;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.systems.Blueprint.json.Blueprint;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintData;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintDataCodec;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintRegistry;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BlueprintManager {
    private static final Logger LOGGER = LogManager.getLogger("Magneticraft2 Blueprint handler");

    public static void loadBlueprints(String modid, ResourceManager resourceManager){
        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            LOGGER.info("Started to register blueprints for mod " + modid);
        }

        for (ResourceLocation resourceLocation : resourceManager.listResources("blueprints", file -> file.toString().endsWith(".json")).keySet()) {
            final String folderName = "blueprints";
            final String namespace = resourceLocation.getNamespace();
            final String filePath = resourceLocation.getPath();
            final String dataPath = filePath.substring(folderName.length() + 1, filePath.length() - ".json".length());
            final ResourceLocation jsonIdentifier = new ResourceLocation(namespace, dataPath);
            try (InputStream inputStream = resourceManager.getResource(resourceLocation).get().open()) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                    LOGGER.info("Trying to build: " + jsonIdentifier);
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                // Parse JSON into JsonElement
                JsonElement jsonElement = JsonParser.parseReader(reader);
                if (jsonElement.isJsonObject()) {
                    try {
                        // Create custom Gson instance with the custom Codec
                        Gson gson = BlueprintDataCodec.createGson();

                        // Decode the JsonElement into BlueprintData using the custom Codec
                        BlueprintData blueprintData = gson.fromJson(jsonElement, BlueprintData.class);
                        if (blueprintData.getBlocks() == null)
                            LOGGER.info("blocks empty");
                        // Register the blocks used in the Blueprint
                        Map<String, Block> blocks = new HashMap<>();
                        for (Map.Entry<String, Block> entry : blueprintData.getBlocks().entrySet()){
                            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                                LOGGER.info("key: " + entry.getKey() + " and value: " + entry.getValue());
                            }
                            blocks.put(entry.getKey(), entry.getValue());
                        }
                        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                            LOGGER.info("Creating Blueprint Object for: " + jsonIdentifier);
                        }
                        // Create the blueprint object
                        try {
                            Blueprint blueprint = new Blueprint(
                                    blueprintData.getName(),
                                    blueprintData.getOwner(),
                                    blueprintData.getStructure(),
                                    blocks
                            );
                            // Register the blueprint
                            BlueprintRegistry.registerBlueprint(modid, blueprint, blueprint.getOwner());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } catch (JsonSyntaxException e) {
                        LOGGER.info("Could not load: " + jsonIdentifier);
                        e.printStackTrace();
                    }
                }
            } catch (IOException e){
                throw new RuntimeException("Failed to read blueprint data from " + jsonIdentifier, e);
            }
        }
        loadLocalBlueprints("blueprints");
    }
    public static void loadLocalBlueprints(String folderPath) {

        // Construct a File object representing the folder where the local blueprints are located
        File folder = new File(folderPath);

        // Check if the folder exists and is a directory
        if (folder.exists() && folder.isDirectory()) {
            // Iterate through the files in the folder
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        try (FileInputStream inputStream = new FileInputStream(file)) {
                            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                                LOGGER.info("Trying to build: " + file.getName());
                            }
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                            // Parse JSON into JsonElement
                            JsonElement jsonElement = JsonParser.parseReader(reader);
                            if (jsonElement.isJsonObject()) {
                                try {
                                    // Create custom Gson instance with the custom Codec
                                    Gson gson = BlueprintDataCodec.createGson();

                                    // Decode the JsonElement into BlueprintData using the custom Codec
                                    BlueprintData blueprintData = gson.fromJson(jsonElement, BlueprintData.class);
                                    if (blueprintData.getBlocks() == null)
                                        LOGGER.info("blocks empty");
                                    // Register the blocks used in the Blueprint
                                    Map<String, Block> blocks = new HashMap<>();
                                    for (Map.Entry<String, Block> entry : blueprintData.getBlocks().entrySet()){
                                        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                                            LOGGER.info("key: " + entry.getKey() + " and value: " + entry.getValue());
                                        }
                                        blocks.put(entry.getKey(), entry.getValue());
                                    }
                                    if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                                        LOGGER.info("Creating Blueprint Object for: " + file.getName());
                                    }
                                    // Create the blueprint object
                                    try {
                                        Blueprint blueprint = new Blueprint(
                                                blueprintData.getName(),
                                                blueprintData.getOwner(),
                                                blueprintData.getStructure(),
                                                blocks
                                        );
                                        // Register the blueprint
                                        BlueprintRegistry.registerBlueprint(magneticraft2.MOD_ID, blueprint, blueprint.getOwner());
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                } catch (JsonSyntaxException e) {
                                    LOGGER.info("Could not load: " + file.getName());
                                    e.printStackTrace();
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            LOGGER.warn("Local blueprint folder not found or is not a directory: " + folderPath);
        }
    }
    public static void clear(){
        LOGGER.info("Clearing registered blueprints");
        BlueprintRegistry.ClearRegisteredBlueprints();
    }
}
