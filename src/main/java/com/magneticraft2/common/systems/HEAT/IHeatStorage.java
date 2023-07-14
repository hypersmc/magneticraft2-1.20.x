package com.magneticraft2.common.systems.HEAT;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public interface IHeatStorage {
    int receiveHeat(int maxReceive, boolean simulate);
    int extractHeat(int maxExtract, boolean simulate);
    int getHeatStored();
    int getMaxHeatStored();
    boolean canSend();
    boolean canReceive();
}
