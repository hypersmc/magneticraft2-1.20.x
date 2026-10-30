package com.magneticraft2.client;

import com.magneticraft2.common.systems.Blueprint.core.BlueprintManager;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/** Client-only Forge setup and registration events. */
@Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Clientsetup {
    private Clientsetup() {}

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        MultiblockManager.loadMultiblocks(MOD_ID, resources);
        BlueprintManager.loadBlueprints(MOD_ID, resources);
        event.enqueueWork(ClientScreens::register);
    }

    @SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
        ClientRenderers.register(event);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        ClientModels.registerAdditional(event);
    }

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        ClientModels.registerGeometryLoaders(event);
    }
}
