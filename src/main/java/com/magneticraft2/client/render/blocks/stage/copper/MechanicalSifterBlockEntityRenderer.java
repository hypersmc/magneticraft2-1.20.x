package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalSifterBlock;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalSifterBlockEntity;
import com.magneticraft2.common.utils.MultiBlockProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

/**
 * Formed Mechanical Sifter renderer.
 *
 * Like the other multiblocks, the complete static frame comes from the
 * replacement model selected in JSON. Only the two moving screen assemblies are
 * rendered separately.
 */
public class MechanicalSifterBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalSifterBlockEntity> {

    private static final ResourceLocation UPPER_TRAY =
            new ResourceLocation(
                    "magneticraft2",
                    "multiblock/mechanical_sifter_upper_tray"
            );
    private static final ResourceLocation LOWER_TRAY =
            new ResourceLocation(
                    "magneticraft2",
                    "multiblock/mechanical_sifter_lower_tray"
            );

    public MechanicalSifterBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(
            MechanicalSifterBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void render(
            MechanicalSifterBlockEntity sifter,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {

        BlockState formedState =
                sifter.getBlockState();

        if (!formedState.hasProperty(MechanicalSifterBlock.IS_FORMED)
                || !formedState.getValue(MechanicalSifterBlock.IS_FORMED)) {
            return;
        }

        Direction facing =
                formedState.getValue(MechanicalSifterBlock.FACING);

        ModelData modelData =
                sifter.getModelData();
        String modelName =
                modelData.get(
                        MultiBlockProperties.MODEL_NAME
                );

        if (modelName == null || modelName.isEmpty()) {
            modelName =
                    "multiblock/mechanical_sifter_"
                            + facing.getName();
        }

        renderModel(
                    new ResourceLocation(
                            "magneticraft2",
                            modelName
                    ),
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );

        poseStack.pushPose();
        applySouthFacingTransform(
                poseStack,
                facing
        );

        double shake =
                sifter.getShakeOffset(
                        partialTicks
                );

        poseStack.pushPose();
        poseStack.translate(
                shake,
                0.0D,
                0.0D
        );
        renderModel(
                UPPER_TRAY,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(
                -shake * 0.70D,
                0.0D,
                0.0D
        );
        renderModel(
                LOWER_TRAY,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        renderStoredItem(
                sifter,
                sifter.getInputStack(),
                0.5D + shake,
                1.55D,
                0.43D,
                0.33F,
                401,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderStoredItem(
                sifter,
                sifter.getOutputStack(),
                0.03D,
                0.30D,
                1.76D,
                0.29F,
                402,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderStoredItem(
                sifter,
                sifter.getByproductStack(),
                0.97D,
                0.30D,
                1.76D,
                0.27F,
                403,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
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

        poseStack.translate(
                0.5D,
                0.0D,
                0.5D
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees(yaw)
        );
        poseStack.translate(
                -0.5D,
                0.0D,
                -0.5D
        );
    }

    private void renderModel(
            ResourceLocation modelLocation,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {

        BakedModel model =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(
                                modelLocation
                        );

        if (model == null) {
            return;
        }

        RandomSource random =
                RandomSource.create(0L);
        List<BakedQuad> quads =
                model.getQuads(
                        (BlockState) null,
                        (Direction) null,
                        random
                );

        var consumer =
                buffer.getBuffer(
                        RenderType.solid()
                );

        for (BakedQuad quad : quads) {
            consumer.putBulkData(
                    poseStack.last(),
                    quad,
                    1.0F,
                    1.0F,
                    1.0F,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private void renderStoredItem(
            MechanicalSifterBlockEntity sifter,
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

        ItemStack display =
                stack.copy();
        display.setCount(1);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        90.0F
                )
        );
        poseStack.scale(
                scale,
                scale,
                scale
        );

        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        display,
                        ItemDisplayContext.NONE,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        buffer,
                        sifter.getLevel(),
                        seed
                );

        poseStack.popPose();
    }
}
