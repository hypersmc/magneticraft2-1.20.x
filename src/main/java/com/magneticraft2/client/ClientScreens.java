package com.magneticraft2.client;

import com.magneticraft2.client.gui.screen.blueprintmaker.blueprintmaker_screen;
import com.magneticraft2.client.gui.screen.gearbox.CustomGearboxScreen;
import com.magneticraft2.client.gui.screen.primitivefurnace.primitivefurnace_screen;
import com.magneticraft2.client.gui.screen.projector.Projector_screen;
import com.magneticraft2.common.registry.registers.ContainerAndScreenRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ClientScreens {
    private ClientScreens() {}

    public static void register() {
        MenuScreens.register(ContainerAndScreenRegistry.Projector_container.get(), Projector_screen::new);
        MenuScreens.register(ContainerAndScreenRegistry.Blueprintmaker_container.get(), blueprintmaker_screen::new);
        MenuScreens.register(ContainerAndScreenRegistry.Primitivefurnace_container.get(), primitivefurnace_screen::new);
        MenuScreens.register(ContainerAndScreenRegistry.CUSTOM_GEARBOX_MENU.get(), CustomGearboxScreen::new);
    }
}
