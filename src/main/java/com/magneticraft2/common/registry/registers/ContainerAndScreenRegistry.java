package com.magneticraft2.common.registry.registers;

import com.magneticraft2.client.gui.container.projector.Projector_container;
import com.magneticraft2.client.gui.screen.projector.Projector_screen;
import com.magneticraft2.common.magneticraft2;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ContainerAndScreenRegistry {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-Container&ScreenRegistry");
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, magneticraft2.MOD_ID);

    public static void init(IEventBus eventBus) {
        CONTAINERS.register(eventBus);
    }

    public static final RegistryObject<MenuType<Projector_container>> Projector_container = CONTAINERS.register("projector", () -> IForgeMenuType.create((((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        Level world = inv.player.getCommandSenderWorld();
        return new Projector_container(windowId, world, pos, inv,inv.player);
    }))));


    public static void Screen(final FMLClientSetupEvent event) {
        MenuScreens.register(Projector_container.get(), Projector_screen::new);
    }
}