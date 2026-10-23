package com.magneticraft2.client.render.blocks.stage.bronze;

/**
 * Client-only, inventory-free motion for the two visible rocks in the classifier.
 * Fixed 20 Hz steps; the rotating octagon carries rocks by friction and gravity
 * pulls them back to the bottom. No entity physics or allocations per frame.
 */
public final class RotaryClassifierOreMotion {
    public static final int FRAGMENT_COUNT = 2;
    public static final double AXIS_Y = 1.50D;
    // frame-design.json: (facetOffset 11.05 - skinThickness 0.85 / 2) / 16
    public static final double SCREEN_APOTHEM = (11.05D - 0.85D / 2D) / 16D;
    // 3D crushed ore: 0.5 GROUND transform, 3px offset, BER scale 0.82.
    // Its visual centre sits 0.136 blocks above the GROUND render origin.
    public static final double MODEL_CENTRE_AT_UNIT_SCALE = 0.166D;
    private static final double HALF_WIDTH = 0.155D;
    private static final double HALF_HEIGHT = 0.118D;
    private static final double SHAFT_CLEARANCE = 0.303D;
    private static final double GRAVITY = 0.019D;
    private static final double RESTITUTION = 0.12D;
    private static final double MAX_SPEED = 0.12D;
    private static final double MAX_AXIAL_Z = 0.60D;

    public static final class Fragment {
        private boolean active;
        private double x, y, z, roll, vx, vy, rollVelocity;
        private double previousX, previousY, previousZ, previousRoll;

        public boolean active() { return active; }
        public double x(float alpha) { return lerp(previousX, x, alpha); }
        public double y(float alpha) { return lerp(previousY, y, alpha); }
        public double z(float alpha) { return lerp(previousZ, z, alpha); }
        public double roll(float alpha) { return lerp(previousRoll, roll, alpha); }
        public double rawX() { return x; }
        public double rawY() { return y; }
        public double rawZ() { return z; }
        public double rawRoll() { return roll; }

        private void snapshot() {
            previousX = x;
            previousY = y;
            previousZ = z;
            previousRoll = roll;
        }

        private void spawn(int index, float progress) {
            active = true;
            x = index == 0 ? -0.07D : 0.13D;
            y = index == 0 ? -0.46D : -0.43D;
            // Approximate the axial position after a client joins mid-batch.
            // During normal play z advances from drum motion, not a progress lerp.
            z = -1.69D + Math.min(1D,
                    Math.max(0D, (progress - (index == 0 ? 0.20D : 0.33D)) / 0.80D)) * 2.05D;
            roll = index * 24D;
            vx = vy = rollVelocity = 0D;
            snapshot();
        }
    }

    public static final class FineChip {
        private boolean active;
        private double x, y, z, vy, previousY;
        public boolean active() { return active; }
        public double x() { return x; }
        public double y(float alpha) { return lerp(previousY, y, alpha); }
        public double z() { return z; }
        private void snapshot() { previousY = y; }
    }

    private final Fragment[] fragments = {new Fragment(), new Fragment()};
    private final FineChip fine = new FineChip();
    private long lastTick = Long.MIN_VALUE;
    private long lastFineTick = Long.MIN_VALUE;
    private float lastProgress;

    public Fragment fragment(int index) { return fragments[index]; }
    public FineChip fine() { return fine; }

    public void reset() {
        for (Fragment f : fragments) f.active = false;
        fine.active = false;
        lastTick = Long.MIN_VALUE;
        lastFineTick = Long.MIN_VALUE;
        lastProgress = 0F;
    }

    /**
     * One physics update per observed world tick (at most two catch-up steps).
     * Rotation comes from Gear V2, not world time or the recipe percentage.
     * A stalled batch freezes the rocks instead of allowing gravity to run.
     */
    public void advance(long tick, float progress, float drumAngleDegrees,
            float signedDegreesPerTick, boolean processing) {
        if (lastTick != Long.MIN_VALUE
                && (tick < lastTick || progress < lastProgress - 0.10F)) {
            reset(); // New batch, world reload, or rewound client clock.
        }
        if (lastTick == Long.MIN_VALUE) lastTick = tick;
        lastProgress = progress;
        if (progress >= 0.20F && !fragments[0].active) fragments[0].spawn(0, progress);
        if (progress >= 0.33F && !fragments[1].active) fragments[1].spawn(1, progress);
        long elapsed = tick - lastTick;
        lastTick = tick;
        if (!processing || Math.abs(signedDegreesPerTick) < 0.001F) {
            for (Fragment f : fragments) if (f.active) f.snapshot();
            fine.active = false;
            return;
        }
        if (elapsed > 2L) {
            // Do not fast-forward unseen chunks by hundreds of physics steps.
            elapsed = 1L;
            for (Fragment f : fragments) if (f.active) f.snapshot();
        }
        for (int step = 0; step < elapsed; step++) {
            double omega = Math.toRadians(signedDegreesPerTick);
            double phase = Math.toRadians(drumAngleDegrees
                    - signedDegreesPerTick * (elapsed - 1L - step));
            for (Fragment f : fragments) if (f.active) {
                f.snapshot();
                simulateRock(f, omega, phase, tick, progress);
            }
            if (fine.active) {
                fine.snapshot();
                fine.vy -= 0.024D;
                fine.y += fine.vy;
                if (fine.y <= 0.49D) fine.active = false; // Top of real internal pan.
            }
        }
    }

    private void simulateRock(Fragment rock, double omega, double phase,
            long tick, float progress) {
        rock.vy -= GRAVITY;
        rock.vx *= 0.985D;
        rock.vy *= 0.985D;
        rock.x += rock.vx;
        rock.y += rock.vy;
        rock.rollVelocity *= 0.93D;
        for (int side = 0; side < 8; side++) {
            double facet = phase + side * Math.PI / 4D;
            double nx = Math.cos(facet), ny = Math.sin(facet);
            double support = projectedRockRadius(rock.roll, nx, ny);
            double limit = SCREEN_APOTHEM - support - 0.006D;
            double gap = limit - (rock.x * nx + rock.y * ny);
            if (gap > 0.012D) continue;
            if (gap < 0D) {
                rock.x += nx * gap;
                rock.y += ny * gap;
            }
            // Outward relative velocity hits the wall. The wall itself moves
            // tangentially at omega*r, so friction lifts the material with it.
            double outward = rock.vx * nx + rock.vy * ny;
            if (outward > 0D) {
                rock.vx -= (1D + RESTITUTION) * outward * nx;
                rock.vy -= (1D + RESTITUTION) * outward * ny;
            }
            double tx = -ny, ty = nx;
            double tangential = rock.vx * tx + rock.vy * ty;
            double wallTangential = omega * SCREEN_APOTHEM;
            double traction = 0.31D;
            rock.vx += (wallTangential - tangential) * tx * traction;
            rock.vy += (wallTangential - tangential) * ty * traction;
            rock.rollVelocity = clamp(-((rock.vx * tx + rock.vy * ty)
                    / 0.17D) * 23D, -14D, 14D);
            if (ny < -0.45D && outward > 0.012D
                    && progress >= 0.32F && progress < 0.93F
                    && !fine.active && (lastFineTick == Long.MIN_VALUE
                    || tick - lastFineTick >= 22L)) {
                // A chip comes from an actual lower-screen contact, not a
                // periodically looping item at an unrelated screen position.
                fine.active = true;
                fine.x = rock.x * 0.7D;
                fine.z = rock.z;
                fine.y = 0.87D;
                fine.vy = -0.012D;
                fine.snapshot();
                lastFineTick = tick;
            }
        }
        // Keep the actual item model clear of the central drive shaft too.
        double shaftDistance = Math.hypot(rock.x, rock.y);
        if (shaftDistance < SHAFT_CLEARANCE) {
            double nx = shaftDistance < 1e-6D ? 0D : rock.x / shaftDistance;
            double ny = shaftDistance < 1e-6D ? -1D : rock.y / shaftDistance;
            rock.x = nx * SHAFT_CLEARANCE;
            rock.y = ny * SHAFT_CLEARANCE;
            double inward = rock.vx * nx + rock.vy * ny;
            if (inward < 0D) {
                rock.vx -= inward * 1.12D * nx;
                rock.vy -= inward * 1.12D * ny;
            }
        }
        rock.vx = clamp(rock.vx, -MAX_SPEED, MAX_SPEED);
        rock.vy = clamp(rock.vy, -MAX_SPEED, MAX_SPEED);
        rock.roll += rock.rollVelocity;
        // A small axial bias represents travel through the gently fed screen.
        // Unlike the old path, it advances only as the powered drum turns.
        rock.z = Math.min(MAX_AXIAL_Z, rock.z + 0.012D + Math.abs(omega) * 0.035D);
    }

    public static double projectedRockRadius(double rollDegrees, double nx, double ny) {
        double angle = Math.toRadians(rollDegrees);
        double c = Math.cos(angle), s = Math.sin(angle);
        // Rotated 2D envelope of the actual crushed-ore GROUND model.
        return HALF_WIDTH * Math.abs(nx * c + ny * s)
                + HALF_HEIGHT * Math.abs(-nx * s + ny * c);
    }

    private static double lerp(double a, double b, float t) {
        return a + (b - a) * clamp(t, 0D, 1D);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
