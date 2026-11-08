import com.magneticraft2.client.render.blocks.stage.iron.IronShearMotion;

public final class ShearMotionTest {
    public static void main(String[] args) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= 1440; i++) {
            double angle = i * Math.PI / 720D;
            double x = IronShearMotion.pinX(angle);
            double y = IronShearMotion.pinY(angle);
            double s = IronShearMotion.sliderY(x, y);
            double distance = Math.hypot(x - IronShearMotion.AXLE_X, y - s);
            check(Math.abs(distance - IronShearMotion.ROD_LENGTH) < 1e-9, "rod length");
            double r = Math.toRadians(IronShearMotion.linkDegrees(x, y, s));
            check(Math.abs(x - IronShearMotion.ROD_LENGTH * Math.sin(r) - 8D) < 1e-5, "link X");
            check(Math.abs(y + IronShearMotion.ROD_LENGTH * Math.cos(r) - s) < 1e-5, "link Y");
            min = Math.min(min, s - 3D);
            max = Math.max(max, s - 3D);
        }
        check(min == 8D && max == 14D, "cutting travel");
        System.out.println("PASS: actual Java linkage, 1,441 poses; constant rod and blade travel 8..14. Not a Forge build.");
    }
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
