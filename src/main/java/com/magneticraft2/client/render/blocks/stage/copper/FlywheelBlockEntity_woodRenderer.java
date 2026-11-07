package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.FlywheelBlockEntity_wood;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Heavy open flywheel. The JSON model owns the bearing frame; this renderer owns
 * the rotating rim, spokes, copper weights and axle.
 */
public class FlywheelBlockEntity_woodRenderer
        implements BlockEntityRenderer<FlywheelBlockEntity_wood> {

    private static final ResourceLocation WOOD_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/oak_planks.png"
            );

    private static final ResourceLocation SHAFT_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/stripped_oak_log.png"
            );

    private static final ResourceLocation COPPER_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/copper_block.png"
            );

    private static final int RIM_SEGMENTS = 12;
    private static final double RIM_RADIUS = 0.365D;

    public FlywheelBlockEntity_woodRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            FlywheelBlockEntity_wood flywheel,
            float partialTicks,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        Direction.Axis axis =
                flywheel.getGearAxis();

        stack.pushPose();
        stack.translate(
                0.5D,
                0.5D,
                0.5D
        );
        orientLocalXToAxis(
                stack,
                axis
        );

        float angle =
                flywheel.getVisualRotationDegrees(
                        partialTicks
                );

        stack.mulPose(
                Axis.XP.rotationDegrees(
                        angle
                )
        );

        VertexConsumer shaftConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                SHAFT_TEXTURE
                        )
                );

        drawBox(
                stack,
                shaftConsumer,
                packedLight,
                1.00D,
                0.20D,
                0.20D
        );

        VertexConsumer woodConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                WOOD_TEXTURE
                        )
                );

        // Four spokes.
        for (int i = 0; i < 4; i++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(
                            i * 45.0F
                    )
            );
            drawBox(
                    stack,
                    woodConsumer,
                    packedLight,
                    0.14D,
                    0.66D,
                    0.085D
            );
            stack.popPose();
        }

        // Segmented heavy wooden rim.
        for (int i = 0; i < RIM_SEGMENTS; i++) {
            double radians =
                    i * Math.PI * 2.0D
                            / RIM_SEGMENTS;

            double y =
                    Math.cos(radians)
                            * RIM_RADIUS;
            double z =
                    Math.sin(radians)
                            * RIM_RADIUS;

            stack.pushPose();
            stack.translate(
                    0.0D,
                    y,
                    z
            );
            stack.mulPose(
                    Axis.XP.rotationDegrees(
                            (float) Math.toDegrees(
                                    radians
                            )
                    )
            );

            drawBox(
                    stack,
                    woodConsumer,
                    packedLight,
                    0.16D,
                    0.105D,
                    0.23D
            );
            stack.popPose();
        }

        VertexConsumer copperConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                COPPER_TEXTURE
                        )
                );

        // Central hub.
        drawBox(
                stack,
                copperConsumer,
                packedLight,
                0.24D,
                0.27D,
                0.27D
        );

        // Four heavy copper rim weights make the inertia visually readable.
        for (int i = 0; i < 4; i++) {
            double radians =
                    i * Math.PI * 0.5D;

            stack.pushPose();
            stack.translate(
                    0.0D,
                    Math.cos(radians)
                            * RIM_RADIUS,
                    Math.sin(radians)
                            * RIM_RADIUS
            );
            stack.mulPose(
                    Axis.XP.rotationDegrees(
                            (float) Math.toDegrees(
                                    radians
                            )
                    )
            );
            drawBox(
                    stack,
                    copperConsumer,
                    packedLight,
                    0.19D,
                    0.18D,
                    0.14D
            );
            stack.popPose();
        }

        stack.popPose();
    }

    public static void orientLocalXToAxis(
            PoseStack stack,
            Direction.Axis axis) {
        switch (axis) {
            case X -> {
            }
            case Y -> stack.mulPose(
                    Axis.ZP.rotationDegrees(
                            90.0F
                    )
            );
            case Z -> stack.mulPose(
                    Axis.YN.rotationDegrees(
                            90.0F
                    )
            );
        }
    }

    private static void drawBox(PoseStack pose, VertexConsumer consumer,
            int light,double x,double y,double z) {
        FlywheelMesh.drawBox(pose,consumer,light,x,y,z,false);
    }
}
