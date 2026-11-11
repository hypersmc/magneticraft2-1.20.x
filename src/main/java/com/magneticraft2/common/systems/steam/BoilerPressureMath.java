package com.magneticraft2.common.systems.steam;

/**
 * Provisional boiler steam-space pressure arithmetic. This intentionally uses
 * the same gameplay scale as the Steam pipe network, but gives the vessel its
 * own configurable volume. Values are not claimed real thermodynamics.
 */
public final class BoilerPressureMath {
    public static final double REFERENCE_K = 453.15;
    public static final double KPA_PER_UNIT_PER_PIPE_VOLUME = 100.0;
    private BoilerPressureMath() {}

    public static double pressureKPa(int steamUnits, double temperatureK,
            double pipeVolumeEquivalents) {
        if (steamUnits <= 0) return 0.0;
        if (!Double.isFinite(temperatureK) || temperatureK <= 0
                || !Double.isFinite(pipeVolumeEquivalents)
                || pipeVolumeEquivalents <= 0)
            throw new IllegalArgumentException("Invalid boiler pressure parameters");
        return KPA_PER_UNIT_PER_PIPE_VOLUME
                * (steamUnits / pipeVolumeEquivalents)
                * (temperatureK / REFERENCE_K);
    }
}
