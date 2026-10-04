package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.MechanicalSifterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Formed 3x2x2 classifier/sifter renderer.
 */
public class MechanicalSifterBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalSifterBlockEntity> {

    private static final ResourceLocation WOOD =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/oak_planks.png"
            );
    private static final ResourceLocation FRAME =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/stripped_spruce_log.png"
            );
    private static final ResourceLocation COPPER =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/copper_block.png"
            );
    private static final ResourceLocation MESH =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/light_gray_wool.png"
            );

    public MechanicalSifterBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            MechanicalSifterBlockEntity sifter,
            float partialTicks,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        if (!sifter.isFormed()) {
            return;
        }

        stack.pushPose();
        stack.translate(0.5D, 0.0D, 0.5D);
        orientLocalZToDirection(
                stack,
                sifter.getFacing()
        );

        double shake =
                sifter.getShakeOffset(
                        partialTicks
                );

        renderFrame(
                stack,
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(FRAME)
                ),
                packedLight
        );

        renderWood(
                stack,
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(WOOD)
                ),
                packedLight,
                shake
        );

        renderMesh(
                stack,
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(MESH)
                ),
                packedLight,
                shake
        );

        renderCopper(
                stack,
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(COPPER)
                ),
                packedLight,
                shake
        );

        renderItem(
                sifter.getInputStack(),
                sifter,
                stack,
                bufferSource,
                packedLight,
                shake,
                1.55D,
                0.36D,
                0.35F
        );
        renderItem(
                sifter.getOutputStack(),
                sifter,
                stack,
                bufferSource,
                packedLight,
                -0.48D,
                0.27D,
                1.68D,
                0.30F
        );
        renderItem(
                sifter.getByproductStack(),
                sifter,
                stack,
                bufferSource,
                packedLight,
                0.48D,
                0.27D,
                1.68D,
                0.28F
        );

        stack.popPose();
    }

    private void renderFrame(
            PoseStack stack,
            VertexConsumer consumer,
            int light) {
        for (double x : new double[]{-1.20D, 1.20D}) {
            stack.pushPose();
            stack.translate(x, 0.11D, 0.82D);
            drawBox(
                    stack,
                    consumer,
                    light,
                    0.18D,
                    0.22D,
                    1.75D
            );
            stack.popPose();
        }

        for (double z : new double[]{0.08D, 1.58D}) {
            stack.pushPose();
            stack.translate(0.0D, 0.15D, z);
            drawBox(
                    stack,
                    consumer,
                    light,
                    2.58D,
                    0.18D,
                    0.18D
            );
            stack.popPose();
        }

        for (double x : new double[]{-1.08D, 1.08D}) {
            for (double z : new double[]{0.25D, 1.43D}) {
                stack.pushPose();
                stack.translate(x, 0.95D, z);
                drawBox(
                        stack,
                        consumer,
                        light,
                        0.15D,
                        1.55D,
                        0.15D
                );
                stack.popPose();
            }
        }

        stack.pushPose();
        stack.translate(0.0D, 1.68D, 0.82D);
        drawBox(
                stack,
                consumer,
                light,
                2.38D,
                0.15D,
                1.48D
        );
        stack.popPose();
    }

    private void renderWood(
            PoseStack stack,
            VertexConsumer consumer,
            int light,
            double shake) {
        renderTray(
                stack,
                consumer,
                light,
                shake,
                1.13D
        );
        renderTray(
                stack,
                consumer,
                light,
                -shake * 0.70D,
                0.73D
        );

        // Feed chute.
        stack.pushPose();
        stack.translate(0.0D, 1.48D, 0.28D);
        stack.mulPose(
                Axis.XP.rotationDegrees(-18.0F)
        );
        drawBox(
                stack,
                consumer,
                light,
                1.45D,
                0.14D,
                0.58D
        );
        stack.popPose();

        // Product/waste discharge chutes.
        for (double x : new double[]{-0.48D, 0.48D}) {
            stack.pushPose();
            stack.translate(x, 0.30D, 1.68D);
            stack.mulPose(
                    Axis.XP.rotationDegrees(20.0F)
            );
            drawBox(
                    stack,
                    consumer,
                    light,
                    0.78D,
                    0.12D,
                    0.60D
            );
            stack.popPose();
        }
    }

    private void renderTray(
            PoseStack stack,
            VertexConsumer consumer,
            int light,
            double shake,
            double y) {
        stack.pushPose();
        stack.translate(shake, y, 0.93D);

        // Open wooden tray frame instead of one opaque slab.
        for (double x : new double[]{-0.95D, 0.95D}) {
            stack.pushPose();
            stack.translate(x, 0.0D, 0.0D);
            drawBox(
                    stack,
                    consumer,
                    light,
                    0.12D,
                    0.12D,
                    1.25D
            );
            stack.popPose();
        }

        for (double z : new double[]{-0.57D, 0.57D}) {
            stack.pushPose();
            stack.translate(0.0D, 0.0D, z);
            drawBox(
                    stack,
                    consumer,
                    light,
                    2.02D,
                    0.12D,
                    0.12D
            );
            stack.popPose();
        }

        stack.popPose();
    }

    private void renderMesh(
            PoseStack stack,
            VertexConsumer consumer,
            int light,
            double shake) {
        for (int tray = 0; tray < 2; tray++) {
            double y =
                    tray == 0
                            ? 1.13D
                            : 0.73D;
            double offset =
                    tray == 0
                            ? shake
                            : -shake * 0.70D;

            // Cross-hatched mesh lines; no full opaque wool plate.
            for (int i = -4; i <= 4; i++) {
                double x = i * 0.20D;
                stack.pushPose();
                stack.translate(
                        offset + x,
                        y + 0.025D,
                        0.93D
                );
                drawBox(
                        stack,
                        consumer,
                        light,
                        0.025D,
                        0.025D,
                        1.08D
                );
                stack.popPose();
            }

            for (int i = -2; i <= 2; i++) {
                double z = i * 0.22D;
                stack.pushPose();
                stack.translate(
                        offset,
                        y + 0.028D,
                        0.93D + z
                );
                drawBox(
                        stack,
                        consumer,
                        light,
                        1.80D,
                        0.025D,
                        0.025D
                );
                stack.popPose();
            }
        }
    }

    private void renderCopper(
            PoseStack stack,
            VertexConsumer consumer,
            int light,
            double shake) {
        // Rear reciprocating drive rod from the crank.
        stack.pushPose();
        stack.translate(
                shake * 0.5D,
                1.12D,
                -0.30D
        );
        drawBox(
                stack,
                consumer,
                light,
                0.12D,
                0.12D,
                0.78D
        );
        stack.popPose();

        // Crosshead.
        stack.pushPose();
        stack.translate(
                shake,
                1.12D,
                0.16D
        );
        drawBox(
                stack,
                consumer,
                light,
                0.42D,
                0.24D,
                0.18D
        );
        stack.popPose();

        // Copper wear strips on both moving trays.
        for (int tray = 0; tray < 2; tray++) {
            double y =
                    tray == 0
                            ? 1.18D
                            : 0.78D;
            double offset =
                    tray == 0
                            ? shake
                            : -shake * 0.70D;

            for (double x : new double[]{-0.98D, 0.98D}) {
                stack.pushPose();
                stack.translate(
                        offset + x,
                        y,
                        0.93D
                );
                drawBox(
                        stack,
                        consumer,
                        light,
                        0.055D,
                        0.12D,
                        1.28D
                );
                stack.popPose();
            }
        }
    }

    private void orientLocalZToDirection(
            PoseStack stack,
            Direction direction) {
        switch (direction) {
            case SOUTH -> {
            }
            case NORTH -> stack.mulPose(
                    Axis.YP.rotationDegrees(180.0F)
            );
            case EAST -> stack.mulPose(
                    Axis.YP.rotationDegrees(90.0F)
            );
            case WEST -> stack.mulPose(
                    Axis.YN.rotationDegrees(90.0F)
            );
            default -> {
            }
        }
    }

    private void renderItem(
            ItemStack item,
            net.minecraft.world.level.block.entity.BlockEntity owner,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            double x,
            double y,
            double z,
            float scale) {
        if (item == null || item.isEmpty()) {
            return;
        }

        ItemRenderer renderer =
                Minecraft.getInstance()
                        .getItemRenderer();
        BakedModel model =
                renderer.getModel(
                        item,
                        owner.getLevel(),
                        null,
                        0
                );

        stack.pushPose();
        stack.translate(x, y, z);
        stack.mulPose(
                Axis.XP.rotationDegrees(90.0F)
        );
        stack.scale(scale, scale, scale);

        renderer.render(
                item,
                ItemDisplayContext.NONE,
                false,
                stack,
                bufferSource,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                model
        );

        stack.popPose();
    }

    private void drawBox(
            PoseStack stack,
            VertexConsumer consumer,
            int light,
            double sx,
            double sy,
            double sz) {
        double x0=-sx*0.5D, x1=sx*0.5D;
        double y0=-sy*0.5D, y1=sy*0.5D;
        double z0=-sz*0.5D, z1=sz*0.5D;
        PoseStack.Pose pose=stack.last();

        quad(consumer,pose,light,x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0,0,1,0);
        quad(consumer,pose,light,x0,y0,z1,x0,y0,z0,x1,y0,z0,x1,y0,z1,0,-1,0);
        quad(consumer,pose,light,x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,0,0,1);
        quad(consumer,pose,light,x1,y0,z0,x0,y0,z0,x0,y1,z0,x1,y1,z0,0,0,-1);
        quad(consumer,pose,light,x1,y0,z1,x1,y0,z0,x1,y1,z0,x1,y1,z1,1,0,0);
        quad(consumer,pose,light,x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,-1,0,0);
    }

    private void quad(
            VertexConsumer c,
            PoseStack.Pose p,
            int light,
            double x0,double y0,double z0,
            double x1,double y1,double z1,
            double x2,double y2,double z2,
            double x3,double y3,double z3,
            float nx,float ny,float nz) {
        vertex(c,p,light,x0,y0,z0,0,0,nx,ny,nz);
        vertex(c,p,light,x1,y1,z1,0,1,nx,ny,nz);
        vertex(c,p,light,x2,y2,z2,1,1,nx,ny,nz);
        vertex(c,p,light,x3,y3,z3,1,0,nx,ny,nz);
    }

    private void vertex(
            VertexConsumer c,
            PoseStack.Pose p,
            int light,
            double x,double y,double z,
            float u,float v,
            float nx,float ny,float nz) {
        c.vertex(
                        p.pose(),
                        (float)x,
                        (float)y,
                        (float)z
                )
                .color(1,1,1,1)
                .uv(u,v)
                .overlayCoords(
                        OverlayTexture.NO_OVERLAY
                )
                .uv2(light)
                .normal(
                        p.normal(),
                        nx,ny,nz
                )
                .endVertex();
    }
}
