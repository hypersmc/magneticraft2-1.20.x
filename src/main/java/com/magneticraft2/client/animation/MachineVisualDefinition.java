package com.magneticraft2.client.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Client resource-pack manifest selecting an animation scene for a machine.
 *
 * Keys are the names seen by the animation JSON; values are real input names
 * explicitly supplied by that machine's Java renderer. Merely declaring an
 * input here cannot create a physical port, new simulation or server state.
 */
public record MachineVisualDefinition(ResourceLocation animation,
                                      Map<String, String> angleChannels,
                                      Map<String, String> integerChannels) {
    public MachineVisualDefinition {
        angleChannels = Map.copyOf(angleChannels);
        integerChannels = Map.copyOf(integerChannels);
    }

    public static MachineVisualDefinition parse(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new JsonParseException("Machine visual manifest must be an object");
        }
        JsonObject root = element.getAsJsonObject();
        if (!"magneticraft2:machine_visual".equals(string(root, "format"))) {
            throw new JsonParseException("Expected format magneticraft2:machine_visual");
        }
        JsonElement version = root.get("format_version");
        if (version == null || !version.isJsonPrimitive()
                || !version.getAsJsonPrimitive().isNumber()
                || version.getAsDouble() != 1D) {
            throw new JsonParseException("Unsupported machine visual format_version (expected 1)");
        }
        String scene = string(root, "animation");
        ResourceLocation animation = ResourceLocation.tryParse(scene);
        if (animation == null) {
            throw new JsonParseException("Invalid animation resource location: " + scene);
        }

        Map<String, String> angles = channels(root, "angle_channels");
        Map<String, String> integers = channels(root, "integer_channels");
        return new MachineVisualDefinition(animation, angles, integers);
    }

    private static Map<String, String> channels(JsonObject root, String key) {
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new JsonParseException(key + " must be an object");
        }
        JsonObject object = value.getAsJsonObject();
        if (object.size() > 64) {
            throw new JsonParseException(key + " cannot contain more than 64 channel mappings");
        }
        Map<String, String> channels = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String alias = identifier(entry.getKey(), key + " alias");
            if (!entry.getValue().isJsonPrimitive()
                    || !entry.getValue().getAsJsonPrimitive().isString()) {
                throw new JsonParseException(key + "." + alias + " must name an exposed input");
            }
            String physical = identifier(entry.getValue().getAsString(), key + " source");
            channels.put(alias, physical);
        }
        return channels;
    }

    private static String string(JsonObject root, String key) {
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new JsonParseException(key + " must be a string");
        }
        return value.getAsString();
    }

    private static String identifier(String value, String description) {
        // Dots are allowed for typed integer selector keys, e.g.
        // sifter.wear_stage; Java remains responsible for supplying them.
        if (value.length() > 128 || !value.matches("[a-z][a-z0-9_.]*")) {
            throw new JsonParseException("Invalid " + description + ": " + value);
        }
        return value;
    }
}
