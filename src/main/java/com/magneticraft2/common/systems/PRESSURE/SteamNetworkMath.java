package com.magneticraft2.common.systems.PRESSURE;

/**
 * Pure Java provisional Steam Network arithmetic. Values are gameplay
 * coefficients, NOT real steam equations of state. Keeping this isolated
 * makes conservation and rupture thresholds independently testable.
 */
public final class SteamNetworkMath {
    public static final double REFERENCE_K = 453.15;
    public static final double KPA_PER_UNIT_PER_PIPE = 100.0;
    private SteamNetworkMath() {}

    public static double pressureKPa(long units, double energy, int pipeCount) {
        if (units <= 0 || pipeCount <= 0 || !Double.isFinite(energy)
                || energy <= 0) return 0;
        return KPA_PER_UNIT_PER_PIPE * (units / (double) pipeCount)
                * (energy / units) / REFERENCE_K;
    }
    public static int share(long total, int count, int index) {
        if (total < 0 || count <= 0 || index < 0 || index >= count)
            throw new IllegalArgumentException("Invalid steam partition");
        long share = total / count + (index < total % count ? 1 : 0);
        return Math.toIntExact(share);
    }
    /** One vented unit at low pressure; larger holes vent more at higher pressure. */
    public static int leakUnits(long units, double pressureKPa) {
        if (units <= 0 || pressureKPa <= 0) return 0;
        int requested = 1 + (int) Math.min(64, Math.floor(pressureKPa / 300.0));
        return (int) Math.min(units, requested);
    }
}
