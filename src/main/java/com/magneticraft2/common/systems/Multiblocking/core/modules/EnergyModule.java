package com.magneticraft2.common.systems.Multiblocking.core.modules;


import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import com.magneticraft2.common.systems.WATT.IWattStorage;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.common.utils.WattStorages;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author JumpWatch on 14-05-2023
 * @Project magneticraft2-1.18.2
 * v1.0.0
 */
public class EnergyModule implements IMultiblockModule {
    public static final Logger LOGGER = LogManager.getLogger("EnergyModule");

    private final WattStorages wattHandler;
    private final LazyOptional<IWattStorage> watt;

    public EnergyModule() {
        this(1000000, 2048);
    }

    public EnergyModule(int capacityW, int maxtransferW) {
        this.wattHandler = new WattStorages(capacityW, maxtransferW) {

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

    @Override
    public void onActivate(Level world, BlockPos pos) {
        if (!world.isClientSide){
            watt.ifPresent(handler -> handler.setReceive(true));
            watt.ifPresent(handler -> handler.setSend(true));
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Energy module activated!");
            }
        }
    }

    @Override
    public void onDeactivate(Level world, BlockPos pos) {
        if (!world.isClientSide){
            watt.ifPresent(handler -> handler.setReceive(false));
            watt.ifPresent(handler -> handler.setSend(false));
            wattHandler.setReceive(false);
            wattHandler.setSend(false);
            wattHandler.setWatt(0);
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Energy module deactivated!");
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
}
