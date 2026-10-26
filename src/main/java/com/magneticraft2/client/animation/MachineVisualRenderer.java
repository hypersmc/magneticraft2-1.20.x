package com.magneticraft2.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic boundary between a machine's real Java state and its resource-pack
 * visual package:
 *
 * machine_visuals/<machine>.json -> animations/<scene>.json -> baked models.
 *
 * Only the Java renderer can supply physical channels and state flags. A
 * manifest can rename/reuse these channels but cannot manufacture another
 * input, power source, item/fluid connection or simulation.
 *
 * All resource definitions are immutable, compiled at reload. Missing/invalid
 * binding, scene, model or input yields null before drawing any scene geometry,
 * leaving the machine's original Java fallback intact.
 */
public final class MachineVisualRenderer {
    private MachineVisualRenderer() {}

    /** The currently selected animation, or null if its visual pack is unavailable. */
    public static AnimationDefinition definition(ResourceLocation machineId) {
        MachineVisualDefinition visual = MachineVisualResourceLoader.get(machineId);
        return visual == null ? null : AnimationResourceLoader.get(visual.animation());
    }

    public static AnimationDefinition render(ResourceLocation machineId,
                                             AnimationFrame physicalInputs,
                                             BlockState blockState,
                                             PoseStack pose, MultiBufferSource buffers,
                                             int light, int overlay) {
        AnimatedComponentRenderer.SceneResult scene = renderScene(
                machineId, physicalInputs, blockState, pose, buffers, light, overlay);
        return scene == null ? null : scene.definition();
    }

    public static AnimatedComponentRenderer.SceneResult renderScene(
            ResourceLocation machineId, AnimationFrame physicalInputs,
            BlockState blockState, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        MachineVisualDefinition visual = MachineVisualResourceLoader.get(machineId);
        if (visual == null) return null;
        AnimationDefinition definition = AnimationResourceLoader.get(visual.animation());
        if (definition == null) return null;

        Map<String, Float> angles = new HashMap<>();
        for (Map.Entry<String, String> binding : visual.angleChannels().entrySet()) {
            Float value = physicalInputs.angle(binding.getValue());
            if (value == null) {
                MachineVisualResourceLoader.warnUnavailableInput(machineId, binding.getValue());
                return null;
            }
            angles.put(binding.getKey(), value);
        }
        Map<String, Integer> integers = new HashMap<>();
        for (Map.Entry<String, String> binding : visual.integerChannels().entrySet()) {
            Integer value = physicalInputs.integer(binding.getValue());
            if (value == null) {
                MachineVisualResourceLoader.warnUnavailableInput(machineId, binding.getValue());
                return null;
            }
            integers.put(binding.getKey(), value);
        }
        // Validate the whole declared contract before drawing any geometry.
        // In particular, an unknown integer wear selector must not silently
        // switch a damaged mesh back to its fresh base model.
        for (AnimationDefinition.Input input : definition.inputs()) {
            if (!angles.containsKey(input.module())) {
                MachineVisualResourceLoader.warnUnavailableInput(machineId, input.module());
                return null;
            }
        }
        for (AnimationDefinition.Component component : definition.components()) {
            AnimationDefinition.ModelVariants variants = component.modelVariants();
            if (variants != null && !integers.containsKey(variants.source())) {
                MachineVisualResourceLoader.warnUnavailableInput(machineId, variants.source());
                return null;
            }
        }
        // Preserve exactly the real, synchronized machine states. The visual
        // pack has no means of changing running, overload, or wear authority.
        AnimationFrame rebound = new AnimationFrame(angles, physicalInputs.states(), integers);
        return AnimatedComponentRenderer.renderScene(definition, rebound, blockState,
                pose, buffers, light, overlay);
    }
}
