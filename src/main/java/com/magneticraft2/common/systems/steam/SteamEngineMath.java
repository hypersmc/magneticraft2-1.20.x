package com.magneticraft2.common.systems.steam;

/** Dependency-free Steam I balance math; values come from datapack rules. */
public final class SteamEngineMath {
    private SteamEngineMath() {}

    public record Drive(boolean available, float rpm, float torque) {}

    public static Drive drive(double pressureKPa, double minimumPressureKPa,
            double ratedPressureKPa, float minimumRpm, float ratedRpm,
            float ratedTorque) {
        if (!Double.isFinite(pressureKPa) || !Double.isFinite(minimumPressureKPa)
                || !Double.isFinite(ratedPressureKPa)
                || minimumPressureKPa <= 0 || ratedPressureKPa <= minimumPressureKPa
                || minimumRpm <= 0 || ratedRpm < minimumRpm || ratedTorque <= 0)
            throw new IllegalArgumentException("Invalid Steam Engine balance");
        if (pressureKPa < minimumPressureKPa)
            return new Drive(false, 0.0F, 0.0F);
        double normalized = Math.max(0.0, Math.min(1.0,
                (pressureKPa - minimumPressureKPa)
                        / (ratedPressureKPa - minimumPressureKPa)));
        float rpm = (float) (minimumRpm + (ratedRpm - minimumRpm) * normalized);
        float torque = (float) (ratedTorque
                * Math.min(1.0, pressureKPa / ratedPressureKPa));
        return new Drive(true, rpm, torque);
    }

    public static int milliSteamPerTick(float sourceDemand, float availableTorque,
            boolean overloaded, int idleMilli, int loadMilliPerTorque) {
        if (idleMilli < 0 || loadMilliPerTorque < 0 || availableTorque < 0)
            throw new IllegalArgumentException("Invalid Steam consumption balance");
        float usefulDemand = overloaded ? 0.0F
                : Math.max(0.0F, Math.min(sourceDemand, availableTorque));
        return idleMilli + Math.round(usefulDemand * loadMilliPerTorque);
    }
}
