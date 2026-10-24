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
    /**
     * Regression for side-wall adhesion: after its first drop a rock must
     * repeatedly climb, break contact and return toward the bottom. Check
     * EACH later interval, not just that the initial fall looked convincing.
     */
    private static void assertRepeatFalls(float rpm, int direction) {
        var motion = new RotaryClassifierOreMotion();
        float speed = direction * rpm * 360.0F / 1200.0F;
        int falls = 0;
        boolean lifted = false;
        double[] minX = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY};
        double[] maxX = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY};
        double[] minY = minX.clone(), maxY = maxX.clone();
        for (int tick = 0; tick <= 175; ++tick) {
            float progress = 0.20F + tick / 220.0F;
            motion.advance(tick, progress, tick * speed, speed, true);
            var rock = motion.fragment(0);
            assertClear(motion, tick * speed, tick);
            if (tick > 15 && rock.rawY() > -0.31D) lifted = true;
            if (lifted && rock.rawY() < -0.43D) {
                falls++;
                lifted = false;
            }
            int window = tick < 30 ? -1 : tick < 75 ? 0 : tick < 120 ? 1
                    : tick < 165 ? 2 : -1;
            if (window >= 0) {
                minX[window] = Math.min(minX[window], rock.rawX());
                maxX[window] = Math.max(maxX[window], rock.rawX());
                minY[window] = Math.min(minY[window], rock.rawY());
                maxY[window] = Math.max(maxY[window], rock.rawY());
            }
        }
        require(falls >= 3, "Screen kept ore attached after initial fall: "
                + rpm + " RPM dir " + direction + " falls=" + falls);
        for (int i = 0; i < 3; ++i) {
            require(maxX[i] - minX[i] > 0.19D,
                    "Ore side-locked during later tumbling window " + i
                            + " at " + rpm + " RPM, dir=" + direction);
            require(maxY[i] - minY[i] > 0.14D,
                    "Ore stopped falling toward mesh floor in window " + i
                            + " at " + rpm + " RPM, dir=" + direction);
        }
    }

    public static void main(String[] args) {
        // Both signed Gear V2 directions and sustained operation must tumble.
        for (float rpm : new float[]{24F, 48F}) {
            assertRepeatFalls(rpm, 1);
            assertRepeatFalls(rpm, -1);
        }
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
        System.out.println("PASS: repeating lift/slip/fall in both directions, octagon/shaft bounds, fines, axial transport, pause and batch reset");
    }
}
