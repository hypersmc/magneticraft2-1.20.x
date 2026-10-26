package com.magneticraft2.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * General resource-pack scene renderer: models, parent joints, wear variants,
 * typed rotations and rigid slider-crank constraints all come from JSON.
 *
 * All visible models and driver inputs are checked before drawing. On an
 * invalid definition or missing baked model, null is returned without any
 * scene geometry written, allowing the caller's complete Java fallback.
 */
public final class AnimatedComponentRenderer {
    private AnimatedComponentRenderer() {}

    public record SceneResult(AnimationDefinition definition,
                              Map<String, SliderCrankSolver.Sample> mechanismPoses) {
        public SceneResult {
            mechanismPoses = Map.copyOf(mechanismPoses);
        }

        public double sliderOffset(String mechanismName) {
            SliderCrankSolver.Sample sample = mechanismPoses.get(mechanismName);
            return sample == null ? 0.0D : sample.stroke();
        }
    }

    /**
     * Backward-compatible entry point for existing Lathe integrations.
     * It still returns null if the definition or required model is invalid.
     */
    public static AnimationDefinition render(ResourceLocation animation,
                                             AnimationFrame frame,
                                             BlockState blockState,
                                             PoseStack pose, MultiBufferSource buffers,
                                             int light, int overlay) {
        SceneResult result = renderScene(animation, frame, blockState, pose,
                buffers, light, overlay);
        return result == null ? null : result.definition();
    }

    /**
     * The scene result also exposes the precise slider displacement so
     * in-world workpieces can follow the same JSON-driven sieve carriage.
     */
    public static SceneResult renderScene(ResourceLocation animation,
                                          AnimationFrame frame,
                                          BlockState blockState,
                                          PoseStack pose, MultiBufferSource buffers,
                                          int light, int overlay) {
        return renderScene(AnimationResourceLoader.get(animation), frame,
                blockState, pose, buffers, light, overlay);
    }

    /**
     * Render an already resolved, immutable animation scene. Machine visual
     * manifests can select the scene without adding another model rendering
     * implementation or re-parsing anything on the render thread.
     */
    public static SceneResult renderScene(AnimationDefinition definition,
                                          AnimationFrame frame,
                                          BlockState blockState,
                                          PoseStack pose, MultiBufferSource buffers,
                                          int light, int overlay) {
        if (definition == null) return null;

        Map<String, SliderCrankSolver.Sample> poses = new HashMap<>();
        for (Map.Entry<String, AnimationDefinition.SliderCrank> entry
                : definition.sliderCranks().entrySet()) {
            AnimationDefinition.SliderCrank mechanism = entry.getValue();
            AnimationDefinition.Input input = definition.input(mechanism.input());
            if (input == null || !"mechanical.visual_angle_degrees".equals(input.source())) {
                return null;
            }
            Float angle = frame.angle(input.module());
            if (angle == null || !Float.isFinite(angle)) return null;
            SliderCrankSolver.Sample solved = SliderCrankSolver.solve(mechanism, angle);
            if (!Double.isFinite(solved.stroke()) || !Double.isFinite(solved.rodAngle())) {
                return null;
            }
            poses.put(entry.getKey(), solved);
        }

        Minecraft minecraft = Minecraft.getInstance();
        ModelManager modelManager = minecraft.getModelManager();
        for (String root : definition.roots()) {
            if (!validateSubtree(definition, root, frame, modelManager)) {
                return null;
            }
        }

        for (String root : definition.roots()) {
            renderSubtree(definition, root, frame, poses, blockState,
                    pose, buffers, minecraft, modelManager, light, overlay);
        }
        return new SceneResult(definition, poses);
    }

    private static ResourceLocation modelFor(AnimationDefinition.Component component,
                                             AnimationFrame frame) {
        AnimationDefinition.ModelVariants variants = component.modelVariants();
        if (variants != null) {
            Integer index = frame.integer(variants.source());
            if (index != null) {
                ResourceLocation model = variants.models().get(index);
                if (model != null) return model;
            }
        }
        return component.model();
    }

    private static boolean validateSubtree(AnimationDefinition definition, String name,
                                           AnimationFrame frame, ModelManager models) {
        AnimationDefinition.Component component = definition.component(name);
        if (!component.visibleWhen().matches(frame.states())) {
            return true; // Children inherit their parent's visibility.
        }
        ResourceLocation modelId = modelFor(component, frame);
        if (modelId != null) {
            BakedModel model = models.getModel(modelId);
            if (model == null || model == models.getMissingModel()) {
                AnimationResourceLoader.warnMissingModel(modelId);
                return false;
            }
        }
        AnimationDefinition.RotationTrack track = definition.rotation(name, frame.states());
        if (track != null) {
            AnimationDefinition.Input input = definition.input(track.input());
            if (input == null || !"mechanical.visual_angle_degrees".equals(input.source())) {
                return false;
            }
            Float angle = frame.angle(input.module());
            if (angle == null || !Float.isFinite(angle)
                    || !Double.isFinite(angle.doubleValue() * track.scale())) {
                return false;
            }
        }
        for (String child : definition.children(name)) {
            if (!validateSubtree(definition, child, frame, models)) return false;
        }
        return true;
    }

    private static void renderSubtree(AnimationDefinition definition, String name,
                                      AnimationFrame frame,
                                      Map<String, SliderCrankSolver.Sample> mechanismPoses,
                                      BlockState blockState,
                                      PoseStack pose, MultiBufferSource buffers,
                                      Minecraft minecraft, ModelManager models,
                                      int light, int overlay) {
        AnimationDefinition.Component component = definition.component(name);
        if (!component.visibleWhen().matches(frame.states())) return;

        pose.pushPose();
        try {
            AnimationDefinition.MechanismRole binding = definition.mechanismRole(name);
            if (binding != null) {
                SliderCrankSolver.Sample solved = mechanismPoses.get(binding.mechanismId());
                AnimationDefinition.SliderCrank mechanism =
                        definition.sliderCranks().get(binding.mechanismId());
                solved.apply(pose, mechanism, binding.role());
            }

            AnimationDefinition.RotationTrack track = definition.rotation(name, frame.states());
            float angle = track == null
                    ? 0F : frame.angle(definition.input(track.input()).module());
            DrivenRotation.apply(pose, component, track, angle);

            ResourceLocation modelId = modelFor(component, frame);
            if (modelId != null) {
                RenderType layer = switch (component.layer()) {
                    case SOLID -> RenderType.solid();
                    case CUTOUT -> RenderType.cutout();
                    case TRANSLUCENT -> RenderType.translucent();
                };
                minecraft.getBlockRenderer().getModelRenderer().renderModel(
                        pose.last(), buffers.getBuffer(layer), blockState,
                        models.getModel(modelId), 1F, 1F, 1F, light, overlay);
            }
            for (String child : definition.children(name)) {
                renderSubtree(definition, child, frame, mechanismPoses, blockState,
                        pose, buffers, minecraft, models, light, overlay);
            }
        } finally {
            pose.popPose();
        }
    }
}
