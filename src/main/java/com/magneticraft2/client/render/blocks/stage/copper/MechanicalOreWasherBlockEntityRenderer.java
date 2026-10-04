package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalOreWasherBlock;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalOreWasherBlockEntity;
import com.magneticraft2.common.systems.GEAR.BeltPath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MechanicalOreWasherBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalOreWasherBlockEntity> {

    private static final ResourceLocation ROTOR_MODEL =
            new ResourceLocation("magneticraft2", "multiblock/mechanical_ore_washer_rotor");
    private static final ResourceLocation SMALL_PULLEY_MODEL =
            new ResourceLocation("magneticraft2", "block/pulley_small_wood");
    private static final ResourceLocation LARGE_PULLEY_MODEL =
            new ResourceLocation("magneticraft2", "block/pulley_large_wood");
    private static final ResourceLocation WATER_STILL =
            new ResourceLocation("minecraft", "block/water_still");

    private static final double INPUT_PULLEY_SCALE = 0.38D;
    private static final double DRIVEN_PULLEY_SCALE = 0.42D;
    private static final double INPUT_PULLEY_RADIUS =
            0.43D * INPUT_PULLEY_SCALE;
    private static final double DRIVEN_PULLEY_RADIUS =
            0.69D * DRIVEN_PULLEY_SCALE;

    private static final Vec3 INPUT_PULLEY_CENTER =
            new Vec3(0.5D, 1.5D, 1.1875D);
    private static final Vec3 DRIVEN_PULLEY_CENTER =
            new Vec3(0.5D, 0.90625D, 1.1875D);

    private static final float DRUM_SPEED_RATIO =
            (float) (INPUT_PULLEY_RADIUS / DRIVEN_PULLEY_RADIUS);

    private static final List<BeltPath.Segment> INTERNAL_BELT =
            BeltPath.createVisualSegments(
                    INPUT_PULLEY_CENTER,
                    DRIVEN_PULLEY_CENTER,
                    Direction.Axis.Z,
                    INPUT_PULLEY_RADIUS,
                    DRIVEN_PULLEY_RADIUS
            );

    private final Map<ResourceLocation, List<BakedQuad>> quadCache =
            new HashMap<>();

    public MechanicalOreWasherBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public void render(
            MechanicalOreWasherBlockEntity washer,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        BlockState formedState = washer.getBlockState();

        if (!formedState.hasProperty(MechanicalOreWasherBlock.IS_FORMED)
                || !formedState.getValue(MechanicalOreWasherBlock.IS_FORMED)) {
            return;
        }

        Direction facing =
                formedState.getValue(MechanicalOreWasherBlock.FACING);

        poseStack.pushPose();
        applySouthFacingTransform(poseStack, facing);

        // The canonical moving model spins around +Z. Rotating that model to
        // NORTH/WEST maps the visual axis to -Z/-X, while Gear V2 shaft
        // renderers intentionally use the positive axis for both facings.
        // Mirror the angle for those two facings so the internal pulley/drum
        // stays phase-correct with the physical input shaft.
        float facingRotationSign =
                (facing == Direction.NORTH
                        || facing == Direction.WEST)
                        ? -1.0F
                        : 1.0F;

        float inputRotation =
                washer.getMechanicalVisualRotationDegrees(partialTicks)
                        * facingRotationSign;
        float drumRotation =
                inputRotation * DRUM_SPEED_RATIO;

        renderPulley(
                SMALL_PULLEY_MODEL,
                INPUT_PULLEY_CENTER,
                INPUT_PULLEY_SCALE,
                inputRotation,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderPulley(
                LARGE_PULLEY_MODEL,
                DRIVEN_PULLEY_CENTER,
                DRIVEN_PULLEY_SCALE,
                drumRotation,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        LeatherBeltRenderHelper.renderSegments(
                INTERNAL_BELT,
                poseStack,
                buffer,
                packedLight,
                Vec3.ZERO,
                -washer.getMechanicalVisualBeltTravelDistance(
                        partialTicks,
                        INPUT_PULLEY_RADIUS
                ) * facingRotationSign
        );

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.90625D, 0.5D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(drumRotation));
        poseStack.translate(-0.5D, -0.90625D, -0.5D);

        renderModel(
                ROTOR_MODEL,
                RenderType.solid(),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        if (washer.hasWaterSupply()) {
            renderWaterSurface(
                    washer,
                    poseStack,
                    buffer,
                    packedLight
            );
        }

        renderStoredItem(
                washer, washer.getInputStack(),
                0.5D, 0.82D, -0.62D, 0.32F, 301,
                poseStack, buffer, packedLight, packedOverlay
        );
        renderStoredItem(
                washer, washer.getOutputStack(),
                0.34D, 0.46D, 1.50D, 0.27F, 302,
                poseStack, buffer, packedLight, packedOverlay
        );
        renderStoredItem(
                washer, washer.getByproductStack(),
                0.66D, 0.46D, 1.50D, 0.25F, 303,
                poseStack, buffer, packedLight, packedOverlay
        );

        poseStack.popPose();
    }

    private void renderPulley(
            ResourceLocation model,
            Vec3 center,
            double scale,
            float rotation,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);

        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale((float) scale, (float) scale, (float) scale);
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        renderModel(
                model,
                RenderType.solid(),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private void renderWaterSurface(
            MechanicalOreWasherBlockEntity washer,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        if (washer.getLevel() == null) {
            return;
        }

        TextureAtlasSprite sprite =
                Minecraft.getInstance()
                        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                        .apply(WATER_STILL);

        int waterColor =
                BiomeColors.getAverageWaterColor(
                        washer.getLevel(),
                        washer.getBlockPos()
                );

        float red = ((waterColor >> 16) & 0xFF) / 255.0F;
        float green = ((waterColor >> 8) & 0xFF) / 255.0F;
        float blue = (waterColor & 0xFF) / 255.0F;

        float minX = -5.5F / 16.0F;
        float maxX = 21.5F / 16.0F;
        float minZ = -4.5F / 16.0F;
        float maxZ = 21.5F / 16.0F;
        float y = 11.65F / 16.0F;

        VertexConsumer consumer =
                buffer.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();

        waterVertex(
                consumer, pose, minX, y, minZ,
                red, green, blue,
                sprite.getU0(), sprite.getV0(), packedLight
        );
        waterVertex(
                consumer, pose, minX, y, maxZ,
                red, green, blue,
                sprite.getU0(), sprite.getV1(), packedLight
        );
        waterVertex(
                consumer, pose, maxX, y, maxZ,
                red, green, blue,
                sprite.getU1(), sprite.getV1(), packedLight
        );
        waterVertex(
                consumer, pose, maxX, y, minZ,
                red, green, blue,
                sprite.getU1(), sprite.getV0(), packedLight
        );
    }

    private void waterVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float u,
            float v,
            int packedLight) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(red, green, blue, 0.82F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private void applySouthFacingTransform(
            PoseStack poseStack,
            Direction facing) {
        float yaw =
                switch (facing) {
                    case EAST -> 90.0F;
                    case NORTH -> 180.0F;
                    case WEST -> -90.0F;
                    default -> 0.0F;
                };

        if (yaw == 0.0F) {
            return;
        }

        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
    }

    private void renderModel(
            ResourceLocation modelLocation,
            RenderType renderType,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        List<BakedQuad> quads =
                quadCache.computeIfAbsent(
                        modelLocation,
                        location -> {
                            BakedModel model =
                                    Minecraft.getInstance()
                                            .getModelManager()
                                            .getModel(location);

                            if (model == null) {
                                return List.of();
                            }

                            return List.copyOf(
                                    model.getQuads(
                                            (BlockState) null,
                                            (Direction) null,
                                            RandomSource.create(0L)
                                    )
                            );
                        }
                );

        if (quads.isEmpty()) {
            return;
        }

        var consumer = buffer.getBuffer(renderType);

        for (BakedQuad quad : quads) {
            consumer.putBulkData(
                    poseStack.last(),
                    quad,
                    1.0F, 1.0F, 1.0F,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private void renderStoredItem(
            MechanicalOreWasherBlockEntity washer,
            ItemStack stack,
            double x,
            double y,
            double z,
            float scale,
            int seed,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack display = stack.copy();
        display.setCount(1);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        display,
                        ItemDisplayContext.NONE,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        buffer,
                        washer.getLevel(),
                        seed
                );

        poseStack.popPose();
    }
}
