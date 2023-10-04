package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.systems.HEAT.CapabilityHeat;
import com.magneticraft2.common.systems.HEAT.IHeatStorage;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressure;
import com.magneticraft2.common.systems.PRESSURE.IPressureStorage;
import com.magneticraft2.common.systems.WATT.CapabilityWatt;
import com.magneticraft2.common.systems.WATT.IWattStorage;
import com.magneticraft2.common.utils.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 01-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public abstract class BaseBlockEntityMagneticraft2 extends BlockEntity implements MenuProvider, ISync {
    public static final Logger LOGGER = LogManager.getLogger("BaseBlockEntityMagneticraft2");
    public MenuProvider menuProvider;
    private MultiblockController multiblockController;
    private Multiblock multiblock;

    protected abstract MultiblockController createMultiblockController();
    protected abstract MultiblockStructure identifyMultiblockStructure(Level world, BlockPos pos);

    //Biomes
    /* Energy */

    public abstract int capacityE();
    public abstract int maxtransferE();
    /* Heat */
    public abstract int capacityH();
    public abstract int maxtransferH();
    /* Watt */
    public abstract int capacityW();
    public abstract int maxtransferW();
    /* Fluid */
    public abstract int capacityF();
    public abstract int tanks();
    /* Inv */
    public abstract int invsize();
    /* Pressure */
    public abstract int capacityP();
    public abstract int maxtransferP();

    /* Create handlers */
    public final EnergyStorages energyHandler = createEnergy(); //Energy (RF)
    public final ItemStackHandler itemHandler = createInv(); //Item
    public final HeatStorages heatHandler = createHeat(); //Heat
    public final WattStorages wattHandler = createWatt(); //Watt
    public final FluidStorages fluidHandler = createFluid(); //Fluid
    /**
     * Pressure system is being removed!
     */
    public final PressureStorages pressureHandler = createPressure(); //Pressure

    public abstract boolean itemcape(); //Is the TileEntity capable of Item handling
    public abstract boolean energycape(); //Is the TileEntity capable of Energy (RF) handling
    public abstract boolean heatcape(); //Is the TileEntity capable of Heat handling
    public abstract boolean wattcape(); //Is the TileEntity capable of Wattage/Voltage handling
    public abstract boolean fluidcape(); //Is the TileEntity capable of Fluid handling
    public abstract boolean pressurecape(); //Is the TileEntity capable of Pressure handling

    public abstract boolean HeatCanReceive();
    public abstract boolean HeatCanSend();
    public abstract boolean WattCanReceive();
    public abstract boolean WattCanSend();
    public abstract boolean EnergyCanReceive();
    public abstract boolean EnergyCanSend();
    public abstract boolean PressureCanReceive();
    public abstract boolean PressureCanSend();
    /**
     * Reworking from AnimationFactory to createFactory
     */
//    @Deprecated(forRemoval = true)
//    public final AnimationFactory factory = new AnimationFactory(this);
    public final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> energyHandler); //Creating LazyOptional for Energy (RF)
    public final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler); //Creating LazyOptional for Item
    public final LazyOptional<IHeatStorage> heat = LazyOptional.of(() -> heatHandler); //Creating LazyOptional for Heat
    public final LazyOptional<IWattStorage> watt = LazyOptional.of(() -> wattHandler); //Creating LazyOptional for Wattage/Voltage
    public final LazyOptional<IFluidHandler> fluid = LazyOptional.of(() -> fluidHandler); //Creating LazyOptional for Fluid
    public final LazyOptional<IPressureStorage> pressure = LazyOptional.of(() -> pressureHandler); //Creating LazyOptional for Pressure

    public BaseBlockEntityMagneticraft2(BlockEntityType<?> pType, BlockPos pWorldPosition, BlockState pBlockState) {
        super(pType, pWorldPosition, pBlockState);
    }
    protected void setMultiblockController(MultiblockController multiblockController) {
        this.multiblockController = multiblockController;
    }
    protected void setMultiblock(Multiblock multiblock){
        this.multiblock = multiblock;
    }

    protected MultiblockController getMultiblockController() {
        return this.multiblockController;
    }
    public void onRightClick() {
        if (this.multiblockController != null) {
            this.multiblockController.createStructure(level,getBlockPos());
        }else{
            createMultiblockController();
        }
    }
    public void onDestry(){
        this.multiblockController.destroyStructure(level);
    }
    public boolean matchesStructure(Level world, BlockPos pos, @NotNull MultiblockStructure structure, Multiblock multiblock) {
        Map<String, List<List<String>>> layout = structure.getLayout();
        Map<String, Block> blocks = structure.getBlocks();
        int[] dimensions = structure.getDimensions();

        // Get the block instance from the registry or wherever you have stored it
        Block controllerBlock = level.getBlockState(pos).getBlock();
        LOGGER.info("Controller Block: " + controllerBlock);

        // Calculate the offset of the controller block dynamically
        BlockPos controllerOffset = findControllerOffset(structure, controllerBlock);
        LOGGER.info("Controller Offset: " + controllerOffset);

        // Adjust the starting position by the offset
        BlockPos adjustedPos = pos.offset(-controllerOffset.getX(), -controllerOffset.getY(), -controllerOffset.getZ());
        LOGGER.info("Adjusted Position: " + adjustedPos);

        for (int y = 0; y < dimensions[1]; y++) {
            LOGGER.info("Checking Layer y: " + (y+1));
            List<List<String>> layer = layout.get("layer" + (y + 1));
            if (layer == null) {
                LOGGER.warn("Layer " + (y+1) + " is not defined in the layout");
                return false;
            }
            for (int z = 0; z < dimensions[2]; z++) {
                LOGGER.info("Checking Layer z: " + (z+1));
                for (int x = 0; x < dimensions[0]; x++) {
                    LOGGER.info("Checking Layer x: " + (x+1));
                    String blockKey = layer.get(z).get(x);
                    if (!blockKey.equals(" ")) {
                        Block expectedBlock = blocks.get(blockKey);
                        if (expectedBlock == null) {
                            LOGGER.warn("Block key " + blockKey + " is not defined in the blocks map");
                            return false;
                        }
                        BlockState stateInWorld = world.getBlockState(adjustedPos.offset(x, y, z));
                        LOGGER.info("Checking Position: " + adjustedPos.offset(x, y, z));
                        LOGGER.info("Expected Block: " + expectedBlock);
                        LOGGER.info("Block in World: " + stateInWorld.getBlock());
                        if (!stateInWorld.is(expectedBlock)) {
                            LOGGER.warn("Mismatch found at position: " + adjustedPos.offset(x, y, z) + " Expected: " + expectedBlock + " Found: " + stateInWorld.getBlock());
                            return false;
                        }
                    }
                }
            }
        }
        this.setMultiblock(multiblock);
        LOGGER.info("Structure matched successfully.");
        return true;
    }

    public BlockPos findControllerOffset(MultiblockStructure structure, Block controllerBlock) {
        Map<String, List<List<String>>> layout = structure.getLayout();
        int[] dimensions = structure.getDimensions();
        Map<String, Block> blocks = structure.getBlocks();

        for (int y = 0; y < dimensions[1]; y++) {
            List<List<String>> layer = layout.get("layer" + (y + 1));
            for (int z = 0; z < dimensions[2]; z++) {
                for (int x = 0; x < dimensions[0]; x++) {
                    String blockKey = layer.get(z).get(x);
                    if (blocks.get(blockKey) == controllerBlock) {
                        // Calculate the offset based on the relative position of the controller block in the structure
                        return new BlockPos(x - dimensions[0] / 2 +1, y - dimensions[1] / 2+1, z - dimensions[2] / 2+1);
                    }
                }
            }
        }
        return BlockPos.ZERO; // Default offset (0, 0, 0) if the controller block is not found in the structure
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction dir) {
        if (itemcape()) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) {
                return handler.cast();
            }
        }
        if (energycape()) {
            if (cap == ForgeCapabilities.ENERGY) {
                return energy.cast();
            }
        }
        if (heatcape()) {
            if (cap == CapabilityHeat.HEAT) {
                return heat.cast();
            }
        }
        if (wattcape()) {
            if (cap == CapabilityWatt.WATT) {
                return watt.cast();
            }
        }
        if (fluidcape()) {
            if (cap == ForgeCapabilities.FLUID_HANDLER) {
                return fluid.cast();
            }
        }
        if (pressurecape()) {
            if (cap == CapabilityPressure.PRESSURE) {
                return pressure.cast();
            }
        }
        return LazyOptional.empty();
    }




    /*
     *Energy, Item, Fluid, Pressure & Heat handler
     */

    public void setHeatHeat(int heat) {
        createHeat().setHeat(heat);
    }
    public void setEnergyEnergy(int energy) {
        createEnergy().setEnergy(energy);
    }

    private FluidStorages createFluid(){
        if (fluidcape()) {
            return new FluidStorages() {
                @Override
                public void setTanks(int tanknumb) {
                    super.setTanks(tanks());
                }

                @Override
                public void setCapacity(int capacity) {
                    super.setCapacity(capacityF());
                }
            };
        }else {
            return null;
        }
    }
    private WattStorages createWatt(){
        if (wattcape()) {
            return new WattStorages(capacityW(), maxtransferW()){
                @Override
                protected void onWattChanged() {
                    setChanged();
                }

                @Override
                public boolean canReceive() {
                    return WattCanReceive();
                }

                @Override
                public boolean canSend() {
                    return WattCanSend();
                }
            };
        }else {
            return null;
        }
    }
    private HeatStorages createHeat(){
        if (heatcape()) {
            return new HeatStorages(capacityH(), maxtransferH()){
                @Override
                protected void onHeatChanged() {
                    setChanged();
                }

                @Override
                public boolean canReceive() {
                    return HeatCanReceive();
                }

                @Override
                public boolean canSend() {
                    return HeatCanSend();
                }

                @Override
                public void setHeat(int heat) {
                    super.setHeat(heat);
                }
            };
        }else {
            return null;
        }
    }
    private EnergyStorages createEnergy() {
        if (energycape()) {
            return new EnergyStorages(capacityE(), maxtransferE()) {
                @Override
                protected void onEnergyChanged() {
                    setChanged();
                }

                @Override
                public boolean canReceive() {
                    return EnergyCanReceive();
                }

                @Override
                public boolean canExtract() {
                    return EnergyCanSend();
                }

            };
        }else{
            return null;
        }
    }
    private PressureStorages createPressure() {
        if (pressurecape()) {
            return new PressureStorages(capacityP(), maxtransferP()) {
                @Override
                protected void onPressureChanged(){
                    setChanged();
                }

                @Override
                public boolean canReceive() {
                    return PressureCanReceive();
                }

                @Override
                public boolean canSend() {
                    return PressureCanSend();
                }
            };
        }else {
            return null;
        }
    }

    private ItemStackHandler createInv() {
        if (itemcape()) {
            return new ItemStackHandler(invsize()) {
                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }

                @Override
                public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                    return super.isItemValid(slot, stack);
                }

                @Nonnull
                @Override
                public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                    return super.insertItem(slot, stack, simulate);
                }
            };
        }else{
            return null;
        }
    }




    /*
     * Saving and loading NBT data
     */

    @Override
    protected void saveAdditional(CompoundTag tag) {
        if (itemcape()) {
            //itemHandler.deserializeNBT(tag.getCompound("inv"));
            try {
                tag.put("inv", itemHandler.serializeNBT());
            }catch (Exception e) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get())e.printStackTrace();
            }
        }
        if (energycape()) {
            //energyHandler.deserializeNBT(tag.getCompound("energy"));
            try {
                tag.put("energy", energyHandler.serializeNBT());
            }catch (Exception e) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get())e.printStackTrace();
            }
        }
        if (heatcape()) {
            //heatHandler.deserializeNBT(tag.getCompound("heat"));
            try {
                tag.put("heat", heatHandler.serializeNBT());
            }catch (Exception e) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get())e.printStackTrace();
            }
        }
        if (wattcape()) {
            //wattHandler.deserializeNBT(tag.getCompound("watt"));
            try {
                tag.put("watt", wattHandler.serializeNBT());
            }catch (Exception e) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get())e.printStackTrace();
            }
        }
        if (pressurecape()) {
            //pressureHandler.deserializeNBT(tag.getCompound("pressure"));
            try {
                tag.put("pressure", pressureHandler.serializeNBT());
            }catch (Exception e) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get())e.printStackTrace();
            }
        }
        if (fluidcape()) {
            //fluidHandler.deserializeNBT(tag.getCompound("fluidamount"));
            //fluidHandler.deserializeNBT(tag.getCompound("fluidtype"));
            try {
                tag.put("fluidamount", fluidHandler.serializeNBT());
                tag.put("fluidtype", fluidHandler.serializeNBT());
            }catch (Exception e) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get())e.printStackTrace();
            }
        }
        super.saveAdditional(tag);
    }



    public Direction getFacing() {
        Level world = getLevel();
        BlockState state = world.getBlockState(getBlockPos());
        if (state.hasProperty(BaseBlockMagneticraft2.FACING)) {
            return state.getValue(BaseBlockMagneticraft2.FACING);
        }

        return Direction.UP;
    }
    @Override
    public void load( CompoundTag tag) {
        if (itemcape()) {
//            tag.put("inv", itemHandler.serializeNBT());
            itemHandler.deserializeNBT(tag.getCompound("inv"));

        }
        if (energycape()) {
//            tag.put("energy", energyHandler.serializeNBT());
            energyHandler.deserializeNBT(tag.getCompound("energy"));
        }
        if (heatcape()) {
//            tag.put("heat", heatHandler.serializeNBT());
            heatHandler.deserializeNBT(tag.getCompound("heat"));
        }
        if (wattcape()) {
//            tag.put("watt", wattHandler.serializeNBT());
            wattHandler.deserializeNBT(tag.getCompound("watt"));
        }
        if (pressurecape()) {
//            tag.put("pressure", pressureHandler.serializeNBT());
            pressureHandler.deserializeNBT(tag.getCompound("pressure"));
        }
        if (fluidcape()) {
//            tag.put("fluidamount", fluidHandler.serializeNBT());
//            tag.put("fluidtype", fluidHandler.serializeNBT());
            fluidHandler.deserializeNBT(tag.getCompound("fluidamount"));
            fluidHandler.deserializeNBT(tag.getCompound("fluidtype"));
        }
        super.load(tag);
    }

    /*
     * Get the dif types of capabilities storage and max storage.
     */

    /* Heat */
    public int getHeatStorage(){
        return this.heatHandler.getHeatStored();
    }
    public int getMaxHeatStorage(){
        return this.heatHandler.getMaxHeatStored();
    }
    public void addHeatToStorage(int heat){
        this.heatHandler.addHeat(heat);
    }
    public void setHeatStorage(int heat) {
        this.heatHandler.setHeat(heat);
    }
    public void removeHeatFromStorage(int heat) {
        this.heatHandler.consumeHeat(heat);
    }

    /* Watt */
    public int getWattStorage(){
        return this.wattHandler.getWattStored();
    }
    public int getMaxWattStorage(){
        return this.wattHandler.getMaxWattStored();
    }

    /* Energy */
    public int getEnergyStorage(){
        return energyHandler.getEnergyStored();
    }
    public int getMaxEnergyStorage(){
        return this.energyHandler.getMaxEnergyStored();
    }
    public void addEnergyToStorage(int energy){
        this.energyHandler.addEnergy(energy);
    }

    public void removeEnergyFromStorage(int energy) {
        this.energyHandler.consumeEnergy(energy);
    }

    /* Item */
    public int getInvSize(){
        return this.itemHandler.getSlots();
    }
    public ItemStack getItemInSlot(int size) {
        return this.itemHandler.getStackInSlot(size);
    }

    /* Fluid */
    public int getFluidTanks(){
        return this.fluidHandler.getTanks();
    }
    public int getFluidCapacity(){
        return 0;
    }


    /* Pressure */

    public int getPressureStorage(){
        return this.pressureHandler.getPressureStored();
    }
    public int getMaxPressureStorage(){
        return this.pressureHandler.getMaxPressureStored();
    }

    /* end */


    /*
     * Used for TOP (TheOneProbe)
     */

    public boolean getHeatCap(){
        return heatcape();
    }
    public boolean getWattCap(){
        return wattcape();
    }
    public boolean getPressureCap(){
        return pressurecape();
    }
    public void onBlockBreak()
    {
    }
}
