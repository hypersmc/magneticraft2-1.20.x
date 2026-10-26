package com.magneticraft2.client.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable, validated client animation definition.
 *
 * Milestone 1 deliberately accepts only mechanical-angle-driven rotations.
 * Do not evaluate expressions from resource packs. Future keyframes and
 * constraints should be additional typed track implementations.
 */
public final class AnimationDefinition {
    public record Component(ResourceLocation model, Vec3 modelOffset, Vec3 pivot) {}
    public record Input(String source, String module) {}
    public record RotationTrack(String target, String input, Direction.Axis axis, double scale) {}
    private record AxisSpec(Direction.Axis axis, double sign) {}

    private final Map<String, Component> components;
    private final Map<String, Input> inputs;
    private final Map<String, RotationTrack> rotations;

    private AnimationDefinition(Map<String, Component> components,
                                Map<String, Input> inputs,
                                Map<String, RotationTrack> rotations) {
        this.components = Map.copyOf(components);
        this.inputs = Map.copyOf(inputs);
        this.rotations = Map.copyOf(rotations);
    }

    public Component component(String name) {
        return components.get(name);
    }

    public Input input(String name) {
        return inputs.get(name);
    }

    public RotationTrack rotation(String name) {
        return rotations.get(name);
    }

    public static AnimationDefinition parse(JsonElement element) {
        JsonObject root = asObject(element, "animation root");
        JsonElement version = required(root, "format_version", "animation root");
        if (!version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()
                || version.getAsDouble() != 1.0D) {
            throw new JsonParseException("Unsupported animation format_version (expected 1)");
        }

        JsonObject coordinates = asObject(
                required(root, "coordinate_system", "animation root"), "coordinate_system");
        if (!"blocks".equals(string(coordinates, "units", "coordinate_system"))
                || !"controller".equals(string(coordinates, "origin", "coordinate_system"))
                || !"south".equals(string(coordinates, "reference_facing", "coordinate_system"))) {
            throw new JsonParseException(
                    "Only block units, controller origin and canonical south facing are supported");
        }

        Map<String, Component> components = new HashMap<>();
        JsonObject parts = asObject(required(root, "components", "animation root"), "components");
        if (parts.size() == 0 || parts.size() > 256) {
            throw new JsonParseException("components must contain 1..256 entries");
        }
        for (Map.Entry<String, JsonElement> entry : parts.entrySet()) {
            String name = identifier(entry.getKey(), "component");
            JsonObject data = asObject(entry.getValue(), "component " + name);
            String modelId = string(data, "model", "component " + name);
            ResourceLocation model = ResourceLocation.tryParse(modelId);
            if (model == null) {
                throw new JsonParseException("Invalid model resource location for " + name + ": " + modelId);
            }
            Vec3 offset = vector(required(data, "model_offset", name), name + ".model_offset");
            Vec3 pivot = vector(required(data, "pivot", name), name + ".pivot");
            components.put(name, new Component(model, offset, pivot));
        }

        Map<String, Input> inputs = new HashMap<>();
        JsonObject declaredInputs = asObject(required(root, "inputs", "animation root"), "inputs");
        if (declaredInputs.size() == 0 || declaredInputs.size() > 64) {
            throw new JsonParseException("inputs must contain 1..64 entries");
        }
        for (Map.Entry<String, JsonElement> entry : declaredInputs.entrySet()) {
            String name = identifier(entry.getKey(), "input");
            JsonObject data = asObject(entry.getValue(), "input " + name);
            String source = string(data, "source", "input " + name);
            if (!"mechanical.visual_angle_degrees".equals(source)) {
                throw new JsonParseException("Unsupported animation input source: " + source);
            }
            String module = identifier(string(data, "module", "input " + name), "module");
            inputs.put(name, new Input(source, module));
        }

        Map<String, RotationTrack> rotations = new HashMap<>();
        JsonObject animations = asObject(
                required(root, "animations", "animation root"), "animations");
        if (animations.size() == 0 || animations.size() > 128) {
            throw new JsonParseException("animations must contain 1..128 entries");
        }
        int trackCount = 0;
        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            String animationName = identifier(entry.getKey(), "animation");
            JsonObject animation = asObject(entry.getValue(), "animation " + animationName);
            if (!"driven".equals(string(animation, "type", animationName))) {
                throw new JsonParseException("Only driven tracks are supported in animation v1 pilot");
            }
            JsonElement trackValue = required(animation, "tracks", animationName);
            if (!trackValue.isJsonArray()) {
                throw new JsonParseException("tracks must be an array in " + animationName);
            }
            JsonArray tracks = trackValue.getAsJsonArray();
            for (JsonElement elementTrack : tracks) {
                if (++trackCount > 512) {
                    throw new JsonParseException("Too many animation tracks");
                }
                JsonObject track = asObject(elementTrack, "track in " + animationName);
                String target = identifier(string(track, "target", animationName), "target");
                String inputName = identifier(string(track, "input", animationName), "input");
                if (!components.containsKey(target) || !inputs.containsKey(inputName)) {
                    throw new JsonParseException(
                            "Track references unknown component or input: " + target + "/" + inputName);
                }
                if (!"rotation".equals(string(track, "property", animationName))) {
                    throw new JsonParseException("Only rotation tracks are supported in the pilot");
                }
                AxisSpec axis = axis(required(track, "axis", animationName));
                double scale = track.has("scale")
                        ? finiteNumber(track.get("scale"), "track scale")
                        : 1.0D;
                if (Math.abs(scale) > 128.0D) {
                    throw new JsonParseException("Rotation track scale exceeds 128");
                }
                RotationTrack compiled = new RotationTrack(
                        target, inputName, axis.axis(), axis.sign() * scale);
                if (rotations.putIfAbsent(target, compiled) != null) {
                    throw new JsonParseException("Duplicate rotation track for " + target);
                }
            }
        }
        if (rotations.isEmpty()) {
            throw new JsonParseException("Animation has no supported rotation tracks");
        }
        return new AnimationDefinition(components, inputs, rotations);
    }

    private static JsonObject asObject(JsonElement element, String context) {
        if (element == null || !element.isJsonObject()) {
            throw new JsonParseException(context + " must be a JSON object");
        }
        return element.getAsJsonObject();
    }

    private static JsonElement required(JsonObject object, String key, String context) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            throw new JsonParseException(context + " is missing " + key);
        }
        return value;
    }

    private static String string(JsonObject object, String key, String context) {
        JsonElement value = required(object, key, context);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new JsonParseException(context + "." + key + " must be a string");
        }
        return value.getAsString();
    }

    private static String identifier(String name, String kind) {
        if (!name.matches("[a-z][a-z0-9_]*")) {
            throw new JsonParseException("Invalid " + kind + " identifier: " + name);
        }
        return name;
    }

    private static double finiteNumber(JsonElement element, String context) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(context + " must be a number");
        }
        double number = element.getAsDouble();
        if (!Double.isFinite(number)) {
            throw new JsonParseException(context + " must be finite");
        }
        return number;
    }

    private static Vec3 vector(JsonElement element, String context) {
        if (!element.isJsonArray() || element.getAsJsonArray().size() != 3) {
            throw new JsonParseException(context + " must have exactly three coordinates");
        }
        JsonArray array = element.getAsJsonArray();
        double x = finiteNumber(array.get(0), context + ".x");
        double y = finiteNumber(array.get(1), context + ".y");
        double z = finiteNumber(array.get(2), context + ".z");
        if (Math.abs(x) > 256 || Math.abs(y) > 256 || Math.abs(z) > 256) {
            throw new JsonParseException(context + " exceeds 256-block safety bound");
        }
        return new Vec3(x, y, z);
    }

    private static AxisSpec axis(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return switch (element.getAsString()) {
                case "x" -> new AxisSpec(Direction.Axis.X, 1.0D);
                case "y" -> new AxisSpec(Direction.Axis.Y, 1.0D);
                case "z" -> new AxisSpec(Direction.Axis.Z, 1.0D);
                case "-x" -> new AxisSpec(Direction.Axis.X, -1.0D);
                case "-y" -> new AxisSpec(Direction.Axis.Y, -1.0D);
                case "-z" -> new AxisSpec(Direction.Axis.Z, -1.0D);
                default -> throw new JsonParseException("Unsupported rotation axis: " + element);
            };
        }
        Vec3 direction = vector(element, "rotation axis");
        if (Math.abs(direction.x) == 1D && direction.y == 0D && direction.z == 0D) {
            return new AxisSpec(Direction.Axis.X, Math.signum(direction.x));
        }
        if (direction.x == 0D && Math.abs(direction.y) == 1D && direction.z == 0D) {
            return new AxisSpec(Direction.Axis.Y, Math.signum(direction.y));
        }
        if (direction.x == 0D && direction.y == 0D && Math.abs(direction.z) == 1D) {
            return new AxisSpec(Direction.Axis.Z, Math.signum(direction.z));
        }
        throw new JsonParseException("The first animation milestone supports only principal rotation axes");
    }
}
