import com.magneticraft2.common.systems.steam.BoilerPressureMath;

public final class BoilerPressureMathTest {
    private static long assertions;
    private static void check(boolean c, String n) {
        assertions++;
        if (!c) throw new AssertionError(n);
    }
    public static void main(String[] args) {
        check(BoilerPressureMath.pressureKPa(0, 453.15, 2.5) == 0.0, "empty");
        check(Math.abs(BoilerPressureMath.pressureKPa(10, 453.15, 2.5) - 400.0) < 1e-9, "10 units");
        check(Math.abs(BoilerPressureMath.pressureKPa(13, 453.15, 2.5) - 520.0) < 1e-9, "13 units");
        check(Math.abs(BoilerPressureMath.pressureKPa(14, 453.15, 2.5) - 560.0) < 1e-9, "relief crossing");
        check(Math.abs(BoilerPressureMath.pressureKPa(16, 453.15, 2.5) - 640.0) < 1e-9, "full buffer");
        check(BoilerPressureMath.pressureKPa(8, 906.3, 2.5) == 640.0, "temperature scaling");
        System.out.println("PASS: " + assertions + " boiler pressure assertions.");
    }
}
