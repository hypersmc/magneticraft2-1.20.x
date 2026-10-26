package com.magneticraft2.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * Exact planar X-axis crank / Z-axis slider constraint.
 *
 * Geometry is rigid: the eccentric crank pin lies on a circle in YZ, the
 * connecting rod always measures rodLength and its far end meets the follower
 * on the slider axis at centerY. The crank, rod, follower and tray consume the
 * same sampled Gear V2 input angle, so there is no independent animation clock.
 *
 * All dimensions are in blocks in the canonical SOUTH local frame.
 */
public final class SliderCrankSolver {
    private SliderCrankSolver() {}

    public record Sample(double crankAngle, double pinY, double pinZ,
                         double rodAngle, double followerZ, double stroke) {

        /** Apply the global pose of one rigid mechanism part. */
        public void apply(PoseStack pose, AnimationDefinition.SliderCrank mechanism,
                          AnimationDefinition.SliderCrankRole role) {
            double x = mechanism.center().x;
            double y = mechanism.center().y;
            double z = mechanism.center().z;
            switch (role) {
                case CRANK -> {
                    pose.translate(x, y, z);
                    pose.mulPose(Axis.XP.rotationDegrees((float) crankAngle));
                }
                case ROD -> {
                    pose.translate(x, pinY, pinZ);
                    pose.mulPose(Axis.XP.rotationDegrees((float) rodAngle));
                }
                case FOLLOWER -> pose.translate(x, y, followerZ);
                case SLIDER -> pose.translate(0D, 0D, stroke);
            }
        }
    }

    public static Sample solve(AnimationDefinition.SliderCrank mechanism,
                               float inputAngleDegrees) {
        double crankAngle = inputAngleDegrees + mechanism.phaseDegrees();
        double theta = Math.toRadians(crankAngle);
        double radius = mechanism.radius();
        double length = mechanism.rodLength();

        // In the local SOUTH frame, the shaft rotates about +X and the
        // connecting rod extends along +Z at its zero pose.
        double lateral = radius * Math.cos(theta);
        double longitudinal = radius * Math.sin(theta);
        double pinY = mechanism.center().y + lateral;
        double pinZ = mechanism.center().z + longitudinal;
        double rodAngle = Math.toDegrees(Math.asin(lateral / length));

        // The rod end is constrained to the slider rail centerline. Its exact
        // closure defines the tray stroke, not an approximation or sine wave.
        double endZ = pinZ + Math.sqrt(length * length - lateral * lateral);
        double followerZ = endZ;
        double stroke = endZ - mechanism.center().z - length;
        return new Sample(crankAngle, pinY, pinZ, rodAngle, followerZ, stroke);
    }
}
