package com.magneticraft2.client;

import com.magneticraft2.client.model.MultiBlockModelLoader;
import com.magneticraft2.client.render.blocks.*;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.resources.ResourceLocation;
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
    }
    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        LOGGER.info("Models are being registered!");

    }
    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        LOGGER.info("Models are being registered!");
        event.register(new ResourceLocation(MOD_ID, "multiblock/primitive_furnace_formed"));
        event.register(new ResourceLocation(MOD_ID, "multiblock/blueprint_maker_multiblock_west"));
        event.register(new ResourceLocation(MOD_ID, "multiblock/blueprint_maker_multiblock_south"));
        event.register(new ResourceLocation(MOD_ID, "multiblock/blueprint_maker_multiblock_north"));
        event.register(new ResourceLocation(MOD_ID, "multiblock/blueprint_maker_multiblock_east"));
    }

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        LOGGER.info("Registering custom geometry loaders!");
        event.register("multiblock", new MultiBlockModelLoader());
    }
}
