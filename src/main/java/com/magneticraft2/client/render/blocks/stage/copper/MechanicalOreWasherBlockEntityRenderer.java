package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.MechanicalOreWasherBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class MechanicalOreWasherBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalOreWasherBlockEntity> {

    private static final ResourceLocation WOOD_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/oak_planks.png");
    private static final ResourceLocation DARK_WOOD_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/stripped_spruce_log.png");
    private static final ResourceLocation COPPER_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/copper_block.png");
    private static final ResourceLocation SHAFT_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/stripped_oak_log.png");
    private static final ResourceLocation WATER_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/light_blue_stained_glass.png");

    public MechanicalOreWasherBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            MechanicalOreWasherBlockEntity washer,
            float partialTicks,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);
        orientLocalXToAxis(stack, washer.getGearAxis());

        VertexConsumer wood =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(WOOD_TEXTURE)
                );
        VertexConsumer darkWood =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(DARK_WOOD_TEXTURE)
                );
        VertexConsumer copper =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(COPPER_TEXTURE)
                );
        VertexConsumer shaft =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(SHAFT_TEXTURE)
                );

        // Heavy timber base and four supports.
        stack.pushPose();
        stack.translate(0.0D, -0.43D, 0.0D);
        drawBox(stack, darkWood, packedLight, 0.94D, 0.12D, 0.88D);
        stack.popPose();

        for (double x : new double[]{-0.34D, 0.34D}) {
            for (double z : new double[]{-0.34D, 0.34D}) {
                stack.pushPose();
                stack.translate(x, -0.15D, z);
                drawBox(stack, darkWood, packedLight, 0.10D, 0.56D, 0.10D);
                stack.popPose();
            }
        }

        // Copper-lined water trough below the rotating drum.
        stack.pushPose();
        stack.translate(0.0D, -0.27D, 0.0D);
        drawBox(stack, copper, packedLight, 0.78D, 0.08D, 0.66D);
        stack.popPose();

        if (washer.hasWaterSupply()) {
            VertexConsumer water =
                    bufferSource.getBuffer(
                            RenderType.entityTranslucent(WATER_TEXTURE)
                    );
            stack.pushPose();
            stack.translate(0.0D, -0.215D, 0.0D);
            drawBox(stack, water, packedLight, 0.70D, 0.025D, 0.56D);
            stack.popPose();
        }

        float angle = washer.getVisualRotationDegrees(partialTicks);

        stack.pushPose();
        stack.mulPose(Axis.XP.rotationDegrees(angle));

        drawBox(stack, shaft, packedLight, 1.05D, 0.14D, 0.14D);

        // Twelve long slats form an open trommel rather than a solid cylinder.
        final int segments = 12;
        final double radius = 0.285D;
        for (int i = 0; i < segments; i++) {
            double radians = i * Math.PI * 2.0D / segments;
            double y = Math.cos(radians) * radius;
            double z = Math.sin(radians) * radius;

            stack.pushPose();
            stack.translate(0.0D, y, z);
            stack.mulPose(
                    Axis.XP.rotationDegrees(
                            (float) Math.toDegrees(radians)
                    )
            );
            drawBox(stack, wood, packedLight, 0.72D, 0.055D, 0.11D);
            stack.popPose();
        }

        // Three copper hoops make the drum feel like a real early machine.
        for (double x : new double[]{-0.31D, 0.0D, 0.31D}) {
            for (int i = 0; i < segments; i++) {
                double radians = i * Math.PI * 2.0D / segments;
                double y = Math.cos(radians) * radius;
                double z = Math.sin(radians) * radius;

                stack.pushPose();
                stack.translate(x, y, z);
                stack.mulPose(
                        Axis.XP.rotationDegrees(
                                (float) Math.toDegrees(radians)
                        )
                );
                drawBox(stack, copper, packedLight, 0.045D, 0.06D, 0.14D);
                stack.popPose();
            }
        }

        stack.popPose();

        // Feed and discharge chutes.
        stack.pushPose();
        stack.translate(-0.39D, 0.20D, 0.0D);
        stack.mulPose(Axis.ZP.rotationDegrees(-18.0F));
        drawBox(stack, darkWood, packedLight, 0.26D, 0.10D, 0.48D);
        stack.popPose();

        stack.pushPose();
        stack.translate(0.39D, -0.02D, 0.0D);
        stack.mulPose(Axis.ZP.rotationDegrees(18.0F));
        drawBox(stack, darkWood, packedLight, 0.26D, 0.10D, 0.48D);
        stack.popPose();

        renderItem(
                washer.getInputStack(),
                washer,
                stack,
                bufferSource,
                packedLight,
                -0.38D,
                0.32D,
                0.0D,
                0.30F
        );
        renderItem(
                washer.getOutputStack(),
                washer,
                stack,
                bufferSource,
                packedLight,
                0.36D,
                -0.12D,
                -0.18D,
                0.28F
        );
        renderItem(
                washer.getByproductStack(),
                washer,
                stack,
                bufferSource,
                packedLight,
                0.36D,
                -0.12D,
                0.18D,
                0.25F
        );

        stack.popPose();
    }

    private void orientLocalXToAxis(PoseStack stack, Direction.Axis axis) {
        switch (axis) {
            case X -> {
            }
            case Y -> stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case Z -> stack.mulPose(Axis.YN.rotationDegrees(90.0F));
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

        ItemRenderer itemRenderer =
                Minecraft.getInstance().getItemRenderer();
        BakedModel model =
                itemRenderer.getModel(
                        item,
                        owner.getLevel(),
                        null,
                        0
                );

        stack.pushPose();
        stack.translate(x, y, z);
        stack.scale(scale, scale, scale);

        itemRenderer.render(
                item,
                ItemDisplayContext.GROUND,
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
            int packedLight,
            double sizeX,
            double sizeY,
            double sizeZ) {
        double x0 = -sizeX * 0.5D;
        double x1 = sizeX * 0.5D;
        double y0 = -sizeY * 0.5D;
        double y1 = sizeY * 0.5D;
        double z0 = -sizeZ * 0.5D;
        double z1 = sizeZ * 0.5D;
        PoseStack.Pose pose = stack.last();

        quad(consumer, pose, packedLight, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, 0,1,0);
        quad(consumer, pose, packedLight, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, 0,-1,0);
        quad(consumer, pose, packedLight, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, 0,0,1);
        quad(consumer, pose, packedLight, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, 0,0,-1);
        quad(consumer, pose, packedLight, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, 1,0,0);
        quad(consumer, pose, packedLight, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1,0,0);
    }

    private void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int light,
            double x0,double y0,double z0,
            double x1,double y1,double z1,
            double x2,double y2,double z2,
            double x3,double y3,double z3,
            float nx,float ny,float nz) {
        vertex(consumer,pose,light,x0,y0,z0,0,0,nx,ny,nz);
        vertex(consumer,pose,light,x1,y1,z1,0,1,nx,ny,nz);
        vertex(consumer,pose,light,x2,y2,z2,1,1,nx,ny,nz);
        vertex(consumer,pose,light,x3,y3,z3,1,0,nx,ny,nz);
    }

    private void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int light,
            double x,double y,double z,
            float u,float v,
            float nx,float ny,float nz) {
        consumer.vertex(pose.pose(), (float)x, (float)y, (float)z)
                .color(1,1,1,1)
                .uv(u,v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(),nx,ny,nz)
                .endVertex();
    }
}
