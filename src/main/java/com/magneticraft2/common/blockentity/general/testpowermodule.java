package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import com.magneticraft2.common.systems.Multiblocking.core.modules.EnergyModule;
import com.magneticraft2.common.systems.WATT.CapabilityWatt;
import com.magneticraft2.common.systems.WATT.IWattStorage;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.common.utils.WattStorages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 21-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class testpowermodule extends BlockEntity implements IMultiblockModule {
    public static final Logger LOGGER = LogManager.getLogger("EnergyModule");

    private final WattStorages wattHandler;
    private final LazyOptional<IWattStorage> watt;
    public testpowermodule(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.testpowermodule.get(), pos, state);
        this.wattHandler = new WattStorages(10000000, 2048){
            @Override
            protected void onWattChanged() {

            }

            @Override
            public boolean canReceive() {
                return true;
            }

            @Override
            public boolean canSend() {
                return true;
            }
        };
        this.watt = LazyOptional.of(() -> wattHandler);
    }

    @Override
    public boolean isValid(Level world, BlockPos pos) {
        return true;
    }

    public int getStored(){
        return wattHandler.getWattStored();
    }

    @Override
    public void onActivate(Level world, BlockPos pos) {
        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            LOGGER.info("Trying to activate: " + getModuleKey());
        }
        if (!world.isClientSide){
            watt.ifPresent(handler -> handler.setReceive(true));
            watt.ifPresent(handler -> handler.setSend(true));
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Activated: " + getModuleKey());
                LOGGER.info("Stored wattage: " + wattHandler.getWattStored());
            }

        }

    }

    @Override
    public void onDeactivate(Level world, BlockPos pos) {
        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            LOGGER.info("Trying to deactivate: " + getModuleKey());
        }
        if (!world.isClientSide){
            watt.ifPresent(handler -> handler.setReceive(false));
            watt.ifPresent(handler -> handler.setSend(false));
            wattHandler.setReceive(false);
            wattHandler.setSend(false);
            wattHandler.setWatt(0);
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Deactivated: " + getModuleKey());
            }
        }
    }

    @Override
    public String getModuleKey() {
        return "energy_storage";
    }

    @Override
    public BlockPos getModuleOffset() {
        return new BlockPos(0, 0, 0);
    }

    @Override
    public IMultiblockModule createModule(Level world, BlockPos pos) {
        return new EnergyModule();
    }


    public LazyOptional<IWattStorage> getWatt() {
        return watt;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityWatt.WATT){
            return watt.cast();
        }
        return LazyOptional.empty();
    }
}
