package com.magneticraft2.common.systems.PRESSURE;

/**
 * Medium-aware, sided gas port. Units are abstract conserved gas quantities.
 * Pressure is gauge kPa; temperature is Kelvin. They are NOT interchangeable
 * with legacy IPressureStorage's integer "pressure" charges.
 *
 * Implementations must honor simulate and may refuse unsafe temperature or
 * pressure. The owning machine enforces its own per-tick production/output budget.
 */
public interface IPressurizedGasHandler {
    GasMedium medium();
    int stored();
    int capacity();
    int maxFlowPerTick();
    double gaugePressureKPa();
    double temperatureK();
    boolean canReceive();
    boolean canExtract();
    int receive(int units, double temperatureK, boolean simulate);
    int extract(int units, boolean simulate);
}
