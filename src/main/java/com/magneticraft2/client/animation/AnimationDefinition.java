package com.magneticraft2.client.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, validated client-side animation scene graph.
 *
 * Only typed, whitelisted mechanics and states are accepted. JSON cannot
 * execute script, invent server-side power, or alter multiblock collisions.
 * The original one-shaft v1 definition remains valid.
 */
public final class AnimationDefinition {
    public enum RenderLayer { SOLID, CUTOUT, TRANSLUCENT }
    public enum State { RUNNING, STOPPED, OVERLOADED, WORN }
    public enum Condition {
        ALWAYS, RUNNING, STOPPED, OVERLOADED, WORN;

        public boolean matches(Set<State> active) {
            return this == ALWAYS || active.contains(State.valueOf(name()));
        }

        static Condition parse(String name) {
            return switch (name) {
                case "always" -> ALWAYS;
                case "running" -> RUNNING;
                case "stopped" -> STOPPED;
                case "overloaded" -> OVERLOADED;
                case "worn" -> WORN;
                default -> throw new JsonParseException("Unknown machine state: " + name);
            };
        }
    }
    /** Null model is a transform-only parent node; parent is null for roots. */
    public record Component(ResourceLocation model, Vec3 modelOffset, Vec3 pivot,
                            RenderLayer layer, String parent, Condition visibleWhen) {}
    public record Input(String source, String module) {}
    public record RotationTrack(String target, String input, Direction.Axis axis,
                                double scale, Condition when) {}
    private record AxisSpec(Direction.Axis axis, double sign) {}

    private final Map<String, Component> components;
    private final Map<String, Input> inputs;
    private final Map<String, Map<Condition, RotationTrack>> rotations;
    private final Map<String, List<String>> children;
    private final List<String> roots;

    private AnimationDefinition(Map<String, Component> components,
                                Map<String, Input> inputs,
                                Map<String, Map<Condition, RotationTrack>> rotations,
                                Map<String, List<String>> children, List<String> roots) {
        this.components = Map.copyOf(components);
        this.inputs = Map.copyOf(inputs);
        this.rotations = Map.copyOf(rotations);
        this.children = Map.copyOf(children);
        this.roots = List.copyOf(roots);
    }

    public Component component(String name) {
        return components.get(name);
    }

    public Collection<Component> components() {
        return components.values();
    }

    public Set<String> componentNames() {
        return components.keySet();
    }

    public List<String> roots() {
        return roots;
    }

    public List<String> children(String name) {
        return children.getOrDefault(name, List.of());
    }

    public Input input(String name) {
        return inputs.get(name);
    }

    /**
     * Most-specific matching state overrides the unconditional track.
     * Stalled/worn flags have higher priority than an active "running" flag,
     * which can coexist with wear on the same machine.
     */
    public RotationTrack rotation(String name, Set<State> activeStates) {
        Map<Condition, RotationTrack> candidates = rotations.get(name);
        if (candidates == null) return null;
        for (Condition condition : List.of(Condition.OVERLOADED, Condition.WORN,
                Condition.RUNNING, Condition.STOPPED, Condition.ALWAYS)) {
            if (condition.matches(activeStates)) {
                RotationTrack track = candidates.get(condition);
                if (track != null) return track;
            }
        }
        return null;
    }

    public static AnimationDefinition parse(JsonElement element) {
        JsonObject root = asObject(element, "animation root");
        if (root.has("format") && !"magneticraft2:visual_animation".equals(
                string(root, "format", "animation root"))) {
            throw new JsonParseException("Unsupported animation format: " + root.get("format"));
        }
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
            throw new JsonParseException("Only block units, controller origin "
                    + "and canonical south facing are supported");
        }

        Map<String, Component> components = new HashMap<>();
        JsonObject parts = asObject(required(root, "components", "animation root"), "components");
        if (parts.size() == 0 || parts.size() > 256) {
            throw new JsonParseException("components must contain 1..256 entries");
        }
        for (Map.Entry<String, JsonElement> entry : parts.entrySet()) {
            String name = identifier(entry.getKey(), "component");
            JsonObject data = asObject(entry.getValue(), "component " + name);
            ResourceLocation model = null;
            if (data.has("model") && !data.get("model").isJsonNull()) {
                String modelId = string(data, "model", "component " + name);
                model = ResourceLocation.tryParse(modelId);
                if (model == null) {
                    throw new JsonParseException("Invalid model resource location for " + name + ": " + modelId);
                }
            }
            Vec3 offset = vector(required(data, "model_offset", name), name + ".model_offset");
            Vec3 pivot = vector(required(data, "pivot", name), name + ".pivot");
            String layerName = data.has("render_layer")
                    ? string(data, "render_layer", "component " + name) : "cutout";
            RenderLayer layer = switch (layerName) {
                case "solid" -> RenderLayer.SOLID;
                case "cutout" -> RenderLayer.CUTOUT;
                case "translucent" -> RenderLayer.TRANSLUCENT;
                default -> throw new JsonParseException("Unknown render_layer for " + name + ": " + layerName);
            };
            String parent = data.has("parent") && !data.get("parent").isJsonNull()
                    ? identifier(string(data, "parent", name), "parent") : null;
            Condition visible = data.has("visible_when")
                    ? Condition.parse(string(data, "visible_when", name)) : Condition.ALWAYS;
            components.put(name, new Component(model, offset, pivot, layer, parent, visible));
        }

        // Parent links are resolved once when resources are loaded, never per
        // render frame. Reject missing links and cycles before publishing.
        Map<String, List<String>> childBuilder = new HashMap<>();
        List<String> roots = new ArrayList<>();
        for (String name : components.keySet()) {
            Component component = components.get(name);
            if (component.parent() == null) {
                roots.add(name);
            } else {
                if (!components.containsKey(component.parent())) {
                    throw new JsonParseException("Unknown parent " + component.parent() + " of " + name);
                }
                if (name.equals(component.parent())) {
                    throw new JsonParseException("Component " + name + " parents itself");
                }
                childBuilder.computeIfAbsent(component.parent(), ignored -> new ArrayList<>()).add(name);
            }
        }
        Collections.sort(roots);
        Map<String, List<String>> children = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : childBuilder.entrySet()) {
            entry.getValue().sort(String::compareTo);
            children.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        int visited = 0;
        for (String name : roots) {
            visited += verifyTreeDepth(name, children, 1);
        }
        if (visited != components.size()) {
            throw new JsonParseException("Component parent cycle detected");
        }

        Map<String, Input> inputs = new HashMap<>();
        JsonObject declaredInputs = asObject(required(root, "inputs", "animation root"), "inputs");
        if (declaredInputs.size() > 64) {
            throw new JsonParseException("inputs cannot exceed 64 entries");
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

        Map<String, Map<Condition, RotationTrack>> rotations = new HashMap<>();
        JsonObject animations = asObject(required(root, "animations", "animation root"), "animations");
        if (animations.size() > 128) {
            throw new JsonParseException("animations cannot exceed 128 entries");
        }
        int trackCount = 0;
        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            String animationName = identifier(entry.getKey(), "animation");
            JsonObject animation = asObject(entry.getValue(), "animation " + animationName);
            if (!"driven".equals(string(animation, "type", animationName))) {
                throw new JsonParseException("Only typed driven tracks are supported");
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
                    throw new JsonParseException("Only rotation tracks are supported");
                }
                AxisSpec axis = axis(required(track, "axis", animationName));
                double scale = track.has("scale")
                        ? finiteNumber(track.get("scale"), "track scale") : 1.0D;
                if (Math.abs(scale) > 128.0D) {
                    throw new JsonParseException("Rotation track scale exceeds 128");
                }
                Condition when = track.has("when")
                        ? Condition.parse(string(track, "when", animationName)) : Condition.ALWAYS;
                RotationTrack compiled = new RotationTrack(
                        target, inputName, axis.axis(), axis.sign() * scale, when);
                Map<Condition, RotationTrack> candidates =
                        rotations.computeIfAbsent(target, ignored -> new EnumMap<>(Condition.class));
                if (candidates.putIfAbsent(when, compiled) != null) {
                    throw new JsonParseException("Duplicate rotation track for " + target + "/" + when);
                }
            }
        }
        if (trackCount == 0) {
            throw new JsonParseException("Animation has no rotation tracks");
        }
        Map<String, Map<Condition, RotationTrack>> frozen = new HashMap<>();
        for (Map.Entry<String, Map<Condition, RotationTrack>> entry : rotations.entrySet()) {
            frozen.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return new AnimationDefinition(components, inputs, frozen, children, roots);
    }

    private static int verifyTreeDepth(String name, Map<String, List<String>> children, int depth) {
        if (depth > 32) {
            throw new JsonParseException("Component hierarchy exceeds depth 32 at " + name);
        }
        int count = 1;
        for (String child : children.getOrDefault(name, List.of())) {
            count += verifyTreeDepth(child, children, depth + 1);
        }
        return count;
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
        throw new JsonParseException("This version supports only principal rotation axes");
    }
}
