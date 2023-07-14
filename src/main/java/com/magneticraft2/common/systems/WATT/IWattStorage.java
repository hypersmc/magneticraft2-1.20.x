package com.magneticraft2.common.systems.WATT;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public interface IWattStorage {
    int receiveWatt(int maxReceive, boolean simulate);
    int extractWatt(int maxExtract, boolean simulate);
    int getWattStored();
    int getMaxWattStored();
    boolean canSend();
    boolean canReceive();

    boolean setSend(boolean val);

    boolean setReceive(boolean val);
}
