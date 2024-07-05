package com.magneticraft2.common.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 11-01-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MOD_ID)
public class beltLineRenderer {
    private static final Logger LOGGER = LogManager.getLogger("MGC2BeltLineRenderer");
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        ItemStack stack = player.getMainHandItem();
        if (stack != null && stack.getTag() != null) {
            if (stack.getTag().contains("connectedbelt")) {
                long directconnect = stack.getTag().getLong("connectedbelt");
                BlockPos blockPos = BlockPos.of(directconnect); // This is the block's position
                BlockPos playerPos = player.blockPosition();

                // Calculate all positions between blockPos and playerPos
                Iterable<BlockPos> positions = BlockPos.betweenClosed(blockPos, playerPos);

                // Render stone blocks at each position
                renderBlocks(event, positions);
            }
        }
    }
    private static void renderBlocks(RenderLevelStageEvent event, Iterable<BlockPos> positions) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderDispatcher = mc.getBlockRenderer();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        for (BlockPos pos : positions) {
            poseStack.pushPose();

            // Calculate the translation relative to the camera position
            Vec3 playerPos = mc.player.position();

            double offsetX = cameraPos.x - playerPos.x;
            double offsetY = cameraPos.y - playerPos.y;
            double offsetZ = cameraPos.z - playerPos.z;

            // Translate the position in world coordinates
            poseStack.translate(offsetX, offsetY, offsetZ);

            // Render the block at the current position
            BlockState blockState = mc.level.getBlockState(pos);
            blockRenderDispatcher.renderSingleBlock(blockState, poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }

        bufferSource.endBatch();
    }





    private static void renderLine(Vec3 start, Vec3 end) {
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(2.0F);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(start.x, start.y, start.z).endVertex();
        bufferBuilder.vertex(end.x, end.y, end.z).endVertex();
        tessellator.end();

        RenderSystem.disableDepthTest();
    }
}
