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
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class MechanicalSifterBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalSifterBlockEntity> {

    private static final ResourceLocation WOOD_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/oak_planks.png");
    private static final ResourceLocation FRAME_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/stripped_spruce_log.png");
    private static final ResourceLocation COPPER_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/copper_block.png");
    private static final ResourceLocation MESH_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/light_gray_wool.png");

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

        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);
        orientLocalZToDirection(stack, sifter.getFacing());

        VertexConsumer wood =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(WOOD_TEXTURE)
                );
        VertexConsumer frame =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(FRAME_TEXTURE)
                );
        VertexConsumer copper =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(COPPER_TEXTURE)
                );
        VertexConsumer mesh =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(MESH_TEXTURE)
                );

        stack.pushPose();
        stack.translate(0.0D, -0.43D, 0.0D);
        drawBox(stack, frame, packedLight, 0.92D, 0.12D, 0.88D);
        stack.popPose();

        for (double x : new double[]{-0.39D, 0.39D}) {
            for (double z : new double[]{-0.34D, 0.34D}) {
                stack.pushPose();
                stack.translate(x, -0.06D, z);
                drawBox(stack, frame, packedLight, 0.09D, 0.74D, 0.09D);
                stack.popPose();
            }
        }

        double shake =
                sifter.getShakeOffset(partialTicks);

        // Two stacked screens: coarse upper screen, finer lower screen.
        for (int tray = 0; tray < 2; tray++) {
            double y = tray == 0 ? 0.18D : -0.06D;
            double trayShake = tray == 0 ? shake : -shake * 0.65D;

            stack.pushPose();
            stack.translate(trayShake, y, 0.0D);

            drawBox(stack, wood, packedLight, 0.82D, 0.055D, 0.72D);

            stack.pushPose();
            stack.translate(0.0D, 0.035D, 0.0D);
            drawBox(stack, mesh, packedLight, 0.69D, 0.018D, 0.59D);
            stack.popPose();

            for (double z : new double[]{-0.34D, 0.34D}) {
                stack.pushPose();
                stack.translate(0.0D, 0.065D, z);
                drawBox(stack, copper, packedLight, 0.84D, 0.055D, 0.045D);
                stack.popPose();
            }

            stack.popPose();
        }

        // Input hopper at the back.
        stack.pushPose();
        stack.translate(0.0D, 0.37D, -0.30D);
        stack.mulPose(Axis.XP.rotationDegrees(-20.0F));
        drawBox(stack, wood, packedLight, 0.55D, 0.10D, 0.34D);
        stack.popPose();

        // Product and gangue chutes split toward the front.
        for (double x : new double[]{-0.20D, 0.20D}) {
            stack.pushPose();
            stack.translate(x, -0.27D, 0.34D);
            stack.mulPose(Axis.XP.rotationDegrees(-22.0F));
            drawBox(stack, wood, packedLight, 0.27D, 0.08D, 0.34D);
            stack.popPose();
        }

        // Crank pushrod enters from behind and drives the upper tray.
        stack.pushPose();
        stack.translate(shake * 0.5D, 0.18D, -0.43D);
        drawBox(stack, copper, packedLight, 0.09D, 0.09D, 0.34D);
        stack.popPose();

        renderItem(
                sifter.getInputStack(),
                sifter,
                stack,
                bufferSource,
                packedLight,
                shake,
                0.28D,
                -0.10D,
                0.27F
        );
        renderItem(
                sifter.getOutputStack(),
                sifter,
                stack,
                bufferSource,
                packedLight,
                -0.20D,
                -0.18D,
                0.36D,
                0.25F
        );
        renderItem(
                sifter.getByproductStack(),
                sifter,
                stack,
                bufferSource,
                packedLight,
                0.20D,
                -0.18D,
                0.36D,
                0.23F
        );

        stack.popPose();
    }

    private void orientLocalZToDirection(
            PoseStack stack,
            Direction direction) {
        switch (direction) {
            case SOUTH -> {
            }
            case NORTH -> stack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> stack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> stack.mulPose(Axis.YN.rotationDegrees(90.0F));
            case UP -> stack.mulPose(Axis.XN.rotationDegrees(90.0F));
            case DOWN -> stack.mulPose(Axis.XP.rotationDegrees(90.0F));
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
        double x0=-sizeX*0.5D, x1=sizeX*0.5D;
        double y0=-sizeY*0.5D, y1=sizeY*0.5D;
        double z0=-sizeZ*0.5D, z1=sizeZ*0.5D;
        PoseStack.Pose pose=stack.last();

        quad(consumer,pose,packedLight,x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0,0,1,0);
        quad(consumer,pose,packedLight,x0,y0,z1,x0,y0,z0,x1,y0,z0,x1,y0,z1,0,-1,0);
        quad(consumer,pose,packedLight,x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,0,0,1);
        quad(consumer,pose,packedLight,x1,y0,z0,x0,y0,z0,x0,y1,z0,x1,y1,z0,0,0,-1);
        quad(consumer,pose,packedLight,x1,y0,z1,x1,y0,z0,x1,y1,z0,x1,y1,z1,1,0,0);
        quad(consumer,pose,packedLight,x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,-1,0,0);
    }

    private void quad(
            VertexConsumer c, PoseStack.Pose p, int light,
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
            VertexConsumer c, PoseStack.Pose p, int light,
            double x,double y,double z,
            float u,float v,
            float nx,float ny,float nz) {
        c.vertex(p.pose(),(float)x,(float)y,(float)z)
                .color(1,1,1,1)
                .uv(u,v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(p.normal(),nx,ny,nz)
                .endVertex();
    }
}
