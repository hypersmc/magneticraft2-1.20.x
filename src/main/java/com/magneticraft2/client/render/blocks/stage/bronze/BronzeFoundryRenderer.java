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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Foundry's changing contents ONLY. The 3x2x3 brickwork, crucible and
 * tapping nozzle are chunk-baked by Rendering V2 and never rebuilt per frame.
 *
 * Coordinates below are canonical SOUTH-oriented MODEL PIXELS relative to
 * the lower-front controller, matching designs/bronze_foundry.json:
 * firebox interior x=2.5..13.5, y=3.5..14, z=2.2..11.4 (grate at z=11.4);
 * crucible interior x=2.6..13.4, z=-13.4..-2.6, floor y=19.9.
 */
public final class BronzeFoundryRenderer implements BlockEntityRenderer<BronzeFoundryBlockEntity> {
    private static final ResourceLocation BRONZE_SURFACE =
            new ResourceLocation("minecraft", "block/raw_copper_block");
    private static final ResourceLocation FIRE_SURFACE =
            new ResourceLocation("minecraft", "block/fire_0");

    // A small air gap to the refractory wall prevents z fighting, while the
    // pool spans nearly the full bowl rather than a tiny square at its center.
    private static final float BOWL_X0 = 2.74F, BOWL_X1 = 13.26F;
    private static final float BOWL_Z0 = -13.26F, BOWL_Z1 = -2.74F;
    private static final float BOWL_BOTTOM = 19.93F;

    public BronzeFoundryRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public int getViewDistance() { return 64; }

    @Override
    public void render(BronzeFoundryBlockEntity entity, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight, int overlay) {
        BlockState state = entity.getBlockState();
        if (!entity.isFormed() || !state.hasProperty(BronzeFoundryBlock.FACING)) return;
        Direction facing = state.getValue(BronzeFoundryBlock.FACING);

        // Match generate.mjs exactly: its canonical SOUTH model rotates
        // +1 quarter-turn to WEST (x,z -> -z,x). PoseStack/Y+ uses the
        // opposite sign convention for that same world-space turn.
        float yaw = switch (facing) {
            case WEST -> -90F;
            case NORTH -> 180F;
            case EAST -> 90F;
            default -> 0F;
        };
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D, 0D, -.5D);

        BronzeFoundryThermalCore thermal = entity.getThermal();

        // Fuel sits behind the iron grate (z=11.4..12.6), not in the opening.
        // In particular, full cubic fuels such as coal BLOCKS are scaled down.
        ItemStack fuel = thermal.fuelDisplay();
        float fuelScale = fuel.getItem() instanceof BlockItem ? .25F : .43F;
        renderItem(entity, fuel, .50D, .47D, .43D, fuelScale, 780,
                pose, buffer, packedLight, overlay);

        if (thermal.burnRemaining() > 0) {
            fireFlames(pose, buffer);
        }
        if (thermal.crucibleUnits() > 0) {
            moltenOrSolidSurface(thermal, pose, buffer, packedLight);
        }

        // The charge rests flat and CENTERED inside the open crucible. When
        // bronze is already inside, lift the next ingot above the fill surface.
        double chargeY = thermal.crucibleUnits() > 0
                ? Math.min(1.72D, (20.1D + 6D * thermal.crucibleUnits()
                    / Math.max(1, thermal.crucibleCapacity())) / 16D + .075D)
                : 1.31D;
        renderItem(entity, thermal.chargeDisplay(), .50D, chargeY, -.50D,
                .43F, 781, pose, buffer, packedLight, overlay);
        pose.popPose();
    }

    /** Two upright crossed flame sheets INSIDE the chamber, behind the grate. */
    private void fireFlames(PoseStack pose, MultiBufferSource source) {
        TextureAtlasSprite sprite = sprite(FIRE_SURFACE);
        VertexConsumer vertices = source.getBuffer(RenderType.translucent());
        PoseStack.Pose matrix = pose.last();
        // Pixel-perfect ~8x7.6 sections; use the LOWER region of fire_0 at the
        // base of each sheet, with the upper texture towards the top.
        float y0 = 4.3F/16F, y1 = 11.9F/16F;
        float u0 = sprite.getU(1), u1 = sprite.getU(9);
        float vBottom = sprite.getV(16), vTop = sprite.getV(8.4F);
        int fullbright = 0x00F000F0;
        float a= .90F, red=1F, green=.72F, blue=.35F;

        // Sheet 1 (slants across X and Z)
        flameFace(vertices,matrix,4F/16F,9.7F/16F,12F/16F,4.2F/16F,
                y0,y1,u0,u1,vBottom,vTop,red,green,blue,a,fullbright);
        flameFace(vertices,matrix,12F/16F,4.2F/16F,4F/16F,9.7F/16F,
                y0,y1,u0,u1,vBottom,vTop,red,green,blue,a,fullbright);
        // Sheet 2: crossed, so the effect is visible from the opening/side.
        flameFace(vertices,matrix,4F/16F,4.2F/16F,12F/16F,9.7F/16F,
                y0,y1,u0,u1,vBottom,vTop,red,green,blue,a,fullbright);
        flameFace(vertices,matrix,12F/16F,9.7F/16F,4F/16F,4.2F/16F,
                y0,y1,u0,u1,vBottom,vTop,red,green,blue,a,fullbright);
    }

    private static void flameFace(VertexConsumer vertices, PoseStack.Pose matrix,
                                  float x0,float z0,float x1,float z1,float y0,float y1,
                                  float u0,float u1,float vBottom,float vTop,
                                  float r,float g,float b,float a,int light) {
        // Reverse winding for each companion face to keep both sides visible.
        vertex(vertices,matrix,x0,y0,z0,u0,vBottom,r,g,b,a,light,0,0,1);
        vertex(vertices,matrix,x1,y0,z1,u1,vBottom,r,g,b,a,light,0,0,1);
        vertex(vertices,matrix,x1,y1,z1,u1,vTop,r,g,b,a,light,0,0,1);
        vertex(vertices,matrix,x0,y1,z0,u0,vTop,r,g,b,a,light,0,0,1);
    }

    /** A liquid surface AND four sides, meeting the inner refractory walls. */
    private void moltenOrSolidSurface(BronzeFoundryThermalCore thermal, PoseStack pose,
                                      MultiBufferSource sources, int packedLight) {
        TextureAtlasSprite sprite = sprite(BRONZE_SURFACE);
        VertexConsumer vertices = sources.getBuffer(RenderType.translucent());
        PoseStack.Pose matrix = pose.last();
        float fill = Math.min(1F, thermal.crucibleUnits() /
                (float) Math.max(1, thermal.crucibleCapacity()));
        float topPixels = 20.1F + 6F * fill, top = topPixels/16F;
        float bottom = BOWL_BOTTOM/16F;
        boolean molten = thermal.isHot();
        int light = molten ? 0x00F000F0 : packedLight;
        float red = molten ? 1F : .53F;
        float green = molten ? .57F : .35F;
        float blue = molten ? .18F : .28F;
        float a = 1F;
        float x0=BOWL_X0/16F, x1=BOWL_X1/16F;
        float z0=BOWL_Z0/16F, z1=BOWL_Z1/16F;
        // UVs reflect the ~10.5x10.5-pixel physical pool, with no stretching.
        float u0=sprite.getU(0),u1=sprite.getU(BOWL_X1-BOWL_X0);
        float v0=sprite.getV(0),v1=sprite.getV(BOWL_Z1-BOWL_Z0);
        vertex(vertices,matrix,x0,top,z0,u0,v0,red,green,blue,a,light,0,1,0);
        vertex(vertices,matrix,x0,top,z1,u0,v1,red,green,blue,a,light,0,1,0);
        vertex(vertices,matrix,x1,top,z1,u1,v1,red,green,blue,a,light,0,1,0);
        vertex(vertices,matrix,x1,top,z0,u1,v0,red,green,blue,a,light,0,1,0);

        // Vertical inside edges make the level read as a filled VOLUME, not a
        // floating quad. World/model pixel UV dimensions match each face.
        float low = sprite.getV(0),high = sprite.getV(topPixels-BOWL_BOTTOM);
        side(vertices,matrix,x0,bottom,z1,x1,top,z1,u0,u1,low,high,
                red,green,blue,a,light,0,0,1);
        side(vertices,matrix,x1,bottom,z0,x0,top,z0,u0,u1,low,high,
                red,green,blue,a,light,0,0,-1);
        side(vertices,matrix,x0,bottom,z0,x0,top,z1,u0,u1,low,high,
                red,green,blue,a,light,-1,0,0);
        side(vertices,matrix,x1,bottom,z1,x1,top,z0,u0,u1,low,high,
                red,green,blue,a,light,1,0,0);
    }

    private static void side(VertexConsumer vertices, PoseStack.Pose matrix,
                             float x0,float y0,float z0,float x1,float y1,float z1,
                             float u0,float u1,float v0,float v1,
                             float r,float g,float b,float a,int light,
                             float nx,float ny,float nz) {
        vertex(vertices,matrix,x0,y0,z0,u0,v1,r,g,b,a,light,nx,ny,nz);
        vertex(vertices,matrix,x1,y0,z1,u1,v1,r,g,b,a,light,nx,ny,nz);
        vertex(vertices,matrix,x1,y1,z1,u1,v0,r,g,b,a,light,nx,ny,nz);
        vertex(vertices,matrix,x0,y1,z0,u0,v0,r,g,b,a,light,nx,ny,nz);
    }

    private static TextureAtlasSprite sprite(ResourceLocation id) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id);
    }

    private static void vertex(VertexConsumer vertices,PoseStack.Pose pose,
                               float x,float y,float z,float u,float v,
                               float r,float g,float b,float a,int light,
                               float nx,float ny,float nz) {
        vertices.vertex(pose.pose(),x,y,z).color(r,g,b,a).uv(u,v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(),nx,ny,nz).endVertex();
    }

    private static void renderItem(BronzeFoundryBlockEntity foundry, ItemStack stack,
                                   double x,double y,double z,float scale,int seed,
                                   PoseStack pose,MultiBufferSource buffer,int light,int overlay) {
        if (stack.isEmpty()) return;
        ItemStack display=stack.copy();
        display.setCount(1);
        pose.pushPose();
        pose.translate(x,y,z);
        ItemRenderer renderer=Minecraft.getInstance().getItemRenderer();
        BakedModel model=renderer.getModel(display,foundry.getLevel(),null,seed);
        if (!model.isGui3d()) pose.mulPose(Axis.XP.rotationDegrees(90F));
        pose.scale(scale,scale,scale);
        renderer.render(display,ItemDisplayContext.NONE,false,pose,buffer,light,overlay,model);
        pose.popPose();
    }
}
