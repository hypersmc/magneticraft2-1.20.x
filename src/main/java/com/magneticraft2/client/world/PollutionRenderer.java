package com.magneticraft2.client.world;

import com.magneticraft2.common.world.pollution.PollutionData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 24-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MOD_ID)
public class PollutionRenderer {
    public static final Logger LOGGER = LogManager.getLogger("MGC2PollutionRenderer");
    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event){
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        ChunkPos chunkPos = new ChunkPos((int) player.getX(), (int) player.getY());
        int pollutionLevel = ClientPollutionData.getPollutionLevel(chunkPos);
        LOGGER.info("pollution: " + pollutionLevel);
//        renderPollutionEffect(event.getPoseStack(), pollutionLevel);

    }
    private static void renderPollutionEffect(PoseStack matrixStack, int pollutionLevel) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getScreenWidth();
        int screenHeight = mc.getWindow().getScreenHeight();

        // Calculate color and alpha based on pollution level
        int maxPollutionLevel = 100; // Define a maximum pollution level for scaling
        float normalizedPollution = Math.min(pollutionLevel, maxPollutionLevel) / (float) maxPollutionLevel;

        int alpha = (int) (normalizedPollution * 255); // Scale alpha with pollution level
        int color = 0x00FF00 + (alpha << 24); // Green color with variable alpha

        // Render a quad covering the entire screen
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrixStack.last().pose(), 0, screenHeight, 0).color(0, 255, 0, alpha).endVertex();
        bufferBuilder.vertex(matrixStack.last().pose(), screenWidth, screenHeight, 0).color(0, 255, 0, alpha).endVertex();
        bufferBuilder.vertex(matrixStack.last().pose(), screenWidth, 0, 0).color(0, 255, 0, alpha).endVertex();
        bufferBuilder.vertex(matrixStack.last().pose(), 0, 0, 0).color(0, 255, 0, alpha).endVertex();
        bufferBuilder.end();

    }

}
