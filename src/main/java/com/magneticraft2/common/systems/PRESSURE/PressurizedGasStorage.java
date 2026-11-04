package com.magneticraft2.common.systems.PRESSURE;

import net.minecraft.nbt.CompoundTag;

/**
 * Bounded medium-specific gas inventory for machine ports, not for individual pipes.
 * The pressure model is a simplified gameplay relation, not a steam equation
 * of state. Boilers must handle phase transitions, heat and safety separately.
 */
public final class PressurizedGasStorage implements IPressurizedGasHandler {
    private final GasMedium medium;
    private final int capacity, maxFlow;
    private final double ratedGaugeKPa, referenceTemperatureK;
    private int stored;
    private double temperatureK;

    public PressurizedGasStorage(GasMedium medium, int capacity, int maxFlow,
            double ratedGaugeKPa, double referenceTemperatureK) {
        if (medium == null || capacity <= 0 || maxFlow <= 0
                || !Double.isFinite(ratedGaugeKPa) || ratedGaugeKPa <= 0
                || !validTemperature(referenceTemperatureK))
            throw new IllegalArgumentException("Invalid gas vessel specification");
        this.medium = medium;
        this.capacity = capacity;
        this.maxFlow = maxFlow;
        this.ratedGaugeKPa = ratedGaugeKPa;
        this.referenceTemperatureK = referenceTemperatureK;
        this.temperatureK = referenceTemperatureK;
    }

    private static boolean validTemperature(double kelvin) {
        return Double.isFinite(kelvin) && kelvin >= 1.0 && kelvin <= 2000.0;
    }
    @Override public GasMedium medium() { return medium; }
    @Override public int stored() { return stored; }
    @Override public int capacity() { return capacity; }
    @Override public int maxFlowPerTick() { return maxFlow; }
    @Override public double temperatureK() { return temperatureK; }
    @Override public boolean canReceive() { return true; }
    @Override public boolean canExtract() { return true; }
    @Override public double gaugePressureKPa() {
        return ratedGaugeKPa * (stored / (double) capacity)
                * (temperatureK / referenceTemperatureK);
    }

    @Override public int receive(int units, double incomingTemperatureK, boolean simulate) {
        if (units <= 0 || !validTemperature(incomingTemperatureK)) return 0;
        // No silent condensation or conversion to water; that requires a
        // dedicated thermal machine and explicit fluid/steam conversion.
        if (medium == GasMedium.STEAM && incomingTemperatureK < 373.15) return 0;
        int offered = Math.min(Math.min(units, maxFlow), capacity - stored);
        if (offered <= 0) return 0;
        // Total pressure is proportional to stored thermal units, so solve
        // the vessel limit BEFORE writing to avoid overpressurising it.
        double headroom = capacity * referenceTemperatureK - stored * temperatureK;
        int pressureLimited = (int) Math.max(0, Math.min((double) offered,
                Math.floor(Math.max(0.0, headroom) / incomingTemperatureK + 1e-9)));
        if (pressureLimited <= 0) return 0;
        if (!simulate) {
            temperatureK = (temperatureK * stored
                    + incomingTemperatureK * pressureLimited) / (stored + pressureLimited);
            stored += pressureLimited;
        }
        return pressureLimited;
    }

    @Override public int extract(int units, boolean simulate) {
        if (units <= 0) return 0;
        int taken = Math.min(Math.min(units, maxFlow), stored);
        if (!simulate) stored -= taken;
        return taken;
    }

    /** Only valid while the resulting fill remains inside the vessel rating. */
    public boolean setTemperatureK(double kelvin) {
        if (!validTemperature(kelvin) ||
                stored * kelvin > capacity * referenceTemperatureK + 1e-9) return false;
        temperatureK = kelvin;
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Medium", medium.name());
        tag.putInt("Amount", stored);
        tag.putDouble("TemperatureK", temperatureK);
        return tag;
    }

    public void load(CompoundTag tag) {
        // Wrong-medium or invalid steam data MUST NOT create another gas.
        if (!medium.name().equals(tag.getString("Medium"))) {
            stored = 0; temperatureK = referenceTemperatureK; return;
        }
        double temp = tag.getDouble("TemperatureK");
        if (!validTemperature(temp)
                || (medium == GasMedium.STEAM && temp < 373.15)) {
            stored = 0; temperatureK = referenceTemperatureK; return;
        }
        int amount = Math.max(0, Math.min(capacity, tag.getInt("Amount")));
        int pressureLimit = (int) Math.max(0,
                Math.floor(capacity * referenceTemperatureK / temp + 1e-9));
        temperatureK = temp;
        stored = Math.min(amount, pressureLimit);
    }
}
