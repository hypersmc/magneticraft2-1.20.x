package com.magneticraft2.common.systems.Blueprint.core;

import com.google.gson.*;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.systems.Blueprint.json.Blueprint;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintData;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintDataCodec;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintDataSavingCodec;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintRegistry;
import com.magneticraft2.common.systems.mgc2Network;
import com.magneticraft2.common.systems.networking.SyncBlueprintsPacket;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.network.PacketDistributor;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
* @version 1.0.0
 */
public class BlueprintManager {
    private static final Logger LOGGER = LogManager.getLogger("Magneticraft2 Blueprint handler");
    private static final Gson BLUEPRINT_SYNC_GSON = BlueprintDataSavingCodec.createGson();

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
        loadLocalBlueprints(new File(folderPath));
    }

    public static void loadLocalBlueprints(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            LOGGER.warn("Local blueprint folder not found or is not a directory: " + (folder == null ? "null" : folder.getPath()));
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                loadLocalBlueprints(file);
            } else if (file.isFile() && file.getName().endsWith(".json")) {
                loadBlueprintFile(file);
            }
        }
    }

    private static void loadBlueprintFile(File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Trying to build: " + file.getName());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            JsonElement jsonElement = JsonParser.parseReader(reader);
            if (jsonElement.isJsonObject()) {
                try {
                    Gson gson = BlueprintDataCodec.createGson();
                    BlueprintData blueprintData = gson.fromJson(jsonElement, BlueprintData.class);
                    if (blueprintData.getBlocks() == null) {
                        LOGGER.info("blocks empty");
                    }

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

                    Blueprint blueprint = new Blueprint(
                            blueprintData.getName(),
                            blueprintData.getOwner(),
                            blueprintData.getStructure(),
                            blocks
                    );
                    BlueprintRegistry.registerBlueprint(magneticraft2.MOD_ID, blueprint, blueprint.getOwner());
                } catch (JsonSyntaxException e) {
                    LOGGER.info("Could not load: " + file.getName());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void syncBlueprintsToPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }

        List<String> blueprintJsons = createBlueprintSyncPayloadForOwner(player.getName().getString());
        mgc2Network.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncBlueprintsPacket(blueprintJsons)
        );
    }

    public static List<String> createBlueprintSyncPayloadForOwner(String owner) {
        List<String> blueprintJsons = new ArrayList<>();
        for (BlueprintRegistry.BlueprintInfo blueprintInfo : BlueprintRegistry.getRegisteredBlueprints().values()) {
            if (!BlueprintRegistry.isBlueprintOwnedByPlayer(blueprintInfo.getOwner(), owner)) {
                continue;
            }

            String json = toSyncJson(blueprintInfo.getBlueprint());
            if (json != null && !json.isBlank()) {
                blueprintJsons.add(json);
            }
        }
        return blueprintJsons;
    }

    public static void receiveSyncedBlueprints(List<String> blueprintJsons) {
        if (blueprintJsons == null) {
            return;
        }

        for (String blueprintJson : blueprintJsons) {
            loadBlueprintJson(blueprintJson, "server sync", true);
        }
    }

    private static String toSyncJson(Blueprint blueprint) {
        if (blueprint == null) {
            return null;
        }

        try {
            return BLUEPRINT_SYNC_GSON.toJson(new BlueprintData(
                    blueprint.getName(),
                    blueprint.getOwner(),
                    blueprint.getStructure(),
                    blueprint.getBlocks()
            ));
        } catch (RuntimeException e) {
            LOGGER.error("Could not serialize blueprint for sync: " + blueprint.getName(), e);
            return null;
        }
    }

    private static void loadBlueprintJson(String json, String sourceName, boolean replaceExisting) {
        if (json == null || json.isBlank()) {
            return;
        }

        try {
            Gson gson = BlueprintDataCodec.createGson();
            BlueprintData blueprintData = gson.fromJson(json, BlueprintData.class);
            if (blueprintData == null || blueprintData.getBlocks() == null) {
                LOGGER.warn("Synced blueprint data was empty from: " + sourceName);
                return;
            }

            Map<String, Block> blocks = new HashMap<>();
            for (Map.Entry<String, Block> entry : blueprintData.getBlocks().entrySet()) {
                blocks.put(entry.getKey(), entry.getValue());
            }

            Blueprint blueprint = new Blueprint(
                    blueprintData.getName(),
                    blueprintData.getOwner(),
                    blueprintData.getStructure(),
                    blocks
            );

            if (replaceExisting) {
                BlueprintRegistry.registerOrReplaceBlueprint(magneticraft2.MOD_ID, blueprint, blueprint.getOwner());
            } else {
                BlueprintRegistry.registerBlueprint(magneticraft2.MOD_ID, blueprint, blueprint.getOwner());
            }
        } catch (JsonSyntaxException e) {
            LOGGER.warn("Could not load synced blueprint from: " + sourceName, e);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not register synced blueprint from: " + sourceName, e);
        }
    }

    public static void clear(){
        LOGGER.info("Clearing registered blueprints");
        BlueprintRegistry.ClearRegisteredBlueprints();
    }
}
