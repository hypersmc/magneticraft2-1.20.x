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
    private static final Map<String, Blueprint> registeredBlueprints = new HashMap<>();
    public static void registerBlueprint(String modid, Blueprint blueprint){
        String blueprintName = blueprint.getName();
        String key = modid + ":" + blueprintName;
        if (registeredBlueprints.containsKey(key)){
            throw new IllegalArgumentException("Blueprint already registered: " + key);
        }
        registeredBlueprints.put(key, blueprint);
    }
    public static Blueprint getRegisteredBlueprint(String modid, String name){
        String key = modid + ":" + name;
        return registeredBlueprints.get(key);
    }
    public static Map<String, Blueprint> getRegisteredBlueprints() {
        return registeredBlueprints;
    }
    public static int getRegisteredBlueprintInNumber(){
        return registeredBlueprints.size();
    }
    public static List<String> getRegisteredBlueprintNames() {
        List<String> blueprintNames = new ArrayList<>();
        for (String key : registeredBlueprints.keySet()) {
            String blueprintName = key.substring(key.indexOf(':') + 1);
            blueprintNames.add(blueprintName);
        }
        return blueprintNames;
    }
    public static String getRegisteredBlueprintByNumber(int index) {
        List<String> blueprintNames = getRegisteredBlueprintNames();

        if (index >= 0 && index <= blueprintNames.size()) {
            return blueprintNames.get(index);
        } else{
            return "te3213st";
        }
    }
}


