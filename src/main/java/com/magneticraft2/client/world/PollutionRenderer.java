package com.magneticraft2.client.world;

import com.mojang.blaze3d.platform.GlStateManager;
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
import org.lwjgl.opengl.GL11;

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
        renderPollutionEffect(event.getPoseStack(), mc.level, pollutionLevel, chunkPos);

    }
    private static void renderPollutionEffect(PoseStack matrixStack, ClientLevel world, int pollutionLevel, ChunkPos chunkPos) {
        int normalizedLevel = Math.min(pollutionLevel, MAX_POLLUTION_LEVEL);
        if (normalizedLevel <= 0) return;

        double posX = chunkPos.x; // Set to the desired X coordinate
        double posY = 200;  // Set to the desired Y coordinate (sky height)
        double posZ = chunkPos.z; // Set to the desired Z coordinate

        double skySize = 512; // Adjust this to cover the sky
        double minX = -skySize / 2;
        double maxX = skySize / 2;
        double minZ = -skySize / 2;
        double maxZ = skySize / 2;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        buffer.vertex(minX + posX, posY, minZ + posZ).uv(0, 0).color(1.0F, 1.0F, 0.1F, 1.0F).endVertex();
        buffer.vertex(maxX + posX, posY, minZ + posZ).uv(1,0).color(1.0F, 1.0F, 0.1F, 1.0F).endVertex();
        buffer.vertex(maxX + posX, posY, maxZ + posZ).uv(1,1).color(1.0F, 1.0F, 0.1F, 1.0F).endVertex();
        buffer.vertex(minX + posX, posY, maxZ + posZ).uv(0,1).color(1.0F, 1.0F, 0.1F, 1.0F).endVertex();

        Minecraft.getInstance().getTextureManager().bindForSetup(POLLUTION_TEXTURE);
        RenderSystem.setShaderTexture(0, POLLUTION_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.depthMask(false);

        RenderSystem.enableDepthTest();

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        tessellator.end();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_EQUAL);
    }

}
