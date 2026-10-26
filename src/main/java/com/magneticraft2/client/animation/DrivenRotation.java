package com.magneticraft2.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Reusable local component transform. The mechanism pose (if any) is applied
 * by the parent renderer BEFORE this local transform.
 *
 * T(offset) · [T(pivot) R(static) T(-pivot)]
 *           · [T(pivot) R(driven) T(-pivot)]
 *
 * For a child, all transforms remain relative to its animated parent's pose.
 */
public final class DrivenRotation {
    private DrivenRotation() {}

    public static void apply(PoseStack pose, AnimationDefinition.Component component,
                             AnimationDefinition.RotationTrack track, float angleDegrees) {
        Vec3 offset = component.modelOffset();
        pose.translate(offset.x, offset.y, offset.z);

        AnimationDefinition.StaticRotation fixed = component.staticRotation();
        if (fixed != null) {
            rotateAboutPivot(pose, component.pivot(), fixed.axis(),
                    (float) fixed.degrees());
        }
        if (track != null) {
            rotateAboutPivot(pose, component.pivot(), track.axis(),
                    (float) (angleDegrees * track.scale()));
        }
    }

    private static void rotateAboutPivot(PoseStack pose, Vec3 pivot,
                                         Direction.Axis axis, float degrees) {
        pose.translate(pivot.x, pivot.y, pivot.z);
        switch (axis) {
            case X -> pose.mulPose(Axis.XP.rotationDegrees(degrees));
            case Y -> pose.mulPose(Axis.YP.rotationDegrees(degrees));
            case Z -> pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        }
        pose.translate(-pivot.x, -pivot.y, -pivot.z);
    }
}
