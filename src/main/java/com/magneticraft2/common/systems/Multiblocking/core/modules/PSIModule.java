package com.magneticraft2.common.systems.Multiblocking.core.modules;

import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import com.magneticraft2.common.systems.PRESSURE.IPressureStorage;
import com.magneticraft2.common.systems.PRESSURE.PressureStorage;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.common.utils.PressureStorages;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author JumpWatch on 14-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class PSIModule implements IMultiblockModule {
    public static final Logger LOGGER = LogManager.getLogger("PSIModule");
    private final PressureStorages pressureStorage;
    private final LazyOptional<IPressureStorage> pressure;

    public PSIModule() {
        this(100, 5);
    }
    public PSIModule(int capacityP, int maxtrasferP) {
        this.pressureStorage = new PressureStorages(capacityP, maxtrasferP){
            @Override
            public boolean canReceive() {
                return true;
            }

            @Override
            public boolean canSend() {
                return true;
            }
        };
        this.pressure = LazyOptional.of(() -> pressureStorage);
    }
    @Override
    public boolean isValid(Level world, BlockPos pos) {
        return true;
    }

    @Override
    public void onActivate(Level world, BlockPos pos) {
        if (world.isClientSide()) {
            pressure.ifPresent(handler -> handler.setSend(true));
            pressure.ifPresent(handler -> handler.setReceive(true));
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("PSI module activated");
            }
        }
    }

    @Override
    public void onDeactivate(Level world, BlockPos pos) {
        if (world.isClientSide()) {
            pressure.ifPresent(handler -> handler.setSend(false));
            pressure.ifPresent(handler -> handler.setReceive(false));
            pressureStorage.setReceive(false);
            pressureStorage.setSend(false);
            pressureStorage.setPressure(0);
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("PSI module deactivated");
            }
        }
    }

    @Override
    public String getModuleKey() {
        return "psi_module";
    }

    @Override
    public BlockPos getModuleOffset() {
        return new BlockPos(0, 0, 0);
    }

    @Override
    public IMultiblockModule createModule(Level world, BlockPos pos) {
        return new PSIModule();
    }
}
