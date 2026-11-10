package com.magneticraft2.common.systems.steam;

/**
 * Integer resource ledger for Steam 0-B warm-up. HEAT is the mod's abstract
 * thermal budget, never degrees, pressure or a water-to-steam conversion.
 * No Minecraft dependencies: the same implementation is exercised by tests.
 */
public final class BoilerWarmupState {
    public static final int MAX_AMOUNT = 1_000_000;
    public static final int MAX_BURN_TICKS = 1_000_000;
    public static final int MAX_HEAT_PER_TICK = 100_000;
    private static final long MAX_LEDGER = Long.MAX_VALUE / 4;
    private int water, heat, burnTicks, heatPerBurnTick;
    private long waterIn, waterOut, waterSpilled;
    private long fuelEnergy, generatedHeat, heatLost, heatInDrainedWater, heatSpilled, fuelDiscarded;

    public record Snapshot(int water, int heat, int burnTicks, int heatPerBurnTick,
            long waterIn, long waterOut, long waterSpilled, long fuelEnergy,
            long generatedHeat, long heatLost, long heatInDrainedWater,
            long heatSpilled, long fuelDiscarded) { }

    public int water() { return water; }
    public int heat() { return heat; }
    public int burnTicks() { return burnTicks; }
    public boolean burning() { return burnTicks > 0; }

    public int fill(int offered, int capacity, boolean simulate) {
        limit(capacity, MAX_AMOUNT);
        if (offered <= 0) return 0;
        int accepted = (int) Math.min(Math.min(offered, Math.max(0, capacity - water)),
                MAX_LEDGER - waterIn);
        if (!simulate && accepted > 0) {
            long incoming = add(waterIn, accepted);
            water += accepted; waterIn = incoming;
        }
        return accepted;
    }

    public int drain(int requested, boolean simulate) {
        if (requested <= 0 || water == 0) return 0;
        int removed = Math.min(requested, water);
        if (!simulate) {
            // Cold vanilla water is returned. Its share of stored HEAT leaves
            // this ledger explicitly; it must not remain to heat free refills.
            int carriedHeat = (int) ((long) heat * removed / water);
            long outgoing = add(waterOut, removed);
            long outgoingHeat = add(heatInDrainedWater, carriedHeat);
            water -= removed; heat -= carriedHeat;
            waterOut = outgoing; heatInDrainedWater = outgoingHeat;
        }
        return removed;
    }

    public boolean ignite(int ticks, int gain) {
        if (burning() || water == 0 || ticks <= 0 || ticks > MAX_BURN_TICKS
                || gain <= 0 || gain > MAX_HEAT_PER_TICK) return false;
        long offeredEnergy = (long) ticks * gain;
        if (fuelEnergy > MAX_LEDGER - offeredEnergy) return false;
        long budget = fuelEnergy + offeredEnergy;
        burnTicks = ticks; heatPerBurnTick = gain; fuelEnergy = budget;
        return true;
    }

    /** No fuel refund: remaining chemical energy is explicitly discarded. */
    public void extinguish() {
        long discarded = add(fuelDiscarded, (long) burnTicks * heatPerBurnTick);
        burnTicks = 0; fuelDiscarded = discarded;
    }

    /**
     * Paused/unloaded sources do not advance fuel. Cooling is independent.
     * Once lit, fuel continues to burn at the cap; excess heat exits via flue.
     * Returns true exactly when a tick of real fuel has been spent.
     */
    public boolean tick(boolean advanceFuel, int capacity, int passiveLoss) {
        limit(capacity, MAX_AMOUNT);
        if (passiveLoss < 0 || passiveLoss > MAX_HEAT_PER_TICK)
            throw new IllegalArgumentException("Invalid passive heat loss");
        int produced = advanceFuel && burnTicks > 0 ? heatPerBurnTick : 0;
        int stored = water == 0 ? 0 : Math.min(produced, Math.max(0, capacity - heat));
        int cooled = Math.min(heat + stored, passiveLoss);
        long generated = add(generatedHeat, produced);
        long loss = add(heatLost, (long) produced - stored + cooled);
        if (produced > 0) burnTicks--;
        heat += stored - cooled; generatedHeat = generated; heatLost = loss;
        return produced > 0;
    }

    /** Claim contents once during actual dismantling, not chunk unload. */
    public Snapshot spill() {
        Snapshot before = snapshot();
        long spilledWater = add(waterSpilled, water);
        long spilledHeat = add(heatSpilled, heat);
        extinguish();
        waterSpilled = spilledWater; heatSpilled = spilledHeat;
        water = 0; heat = 0;
        return before;
    }

    public Snapshot snapshot() {
        return new Snapshot(water, heat, burnTicks, heatPerBurnTick, waterIn,
                waterOut, waterSpilled, fuelEnergy, generatedHeat, heatLost,
                heatInDrainedWater, heatSpilled, fuelDiscarded);
    }

    /** Reject corrupt saves instead of silently creating/clipping resources. */
    public void restore(Snapshot s) {
        if (!valid(s)) throw new IllegalArgumentException("Invalid boiler warm-up ledger");
        water = s.water(); heat = s.heat(); burnTicks = s.burnTicks();
        heatPerBurnTick = s.heatPerBurnTick(); waterIn = s.waterIn();
        waterOut = s.waterOut(); waterSpilled = s.waterSpilled();
        fuelEnergy = s.fuelEnergy(); generatedHeat = s.generatedHeat();
        heatLost = s.heatLost(); heatInDrainedWater = s.heatInDrainedWater();
        heatSpilled = s.heatSpilled(); fuelDiscarded = s.fuelDiscarded();
    }

    public static boolean valid(Snapshot s) {
        if (s == null || s.water() < 0 || s.water() > MAX_AMOUNT
                || s.heat() < 0 || s.heat() > MAX_AMOUNT
                || (s.water() == 0 && s.heat() != 0)
                || s.burnTicks() < 0 || s.burnTicks() > MAX_BURN_TICKS
                || s.heatPerBurnTick() < 0 || s.heatPerBurnTick() > MAX_HEAT_PER_TICK
                || (s.burnTicks() > 0 && s.heatPerBurnTick() == 0)) return false;
        for (long v : new long[]{s.waterIn(), s.waterOut(), s.waterSpilled(),
                s.fuelEnergy(), s.generatedHeat(), s.heatLost(),
                s.heatInDrainedWater(), s.heatSpilled(), s.fuelDiscarded()})
            if (v < 0 || v > MAX_LEDGER) return false;
        return s.waterIn() == (long) s.water() + s.waterOut() + s.waterSpilled()
                && s.generatedHeat() == (long) s.heat() + s.heatLost()
                    + s.heatInDrainedWater() + s.heatSpilled()
                && s.fuelEnergy() == (long) s.burnTicks() * s.heatPerBurnTick()
                    + s.generatedHeat() + s.fuelDiscarded();
    }

    private static void limit(int value, int maximum) {
        if (value <= 0 || value > maximum) throw new IllegalArgumentException("Invalid capacity");
    }
    private static long add(long value, long increment) {
        if (increment < 0 || value > MAX_LEDGER - increment)
            throw new IllegalStateException("Boiler ledger overflow");
        return value + increment;
    }
}
