package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.magneticraft2;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author JumpWatch on 01-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeTabRegistry {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-CreatuveTabRegistry");
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, magneticraft2.MOD_ID);

    public static void init(IEventBus eventBus) {
        TABS.register(eventBus);
    }
    //Tabs after this line
    public static final RegistryObject<CreativeModeTab> MC2Blocks = TABS.register("magneticraft2_blocks", () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 3)
                    .title(Component.translatable("itemGroup.Magneticraft2.Blocks")).build());
}
