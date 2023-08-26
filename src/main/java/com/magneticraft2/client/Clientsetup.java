package com.magneticraft2.client;

import com.magneticraft2.client.render.blocks.BlueprintMakerBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.PitKilnBlockEntityRenderer;
import com.magneticraft2.client.render.blocks.ProjectorBlockEntityRenderer;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author JumpWatch on 30-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod.EventBusSubscriber(modid = magneticraft2.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Clientsetup {
    private static final Logger LOGGER = LogManager.getLogger("magneticraft2_clientsetup");
    public static void init(FMLClientSetupEvent e){

    }
    @SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
        LOGGER.info("Renders are being registered!");
        event.registerBlockEntityRenderer(BlockEntityRegistry.PitKilnblockEntity.get(), PitKilnBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.projectortestBlockEntity.get(), ProjectorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.blueprintmakerBlockEntity.get(), BlueprintMakerBlockEntityRenderer::new);
    }
    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        LOGGER.info("Models are being registered!");

    }
    @SubscribeEvent
    public static void TextureStrach(TextureStitchEvent event) {
//        if (event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS))
//            event.addSprite(new ResourceLocation("forge:white"));
//        if (event.getAtlas().location().equals(BLOCK_ATLAS))
//        {
//            event.addSprite(TEXTURE);
//        }

    }
}
