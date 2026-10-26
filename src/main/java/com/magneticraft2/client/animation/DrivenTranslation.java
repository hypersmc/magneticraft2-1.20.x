package com.magneticraft2.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;

/**
 * Deterministic, bounded straight-line motion from the phase of an existing
 * mechanical shaft. No per-machine timer or physics approximation.
 *
 * Uses one immutable translation track and one current Gear V2 visual angle;
 * all components driven by the same source stay in exact phase.
 */
public final class DrivenTranslation {
    private DrivenTranslation() {}

    public static double distance(AnimationDefinition.TranslationTrack track,
                                  float angleDegrees) {
        double radians = Math.toRadians(angleDegrees * track.scale()
                + track.phaseDegrees());
        double wave = switch (track.waveform()) {
            case COSINE -> Math.cos(radians);
            case SINE -> Math.sin(radians);
        };
        return track.distance() * (track.bias() + track.amplitude() * wave);
    }

    /** Slide along a fixed principal axis in the PARENT local frame. */
    public static void apply(PoseStack pose, AnimationDefinition.TranslationTrack track,
                             float angleDegrees) {
        applyDistance(pose, track, distance(track, angleDegrees));
    }

    /** Reuse an already evaluated scene displacement (including item effects). */
    public static void applyDistance(PoseStack pose,
                                     AnimationDefinition.TranslationTrack track,
                                     double travel) {
        switch (track.axis()) {
            case X -> pose.translate(travel, 0D, 0D);
            case Y -> pose.translate(0D, travel, 0D);
            case Z -> pose.translate(0D, 0D, travel);
        }
    }
}
