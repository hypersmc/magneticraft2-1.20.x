import com.magneticraft2.common.systems.steam.BoilerSteamState;
import com.magneticraft2.common.systems.steam.BoilerWarmupState;
import java.util.Random;

/** Dependency-free Steam 0-C conservation checks; not Forge/runtime proof. */
public final class BoilerSteamStateTest {
    private static long assertions;
    private static void check(boolean condition, String name) {
        assertions++;
        if (!condition) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        BoilerWarmupState water = new BoilerWarmupState();
        BoilerSteamState steam = new BoilerSteamState();

        check(water.fill(8000, 8000, false) == 8000, "fill");
        check(water.ignite(3200, 36), "ignite");
        for (int i = 0; i < 800; i++) water.tick(true, 36000, 2);
        check(water.canConvertToSteam(250, 1000, 24000), "phase threshold");
        long beforeWater = water.snapshot().waterToSteam();
        long beforeHeat = water.snapshot().heatToSteam();
        check(water.convertToSteam(250, 1000, 24000), "phase debit");
        check(steam.produce(1, 16) == 1, "buffer one unit");
        check(water.snapshot().waterToSteam() == beforeWater + 250, "water conversion ledger");
        check(water.snapshot().heatToSteam() == beforeHeat + 1000, "heat conversion ledger");
        check(BoilerWarmupState.valid(water.snapshot()), "water ledger valid");
        check(BoilerSteamState.valid(steam.snapshot()), "Steam ledger valid");

        var full = steam.snapshot();
        for (int i = steam.stored(); i < 16; i++) check(steam.produce(1, 16) == 1, "fill Steam buffer");
        check(steam.produce(1, 16) == 0, "buffer backpressure");
        check(BoilerSteamState.valid(steam.snapshot()), "full buffer valid");
        check(steam.extract(4) == 4, "bounded network export");
        check(steam.vent(3) == 3, "local relief vent");
        check(BoilerSteamState.valid(steam.snapshot()), "relief conservation");
        check(BoilerSteamState.valid(steam.snapshot()), "export conservation");

        BoilerSteamState restored = new BoilerSteamState();
        restored.restore(steam.snapshot());
        check(restored.snapshot().equals(steam.snapshot()), "save reload exact");
        check(restored.spill() == restored.snapshot().spilled(), "spill amount accounted");
        check(BoilerSteamState.valid(restored.snapshot()), "spill conservation");

        BoilerWarmupState lastWater = new BoilerWarmupState();
        check(lastWater.fill(250, 8000, false) == 250, "last charge");
        check(lastWater.ignite(1000, 36), "last charge ignite");
        for (int i = 0; i < 800; i++) lastWater.tick(true, 36000, 0);
        check(lastWater.convertToSteam(250, 1000, 24000), "last water converts");
        check(lastWater.water() == 0 && lastWater.heat() == 0, "dry vessel carries no HEAT");
        check(BoilerWarmupState.valid(lastWater.snapshot()), "last-water conservation");

        Random random = new Random(42);
        BoilerSteamState fuzz = new BoilerSteamState();
        for (int i = 0; i < 100000; i++) {
            switch (random.nextInt(5)) {
                case 0 -> fuzz.produce(random.nextInt(8), 64);
                case 1 -> fuzz.extract(random.nextInt(8));
                case 2 -> {
                    BoilerSteamState clone = new BoilerSteamState();
                    clone.restore(fuzz.snapshot());
                    fuzz = clone;
                }
                case 3 -> fuzz.vent(random.nextInt(8));
                default -> fuzz.spill();
            }
            check(BoilerSteamState.valid(fuzz.snapshot()), "random Steam conservation");
        }

        check(!full.equals(steam.snapshot()), "export changes state");
        System.out.println("PASS: " + assertions + " Steam 0-C conservation assertions.");
        System.out.println("NOT a Forge build, pressure-network integration, rendering or multiplayer test.");
    }
}
