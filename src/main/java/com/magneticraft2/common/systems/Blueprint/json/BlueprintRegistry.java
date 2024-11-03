package com.magneticraft2.common.systems.Blueprint.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BlueprintRegistry {
    private static final Map<String, BlueprintInfo> registeredBlueprints = new HashMap<>();

    public static void registerBlueprint(String modid, Blueprint blueprint, String owner) {
        String blueprintName = blueprint.getName();
        String key = modid + ":" + blueprintName;
        if (registeredBlueprints.containsKey(key)) {
            return;
        }
        registeredBlueprints.put(key, new BlueprintInfo(blueprintName, blueprint, owner));
    }

    public static Blueprint getRegisteredBlueprint(String modid, String name) {
        String key = modid + ":" + name;
        BlueprintInfo blueprintInfo = registeredBlueprints.get(key);
        return (blueprintInfo != null) ? blueprintInfo.getBlueprint() : null;
    }

    public static Map<String, BlueprintInfo> getRegisteredBlueprints() {
        return registeredBlueprints;
    }

    public static int getRegisteredBlueprintInNumber() {
        return registeredBlueprints.size();
    }

    public static List<String> getRegisteredBlueprintNames() {
        List<String> blueprintNames = new ArrayList<>();
        for (BlueprintInfo blueprintInfo : registeredBlueprints.values()) {
            blueprintNames.add(blueprintInfo.getName());
        }
        return blueprintNames;
    }

    public static String getRegisteredBlueprintByNumber(int index) {
        List<String> blueprintNames = getRegisteredBlueprintNames();

        if (index >= 0 && index < blueprintNames.size()) {
            return blueprintNames.get(index);
        } else {
            return "te3213st";
        }
    }
    public static String getRegisteredBlueprintByNumberAndOwner(int index, String owner) {
        List<BlueprintInfo> blueprintInfos = new ArrayList<>(registeredBlueprints.values());

        for (BlueprintInfo blueprintInfo : blueprintInfos) {
            if (blueprintInfo.getOwner().equalsIgnoreCase("everyone") || blueprintInfo.getOwner().equals(owner)) {
                index--;
                if (index < 0) {
                    return blueprintInfo.getName();
                }
            }
        }

        return null; // If no matching blueprint is found for the given owner and index
    }
    // Custom class to represent Blueprint information
    public static class BlueprintInfo {
        private String name;
        private Blueprint blueprint;
        private String owner;

        public BlueprintInfo(String name, Blueprint blueprint, String owner) {
            this.name = name;
            this.blueprint = blueprint;
            this.owner = owner;
        }

        public String getName() {
            return name;
        }

        public Blueprint getBlueprint() {
            return blueprint;
        }

        public String getOwner() {
            return owner;
        }
    }
}