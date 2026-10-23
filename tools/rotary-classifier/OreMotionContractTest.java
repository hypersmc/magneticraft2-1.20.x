import com.magneticraft2.client.render.blocks.stage.bronze.RotaryClassifierOreMotion;

/** Can run with only javac/java; no Minecraft or Gradle dependencies. */
public final class OreMotionContractTest {
    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
    private static void close(double a, double b, String reason) {
        require(Math.abs(a-b)<1e-9, reason + ": " + a + " vs " + b);
    }
    private static void assertClear(RotaryClassifierOreMotion m, float angle, long tick) {
        for (int i = 0; i < RotaryClassifierOreMotion.FRAGMENT_COUNT; ++i) {
            var f = m.fragment(i);
            if (!f.active()) continue;
            require(Double.isFinite(f.rawX()) && Double.isFinite(f.rawY())
                    && Double.isFinite(f.rawZ()) && Double.isFinite(f.rawRoll()),
                    "Non-finite rock position/angle at " + tick);
            double shaft = Math.hypot(f.rawX(), f.rawY());
            require(shaft >= 0.3029D, "Rock intersects shaft at " + tick + ": " + shaft);
            for (int side = 0; side < 8; ++side) {
                double phi = Math.toRadians(angle) + side * Math.PI / 4D;
                double nx = Math.cos(phi), ny = Math.sin(phi);
                double clearance = RotaryClassifierOreMotion.SCREEN_APOTHEM
                        - RotaryClassifierOreMotion.projectedRockRadius(f.rawRoll(), nx, ny);
                double position = f.rawX() * nx + f.rawY() * ny;
                require(position <= clearance + 0.025D,
                        "Rock clips octagon on side " + side + " at " + tick
                        + ": delta " + (position - clearance));
            }
            require(f.rawZ() <= .6000001D, "Rock crossed open front rim");
            require(f.rawZ() >= -1.71D, "Rock travelled through rear drive");
        }
    }
    public static void main(String[] args) {
        final var positive = new RotaryClassifierOreMotion();
        final var negative = new RotaryClassifierOreMotion();
        final float speed = 7.2F; // Gear V2 24 RPM: degrees/tick
        boolean fineSeen = false;
        for (long tick = 0; tick <= 175; tick++) {
            float progress = 0.2F + (float) tick / 220F;
            positive.advance(tick, progress, tick * speed, speed, true);
            negative.advance(tick, progress, -tick * speed, -speed, true);
            fineSeen |= positive.fine().active();
            assertClear(positive, tick * speed, tick);
            assertClear(negative, -tick * speed, tick);
        }
        require(positive.fragment(1).active(), "Second rock never entered the screen");
        require(fineSeen, "No fine chips produced by mesh impacts");
        require(positive.fragment(0).rawZ() > 0.25D,
                "The powered drum never transported the rock toward discharge");
        // Reverse Gear V2 must reverse physical wall traction, not just item yaw.
        var clockwise = new RotaryClassifierOreMotion();
        var anticlockwise = new RotaryClassifierOreMotion();
        for (long t=0;t<7;t++) {
            clockwise.advance(t,.25F,t*speed,speed,true);
            anticlockwise.advance(t,.25F,-t*speed,-speed,true);
        }
        require(clockwise.fragment(0).rawX() * anticlockwise.fragment(0).rawX() < 0
                || Math.abs(clockwise.fragment(0).rawX() - anticlockwise.fragment(0).rawX()) > .05,
                "Reversing the drum does not reverse lateral motion");
        var stopped = positive.fragment(0);
        double x=stopped.rawX(), y=stopped.rawY(), z=stopped.rawZ(), roll=stopped.rawRoll();
        for (long t=176;t<216;t++) {
            positive.advance(t,.995F, t*speed, 0F, false);
            close(stopped.rawX(),x,"X changed with stopped drum");
            close(stopped.rawY(),y,"Y changed with stopped drum");
            close(stopped.rawZ(),z,"Z changed with stopped drum");
            close(stopped.rawRoll(),roll,"Rotation continued with stopped drum");
        }
        require(!positive.fine().active(), "Chip kept falling while stopped");
        positive.advance(217L,.02F,0F,0F,false);
        require(!positive.fragment(0).active(), "New batch retained previous material");
        System.out.println("PASS: octagon/shaft bounds, reversed traction, fine impacts, powered axial transport, pause and batch reset");
    }
}
