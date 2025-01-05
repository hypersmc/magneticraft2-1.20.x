package com.magneticraft2.client;

import com.magneticraft2.client.model.MultiBlockModelLoader;
import com.magneticraft2.client.render.blocks.*;
import com.magneticraft2.client.render.blocks.stage.copper.LargeGearWithHandleBlock_woodRenderer;
import com.magneticraft2.client.render.blocks.stage.stone.PitKilnBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.stone.PrimitiveFurnaceBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.stage.stone.PrimitiveFurnaceNoGUIBlockEntityRenderer;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 30-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Clientsetup {
    private static final Logger LOGGER = LogManager.getLogger("magneticraft2_clientsetup");
    public static void init(FMLClientSetupEvent e){

    }
    @SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
        LOGGER.info("Renders are being registered!");
        event.registerBlockEntityRenderer(BlockEntityRegistry.PitKilnblockEntity.get(), PitKilnBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.projectortestBlockEntity.get(), ProjectorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.blueprintmultiblockentity.get(), BlueprintMultiblockRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.primitivefurnacemultiblockentity.get(), PrimitiveFurnaceBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.primitivefurnacemultiblockentity_nogui.get(), PrimitiveFurnaceNoGUIBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.GEAR_LARGE_WITH_HANDLE_BE_WOOD.get(), LargeGearWithHandleBlock_woodRenderer::new);
    }
    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        LOGGER.info("Models are being registered!");

    }
    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        LOGGER.info("Models are being registered!");

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        String folderPath = "models/multiblock";

        // Get all resources in the multiblock folder
        for (ResourceLocation resourceLocation : resourceManager.listResources(folderPath, path -> path.toString().endsWith(".json")).keySet()) {
            // Remove the "models/" prefix and ".json" suffix for registering the model
            String modelPath = resourceLocation.getPath().substring("models/".length(), resourceLocation.getPath().length() - ".json".length());
            ResourceLocation modelResourceLocation = new ResourceLocation(resourceLocation.getNamespace(), modelPath);

            // Register the model
            event.register(modelResourceLocation);
            LOGGER.info("Registered model: " + modelResourceLocation);
        }
    }

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        LOGGER.info("Registering custom geometry loaders!");
        event.register("multiblock", new MultiBlockModelLoader());
    }
}
