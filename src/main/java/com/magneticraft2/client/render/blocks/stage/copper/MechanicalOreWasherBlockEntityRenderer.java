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
import net.minecraft.core.particles.ParticleTypes;
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

    // Keep the stock Magneticraft pulleys large enough that their rim/spokes
    // remain readable around the leather belt. At 0.5 scale the small + large
    // pulley still fit between the input shaft and trommel axle without overlap.
    private static final double INPUT_PULLEY_SCALE = 0.50D;
    private static final double DRIVEN_PULLEY_SCALE = 0.50D;
    private static final double INPUT_PULLEY_RADIUS =
            0.43D * INPUT_PULLEY_SCALE;
    private static final double DRIVEN_PULLEY_RADIUS =
            0.69D * DRIVEN_PULLEY_SCALE;

    // Both pulleys live on the real mechanical shaft plane. The formed
    // Mechanical Input module occupies the SOUTH-side block and its shaft axis
    // passes through z=1.5 relative to the controller.
    private static final Vec3 INPUT_PULLEY_CENTER =
            new Vec3(0.5D, 1.5D, 1.5D);
    private static final Vec3 DRIVEN_PULLEY_CENTER =
            new Vec3(0.5D, 0.90625D, 1.5D);

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
    private final Map<Long, Long> processingEffectTicks =
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

        // Sample belt travel once for this frame. Unlike the 0..360 Gear V2
        // display angle, this accumulator is continuous and therefore safe to
        // convert through a non-1:1 pulley ratio without wrap-around jumps.
        double beltTravel =
                washer.getMechanicalVisualBeltTravelDistance(
                        partialTicks,
                        INPUT_PULLEY_RADIUS
                );

        double drivenCircumference =
                Math.PI * 2.0D * DRIVEN_PULLEY_RADIUS;

        float drumRotation =
                (float) (
                        (-beltTravel / drivenCircumference)
                                * 360.0D
                ) * facingRotationSign;

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
                -beltTravel * facingRotationSign
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
                    partialTicks,
                    poseStack,
                    buffer,
                    packedLight
            );

            if (washer.isProcessing()) {
                spawnProcessingEffects(washer);
            }
        }

        renderInputItems(
                washer,
                partialTicks,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        // The output shelf sits around Y=11/16 in the formed SOUTH model.
        // These used to render at Y=0.49, which buried most items inside the
        // copper/wood chute. Keep both stacks visibly on top of the real tray.
        renderStoredItem(
                washer, washer.getOutputStack(),
                0.36D, 0.735D, 1.60D, 0.38F, 302,
                poseStack, buffer, packedLight, packedOverlay
        );
        renderStoredItem(
                washer, washer.getByproductStack(),
                0.64D, 0.735D, 1.60D, 0.34F, 303,
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
            float partialTicks,
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

        // The water belongs in the actual center trough, not across the entire
        // formed 3x3 footprint. Keeping it inside these walls avoids clipping
        // through the frame, item ports and side fluid modules.
        float minX = -1.0F / 16.0F;
        float maxX = 17.0F / 16.0F;
        float minZ = -1.0F / 16.0F;
        float maxZ = 18.0F / 16.0F;
        float fill =
                washer.getWaterFillRatio();
        float y =
                (7.50F + 3.10F * fill)
                        / 16.0F;

        if (washer.isProcessing()) {
            double visualTime =
                    washer.getLevel().getGameTime()
                            + partialTicks;
            y += (float) Math.sin(
                    visualTime * 0.65D
            ) * (0.20F / 16.0F);
        }

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

    private void spawnProcessingEffects(
            MechanicalOreWasherBlockEntity washer) {
        if (washer.getLevel() == null) {
            return;
        }

        long gameTime =
                washer.getLevel().getGameTime();

        if (gameTime % 4L != 0L) {
            return;
        }

        long key =
                washer.getBlockPos().asLong();
        Long previous =
                processingEffectTicks.get(key);

        if (previous != null
                && previous == gameTime) {
            return;
        }

        processingEffectTicks.put(
                key,
                gameTime
        );

        double y =
                washer.getBlockPos().getY()
                        + (8.1D
                        + 3.1D
                        * washer.getWaterFillRatio())
                        / 16.0D;

        for (int i = 0; i < 2; i++) {
            double x =
                    washer.getBlockPos().getX()
                            + 0.5D
                            + (washer.getLevel()
                            .random.nextDouble()
                            - 0.5D) * 0.55D;
            double z =
                    washer.getBlockPos().getZ()
                            + 0.5D
                            + (washer.getLevel()
                            .random.nextDouble()
                            - 0.5D) * 0.55D;

            washer.getLevel().addParticle(
                    ParticleTypes.SPLASH,
                    x,
                    y,
                    z,
                    0.0D,
                    0.025D,
                    0.0D
            );
        }

        washer.getLevel().addParticle(
                ParticleTypes.BUBBLE_POP,
                washer.getBlockPos().getX()
                        + 0.5D,
                y + 0.02D,
                washer.getBlockPos().getZ()
                        + 0.5D,
                0.0D,
                0.015D,
                0.0D
        );
    }

    private void renderInputItems(
            MechanicalOreWasherBlockEntity washer,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        ItemStack stack =
                washer.getInputStack();

        if (stack.isEmpty()) {
            return;
        }

        int stationaryCount =
                washer.isProcessing()
                        ? Math.max(
                                0,
                                stack.getCount() - 1
                        )
                        : stack.getCount();

        int visiblePile =
                Math.min(
                        stationaryCount,
                        5
                );

        double[][] offsets = {
                {0.00D, 0.000D, 0.00D},
                {-0.075D, 0.018D, 0.045D},
                {0.075D, 0.036D, 0.075D},
                {-0.035D, 0.054D, 0.105D},
                {0.050D, 0.072D, 0.135D}
        };

        for (int i = 0; i < visiblePile; i++) {
            renderStoredItem(
                    washer,
                    stack,
                    0.5D + offsets[i][0],
                    0.82D + offsets[i][1],
                    -0.62D + offsets[i][2],
                    0.42F,
                    301 + i,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }

        if (!washer.isProcessing()) {
            return;
        }

        double visualTime =
                washer.getLevel() == null
                        ? partialTicks
                        : washer.getLevel()
                        .getGameTime()
                        + partialTicks;

        double feedPhase =
                0.5D
                        + 0.5D
                        * Math.sin(
                                visualTime * 0.24D
                        );

        renderStoredItem(
                washer,
                stack,
                0.5D,
                0.83D
                        + Math.sin(
                                visualTime * 0.50D
                        ) * 0.012D,
                -0.60D
                        + feedPhase * 0.34D,
                0.37F,
                399,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
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
