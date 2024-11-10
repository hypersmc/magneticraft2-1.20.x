package com.magneticraft2.client.world;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
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
    public static final ResourceLocation POLLUTION_TEXTURE = new ResourceLocation(MOD_ID, "textures/pollution.png");
    private static final int MAX_POLLUTION_LEVEL = 100;
    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event){
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        ChunkPos chunkPos = new ChunkPos((int) player.getX(), (int) player.getZ());
        int pollutionLevel = ClientPollutionData.getPollutionLevel(chunkPos);

        // Check neighboring chunks
        ChunkPos[] neighbors = {
                new ChunkPos(chunkPos.x - 1, chunkPos.z),
                new ChunkPos(chunkPos.x + 1, chunkPos.z),
                new ChunkPos(chunkPos.x, chunkPos.z - 1),
                new ChunkPos(chunkPos.x, chunkPos.z + 1),
        };

        // Get pollution levels for neighboring chunks and calculate an average
        int totalPollution = pollutionLevel;
        int numChunks = 1;

        for (ChunkPos neighbor : neighbors) {
            int neighborPollution = ClientPollutionData.getPollutionLevel(neighbor);
            totalPollution += neighborPollution;
            numChunks++;
        }

        int averagePollutionLevel = totalPollution / numChunks;
        //        LOGGER.info("pollution: " + pollutionLevel);
        if (pollutionLevel > 0) {
            renderPollutionEffect(event.getPoseStack(), mc.level, pollutionLevel, player);
        }
    }
//    public static void adjustFogBasedOnPollution(FogD event, int pollutionLevel) {
//
//    }
    private static void renderPollutionEffect(PoseStack matrixStack, ClientLevel world, int pollutionLevel, Player player) {
        int normalizedLevel = Math.min(pollutionLevel, MAX_POLLUTION_LEVEL);
        if (normalizedLevel <= 0) return;
        // When I wrote this, only God and I understood what I was doing
        // Now, God only knows
//        double playerX = player.getX();
//        double playerY = player.getY() + player.getEyeHeight() - 1; // Adjusted height for better effect
//        double playerZ = player.getZ();
//
//        Tesselator tessellator = Tesselator.getInstance();
//        BufferBuilder buffer = tessellator.getBuilder();
//        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
//
//        RenderSystem.setShaderTexture(0, POLLUTION_TEXTURE);
//        RenderSystem.enableBlend();
//        RenderSystem.defaultBlendFunc();
//        RenderSystem.depthMask(false);
//
//        double distance = 5.0;  // Closer distance for more immersive effect
//        double size = 15.0;  // Smaller size for finer appearance
//
//        Vec3 viewVec = player.getViewVector(1.0F);
//        Vec3 rightVec = viewVec.cross(new Vec3(0, 1, 0)).normalize().scale(size / 2);
//        Vec3 upVec = rightVec.cross(viewVec).normalize().scale(size / 2);
//
//        Vec3 center = new Vec3(playerX, playerY, playerZ).add(viewVec.scale(distance));
//
//        Vec3 p1 = center.subtract(rightVec).subtract(upVec);
//        Vec3 p2 = center.add(rightVec).subtract(upVec);
//        Vec3 p3 = center.add(rightVec).add(upVec);
//        Vec3 p4 = center.subtract(rightVec).add(upVec);
//
//        float alpha = 0.2F + 0.3F * (normalizedLevel / (float) MAX_POLLUTION_LEVEL);  // Lighter effect
//
//        // Use smaller UV mapping for more detailed texture
//        buffer.vertex(p1.x, p1.y, p1.z).uv(0, 0).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
//        buffer.vertex(p2.x, p2.y, p2.z).uv(1, 0).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
//        buffer.vertex(p3.x, p3.y, p3.z).uv(1, 1).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
//        buffer.vertex(p4.x, p4.y, p4.z).uv(0, 1).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
//
//        tessellator.end();
//
//        RenderSystem.disableBlend();
//        RenderSystem.depthMask(true);
    }

    @SubscribeEvent
    public static void onColorFOG(ViewportEvent.ComputeFogColor ev){
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player != null && mc.level != null) {
            ChunkPos chunkPos = new ChunkPos((int) player.getX(), (int) player.getZ());
            int pollutionLevel = ClientPollutionData.getPollutionLevel(chunkPos);

            if (pollutionLevel > 0) {

                // Set fog color if desired (grayish or yellowish tint for pollution)
                float r = 0.7F + (pollutionLevel / 100.0F) * 0.3F;
                float g = 0.7F - (pollutionLevel / 100.0F) * 0.5F;
                float b = 0.7F - (pollutionLevel / 100.0F) * 0.5F;
                ev.setRed(r);
                ev.setGreen(g);
                ev.setBlue(b);
                // Apply the color tint to the fog
//                event.setFogColor(new Vec3(r, g, b));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            // Get player's current chunk and pollution level
            ChunkPos chunkPos = new ChunkPos((int) player.getX(), (int) player.getZ());
            int pollutionLevel = ClientPollutionData.getPollutionLevel(chunkPos);

            if (pollutionLevel > 0) {
                // Calculate the fog density based on pollution level
                float fogDensity = 0.02F + (pollutionLevel / 100.0F) * 0.2F; // Adjust this value for desired effect

                // Set fog properties
                RenderSystem.setShaderFogStart(0.0F);  // Start distance of the fog (closer to player)
                RenderSystem.setShaderFogEnd(150.0F - (pollutionLevel * 1.5F)); // Far distance of the fog, reduced with higher pollution

                // Apply fog color (a grayish tint to simulate pollution)
                float fogRed = 0.6F;
                float fogGreen = 0.6F;
                float fogBlue = 0.6F;

                // Set the fog color
                event.setCanceled(true);  // Cancel default fog handling to apply our custom fog
            }
        }
    }

}
