package com.magneticraft2.client.render.blocks.stage.iron;

/** Pixel-space slider-crank geometry; no game state or synthetic clock. */
public final class IronShearMotion {
    public static final double AXLE_X = 8D;
    public static final double AXLE_Y = 24D;
    public static final double CRANK_RADIUS = 3D;
    public static final double ROD_LENGTH = 10D;

    private IronShearMotion() {}

    public static double pinX(double radians) {
        return AXLE_X + CRANK_RADIUS * Math.sin(radians);
    }

    public static double pinY(double radians) {
        return AXLE_Y - CRANK_RADIUS * Math.cos(radians);
    }

    public static double sliderY(double pinX, double pinY) {
        double offset = pinX - AXLE_X;
        return pinY - Math.sqrt(ROD_LENGTH * ROD_LENGTH - offset * offset);
    }

    /** Rotation taking the link's local +Y from its crank pin to its slider pin. */
    public static float linkDegrees(double pinX, double pinY, double sliderY) {
        return (float) Math.toDegrees(Math.atan2(pinX - AXLE_X, sliderY - pinY));
    }
}
