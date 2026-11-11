package com.magneticraft2.common.systems.steam;

/** Dependency-free gauge quantization and relief hysteresis. */
public final class BoilerSafetyLogic {
    private BoilerSafetyLogic() {}

    public static boolean nextReliefOpen(boolean current, double pressureKPa,
            double openKPa, double closeKPa) {
        if (!Double.isFinite(pressureKPa) || !Double.isFinite(openKPa)
                || !Double.isFinite(closeKPa) || openKPa <= 0
                || closeKPa < 0 || closeKPa >= openKPa)
            throw new IllegalArgumentException("Invalid relief parameters");
        if (!current && pressureKPa >= openKPa) return true;
        if (current && pressureKPa <= closeKPa) return false;
        return current;
    }

    public static int gaugeStep(double pressureKPa, double maxKPa) {
        if (!Double.isFinite(pressureKPa) || !Double.isFinite(maxKPa) || maxKPa <= 0)
            throw new IllegalArgumentException("Invalid gauge parameters");
        double normalized = Math.max(0.0, Math.min(1.0, pressureKPa / maxKPa));
        return Math.max(0, Math.min(5, (int) Math.round(normalized * 5.0)));
    }
}
