package com.magneticraft2.common.systems.Multiblocking.core.modules;

import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * @author JumpWatch on 14-05-2023
 * @Project magneticraft2-1.18.2
 * v1.0.0
 */
public class FuelModule implements IMultiblockModule {

    private static final int MAX_FUEL_AMOUNT = 10000;
    private static final int FUEL_CONSUME_RATE = 1;
    private static final boolean status = false;

    private int fuelAmount;

    public FuelModule() {
        this.fuelAmount = 0;
    }

    public int getFuelAmount() {
        return fuelAmount;
    }

    public void setFuelAmount(int fuelAmount) {
        this.fuelAmount = fuelAmount;
    }

    @Override
    public boolean isValid(Level world, BlockPos pos) {
        return true;
    }

    @Override
    public void onActivate(Level world, BlockPos pos) {
        // Do nothing atm
    }

    @Override
    public void onDeactivate(Level world, BlockPos pos) {
        // Do nothing
    }

    @Override
    public String getModuleKey() {
        return "fuel_storage";
    }

    @Override
    public BlockPos getModuleOffset() {
        return new BlockPos(0, 0, 0);
    }

    @Override
    public IMultiblockModule createModule(Level world, BlockPos pos) {
        return new FuelModule();
    }

    // Method called by the reactor to consume fuel
    public int consumeFuel(int fuelAmount) {
        int fuelConsumed = Math.min(this.fuelAmount, fuelAmount);
        this.fuelAmount -= fuelConsumed;
        return fuelConsumed;
    }

    // Method called every tick to update the fuel amount
    public void tick() {
        if (fuelAmount > 0) {
            fuelAmount = Math.max(0, fuelAmount - FUEL_CONSUME_RATE);
        }
    }
}