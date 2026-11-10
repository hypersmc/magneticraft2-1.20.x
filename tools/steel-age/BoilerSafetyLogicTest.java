import com.magneticraft2.common.systems.steam.BoilerSafetyLogic;

public final class BoilerSafetyLogicTest {
    private static long assertions;
    private static void check(boolean value, String name) {
        assertions++;
        if (!value) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        check(!BoilerSafetyLogic.nextReliefOpen(false, 549.9, 550, 500), "closed below open");
        check(BoilerSafetyLogic.nextReliefOpen(false, 550, 550, 500), "opens at threshold");
        check(BoilerSafetyLogic.nextReliefOpen(true, 525, 550, 500), "hysteresis remains open");
        check(!BoilerSafetyLogic.nextReliefOpen(true, 500, 550, 500), "closes at reset");
        check(BoilerSafetyLogic.gaugeStep(0, 600) == 0, "zero gauge");
        check(BoilerSafetyLogic.gaugeStep(120, 600) == 1, "one fifth");
        check(BoilerSafetyLogic.gaugeStep(300, 600) == 3, "mid gauge");
        check(BoilerSafetyLogic.gaugeStep(600, 600) == 5, "full gauge");
        check(BoilerSafetyLogic.gaugeStep(900, 600) == 5, "gauge clamps high");
        check(BoilerSafetyLogic.gaugeStep(-10, 600) == 0, "gauge clamps low");
        System.out.println("PASS: " + assertions + " boiler safety logic assertions.");
        System.out.println("NOT a Forge build, network vent, model, sound or multiplayer test.");
    }
}
