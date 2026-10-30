package com.magneticraft2.common.registry.registers;

import com.magneticraft2.client.gui.container.blueprintmaker.blueprintmaker_container;
import com.magneticraft2.client.gui.container.gearbox.CustomGearboxMenu;
import com.magneticraft2.client.gui.container.primitivefurnace.primitivefurnace_container;
import com.magneticraft2.client.gui.container.projector.Projector_container;
import com.magneticraft2.common.magneticraft2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
* @version 1.0.0
 */
public class ContainerAndScreenRegistry {
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, magneticraft2.MOD_ID);

    public static void init(IEventBus eventBus) {
        CONTAINERS.register(eventBus);
    }

    public static final RegistryObject<MenuType<Projector_container>> Projector_container = CONTAINERS.register("projector", () -> IForgeMenuType.create((((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        Level world = inv.player.getCommandSenderWorld();
        return new Projector_container(windowId, world, pos, inv,inv.player);
    }))));
    public static final RegistryObject<MenuType<blueprintmaker_container>> Blueprintmaker_container = CONTAINERS.register("blueprint_multiblock", () -> IForgeMenuType.create(((((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        Level world = inv.player.getCommandSenderWorld();
        return new blueprintmaker_container(windowId, world, pos, inv,inv.player);
    })))));
    public static final RegistryObject<MenuType<primitivefurnace_container>> Primitivefurnace_container = CONTAINERS.register("primitivefurnace_multiblock", () -> IForgeMenuType.create(((((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        Level world = inv.player.getCommandSenderWorld();
        return new primitivefurnace_container(windowId, world, pos, inv,inv.player);
    })))));

    public static final RegistryObject<MenuType<CustomGearboxMenu>> CUSTOM_GEARBOX_MENU =
            CONTAINERS.register("custom_gearbox_wood", () ->
                    IForgeMenuType.create((windowId, inv, data) -> {
                        BlockPos pos = data.readBlockPos();
                        Level world = inv.player.getCommandSenderWorld();
                        return new CustomGearboxMenu(
                                windowId,
                                world,
                                pos,
                                inv,
                                inv.player
                        );
                    }));
}
