package com.magneticraft2.common.systems.steam;

/** Dependency-free conserved Steam inventory owned by one boiler vessel. */
public final class BoilerSteamState {
    public static final int MAX_UNITS = 1_000_000;
    private static final long MAX_LEDGER = Long.MAX_VALUE / 4;
    private int stored;
    private long produced, exported, vented, spilled;

    public record Snapshot(int stored, long produced, long exported,
            long vented, long spilled) {
        /** Save-format 2/3 compatibility: relief did not debit local Steam. */
        public Snapshot(int stored, long produced, long exported, long spilled) {
            this(stored, produced, exported, 0, spilled);
        }
    }

    public int stored() { return stored; }
    public int space(int capacity) {
        checkCapacity(capacity);
        return Math.max(0, capacity - stored);
    }
    public int produce(int offered, int capacity) {
        checkCapacity(capacity);
        if (offered <= 0) return 0;
        int accepted = Math.min(offered, space(capacity));
        if (produced > MAX_LEDGER - accepted) return 0;
        stored += accepted;
        produced += accepted;
        return accepted;
    }
    /** Steam transferred into the external pipe network. */
    public int extract(int requested) {
        if (requested <= 0) return 0;
        int amount = Math.min(requested, stored);
        if (exported > MAX_LEDGER - amount) return 0;
        stored -= amount;
        exported += amount;
        return amount;
    }
    /** Steam deliberately discharged through the boiler's own relief valve. */
    public int vent(int requested) {
        if (requested <= 0) return 0;
        int amount = Math.min(requested, stored);
        if (vented > MAX_LEDGER - amount) return 0;
        stored -= amount;
        vented += amount;
        return amount;
    }
    /** Remaining vessel inventory discarded during teardown. */
    public int spill() {
        int amount = stored;
        if (spilled > MAX_LEDGER - amount)
            throw new IllegalStateException("Steam ledger overflow");
        stored = 0;
        spilled += amount;
        return amount;
    }
    public Snapshot snapshot() {
        return new Snapshot(stored, produced, exported, vented, spilled);
    }
    public void restore(Snapshot s) {
        if (!valid(s)) throw new IllegalArgumentException("Invalid boiler Steam ledger");
        stored=s.stored(); produced=s.produced(); exported=s.exported();
        vented=s.vented(); spilled=s.spilled();
    }
    public static boolean valid(Snapshot s) {
        return s != null && s.stored() >= 0 && s.stored() <= MAX_UNITS
                && s.produced() >= 0 && s.produced() <= MAX_LEDGER
                && s.exported() >= 0 && s.exported() <= MAX_LEDGER
                && s.vented() >= 0 && s.vented() <= MAX_LEDGER
                && s.spilled() >= 0 && s.spilled() <= MAX_LEDGER
                && s.produced() == (long) s.stored() + s.exported()
                    + s.vented() + s.spilled();
    }
    private static void checkCapacity(int capacity) {
        if (capacity <= 0 || capacity > MAX_UNITS)
            throw new IllegalArgumentException("Invalid Steam capacity");
    }
}
