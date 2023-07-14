package com.magneticraft2.common.systems.PRESSURE;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public interface IPressureStorage {

    int receivePressure(int maxReceive, boolean simulate);
    int extractPressure(int maxExtract, boolean simulate);
    int getPressureStored();
    int getMaxPressureStored();
    boolean canSend();
    boolean canReceive();
}
