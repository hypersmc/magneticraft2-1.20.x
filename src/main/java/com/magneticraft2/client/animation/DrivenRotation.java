package com.magneticraft2.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;

/**
 * Reusable transform evaluation: T(offset) T(pivot) R(axis,phase*scale) T(-pivot).
 *
 * For a child component, this is evaluated inside its parent's transformed
 * PoseStack. For a static component/group with no active track, only the
 * local offset is applied; descendants still inherit the parent transform.
 */
public final class DrivenRotation {
    private DrivenRotation() {}

    public static void apply(PoseStack pose, AnimationDefinition.Component component,
                             AnimationDefinition.RotationTrack track, float angleDegrees) {
        Vec3 offset = component.modelOffset();
        pose.translate(offset.x, offset.y, offset.z);
        if (track == null) return;

        Vec3 pivot = component.pivot();
        pose.translate(pivot.x, pivot.y, pivot.z);
        float degrees = (float) (angleDegrees * track.scale());
        switch (track.axis()) {
            case X -> pose.mulPose(Axis.XP.rotationDegrees(degrees));
            case Y -> pose.mulPose(Axis.YP.rotationDegrees(degrees));
            case Z -> pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        }
        pose.translate(-pivot.x, -pivot.y, -pivot.z);
    }
}
