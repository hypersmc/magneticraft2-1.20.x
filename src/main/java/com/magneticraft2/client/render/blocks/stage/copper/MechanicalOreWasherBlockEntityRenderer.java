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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Formed 3x2x3 trommel renderer.
 *
 * Static construction blocks are replaced by multiblock filler blocks. The
 * controller renderer owns the complete formed machine and deliberately draws
 * one material at a time so MultiBufferSource cannot leak the last texture over
 * the entire machine (the cause of the previous black geometry).
 */
public class MechanicalOreWasherBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalOreWasherBlockEntity> {

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
    private static final ResourceLocation WATER =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/light_blue_stained_glass.png"
            );

    private static final int DRUM_SEGMENTS = 12;
    private static final double DRUM_RADIUS = 0.56D;

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

        if (!washer.isFormed()) {
            return;
        }

        Direction facing =
                washer.getBlockState()
                        .getValue(
                                com.magneticraft2.common.block.stage.copper
                                        .MechanicalOreWasherBlock.FACING
                        );

        stack.pushPose();
        stack.translate(0.5D, 0.0D, 0.5D);
        orientLocalZToDirection(stack, facing);

        renderTimberFrame(
                stack,
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(FRAME)
                ),
                packedLight
        );

        renderWoodenDrumAndChutes(
                washer,
                partialTicks,
                stack,
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(WOOD)
                ),
                packedLight
        );

        renderCopperHardware(
                washer,
                partialTicks,
                stack,
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(COPPER)
                ),
                packedLight
        );

        if (washer.hasWaterSupply()) {
            VertexConsumer water =
                    bufferSource.getBuffer(
                            RenderType.entityTranslucent(WATER)
                    );

            stack.pushPose();
            stack.translate(
                    0.0D,
                    0.39D,
                    1.28D
            );
            drawBox(
                    stack,
                    water,
                    packedLight,
                    2.15D,
                    0.035D,
                    1.68D
            );
            stack.popPose();
        }

        renderItem(
                washer.getInputStack(),
                washer,
                stack,
                bufferSource,
                packedLight,
                0.0D,
                1.67D,
                0.28D,
                0.38F
        );
        renderItem(
                washer.getOutputStack(),
                washer,
                stack,
                bufferSource,
                packedLight,
                -0.48D,
                0.34D,
                2.42D,
                0.32F
        );
        renderItem(
                washer.getByproductStack(),
                washer,
                stack,
                bufferSource,
                packedLight,
                0.48D,
                0.34D,
                2.42D,
                0.29F
        );

        stack.popPose();
    }

    private void renderTimberFrame(
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight) {
        // Long foundation skids.
        for (double x : new double[]{-1.18D, 1.18D}) {
            stack.pushPose();
            stack.translate(x, 0.11D, 1.25D);
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    0.18D,
                    0.22D,
                    2.55D
            );
            stack.popPose();
        }

        // Cross ties.
        for (double z : new double[]{0.12D, 1.25D, 2.38D}) {
            stack.pushPose();
            stack.translate(0.0D, 0.16D, z);
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    2.55D,
                    0.18D,
                    0.18D
            );
            stack.popPose();
        }

        // Four tall A-frame posts.
        for (double x : new double[]{-1.05D, 1.05D}) {
            for (double z : new double[]{0.47D, 2.02D}) {
                stack.pushPose();
                stack.translate(x, 0.96D, z);
                drawBox(
                        stack,
                        consumer,
                        packedLight,
                        0.16D,
                        1.55D,
                        0.16D
                );
                stack.popPose();
            }
        }

        // Upper braces over the drum.
        for (double z : new double[]{0.48D, 2.02D}) {
            stack.pushPose();
            stack.translate(0.0D, 1.68D, z);
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    2.25D,
                    0.16D,
                    0.16D
            );
            stack.popPose();
        }
    }

    private void renderWoodenDrumAndChutes(
            MechanicalOreWasherBlockEntity washer,
            float partialTicks,
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight) {
        float rotation =
                washer.getMechanicalVisualRotationDegrees(
                        partialTicks
                );

        stack.pushPose();
        stack.translate(0.0D, 1.08D, 1.27D);
        stack.mulPose(
                Axis.ZP.rotationDegrees(rotation)
        );

        // Open slatted trommel: the gaps are intentional and now occupy real
        // multiblock volume rather than a one-block BER.
        for (int i = 0; i < DRUM_SEGMENTS; i++) {
            double radians =
                    i * Math.PI * 2.0D
                            / DRUM_SEGMENTS;

            stack.pushPose();
            stack.translate(
                    Math.cos(radians)
                            * DRUM_RADIUS,
                    Math.sin(radians)
                            * DRUM_RADIUS,
                    0.0D
            );
            stack.mulPose(
                    Axis.ZP.rotationDegrees(
                            (float) Math.toDegrees(
                                    radians
                            )
                    )
            );
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    0.12D,
                    0.075D,
                    1.78D
            );
            stack.popPose();
        }

        stack.popPose();

        // Feed hopper at the rear.
        stack.pushPose();
        stack.translate(0.0D, 1.60D, 0.18D);
        stack.mulPose(
                Axis.XP.rotationDegrees(-16.0F)
        );
        drawBox(
                stack,
                consumer,
                packedLight,
                1.12D,
                0.15D,
                0.52D
        );
        stack.popPose();

        // Split discharge chutes at the front.
        for (double x : new double[]{-0.48D, 0.48D}) {
            stack.pushPose();
            stack.translate(x, 0.30D, 2.42D);
            stack.mulPose(
                    Axis.XP.rotationDegrees(18.0F)
            );
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    0.72D,
                    0.12D,
                    0.58D
            );
            stack.popPose();
        }
    }

    private void renderCopperHardware(
            MechanicalOreWasherBlockEntity washer,
            float partialTicks,
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight) {
        // Water trough.
        stack.pushPose();
        stack.translate(0.0D, 0.34D, 1.28D);
        drawBox(
                stack,
                consumer,
                packedLight,
                2.35D,
                0.12D,
                1.88D
        );
        stack.popPose();

        float rotation =
                washer.getMechanicalVisualRotationDegrees(
                        partialTicks
                );

        stack.pushPose();
        stack.translate(0.0D, 1.08D, 1.27D);
        stack.mulPose(
                Axis.ZP.rotationDegrees(rotation)
        );

        // Through axle.
        drawBox(
                stack,
                consumer,
                packedLight,
                0.18D,
                0.18D,
                2.85D
        );

        // Three segmented copper hoops.
        for (double z : new double[]{-0.70D, 0.0D, 0.70D}) {
            for (int i = 0; i < DRUM_SEGMENTS; i++) {
                double radians =
                        i * Math.PI * 2.0D
                                / DRUM_SEGMENTS;

                stack.pushPose();
                stack.translate(
                        Math.cos(radians)
                                * DRUM_RADIUS,
                        Math.sin(radians)
                                * DRUM_RADIUS,
                        z
                );
                stack.mulPose(
                        Axis.ZP.rotationDegrees(
                                (float) Math.toDegrees(
                                        radians
                                )
                        )
                );
                drawBox(
                        stack,
                        consumer,
                        packedLight,
                        0.15D,
                        0.075D,
                        0.055D
                );
                stack.popPose();
            }
        }

        stack.popPose();

        // External bearing/input plate at the mechanical end.
        stack.pushPose();
        stack.translate(0.0D, 1.08D, 2.49D);
        drawBox(
                stack,
                consumer,
                packedLight,
                0.52D,
                0.52D,
                0.10D
        );
        stack.popPose();
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
            int packedLight,
            double sx,
            double sy,
            double sz) {
        double x0 = -sx * 0.5D;
        double x1 = sx * 0.5D;
        double y0 = -sy * 0.5D;
        double y1 = sy * 0.5D;
        double z0 = -sz * 0.5D;
        double z1 = sz * 0.5D;
        PoseStack.Pose pose = stack.last();

        quad(consumer,pose,packedLight,x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0,0,1,0);
        quad(consumer,pose,packedLight,x0,y0,z1,x0,y0,z0,x1,y0,z0,x1,y0,z1,0,-1,0);
        quad(consumer,pose,packedLight,x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,0,0,1);
        quad(consumer,pose,packedLight,x1,y0,z0,x0,y0,z0,x0,y1,z0,x1,y1,z0,0,0,-1);
        quad(consumer,pose,packedLight,x1,y0,z1,x1,y0,z0,x1,y1,z0,x1,y1,z1,1,0,0);
        quad(consumer,pose,packedLight,x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,-1,0,0);
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
                        (float) x,
                        (float) y,
                        (float) z
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
