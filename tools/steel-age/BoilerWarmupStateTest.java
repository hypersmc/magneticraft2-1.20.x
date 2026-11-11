import com.magneticraft2.common.systems.steam.BoilerWarmupState;
import java.util.Random;

/** Real Java domain implementation tests; not a Forge/in-world substitute. */
public final class BoilerWarmupStateTest {
    private static long assertions;
    private static void check(boolean condition, String name) {
        assertions++;
        if (!condition) throw new AssertionError(name);
    }
    private static void valid(BoilerWarmupState s) {
        check(BoilerWarmupState.valid(s.snapshot()), "ledger conservation");
    }
    public static void main(String[] args) {
        BoilerWarmupState s = new BoilerWarmupState();
        check(!s.ignite(1600, 36), "no dry ignition");
        var zero = s.snapshot();
        check(s.fill(1000, 8000, true) == 1000, "simulate fill amount");
        check(s.snapshot().equals(zero), "simulate cannot mutate");
        check(s.fill(1000, 8000, false) == 1000, "bucket fill");
        check(s.ignite(1600, 36), "coal ignition");
        check(!s.ignite(1600, 36), "no second burn overlaps");
        for (int i = 0; i < 1600; i++) { check(s.tick(true, 36000, 2), "burn tick"); valid(s); }
        check(!s.burning() && s.heat() <= 36000, "finite burn, capacity respected");
        check(s.snapshot().generatedHeat() == 57600, "coal heat budget");
        check(s.snapshot().heatLost() == 57600L - s.heat(), "exhaust and cooling explicit");
        var before = s.snapshot();
        check(s.drain(500, true) == 500 && before.equals(s.snapshot()), "pure drain simulation");
        check(s.drain(500, false) == 500, "half bucket drain");
        check(s.heat() == before.heat() - (int)((long)before.heat() * 500 / 1000), "drain carries heat share");
        check(s.fill(Integer.MAX_VALUE, 8000, false) == 7500, "bounded giant input");
        check(s.fill(1, 8000, false) == 0, "full tank unchanged");
        check(s.fill(1, 2000, false) == 0 && s.water() == 8000, "pack cap reduction preserves water");
        check(s.drain(Integer.MAX_VALUE, false) == 8000 && s.heat() == 0, "last water carries remaining heat");
        valid(s);
        s.fill(1000, 8000, false); s.ignite(100, 36);
        var paused = s.snapshot();
        for (int i=0;i<50;i++) s.tick(false, 36000, 2);
        check(s.burnTicks() == paused.burnTicks(), "unloaded dependency pauses fuel");
        check(s.snapshot().generatedHeat() == paused.generatedHeat(), "pause cannot mint heat");
        for(int i=0;i<7;i++) s.tick(true, 36000, 2);
        var saved = s.snapshot();
        BoilerWarmupState restored = new BoilerWarmupState(); restored.restore(saved);
        check(restored.snapshot().equals(saved), "save/load exact values");
        for(int i=0;i<93;i++) { s.tick(true, 36000, 2); restored.tick(true, 36000, 2); }
        check(restored.snapshot().equals(s.snapshot()), "reload continuation no refill/reset");
        s.ignite(100, 17); for(int i=0;i<4;i++) s.tick(true, 36000, 0);
        s.extinguish();
        check(s.snapshot().fuelDiscarded() >= 96L*17, "unspent chemical budget accounted");
        valid(s);
        var spilled=s.spill();
        check(spilled.water()>0 && s.water()==0 && s.heat()==0 && !s.burning(), "teardown clears contents");
        var once=s.snapshot(); s.spill(); check(s.snapshot().equals(once), "teardown exactly once");
        valid(s);

        // Adversarial persisted snapshots must fail without changing valid state.
        var baseline = restored.snapshot();
        var bad = new BoilerWarmupState.Snapshot(-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        try { restored.restore(bad); throw new AssertionError("negative water accepted"); }
        catch(IllegalArgumentException expected) { assertions++; }
        check(restored.snapshot().equals(baseline), "rejected restore atomic");
        bad = new BoilerWarmupState.Snapshot(1, 100, 1, 0, 1, 0, 0, 100, 100, 0, 0, 0, 0);
        check(!BoilerWarmupState.valid(bad), "reject fuel with zero rate");
        bad = new BoilerWarmupState.Snapshot(1, 10, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0);
        check(!BoilerWarmupState.valid(bad), "reject free heat");
        bad = new BoilerWarmupState.Snapshot(1000, 0, 0, 0, 999, 0, 0, 0, 0, 0, 0, 0, 0);
        check(!BoilerWarmupState.valid(bad), "reject free water");

        long bound = Long.MAX_VALUE / 4;
        BoilerWarmupState saturated = new BoilerWarmupState();
        saturated.restore(new BoilerWarmupState.Snapshot(1, 0, 0, 0,
                bound, bound - 1, 0, bound, 0, 0, 0, 0, bound));
        check(saturated.fill(1000, 8000, false) == 0, "saturated water ledger fails closed");
        check(!saturated.ignite(100, 36), "saturated fuel ledger fails closed");
        valid(saturated);

        // Independent input/output accounting over randomized load, cap and flow sequences.
        for (int seed=0; seed<32; seed++) {
            Random random=new Random(seed); BoilerWarmupState q=new BoilerWarmupState();
            long waterIn=0,waterOut=0,heatProduced=0;
            for (int i=0;i<4000;i++) {
                switch(random.nextInt(8)) {
                    case 0 -> waterIn += q.fill(random.nextInt(2000)-10, 8000, false);
                    case 1 -> waterOut += q.drain(random.nextInt(2000)-10, false);
                    case 2 -> { var old=q.snapshot(); q.fill(1000,8000,true); q.drain(1000,true); check(old.equals(q.snapshot()),"random simulate pure"); }
                    case 3 -> q.ignite(random.nextInt(200)+1, random.nextInt(64)+1);
                    case 4 -> q.extinguish();
                    case 5 -> { var old=q.snapshot(); boolean spent=q.tick(random.nextBoolean(), 36000, 2); if(spent)heatProduced+=old.heatPerBurnTick(); }
                    case 6 -> { BoilerWarmupState clone=new BoilerWarmupState(); clone.restore(q.snapshot()); q=clone; }
                    default -> { var old=q.spill(); waterOut+=old.water(); }
                }
                valid(q);
                check(waterIn-waterOut==q.water(),"independent water accounting");
                check(q.snapshot().generatedHeat()==heatProduced,"independent generated energy");
            }
        }
        System.out.println("PASS: " + assertions + " assertions; actual Java warm-up domain implementation.");
        System.out.println("NOT a Forge compilation, bucket integration, rendering or multiplayer test.");
    }
}
