package com.magneticraft2.common.registry.registers;

import com.magneticraft2.common.magneticraft2;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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
    public static final RegistryObject<CreativeModeTab> MGC2Blocks = TABS.register("magneticraft2_blocks", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.Magneticraft2.Blocks"))
                    .icon(() -> new ItemStack(ItemRegistry.item_pitkiln.get()))
                    .displayItems((enabledFeatures, entries) -> {
                        entries.accept(ItemRegistry.item_pitkiln.get());
                    })
                    .build());
    public static final RegistryObject<CreativeModeTab> MGC2Items = TABS.register("magneticraft2_items", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.Magneticraft2.Items"))
                    .icon(() -> new ItemStack(ItemRegistry.item_clay_pot.get()))
                    .displayItems((enabledFeatures, entries) -> {
                        entries.accept(ItemRegistry.item_ceramic_pot.get());
                        entries.accept(ItemRegistry.item_clay_pot.get());
                        entries.accept(ItemRegistry.item_fire_starter.get());
                        entries.accept(ItemRegistry.item_stone_knife.get());
                        entries.accept(ItemRegistry.item_pebble.get());
                        entries.accept(ItemRegistry.item_blueprintmarker.get());
                        entries.accept(ItemRegistry.item_blueprintmaker.get());
//                        entries.accept(ItemRegistry.);
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> MGC2Machines = TABS.register("magneticraft2_machines", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.Magneticraft2.Machines"))
//                    .icon()
                    .displayItems((enabledFeatures, entries) -> {

                    })
                    .build());
    public static final RegistryObject<CreativeModeTab> MGC2Plants = TABS.register("magneticraft2_plants", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.Magneticraft2.Plants"))
                    .icon(() -> new ItemStack(ItemRegistry.rice.get()))
                    .displayItems((enabledFeatures, entries) -> {
                        entries.accept(ItemRegistry.rice_seed.get());
                        entries.accept(ItemRegistry.rice.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> MGC2Ores = TABS.register("magneticraft2_ores", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.Magneticraft2.Ores"))
                    .icon(() -> new ItemStack(ItemRegistry.MAGNETITE_ITEM.get()))
                    .displayItems((enabledFeatures, entries) -> {
                        entries.accept(ItemRegistry.CHROMITE_DEEPSLATE_ITEM.get());
                        entries.accept(ItemRegistry.CINNABAR_DEEPSLATE_ITEM.get());
                        entries.accept(ItemRegistry.GALENA_DEEPSLATE_ITEM.get());
                        entries.accept(ItemRegistry.GARNIERITE_DEEPSLATE_ITEM.get());
                        entries.accept(ItemRegistry.LIMONITE_DEEPSLATE_ITEM.get());
                        entries.accept(ItemRegistry.MAGNETITE_DEEPSLATE_ITEM.get());
                        entries.accept(ItemRegistry.MANGANESE_DEEPSLATE_ITEM.get());
                        entries.accept(ItemRegistry.QUARTZ_DEEPSLATE_ITEM.get());
                        entries.accept(ItemRegistry.SILICIUM_DEEPSLATE_ITEM.get());
                        entries.accept(ItemRegistry.SULFUR_DEEPSLATE_ITEM.get());
                        entries.accept(ItemRegistry.TANTALITE_DEEPSLATE_ITEM.get());
                        entries.accept(ItemRegistry.ANTHRACITE_ITEM.get());
                        entries.accept(ItemRegistry.APATITE_ITEM.get());
                        entries.accept(ItemRegistry.BAUXITE_ITEM.get());
                        entries.accept(ItemRegistry.CASSITERITE_ITEM.get());
                        entries.accept(ItemRegistry.CHALCOCITE_ITEM.get());
                        entries.accept(ItemRegistry.CHROMITE_ITEM.get());
                        entries.accept(ItemRegistry.CINNABAR_ITEM.get());
                        entries.accept(ItemRegistry.COBALTITE_ITEM.get());
                        entries.accept(ItemRegistry.CRYOLITE_ITEM.get());
                        entries.accept(ItemRegistry.GALENA_ITEM.get());
                        entries.accept(ItemRegistry.GARNIERITE_ITEM.get());
                        entries.accept(ItemRegistry.GRAPHITE_ITEM.get());
                        entries.accept(ItemRegistry.HEMATITE_ITEM.get());
                        entries.accept(ItemRegistry.KAOLINITE_ITEM.get());
                        entries.accept(ItemRegistry.KIMBERLITE_ITEM.get());
                        entries.accept(ItemRegistry.LIGNITE_ITEM.get());
                        entries.accept(ItemRegistry.LIMONITE_ITEM.get());
                        entries.accept(ItemRegistry.MAGNETITE_ITEM.get());
                        entries.accept(ItemRegistry.MANGANESE_ITEM.get());
                        entries.accept(ItemRegistry.OSMIRIDIUM_ITEM.get());
                        entries.accept(ItemRegistry.PLATINIUM_ITEM.get());
                        entries.accept(ItemRegistry.QUARTZ_ITEM.get());
                        entries.accept(ItemRegistry.SALTPETER_ITEM.get());
                        entries.accept(ItemRegistry.SILICIUM_ITEM.get());
                        entries.accept(ItemRegistry.SILVER_ITEM.get());
                        entries.accept(ItemRegistry.SPHALERITE_ITEM.get());
                        entries.accept(ItemRegistry.SULFUR_ITEM.get());
                        entries.accept(ItemRegistry.TANTALITE_ITEM.get());
                        entries.accept(ItemRegistry.TITANITE_ITEM.get());
                        entries.accept(ItemRegistry.URANINITE_ITEM.get());
                        entries.accept(ItemRegistry.WOLFRAMITE_ITEM.get());
                    })
                    .build());
}
