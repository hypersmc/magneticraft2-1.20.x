package com.magneticraft2.common.systems.Multiblocking.core.modules;

import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * @author JumpWatch on 14-05-2023
 * @Project magneticraft2-1.18.2
 * v1.0.0
 */
public class FluidModule implements IMultiblockModule, IFluidHandler {

    private static final int CAPACITY = 1000; // The fluid capacity of the module

    private final FluidTank fluidTank; // The fluid tank for this module
    private final LazyOptional<IFluidHandler> fluidHandler; // The lazy optional for the fluid handler capability

    public FluidModule() {
        this.fluidTank = new FluidTank(CAPACITY);
        this.fluidHandler = LazyOptional.of(() -> this);
    }

    @Override
    public boolean isValid(Level world, BlockPos pos) {
        return true;
    }

    @Override
    public void onActivate(Level world, BlockPos pos) {
        // Nothing to do here
    }

    @Override
    public void onDeactivate(Level world, BlockPos pos) {
        // Nothing to do here
    }

    @Override
    public String getModuleKey() {
        return "fluid";
    }

    @Override
    public BlockPos getModuleOffset() {
        return new BlockPos(0, 0, 0);
    }

    @Override
    public IMultiblockModule createModule(Level world, BlockPos pos) {
        return new FluidModule();
    }

    // IFluidHandler implementation

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return fluidTank.getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return fluidTank.getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true; // Accept any fluid
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return fluidTank.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return fluidTank.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return fluidTank.drain(maxDrain, action);
    }

    // Getter for the fluid handler capability
    public LazyOptional<IFluidHandler> getFluidHandler() {
        return fluidHandler;
    }
}