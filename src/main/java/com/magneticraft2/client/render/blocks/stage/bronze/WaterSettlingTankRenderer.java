package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.blockentity.stage.bronze.WaterSettlingTankBlockEntity;
import com.magneticraft2.common.registry.registers.FluidRegistry;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/**
 * The masonry, bridge and baffle are baked V2 geometry.  Only the genuine
 * stored fluid surfaces and real stored sediment output need the BER.
 * No animation runs in an empty tank and there are no per-frame particles.
 */
public final class WaterSettlingTankRenderer
        implements BlockEntityRenderer<WaterSettlingTankBlockEntity> {
    public WaterSettlingTankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public int getViewDistance() { return 48; }

    @Override
    public void render(WaterSettlingTankBlockEntity tank, float partialTicks,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!tank.isFormed()) return;

        float yaw = switch (tank.getFacing()) {
            case WEST -> -90F;
            case NORTH -> 180F;
            case EAST -> 90F;
            default -> 0F;
        };
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5D, 0D, -0.5D);

        if (tank.getDirtyAmount() > 0) {
            // Two back rows represent the actual dirty reservoir.
            fluidSurface(pose, buffers, light, new FluidStack(
                    FluidRegistry.DIRTY_WATER.get(), tank.getDirtyAmount()),
                    tank.getDirtyFillRatio(), -3, -2);
        }
        if (tank.getCleanAmount() > 0) {
            // The front chamber is divided from dirty water by a real baffle.
            fluidSurface(pose, buffers, light, new FluidStack(
                    Fluids.WATER, tank.getCleanAmount()),
                    tank.getCleanFillRatio(), -1, -1);
        }
        ItemStack sediment = tank.getSedimentStack();
        if (!sediment.isEmpty()) {
            ItemRenderer items = Minecraft.getInstance().getItemRenderer();
            pose.pushPose();
            // Actual primary item-output module, not a decorative bin.
            pose.translate(-0.5D, 0.775D, 0.5D);
            pose.scale(0.8F, 0.8F, 0.8F);
            items.render(sediment, ItemDisplayContext.GROUND, false,
                    pose, buffers, light, overlay,
                    items.getModel(sediment, tank.getLevel(), null, 420));
            pose.popPose();
        }
        pose.popPose();
    }

    private static void fluidSurface(PoseStack pose, MultiBufferSource buffers,
            int light, FluidStack fluid, float ratio, int firstZ, int lastZ) {
        IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(fluid.getFluid());
        var location = client.getStillTexture(fluid);
        if (location == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(location);
        int tint = client.getTintColor(fluid);
        float alpha = ((tint >>> 24) & 255) / 255F;
        if (alpha <= 0) alpha = 1F;
        alpha = Math.min(alpha, 0.86F);
        float red = ((tint >>> 16) & 255) / 255F;
        float green = ((tint >>> 8) & 255) / 255F;
        float blue = (tint & 255) / 255F;
        float height = 1.02F + 0.63F * Math.max(0F, Math.min(1F, ratio));
        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        PoseStack.Pose last = pose.last();
        // Tile the actual Minecraft still-fluid sprite ONCE PER BLOCK. No
        // three-block texture stretching and no tessellation per frame.
        for (int z = firstZ; z <= lastZ; z++) for (int x = -1; x <= 1; x++) {
            float west = x == -1 ? x + 0.06F : x;
            float east = x == 1 ? x + 0.94F : x + 1F;
            float north = z == firstZ ? z + 0.06F : z;
            float south = z == lastZ ? z + 0.94F : z + 1F;
            vertex(consumer,last,west,height,north,sprite.getU0(),sprite.getV0(),
                    red,green,blue,alpha,light);
            vertex(consumer,last,west,height,south,sprite.getU0(),sprite.getV1(),
                    red,green,blue,alpha,light);
            vertex(consumer,last,east,height,south,sprite.getU1(),sprite.getV1(),
                    red,green,blue,alpha,light);
            vertex(consumer,last,east,height,north,sprite.getU1(),sprite.getV0(),
                    red,green,blue,alpha,light);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose last,
            float x,float y,float z,float u,float v,
            float red,float green,float blue,float alpha,int light) {
        consumer.vertex(last.pose(),x,y,z)
                .color(red,green,blue,alpha)
                .uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(last.normal(),0F,1F,0F).endVertex();
    }
}
