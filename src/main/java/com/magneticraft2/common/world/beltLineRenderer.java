package com.magneticraft2.common.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
                BlockPos blockPos = BlockPos.of(directconnect); //This is the blocks position
                BlockPos playerpos = player.getOnPos();

                Vec3 blockVec = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                Vec3 playerVec = new Vec3(player.getX(), player.getEyeY(), player.getZ());
                // Render the line
                renderLine(blockVec, playerVec);
            }
        }
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
