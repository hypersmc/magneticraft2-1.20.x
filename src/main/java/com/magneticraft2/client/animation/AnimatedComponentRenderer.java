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
 * Generic model renderer for an animated named component.
 *
 * The renderer uses the model id from the RESOURCE PACK definition, not a
 * Java-side expected-model constant. Returns false if the resource is absent
 * or invalid, so the caller can keep its previous Java renderer as fallback.
 *
 * Geometry, textures, pivot, render layer, translation and rotation are all
 * independent of the machine's server-side progression/mechanical simulation.
 */
public final class AnimatedComponentRenderer {
    private AnimatedComponentRenderer() {}

    public static boolean renderDriven(ResourceLocation animation, String componentName,
                                       String requiredModule, float mechanicalAngleDegrees,
                                       BlockState blockState, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay) {
        AnimationDefinition definition = AnimationResourceLoader.get(animation);
        if (definition == null) {
            return false;
        }
        AnimationDefinition.Component component = definition.component(componentName);
        AnimationDefinition.RotationTrack track = definition.rotation(componentName);
        if (component == null || track == null) {
            return false;
        }
        AnimationDefinition.Input input = definition.input(track.input());
        if (input == null || !requiredModule.equals(input.module())
                || !"mechanical.visual_angle_degrees".equals(input.source())) {
            return false;
        }
        if (!Float.isFinite(mechanicalAngleDegrees)
                || !Double.isFinite(mechanicalAngleDegrees * track.scale())) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ModelManager models = minecraft.getModelManager();
        BakedModel model = models.getModel(component.model());
        if (model == null || model == models.getMissingModel()) {
            AnimationResourceLoader.warnMissingModel(component.model());
            return false;
        }

        RenderType layer = switch (component.layer()) {
            case SOLID -> RenderType.solid();
            case CUTOUT -> RenderType.cutout();
            case TRANSLUCENT -> RenderType.translucent();
        };

        pose.pushPose();
        try {
            if (!DrivenRotation.apply(pose, component, track, mechanicalAngleDegrees)) {
                return false;
            }
            minecraft.getBlockRenderer().getModelRenderer().renderModel(
                    pose.last(), buffers.getBuffer(layer), blockState, model,
                    1F, 1F, 1F, light, overlay);
            return true;
        } finally {
            pose.popPose();
        }
    }
}
