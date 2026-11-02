package com.magneticraft2.common.systems.PRESSURE;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

/** Sided gas capability, distinct from water fluids and legacy bellows charges. */
public final class CapabilityPressurizedGas {
    public static final Capability<IPressurizedGasHandler> GAS =
            CapabilityManager.get(new CapabilityToken<>() {});
    private CapabilityPressurizedGas() {}
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IPressurizedGasHandler.class);
    }
}
