package com.magneticraft2.common.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import javax.annotation.Nonnull;

/**
 * @author JumpWatch on 01-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class FluidStorages implements IFluidHandler, INBTSerializable<CompoundTag> {

    private int tank;
    private int capacity;
    private FluidStack fluid;
    public static final FluidStorages INSTANCE = new FluidStorages();


    @Override
    public int getTanks() {
        return tank;
    }
    public void setTanks(int tanknumb) {
        this.tank = tanknumb;
    }
    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return fluid;
    }

    public void setFluidType(FluidStack fluid) {
        this.fluid = fluid;
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacity;
    }
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("fluidtype", String.valueOf(fluid));
        tag.putInt("fluidamount", capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        setCapacity(nbt.getInt("fluidamount"));
        setFluidType((FluidStack) nbt.get("fluidtype"));
    }
}