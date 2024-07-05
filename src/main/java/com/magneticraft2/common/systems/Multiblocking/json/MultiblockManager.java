package com.magneticraft2.common.systems.Multiblocking.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockManager {
    private static final Logger LOGGER = LogManager.getLogger("Magneticraft2 Multiblock handler");
    @SubscribeEvent
    static void registerModels(ModelEvent.RegisterAdditional event) {
//        event.register();
    }
    public static void loadMultiblocks(String modid, ResourceManager resourceManager) {
        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            LOGGER.info("Started to register multiblocks for mod " + modid);
        }
        for (ResourceLocation resourceLocation : resourceManager.listResources("multiblocks", file -> file.toString().endsWith(".json")).keySet()) {
            final String folderName = "multiblocks";
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

                // Check if the parsed JSON is an object
                if (jsonElement.isJsonObject()) {
                    try {
                        // Create custom Gson instance with the custom Codec
                        Gson gson = MultiblockDataCodec.createGson();

                        // Decode the JsonElement into MultiblockData using the custom Codec
                        MultiblockData multiblockData = gson.fromJson(jsonElement, MultiblockData.class);
                        if (multiblockData.getBlocks() == null)
                            LOGGER.info("blocks empty");
                        // Register the blocks used in the multiblock
                        Map<String, Block> blocks = new HashMap<>();
                        for (Map.Entry<String, Block> entry : multiblockData.getBlocks().entrySet()) {
                            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                                LOGGER.info("key: " + entry.getKey() + " and value: " + entry.getValue());
                            }
                            blocks.put(entry.getKey(), entry.getValue());
                        }
                        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                            LOGGER.info("Creating Multiblock Object for: " + jsonIdentifier);
                        }
                        // Create the multiblock object
                        try {
                            Multiblock multiblock = new Multiblock(
                                    multiblockData.getName(),
                                    multiblockData.getStructure(),
                                    blocks,
                                    multiblockData.getSettings()
                            );
                            // Register the multiblock
                            MultiblockRegistry.registerMultiblock(modid, multiblock);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    } catch (JsonSyntaxException e) {
                        LOGGER.info("Could not load: " + jsonIdentifier);
                        e.printStackTrace();
                    }
                } else {
                    LOGGER.info("Invalid JSON structure. Expected an object.");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read multiblock data from " + jsonIdentifier, e);
            }
        }
    }
}
