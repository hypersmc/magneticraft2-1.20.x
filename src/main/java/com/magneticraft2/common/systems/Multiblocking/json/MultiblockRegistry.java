package com.magneticraft2.common.systems.Multiblocking.json;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockRegistry {
    private static final Map<String, Multiblock> registeredMultiblocks = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger("Magneticraft2 Multiblock Registry");

    public static void registerMultiblock(String modid, Multiblock multiblock) {
        String multiblockName = multiblock.getName();
        String key = modid + ":" + multiblockName;
        if (registeredMultiblocks.containsKey(key)) {
            LOGGER.info("Multiblock already registered: " + key);
        }
        registeredMultiblocks.put(key, multiblock);
    }
    public static void ClearRegisteredMultiblocks() {
        registeredMultiblocks.clear();
        LOGGER.info("Multiblock registry cleared");
    }

    public static Multiblock getRegisteredMultiblock(String modid, String name) {
        String key = modid + ":" + name;
        return registeredMultiblocks.get(key);
    }

    public static Map<String, Multiblock> getRegisteredMultiblocks() {
        return registeredMultiblocks;
    }
    public static List<String> getRegisteredMultiblockNames() {
        List<String> multiblockNames = new ArrayList<>();
        for (String key : registeredMultiblocks.keySet()) {
            String multiblockName = key.substring(key.indexOf(':') + 1);
            multiblockNames.add(multiblockName);
        }
        return multiblockNames;
    }
}
