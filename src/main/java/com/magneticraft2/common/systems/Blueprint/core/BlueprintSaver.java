package com.magneticraft2.common.systems.Blueprint.core;

import com.google.gson.Gson;
import com.magneticraft2.common.systems.Blueprint.json.Blueprint;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintData;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintDataSavingCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author JumpWatch on 25-08-2023
 * @Project mgc2-1.20
* @version 1.0.0
 */
public class BlueprintSaver {
    private static final Gson GSON = BlueprintDataSavingCodec.createGson();
    private static final Logger LOGGER = LogManager.getLogger("MGC2-blueprintsave");

    public static void saveBlueprintClient(Blueprint blueprint, File saveDirectory, String owner) {
        String fileName = safeFileName(blueprint.getName()) + "-" + safeFileName(owner) + ".json";
        saveBlueprint(blueprint, saveDirectory, fileName);
    }

    public static void saveBlueprintServer(Blueprint blueprint, File saveDirectory) {
        String fileName = safeFileName(blueprint.getName()) + ".json";
        saveBlueprint(blueprint, saveDirectory, fileName);
    }

    private static void saveBlueprint(Blueprint blueprint, File saveDirectory, String fileName) {
        if (!saveDirectory.exists() && !saveDirectory.mkdirs()) {
            LOGGER.error("Could not create blueprint save directory: " + saveDirectory.getPath());
            return;
        }

        File blueprintFile = new File(saveDirectory, fileName);

        try (FileWriter writer = new FileWriter(blueprintFile)) {
            String json = GSON.toJson(new BlueprintData(
                    blueprint.getName(),
                    blueprint.getOwner(),
                    blueprint.getStructure(),
                    blueprint.getBlocks()
            ));
            writer.write(json);
            LOGGER.info("Saved: " + fileName + " at: " + saveDirectory.getPath());
        } catch (IOException e) {
            LOGGER.error("Failed to save blueprint: " + fileName, e);
        }
    }

    private static String safeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "unnamed";
        }
        return value.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
