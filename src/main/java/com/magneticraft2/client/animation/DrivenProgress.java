package com.magneticraft2.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import java.util.List;

/**
 * Progress-keyframed local pose. All keys are strictly sorted, normalized,
 * and bounded at resource reload. Interpolation allocates no per-frame mesh
 * or temporary transform objects and cannot mutate machine gameplay.
 */
public final class DrivenProgress {
    private DrivenProgress() {}

    public static void apply(PoseStack pose, AnimationDefinition.TransformTrack track,
                             float progress) {
        List<AnimationDefinition.TransformKeyframe> keys = track.keyframes();
        float time = Math.max(0F, Math.min(1F, progress));
        int next = 1;
        while (next < keys.size() - 1 && time > keys.get(next).time()) {
            next++;
        }
        AnimationDefinition.TransformKeyframe a = keys.get(next - 1);
        AnimationDefinition.TransformKeyframe b = keys.get(next);
        double factor = (time - a.time()) / (b.time() - a.time());
        pose.translate(
                a.position().x + (b.position().x - a.position().x) * factor,
                a.position().y + (b.position().y - a.position().y) * factor,
                a.position().z + (b.position().z - a.position().z) * factor);
        float x = (float) (a.rotationDegrees().x
                + (b.rotationDegrees().x - a.rotationDegrees().x) * factor);
        float y = (float) (a.rotationDegrees().y
                + (b.rotationDegrees().y - a.rotationDegrees().y) * factor);
        float z = (float) (a.rotationDegrees().z
                + (b.rotationDegrees().z - a.rotationDegrees().z) * factor);
        if (x != 0F) pose.mulPose(Axis.XP.rotationDegrees(x));
        if (y != 0F) pose.mulPose(Axis.YP.rotationDegrees(y));
        if (z != 0F) pose.mulPose(Axis.ZP.rotationDegrees(z));
    }
}
