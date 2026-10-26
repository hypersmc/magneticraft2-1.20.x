package com.magneticraft2.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Stateless pose evaluator. All input angles come from Gear V2's existing
 * smoothed mechanical phase, never a separate client-side animation clock.
 *
 * The caller owns pushPose/popPose and the baked model draw.
 */
public final class DrivenRotation {
    private DrivenRotation() {}

    /**
     * Apply one typed driven-rotation track.
     * Returns false without modifying the pose if the definition is unavailable
     * or the requested model/input/track does not match.
     */
    public static boolean apply(PoseStack pose, ResourceLocation definitionId,
                                String target, ResourceLocation expectedModel,
                                String requiredModule, float mechanicalAngleDegrees) {
        AnimationDefinition definition = AnimationResourceLoader.get(definitionId);
        if (definition == null || !Float.isFinite(mechanicalAngleDegrees)) {
            return false;
        }
        AnimationDefinition.Component component = definition.component(target);
        AnimationDefinition.RotationTrack track = definition.rotation(target);
        if (component == null || track == null || !expectedModel.equals(component.model())) {
            return false;
        }
        AnimationDefinition.Input input = definition.input(track.input());
        if (input == null || !requiredModule.equals(input.module())
                || !"mechanical.visual_angle_degrees".equals(input.source())) {
            return false;
        }

        Vec3 offset = component.modelOffset();
        Vec3 pivot = component.pivot();
        double degrees = mechanicalAngleDegrees * track.scale();
        if (!Double.isFinite(degrees)) {
            return false;
        }
        pose.translate(offset.x, offset.y, offset.z);
        pose.translate(pivot.x, pivot.y, pivot.z);
        float angle = (float) degrees;
        switch (track.axis()) {
            case X -> pose.mulPose(Axis.XP.rotationDegrees(angle));
            case Y -> pose.mulPose(Axis.YP.rotationDegrees(angle));
            case Z -> pose.mulPose(Axis.ZP.rotationDegrees(angle));
        }
        pose.translate(-pivot.x, -pivot.y, -pivot.z);
        return true;
    }
}
