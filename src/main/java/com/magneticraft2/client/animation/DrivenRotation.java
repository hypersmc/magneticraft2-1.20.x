package com.magneticraft2.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;

/**
 * Pure, stateless pose evaluator: no clock, network traffic, or game logic.
 * An animation track consumes the already-smoothed Gear V2 visual angle.
 */
public final class DrivenRotation {
    private DrivenRotation() {}

    /** The caller owns the PoseStack push/pop and renders the resolved model. */
    public static boolean apply(PoseStack pose, AnimationDefinition.Component component,
                                AnimationDefinition.RotationTrack track,
                                float mechanicalAngleDegrees) {
        if (!Float.isFinite(mechanicalAngleDegrees)) {
            return false;
        }
        double degrees = mechanicalAngleDegrees * track.scale();
        if (!Double.isFinite(degrees)) {
            return false;
        }

        Vec3 offset = component.modelOffset();
        Vec3 pivot = component.pivot();
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
