package com.magneticraft2.common.systems.WATT;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class WattStorage implements IWattStorage {

    protected int watt;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public WattStorage(int capacity) {
        this(capacity,capacity,capacity,0);
    }
    public WattStorage(int capacity, int maxTransfer){
        this(capacity,maxTransfer,maxTransfer,0);
    }
    public WattStorage(int capacity, int maxReceive, int maxExtract){
        this(capacity,maxReceive,maxExtract,0);
    }
    public WattStorage(int capacity, int maxReceive, int maxExtract, int watt){
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.watt = Math.max(0, Math.min(capacity, watt));
    }

    @Override
    public int receiveWatt(int maxReceive, boolean simulate) {
        if (!canReceive())
            return 0;
        int wattReceived = Math.min(capacity - watt, Math.min(this.maxReceive,maxReceive));
        if (!simulate)
            watt += wattReceived;
        return wattReceived;
    }

    @Override
    public int extractWatt(int maxExtract, boolean simulate) {
        if (!canReceive())
            return 0;
        int wattExtracted = Math.min(watt, Math.min(this.maxExtract,maxExtract));
        if (!simulate)
            watt -= wattExtracted;
        return wattExtracted;
    }

    @Override
    public int getWattStored() {
        return watt;
    }

    @Override
    public int getMaxWattStored() {
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

    @Override
    public boolean setSend(boolean val) {
        if (val) {
            return this.maxExtract > 0;
        }else {
            return this.maxExtract < 0;
        }
    }

    @Override
    public boolean setReceive(boolean val) {
        if (val) {
            return this.maxReceive > 0;
        }else {
            return this.maxReceive < 0;
        }
    }
}
