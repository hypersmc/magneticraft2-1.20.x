package com.magneticraft2.common.systems.steam;

/**
 * Integer resource ledger for boiler water/HEAT. HEAT is Magneticraft2's
 * abstract thermal budget, never degrees or pressure. Steam phase conversion
 * debits this ledger explicitly; the Steam unit ledger is separate.
 */
public final class BoilerWarmupState {
    public static final int MAX_AMOUNT = 1_000_000;
    public static final int MAX_BURN_TICKS = 1_000_000;
    public static final int MAX_HEAT_PER_TICK = 100_000;
    private static final long MAX_LEDGER = Long.MAX_VALUE / 4;
    private int water, heat, burnTicks, heatPerBurnTick;
    private long waterIn, waterOut, waterSpilled, waterToSteam;
    private long fuelEnergy, generatedHeat, heatLost, heatInDrainedWater,
            heatSpilled, fuelDiscarded, heatToSteam;

    public record Snapshot(int water, int heat, int burnTicks, int heatPerBurnTick,
            long waterIn, long waterOut, long waterSpilled, long fuelEnergy,
            long generatedHeat, long heatLost, long heatInDrainedWater,
            long heatSpilled, long fuelDiscarded, long waterToSteam, long heatToSteam) {
        /** Save-format-1/test compatibility: no Steam conversion had occurred. */
        public Snapshot(int water, int heat, int burnTicks, int heatPerBurnTick,
                long waterIn, long waterOut, long waterSpilled, long fuelEnergy,
                long generatedHeat, long heatLost, long heatInDrainedWater,
                long heatSpilled, long fuelDiscarded) {
            this(water, heat, burnTicks, heatPerBurnTick, waterIn, waterOut,
                    waterSpilled, fuelEnergy, generatedHeat, heatLost,
                    heatInDrainedWater, heatSpilled, fuelDiscarded, 0, 0);
        }
    }

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
            water += accepted;
            waterIn = add(waterIn, accepted);
        }
        return accepted;
    }

    public int drain(int requested, boolean simulate) {
        if (requested <= 0 || water == 0) return 0;
        int removed = Math.min(requested, water);
        if (!simulate) {
            int carriedHeat = (int) ((long) heat * removed / water);
            water -= removed;
            heat -= carriedHeat;
            waterOut = add(waterOut, removed);
            heatInDrainedWater = add(heatInDrainedWater, carriedHeat);
        }
        return removed;
    }

    public boolean ignite(int ticks, int gain) {
        if (burning() || water == 0 || ticks <= 0 || ticks > MAX_BURN_TICKS
                || gain <= 0 || gain > MAX_HEAT_PER_TICK) return false;
        long offeredEnergy = (long) ticks * gain;
        if (fuelEnergy > MAX_LEDGER - offeredEnergy) return false;
        burnTicks = ticks;
        heatPerBurnTick = gain;
        fuelEnergy += offeredEnergy;
        return true;
    }

    /** No fuel refund: remaining chemical energy is explicitly discarded. */
    public void extinguish() {
        fuelDiscarded = add(fuelDiscarded, (long) burnTicks * heatPerBurnTick);
        burnTicks = 0;
    }

    public boolean tick(boolean advanceFuel, int capacity, int passiveLoss) {
        limit(capacity, MAX_AMOUNT);
        if (passiveLoss < 0 || passiveLoss > MAX_HEAT_PER_TICK)
            throw new IllegalArgumentException("Invalid passive heat loss");
        int produced = advanceFuel && burnTicks > 0 ? heatPerBurnTick : 0;
        int stored = water == 0 ? 0 : Math.min(produced, Math.max(0, capacity - heat));
        int cooled = Math.min(heat + stored, passiveLoss);
        generatedHeat = add(generatedHeat, produced);
        heatLost = add(heatLost, (long) produced - stored + cooled);
        if (produced > 0) burnTicks--;
        heat += stored - cooled;
        return produced > 0;
    }

    public boolean canConvertToSteam(int waterCost, int heatCost, int threshold) {
        if (waterCost <= 0 || heatCost <= 0 || threshold <= 0)
            throw new IllegalArgumentException("Invalid phase conversion");
        return water >= waterCost && heat >= heatCost && heat >= threshold;
    }

    /**
     * Atomic water + HEAT debit for exactly one authorized Steam unit.
     * If the last water is boiled, remaining sensible HEAT is explicitly lost
     * instead of surviving in an impossible dry water buffer.
     */
    public boolean convertToSteam(int waterCost, int heatCost, int threshold) {
        if (!canConvertToSteam(waterCost, heatCost, threshold)) return false;
        int orphanedHeat = water == waterCost ? heat - heatCost : 0;
        water -= waterCost;
        heat -= heatCost + orphanedHeat;
        waterToSteam = add(waterToSteam, waterCost);
        heatToSteam = add(heatToSteam, heatCost);
        if (orphanedHeat > 0) heatLost = add(heatLost, orphanedHeat);
        return true;
    }

    public Snapshot spill() {
        Snapshot before = snapshot();
        waterSpilled = add(waterSpilled, water);
        heatSpilled = add(heatSpilled, heat);
        extinguish();
        water = 0;
        heat = 0;
        return before;
    }

    public Snapshot snapshot() {
        return new Snapshot(water, heat, burnTicks, heatPerBurnTick, waterIn,
                waterOut, waterSpilled, fuelEnergy, generatedHeat, heatLost,
                heatInDrainedWater, heatSpilled, fuelDiscarded, waterToSteam, heatToSteam);
    }

    public void restore(Snapshot s) {
        if (!valid(s)) throw new IllegalArgumentException("Invalid boiler warm-up ledger");
        water=s.water(); heat=s.heat(); burnTicks=s.burnTicks();
        heatPerBurnTick=s.heatPerBurnTick(); waterIn=s.waterIn();
        waterOut=s.waterOut(); waterSpilled=s.waterSpilled(); fuelEnergy=s.fuelEnergy();
        generatedHeat=s.generatedHeat(); heatLost=s.heatLost();
        heatInDrainedWater=s.heatInDrainedWater(); heatSpilled=s.heatSpilled();
        fuelDiscarded=s.fuelDiscarded(); waterToSteam=s.waterToSteam();
        heatToSteam=s.heatToSteam();
    }

    public static boolean valid(Snapshot s) {
        if (s == null || s.water() < 0 || s.water() > MAX_AMOUNT
                || s.heat() < 0 || s.heat() > MAX_AMOUNT
                || (s.water() == 0 && s.heat() != 0)
                || s.burnTicks() < 0 || s.burnTicks() > MAX_BURN_TICKS
                || s.heatPerBurnTick() < 0 || s.heatPerBurnTick() > MAX_HEAT_PER_TICK
                || (s.burnTicks() > 0 && s.heatPerBurnTick() == 0)) return false;
        for (long v : new long[]{s.waterIn(), s.waterOut(), s.waterSpilled(),
                s.waterToSteam(), s.fuelEnergy(), s.generatedHeat(), s.heatLost(),
                s.heatInDrainedWater(), s.heatSpilled(), s.fuelDiscarded(), s.heatToSteam()})
            if (v < 0 || v > MAX_LEDGER) return false;
        return s.waterIn() == (long) s.water() + s.waterOut()
                    + s.waterSpilled() + s.waterToSteam()
                && s.generatedHeat() == (long) s.heat() + s.heatLost()
                    + s.heatInDrainedWater() + s.heatSpilled() + s.heatToSteam()
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
