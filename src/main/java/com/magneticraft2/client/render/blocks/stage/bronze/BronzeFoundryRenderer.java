package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeFoundryBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeFoundryBlockEntity;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeFoundryThermalCore;
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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Only changes in-world content: one fuel item, one crucible charge, and
 * one textured molten/solidified surface. All bricks, frame and tap remain
 * chunk-baked via Rendering V2. No vertex caches or rebuilds per frame.
 */
public final class BronzeFoundryRenderer implements BlockEntityRenderer<BronzeFoundryBlockEntity> {
    private static final ResourceLocation BRONZE_SURFACE =
            new ResourceLocation("minecraft", "block/raw_copper_block");

    public BronzeFoundryRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public int getViewDistance() { return 64; }

    @Override
    public void render(BronzeFoundryBlockEntity entity, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight, int overlay) {
        BlockState state = entity.getBlockState();
        if (!entity.isFormed() || !state.hasProperty(BronzeFoundryBlock.FACING)) return;

        Direction facing = state.getValue(BronzeFoundryBlock.FACING);
        float yaw = switch (facing) {
            case EAST -> 90F;
            case NORTH -> 180F;
            case WEST -> -90F;
            default -> 0F;
        };
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5D, 0D, -0.5D);

        BronzeFoundryThermalCore thermal = entity.getThermal();

        // Fuel is inside the lower, recessed firebox, not placed on the face.
        renderItem(entity, thermal.fuelDisplay(), 0.5D, 0.60D, 0.70D,
                0.35F, 780, pose, buffer, packedLight, overlay);

        if (thermal.crucibleUnits() > 0) {
            // A cold metallic crust darkens; when reheated the exact same
            // conserved bronze units show a warm liquid surface.
            surface(thermal, pose, buffer, packedLight);
        }

        // Solid charge sits near the cup rim, above any filled contents.
        renderItem(entity, thermal.chargeDisplay(), 0.32D, 1.71D, -0.51D,
                0.34F, 781, pose, buffer, packedLight, overlay);
        pose.popPose();
    }

    private void surface(BronzeFoundryThermalCore thermal, PoseStack pose,
                         MultiBufferSource sources, int packedLight) {
        float fill = Math.min(1F, thermal.crucibleUnits() /
                (float) Math.max(1, thermal.crucibleCapacity()));
        float y = (20.1F + 6.0F * fill) / 16F;
        boolean hot = thermal.isHot();

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(BRONZE_SURFACE);
        VertexConsumer consumer = sources.getBuffer(RenderType.translucent());
        PoseStack.Pose matrix = pose.last();
        int light = hot ? 0x00F000F0 : packedLight;
        float red = hot ? 1F : 0.48F;
        float green = hot ? 0.53F : 0.30F;
        float blue = hot ? 0.16F : 0.22F;
        float alpha = hot ? 0.94F : 1.0F;

        // 8x8 model pixels use 8x8 texture pixels, not a full 16x16
        // stretched texture. The surface never overlaps the crucible rim.
        float x0 = 4F / 16F, x1 = 12F / 16F;
        float z0 = -12F / 16F, z1 = -4F / 16F;
        vertex(consumer, matrix, x0, y, z0,
                sprite.getU(0), sprite.getV(0), red, green, blue, alpha, light);
        vertex(consumer, matrix, x0, y, z1,
                sprite.getU(0), sprite.getV(8), red, green, blue, alpha, light);
        vertex(consumer, matrix, x1, y, z1,
                sprite.getU(8), sprite.getV(8), red, green, blue, alpha, light);
        vertex(consumer, matrix, x1, y, z0,
                sprite.getU(8), sprite.getV(0), red, green, blue, alpha, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v,
                               float r, float g, float b, float a, int light) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(r, g, b, a).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), 0F, 1F, 0F).endVertex();
    }

    private void renderItem(BronzeFoundryBlockEntity foundry, ItemStack stack,
                            double x, double y, double z, float scale, int seed,
                            PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (stack.isEmpty()) return;
        ItemStack display = stack.copy();
        display.setCount(1);

        pose.pushPose();
        pose.translate(x, y, z);
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = renderer.getModel(display, foundry.getLevel(), null, seed);
        if (!model.isGui3d()) pose.mulPose(Axis.XP.rotationDegrees(90F));
        pose.scale(scale, scale, scale);
        renderer.render(display, ItemDisplayContext.NONE, false,
                pose, buffer, light, overlay, model);
        pose.popPose();
    }
}
