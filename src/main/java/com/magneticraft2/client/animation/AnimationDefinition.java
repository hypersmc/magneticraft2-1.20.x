package com.magneticraft2.client.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.magneticraft2.common.systems.GEAR.BeltPath;
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
    public record StaticRotation(Direction.Axis axis, double degrees) {}
    public record ModelVariants(String source, Map<Integer, ResourceLocation> models) {
        public ModelVariants {
            models = Map.copyOf(models);
        }
    }
    public record Component(ResourceLocation model, Vec3 modelOffset, Vec3 pivot,
                            RenderLayer layer, String parent, Condition visibleWhen,
                            StaticRotation staticRotation, ModelVariants modelVariants) {}
    public enum SliderCrankRole { CRANK, ROD, FOLLOWER, SLIDER }
    public record SliderCrank(String input, Vec3 center, double radius, double rodLength,
                              double phaseDegrees, String crank, String rod,
                              String follower, String slider) {}
    public record MechanismRole(String mechanismId, SliderCrankRole role) {}
    public record Input(String source, String module) {}

    /**
     * An immutable, precomputed loop of UV-tiled belt geometry. JSON controls
     * geometry and appearance; the machine supplies a real, cumulative signed
     * shaft angle on its client renderer. Path segments are built on reload,
     * never once per render frame.
     *
     * centerRadius specifies the ACTUAL rendered belt centerline radius.
     * BeltPath internally adds BELT_CLEARANCE to its radius arguments, so
     * we subtract that constant when generating the visual-only path.
     */
    public record BeltLoop(String input, ResourceLocation texture,
                           Vec3 startCenter, Vec3 endCenter, Direction.Axis axis,
                           double startRadius, double endRadius,
                           double driveRadius, double halfWidth,
                           double halfThickness, double textureRepeat,
                           double speedScale, double textureOffset,
                           List<BeltPath.Segment> segments) {
        public BeltLoop {
            segments = List.copyOf(segments);
        }
    }
    /** Rotation phase is added AFTER the input angle has been scaled. */
    public record RotationTrack(String target, String input, Direction.Axis axis,
                                double scale, double phaseDegrees, Condition when) {}

    public enum Waveform {
        COSINE, SINE;
        static Waveform parse(String value) {
            return switch (value) {
                case "cosine" -> COSINE;
                case "sine" -> SINE;
                default -> throw new JsonParseException("Unsupported translation waveform: " + value);
            };
        }
    }

    /**
     * A linear slide along a principal machine axis, driven by a real phase.
     * displacement = distance * (bias + amplitude * wave(angle*scale + phase)).
     * A missing conditionally active track means zero displacement.
     */
    public record TranslationTrack(String target, String input, Direction.Axis axis,
                                   double distance, double scale, double amplitude,
                                   double bias, double phaseDegrees, Waveform waveform,
                                   Condition when) {}
    private record AxisSpec(Direction.Axis axis, double sign) {}

    private final Map<String, Component> components;
    private final Map<String, Input> inputs;
    private final Map<String, Map<Condition, RotationTrack>> rotations;
    private final Map<String, Map<Condition, TranslationTrack>> translations;
    private final Map<String, List<String>> children;
    private final List<String> roots;
    private final Map<String, SliderCrank> sliderCranks;
    private final Map<String, MechanismRole> mechanismRoles;
    private final Map<String, BeltLoop> beltLoops;

    private AnimationDefinition(Map<String, Component> components,
                                Map<String, Input> inputs,
                                Map<String, Map<Condition, RotationTrack>> rotations,
                                Map<String, Map<Condition, TranslationTrack>> translations,
                                Map<String, List<String>> children, List<String> roots,
                                Map<String, SliderCrank> sliderCranks,
                                Map<String, MechanismRole> mechanismRoles,
                                Map<String, BeltLoop> beltLoops) {
        this.components = Map.copyOf(components);
        this.inputs = Map.copyOf(inputs);
        this.rotations = Map.copyOf(rotations);
        this.translations = Map.copyOf(translations);
        this.children = Map.copyOf(children);
        this.roots = List.copyOf(roots);
        this.sliderCranks = Map.copyOf(sliderCranks);
        this.mechanismRoles = Map.copyOf(mechanismRoles);
        this.beltLoops = Map.copyOf(beltLoops);
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

    /** Typed visual inputs, checked against a real Java-supplied frame. */
    public Collection<Input> inputs() {
        return inputs.values();
    }

    public Map<String, SliderCrank> sliderCranks() {
        return sliderCranks;
    }

    public MechanismRole mechanismRole(String componentName) {
        return mechanismRoles.get(componentName);
    }

    /** Named, immutable loop definitions used by any opted-in machine renderer. */
    public Map<String, BeltLoop> beltLoops() {
        return beltLoops;
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

    /** Return the most-specific active linear translation, or null for rest. */
    public TranslationTrack translation(String name, Set<State> activeStates) {
        Map<Condition, TranslationTrack> candidates = translations.get(name);
        if (candidates == null) return null;
        for (Condition condition : List.of(Condition.OVERLOADED, Condition.WORN,
                Condition.RUNNING, Condition.STOPPED, Condition.ALWAYS)) {
            if (condition.matches(activeStates)) {
                TranslationTrack track = candidates.get(condition);
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
            StaticRotation staticRotation = null;
            if (data.has("static_rotation")) {
                JsonObject rotation = asObject(data.get("static_rotation"), name + ".static_rotation");
                AxisSpec localAxis = axis(required(rotation, "axis", name + ".static_rotation"));
                double degrees = finiteNumber(required(rotation, "degrees",
                        name + ".static_rotation"), "static rotation degrees");
                if (Math.abs(degrees) > 3600.0D) {
                    throw new JsonParseException("Static rotation exceeds 3600 degrees for " + name);
                }
                staticRotation = new StaticRotation(localAxis.axis(), degrees * localAxis.sign());
            }
            ModelVariants variants = null;
            if (data.has("model_variants")) {
                if (model == null) {
                    throw new JsonParseException(name + " needs a base model for model_variants fallback");
                }
                JsonObject variant = asObject(data.get("model_variants"), name + ".model_variants");
                String selector = string(variant, "source", name + ".model_variants");
                // This is a visual input ALIAS, not a game-state query.
                // The machine visual manifest must map it to an integer
                // supplied by Java. Unknown inputs fail preflight before any
                // model renders; JSON cannot access arbitrary game state.
                if (selector.length() > 128
                        || !selector.matches("[a-z][a-z0-9_.]*")) {
                    throw new JsonParseException("Invalid model variant input alias: " + selector);
                }
                JsonObject models = asObject(required(variant, "models", name), name + ".models");
                if (models.size() == 0 || models.size() > 6) {
                    throw new JsonParseException(name + " model_variants must contain 1..6 stages");
                }
                Map<Integer, ResourceLocation> byStage = new HashMap<>();
                for (Map.Entry<String, JsonElement> modelEntry : models.entrySet()) {
                    int stage;
                    try {
                        stage = Integer.parseInt(modelEntry.getKey());
                    } catch (NumberFormatException exception) {
                        throw new JsonParseException("Invalid wear stage " + modelEntry.getKey(), exception);
                    }
                    if (stage < 0 || stage > 5 || !Integer.toString(stage).equals(modelEntry.getKey())) {
                        throw new JsonParseException("Wear stage must be between 0 and 5: " + stage);
                    }
                    if (!modelEntry.getValue().isJsonPrimitive()
                            || !modelEntry.getValue().getAsJsonPrimitive().isString()) {
                        throw new JsonParseException("Variant model for stage " + stage + " must be an ID");
                    }
                    String resource = modelEntry.getValue().getAsString();
                    ResourceLocation stageModel = ResourceLocation.tryParse(resource);
                    if (stageModel == null) {
                        throw new JsonParseException("Invalid variant model " + resource);
                    }
                    byStage.put(stage, stageModel);
                }
                variants = new ModelVariants(selector, byStage);
            }
            components.put(name, new Component(model, offset, pivot, layer, parent, visible,
                    staticRotation, variants));
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
        Map<String, Map<Condition, TranslationTrack>> translations = new HashMap<>();
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
                String property = string(track, "property", animationName);
                AxisSpec axis = axis(required(track, "axis", animationName));
                double scale = track.has("scale")
                        ? finiteNumber(track.get("scale"), "track scale") : 1.0D;
                if (Math.abs(scale) > 128.0D) {
                    throw new JsonParseException("Track angle scale exceeds 128");
                }
                double phase = track.has("phase_degrees")
                        ? finiteNumber(track.get("phase_degrees"), "track phase_degrees") : 0.0D;
                if (Math.abs(phase) > 3600.0D) {
                    throw new JsonParseException("Track phase_degrees exceeds 3600");
                }
                Condition when = track.has("when")
                        ? Condition.parse(string(track, "when", animationName)) : Condition.ALWAYS;

                switch (property) {
                    case "rotation" -> {
                        RotationTrack compiled = new RotationTrack(target, inputName,
                                axis.axis(), axis.sign() * scale, axis.sign() * phase, when);
                        Map<Condition, RotationTrack> candidates =
                                rotations.computeIfAbsent(target, ignored -> new EnumMap<>(Condition.class));
                        if (candidates.putIfAbsent(when, compiled) != null) {
                            throw new JsonParseException("Duplicate rotation track for " + target + "/" + when);
                        }
                    }
                    case "translation" -> {
                        double distance = finiteNumber(required(track, "distance", animationName),
                                "track distance");
                        double amplitude = track.has("amplitude")
                                ? finiteNumber(track.get("amplitude"), "track amplitude") : 1.0D;
                        double bias = track.has("bias")
                                ? finiteNumber(track.get("bias"), "track bias") : 0.0D;
                        Waveform waveform = Waveform.parse(string(track, "waveform", animationName));
                        if (Math.abs(distance) > 16.0D || Math.abs(amplitude) > 4.0D
                                || Math.abs(bias) > 4.0D
                                || Math.abs(distance) * (Math.abs(amplitude) + Math.abs(bias)) > 16.0D) {
                            throw new JsonParseException("Translation track would leave the 16-block bound");
                        }
                        TranslationTrack compiled = new TranslationTrack(target, inputName,
                                axis.axis(), axis.sign() * distance, scale, amplitude,
                                bias, phase, waveform, when);
                        Map<Condition, TranslationTrack> candidates =
                                translations.computeIfAbsent(target, ignored -> new EnumMap<>(Condition.class));
                        if (candidates.putIfAbsent(when, compiled) != null) {
                            throw new JsonParseException("Duplicate translation track for " + target + "/" + when);
                        }
                    }
                    default -> throw new JsonParseException("Unsupported animation track property: " + property);
                }
            }
        }
        // Continuous loop geometry uses the shared physical BeltPath.
        // These optional visual loops do NOT change the server's belt/collision
        // geometry or create Gear V2 connections.
        Map<String, BeltLoop> beltLoops = new HashMap<>();
        double totalEstimatedBeltTiles = 0.0D;
        if (root.has("belt_loops")) {
            JsonObject loops = asObject(root.get("belt_loops"), "belt_loops");
            if (loops.size() > 8) {
                throw new JsonParseException("belt_loops cannot exceed 8 loops");
            }
            for (Map.Entry<String, JsonElement> entry : loops.entrySet()) {
                String name = identifier(entry.getKey(), "belt loop");
                JsonObject data = asObject(entry.getValue(), "belt loop " + name);
                if (!"open_belt".equals(string(data, "type", name))) {
                    throw new JsonParseException("Unsupported belt type in " + name);
                }
                String inputName = identifier(string(data, "input", name), "belt input");
                Input input = inputs.get(inputName);
                if (input == null || !"mechanical.visual_angle_degrees".equals(input.source())) {
                    throw new JsonParseException(name
                            + " requires a real visual-angle input from the machine");
                }
                String textureId = string(data, "texture", name);
                ResourceLocation texture = ResourceLocation.tryParse(textureId);
                if (texture == null
                        || !texture.getPath().startsWith("textures/")
                        || !texture.getPath().endsWith(".png")) {
                    throw new JsonParseException(name
                            + " requires a full texture resource location under textures/*.png");
                }
                Vec3 start = vector(required(data, "start_center", name),
                        name + ".start_center");
                Vec3 end = vector(required(data, "end_center", name),
                        name + ".end_center");
                AxisSpec axis = axis(required(data, "rotation_axis", name));
                if (axis.sign() != 1.0D) {
                    throw new JsonParseException(name + " needs a positive principal axis");
                }
                double r1 = finiteNumber(required(data, "start_radius", name),
                        name + ".start_radius");
                double r2 = finiteNumber(required(data, "end_radius", name),
                        name + ".end_radius");
                double driveRadius = data.has("drive_radius")
                        ? finiteNumber(data.get("drive_radius"), name + ".drive_radius") : r1;
                double halfWidth = finiteNumber(required(data, "half_width", name),
                        name + ".half_width");
                double halfThickness = finiteNumber(required(data, "half_thickness", name),
                        name + ".half_thickness");
                double repeat = finiteNumber(required(data, "texture_repeat", name),
                        name + ".texture_repeat");
                double speed = data.has("speed_scale")
                        ? finiteNumber(data.get("speed_scale"), name + ".speed_scale") : 1.0D;
                double textureOffset = data.has("texture_offset")
                        ? finiteNumber(data.get("texture_offset"), name + ".texture_offset") : 0.0D;
                double centerDist = start.distanceTo(end);
                if (r1 <= BeltPath.BELT_CLEARANCE || r2 <= BeltPath.BELT_CLEARANCE
                        || driveRadius <= 0D || r1 > 4D || r2 > 4D
                        || driveRadius > 4D || halfWidth <= 0D || halfWidth > 0.5D
                        || halfThickness <= 0D || halfThickness > 0.5D
                        || repeat < 0.0625D || repeat > 4D
                        || halfWidth * 2.0D > repeat
                        || halfThickness * 2.0D > repeat
                        || Math.abs(speed) > 16D || Math.abs(textureOffset) > 64D
                        || centerDist < 0.125D || centerDist > 16D
                        || centerDist <= Math.abs(r1 - r2)) {
                    throw new JsonParseException(name
                            + " has invalid radii, thickness, tile repeat, travel or span");
                }
                // Explicitly reject geometry that collides with the pulley
                // axis and causes BeltPath to return no tangent segments.
                List<BeltPath.Segment> segments = BeltPath.createVisualSegments(
                        start, end, axis.axis(),
                        r1 - BeltPath.BELT_CLEARANCE,
                        r2 - BeltPath.BELT_CLEARANCE);
                if (segments.isEmpty()) {
                    throw new JsonParseException(name
                            + " could not produce a valid BeltPath loop");
                }
                // Bound path tessellation and UV tile-splitting cost for
                // custom resource packs. No unbounded per-frame quads.
                double length = segments.get(segments.size() - 1).endDistance();
                double estimatedTiles = segments.size() + length / repeat + 1.0D;
                totalEstimatedBeltTiles += estimatedTiles;
                if (estimatedTiles > 192.0D || totalEstimatedBeltTiles > 512.0D) {
                    throw new JsonParseException(name
                            + " exceeds per-frame visual belt tile budget");
                }
                beltLoops.put(name, new BeltLoop(inputName, texture, start, end,
                        axis.axis(), r1, r2, driveRadius, halfWidth,
                        halfThickness, repeat, speed, textureOffset, segments));
            }
        }

        // Mechanisms bind rigid components to a single typed input. Only the
        // X-axis crank and Z-axis slider are currently supported; other axes
        // must get an explicit tested solver rather than guessed transforms.
        Map<String, SliderCrank> mechanisms = new HashMap<>();
        Map<String, MechanismRole> mechanismRoles = new HashMap<>();
        if (root.has("mechanisms")) {
            JsonObject definitions = asObject(root.get("mechanisms"), "mechanisms");
            if (definitions.size() > 16) {
                throw new JsonParseException("Too many mechanisms (maximum 16)");
            }
            for (Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
                String name = identifier(entry.getKey(), "mechanism");
                JsonObject data = asObject(entry.getValue(), "mechanism " + name);
                if (!"slider_crank".equals(string(data, "type", name))) {
                    throw new JsonParseException("Unsupported mechanism type in " + name);
                }
                String inputName = identifier(string(data, "input", name), "mechanism input");
                if (!inputs.containsKey(inputName)) {
                    throw new JsonParseException("Unknown mechanism input " + inputName);
                }
                if (!"x".equals(string(data, "rotation_axis", name))
                        || !"z".equals(string(data, "slide_axis", name))) {
                    throw new JsonParseException(name + " requires X rotation and Z slider axes");
                }
                Vec3 center = vector(required(data, "center", name), name + ".center");
                double radius = finiteNumber(required(data, "radius", name), name + ".radius");
                double length = finiteNumber(required(data, "rod_length", name), name + ".rod_length");
                if (radius <= 0.0D || radius > 16.0D || length <= radius || length > 16.0D) {
                    throw new JsonParseException(name + " requires 0 < radius < rod_length <= 16");
                }
                double phase = data.has("phase_degrees")
                        ? finiteNumber(data.get("phase_degrees"), name + ".phase_degrees") : 0.0D;
                if (Math.abs(phase) > 3600.0D) {
                    throw new JsonParseException(name + " phase_degrees exceeds 3600");
                }
                JsonObject partsByRole = asObject(required(data, "parts", name), name + ".parts");
                String crank = identifier(string(partsByRole, "crank", name), "crank component");
                String rod = identifier(string(partsByRole, "rod", name), "rod component");
                String follower = identifier(string(partsByRole, "follower", name), "follower component");
                String slider = identifier(string(partsByRole, "slider", name), "slider component");
                String[] roleNames = { crank, rod, follower, slider };
                SliderCrankRole[] roles = SliderCrankRole.values();
                for (int i = 0; i < roleNames.length; i++) {
                    String target = roleNames[i];
                    Component targetPart = components.get(target);
                    if (targetPart == null || targetPart.parent() != null) {
                        throw new JsonParseException(name + " role " + roles[i]
                                + " must reference an existing root component: " + target);
                    }
                    if (rotations.containsKey(target) || translations.containsKey(target)) {
                        throw new JsonParseException(name
                                + " conflicts with driven tracks on slider-crank role " + target);
                    }
                    if (mechanismRoles.putIfAbsent(target, new MechanismRole(name, roles[i])) != null) {
                        throw new JsonParseException("Component " + target
                                + " is bound to multiple mechanism roles");
                    }
                }
                mechanisms.put(name, new SliderCrank(inputName, center, radius, length,
                        phase, crank, rod, follower, slider));
            }
        }
        // Static-only scenes are valid too: a visual pack may provide
        // decorative model components without making up a mechanical clock.
        // Machine renderers still decide which legacy parts are required.
        Map<String, Map<Condition, RotationTrack>> frozen = new HashMap<>();
        for (Map.Entry<String, Map<Condition, RotationTrack>> entry : rotations.entrySet()) {
            frozen.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        Map<String, Map<Condition, TranslationTrack>> frozenTranslations = new HashMap<>();
        for (Map.Entry<String, Map<Condition, TranslationTrack>> entry : translations.entrySet()) {
            frozenTranslations.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return new AnimationDefinition(components, inputs, frozen, frozenTranslations,
                children, roots, mechanisms, mechanismRoles, beltLoops);
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
