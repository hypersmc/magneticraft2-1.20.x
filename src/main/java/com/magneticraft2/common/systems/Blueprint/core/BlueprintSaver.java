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
 * v1.0.0
 */
public class BlueprintSaver {
    private static final Gson GSON = BlueprintDataSavingCodec.createGson();
    private static final Logger LOGGER = LogManager.getLogger("MGC2-blueprintsave");

    public static void saveBlueprintClient(Blueprint blueprint, File saveDirectory, String owner) {
        if (!saveDirectory.exists())
            saveDirectory.mkdirs();
        String fileName = blueprint.getName() + "-" + owner + ".json";
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
            e.printStackTrace();
        }
    }
    public static void saveBlueprintServer(Blueprint blueprint, File saveDirectory){

    }
}
