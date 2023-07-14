package com.magneticraft2.common.systems.PRESSURE;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class PressureStorage implements IPressureStorage{

    protected int pressure;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public PressureStorage(int capacity) {
        this(capacity,capacity,capacity,0);
    }
    public PressureStorage(int capacity, int maxTransfer){
        this(capacity,maxTransfer,maxTransfer,0);
    }
    public PressureStorage(int capacity, int maxReceive, int maxExtract){
        this(capacity,maxReceive,maxExtract,0);
    }
    public PressureStorage(int capacity, int maxReceive, int maxExtract, int pressure){
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.pressure = Math.max(0, Math.min(capacity, pressure));
    }

    @Override
    public int receivePressure(int maxReceive, boolean simulate) {
        if (!canReceive())
            return 0;
        int pressureReceived = Math.min(capacity - pressure, Math.min(this.maxReceive,maxReceive));
        if (!simulate)
            pressure += pressureReceived;
        return pressureReceived;
    }

    @Override
    public int extractPressure(int maxExtract, boolean simulate) {
        if (!canReceive())
            return 0;
        int pressureExtracted = Math.min(pressure, Math.min(this.maxExtract,maxExtract));
        if (!simulate)
            pressure -= pressureExtracted;
        return pressureExtracted;
    }

    @Override
    public int getPressureStored() {
        return pressure;
    }

    @Override
    public int getMaxPressureStored() {
        return capacity;
    }

    @Override
    public boolean canSend() {
        return this.maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0;
    }
}
