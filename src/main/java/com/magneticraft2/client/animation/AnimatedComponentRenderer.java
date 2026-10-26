package com.magneticraft2.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Resource-pack-driven renderer for an entire named component hierarchy.
 *
 * Preflights all visible models and bound inputs before drawing anything.
 * If a model is missing, the caller gets null and can render its complete
 * legacy Java fallback instead of showing half a machine or a missing cube.
 *
 * A successful result exposes the handled component IDs so older resource
 * packs that define only the original rotor can coexist with newly migrated
 * Java fallback parts. Hidden components still count as handled.
 */
public final class AnimatedComponentRenderer {
    private AnimatedComponentRenderer() {}

    public static AnimationDefinition render(ResourceLocation animation,
                                             AnimationFrame frame,
                                             BlockState blockState,
                                             PoseStack pose, MultiBufferSource buffers,
                                             int light, int overlay) {
        AnimationDefinition definition = AnimationResourceLoader.get(animation);
        if (definition == null) return null;

        Minecraft minecraft = Minecraft.getInstance();
        ModelManager modelManager = minecraft.getModelManager();
        for (String root : definition.roots()) {
            if (!validateSubtree(definition, root, frame, modelManager)) {
                return null;
            }
        }

        for (String root : definition.roots()) {
            renderSubtree(definition, root, frame, blockState, pose, buffers,
                    minecraft, modelManager, light, overlay);
        }
        return definition;
    }

    private static boolean validateSubtree(AnimationDefinition definition, String name,
                                           AnimationFrame frame, ModelManager models) {
        AnimationDefinition.Component component = definition.component(name);
        if (!component.visibleWhen().matches(frame.states())) {
            return true; // Children inherit visibility of their parent.
        }
        if (component.model() != null) {
            BakedModel model = models.getModel(component.model());
            if (model == null || model == models.getMissingModel()) {
                AnimationResourceLoader.warnMissingModel(component.model());
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
                                      AnimationFrame frame, BlockState blockState,
                                      PoseStack pose, MultiBufferSource buffers,
                                      Minecraft minecraft, ModelManager models,
                                      int light, int overlay) {
        AnimationDefinition.Component component = definition.component(name);
        if (!component.visibleWhen().matches(frame.states())) return;

        pose.pushPose();
        try {
            AnimationDefinition.RotationTrack track = definition.rotation(name, frame.states());
            float angle = 0F;
            if (track != null) {
                // Preflight guarantees the named input is available.
                angle = frame.angle(definition.input(track.input()).module());
            }
            DrivenRotation.apply(pose, component, track, angle);

            // Null model is a grouping joint: children inherit its pose but
            // it does not emit any geometry or duplicate an existing model.
            if (component.model() != null) {
                RenderType layer = switch (component.layer()) {
                    case SOLID -> RenderType.solid();
                    case CUTOUT -> RenderType.cutout();
                    case TRANSLUCENT -> RenderType.translucent();
                };
                minecraft.getBlockRenderer().getModelRenderer().renderModel(
                        pose.last(), buffers.getBuffer(layer), blockState,
                        models.getModel(component.model()), 1F, 1F, 1F, light, overlay);
            }
            for (String child : definition.children(name)) {
                renderSubtree(definition, child, frame, blockState, pose,
                        buffers, minecraft, models, light, overlay);
            }
        } finally {
            pose.popPose();
        }
    }
}
