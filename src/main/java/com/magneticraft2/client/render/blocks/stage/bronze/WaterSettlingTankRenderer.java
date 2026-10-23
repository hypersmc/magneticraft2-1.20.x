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
 * The ceramic-lined vessel, service bridge, central weir and real pipes use
 * baked Voxel V2 parts. Only synchronized fluid VOLUMES and actual stored
 * sediment are rendered dynamically; no particles, fake item entities, or
 * per-tick mesh regeneration.
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

        // Use the OPEN interior geometry, not the outside dimensions of the
        // basin. In V4 the water extended ~0.28 blocks INTO EACH wall,
        // giving translucent/opaque geometry and water overlap artifacts.
        // Both surfaces are tied solely to the real server-side tank amounts.
        if (tank.getDirtyAmount() > 0) {
            renderPool(pose, buffers, light, new FluidStack(
                    FluidRegistry.DIRTY_WATER.get(), tank.getDirtyAmount()),
                    tank.getDirtyFillRatio(),
                    -0.975F, 1.975F, -3.53F, -1.29F, 0.67F);
        }
        if (tank.getCleanAmount() > 0) {
            renderPool(pose, buffers, light, new FluidStack(
                    Fluids.WATER, tank.getCleanAmount()),
                    tank.getCleanFillRatio(),
                    -0.975F, 1.975F, -0.96F, -0.24F, 0.55F);
        }

        ItemStack sediment = tank.getSedimentStack();
        if (!sediment.isEmpty()) {
            ItemRenderer items = Minecraft.getInstance().getItemRenderer();
            pose.pushPose();
            // ONLY the actual front-left stored item-output module.
            pose.translate(-0.5D, 0.775D, 0.5D);
            pose.scale(0.8F, 0.8F, 0.8F);
            items.render(sediment, ItemDisplayContext.GROUND, false,
                    pose, buffers, light, overlay,
                    items.getModel(sediment, tank.getLevel(), null, 420));
            pose.popPose();
        }
        pose.popPose();
    }

    /**
     * Top plus four softly shaded transparent vertical fluid sides. UV
     * coordinates follow the Minecraft still-water sprite per WORLD BLOCK:
     * the clipped edge tiles never stretch or compress the texture.
     * Surfaces remain below the weir/metal lip and out of all real ports.
     */
    private static void renderPool(PoseStack pose, MultiBufferSource buffers,
            int light, FluidStack fluid, float ratio,
            float minX, float maxX, float minZ, float maxZ, float targetOpacity) {
        IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(fluid.getFluid());
        var texture = client.getStillTexture(fluid);
        if (texture == null || ratio <= 0F) return;
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        int tint = client.getTintColor(fluid);
        float r = ((tint >>> 16) & 255) / 255F;
        float g = ((tint >>> 8) & 255) / 255F;
        float b = (tint & 255) / 255F;
        float tintAlpha = ((tint >>> 24) & 255) / 255F;
        float alpha = Math.min(tintAlpha <= 0F ? 1F : tintAlpha, targetOpacity);
        // Water stays below the LOWER weir cap (Y=1 + 9/16), never through it.
        // Dirty and clean levels remain separate and inventory-driven.
        float top = 1.045F + 0.495F * Math.max(0F, Math.min(1F, ratio));
        float bottom = 1.014F;
        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        PoseStack.Pose matrix = pose.last();

        for (int tz = (int)Math.floor(minZ); tz < Math.ceil(maxZ); tz++)
        for (int tx = (int)Math.floor(minX); tx < Math.ceil(maxX); tx++) {
            float x0 = Math.max(tx, minX), x1 = Math.min(tx+1F, maxX);
            float z0 = Math.max(tz, minZ), z1 = Math.min(tz+1F, maxZ);
            if (x1 <= x0 || z1 <= z0) continue;
            float u0 = u(sprite, x0-tx), u1 = u(sprite, x1-tx);
            float v0 = v(sprite, z0-tz), v1 = v(sprite, z1-tz);
            vertex(consumer,matrix,x0,top,z0,u0,v0,r,g,b,alpha,light,0F,1F,0F);
            vertex(consumer,matrix,x0,top,z1,u0,v1,r,g,b,alpha,light,0F,1F,0F);
            vertex(consumer,matrix,x1,top,z1,u1,v1,r,g,b,alpha,light,0F,1F,0F);
            vertex(consumer,matrix,x1,top,z0,u1,v0,r,g,b,alpha,light,0F,1F,0F);
        }

        float sideAlpha = alpha * 0.62F;
        float sr = r * 0.83F, sg = g * 0.83F, sb = b * 0.83F;
        float vBottom = v(sprite, 0F), vTop = v(sprite, top-bottom);
        for (int tx=(int)Math.floor(minX); tx < Math.ceil(maxX); tx++) {
            float x0=Math.max(minX,tx),x1=Math.min(maxX,tx+1F);
            if(x1<=x0)continue;
            float u0=u(sprite,x0-tx),u1=u(sprite,x1-tx);
            quad(consumer,matrix,x1,bottom,minZ,x0,bottom,minZ,
                    x0,top,minZ,x1,top,minZ,u1,u0,vBottom,vTop,
                    sr,sg,sb,sideAlpha,light,0F,0F,-1F);
            quad(consumer,matrix,x0,bottom,maxZ,x1,bottom,maxZ,
                    x1,top,maxZ,x0,top,maxZ,u0,u1,vBottom,vTop,
                    sr,sg,sb,sideAlpha,light,0F,0F,1F);
        }
        for (int tz=(int)Math.floor(minZ); tz < Math.ceil(maxZ); tz++) {
            float z0=Math.max(minZ,tz),z1=Math.min(maxZ,tz+1F);
            if(z1<=z0)continue;
            float u0=u(sprite,z0-tz),u1=u(sprite,z1-tz);
            quad(consumer,matrix,minX,bottom,z0,minX,bottom,z1,
                    minX,top,z1,minX,top,z0,u0,u1,vBottom,vTop,
                    sr,sg,sb,sideAlpha,light,-1F,0F,0F);
            quad(consumer,matrix,maxX,bottom,z1,maxX,bottom,z0,
                    maxX,top,z0,maxX,top,z1,u1,u0,vBottom,vTop,
                    sr,sg,sb,sideAlpha,light,1F,0F,0F);
        }
    }

    private static float u(TextureAtlasSprite s, float fraction) {
        return s.getU0() + (s.getU1()-s.getU0())*fraction;
    }
    private static float v(TextureAtlasSprite s, float fraction) {
        return s.getV0() + (s.getV1()-s.getV0())*fraction;
    }
    private static void quad(VertexConsumer b, PoseStack.Pose matrix,
            float x0,float y0,float z0,float x1,float y1,float z1,
            float x2,float y2,float z2,float x3,float y3,float z3,
            float u0,float u1,float v0,float v1,
            float r,float g,float bl,float alpha,int light,float nx,float ny,float nz) {
        vertex(b,matrix,x0,y0,z0,u0,v0,r,g,bl,alpha,light,nx,ny,nz);
        vertex(b,matrix,x1,y1,z1,u1,v0,r,g,bl,alpha,light,nx,ny,nz);
        vertex(b,matrix,x2,y2,z2,u1,v1,r,g,bl,alpha,light,nx,ny,nz);
        vertex(b,matrix,x3,y3,z3,u0,v1,r,g,bl,alpha,light,nx,ny,nz);
    }
    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
            float x,float y,float z,float u,float v,
            float r,float g,float b,float alpha,int light,float nx,float ny,float nz) {
        buffer.vertex(pose.pose(),x,y,z)
                .color(r,g,b,alpha).uv(u,v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(),nx,ny,nz).endVertex();
    }
}
