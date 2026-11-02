package com.magneticraft2.common.systems.PRESSURE;

/**
 * Non-persistent wrapper for the pre-existing air-charge inventory.
 * A charge is NOT a pressure measurement. Legacy-to-legacy transfers therefore
 * deliberately skip the new kPa pressure rule in PressurizedPipeNetwork.
 * Older blower/roaster/bellows NBT is not migrated or rewritten.
 */
public final class LegacyAirPort implements IPressurizedGasHandler {
    private final IPressureStorage storage;
    public LegacyAirPort(IPressureStorage storage) {
        if (storage == null) throw new IllegalArgumentException("Missing legacy air storage");
        this.storage = storage;
    }
    @Override public GasMedium medium() { return GasMedium.AIR; }
    @Override public int stored() { return Math.max(0, storage.getPressureStored()); }
    @Override public int capacity() { return Math.max(1, storage.getMaxPressureStored()); }
    @Override public int maxFlowPerTick() { return 1; }
    // Unknown for pre-existing air charges. Not an invented source pressure.
    @Override public double gaugePressureKPa() { return 0.0; }
    @Override public double temperatureK() { return 293.15; }
    @Override public boolean canReceive() { return storage.canReceive(); }
    @Override public boolean canExtract() { return storage.canSend(); }
    @Override public int receive(int units, double kelvin, boolean simulate) {
        if (!canReceive() || units <= 0 || !Double.isFinite(kelvin)
                || kelvin < 1 || kelvin > 2000) return 0;
        return storage.receivePressure(Math.min(units, maxFlowPerTick()), simulate);
    }
    @Override public int extract(int units, boolean simulate) {
        if (!canExtract() || units <= 0) return 0;
        return storage.extractPressure(Math.min(units, maxFlowPerTick()), simulate);
    }
}
