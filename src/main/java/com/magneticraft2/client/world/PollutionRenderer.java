package com.magneticraft2.client.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
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
    private static final ResourceLocation POLLUTION_TEXTURE = new ResourceLocation(MOD_ID, "textures/pollution.png");
    private static final int MAX_POLLUTION_LEVEL = 100;
    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event){
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        ChunkPos chunkPos = new ChunkPos((int) player.getX(), (int) player.getZ());
        int pollutionLevel = ClientPollutionData.getPollutionLevel(chunkPos);
//        LOGGER.info("pollution: " + pollutionLevel);
        renderPollutionEffect(event.getPoseStack(), mc.level, pollutionLevel, mc.gameRenderer.getMainCamera().getEntity(), chunkPos);

    }
    private static void renderPollutionEffect(PoseStack matrixStack, ClientLevel world, int pollutionLevel, Entity cameraEntity, ChunkPos chunkPos) {
        int normalizedLevel = Math.min(pollutionLevel, MAX_POLLUTION_LEVEL);
        if (normalizedLevel <= 0) return;

        matrixStack.pushPose();
        matrixStack.translate(-chunkPos.x, -cameraEntity.getY(), -chunkPos.z);

        Minecraft.getInstance().getTextureManager().bindForSetup(POLLUTION_TEXTURE);
        RenderSystem.setShaderTexture(0, POLLUTION_TEXTURE);

        // Adjust these coordinates to position the pollution effect in the sky
        double skyTop = 255; // Adjust this to the sky height
        double skySize = 512; // Adjust this to cover the sky
        double minX = -skySize / 2;
        double maxX = skySize / 2;
        double minZ = -skySize / 2;
        double maxZ = skySize / 2;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        buffer.vertex(minX, skyTop, minZ).uv(0, 0).endVertex();
        buffer.vertex(maxX, skyTop, minZ).uv(1, 0).endVertex();
        buffer.vertex(maxX, skyTop, maxZ).uv(1, 1).endVertex();
        buffer.vertex(minX, skyTop, maxZ).uv(0, 1).endVertex();

        tessellator.end();

        matrixStack.popPose();
    }

}
