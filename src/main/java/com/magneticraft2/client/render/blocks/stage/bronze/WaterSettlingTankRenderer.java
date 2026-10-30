package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.blockentity.stage.bronze.WaterSettlingTankBlockEntity;
import com.magneticraft2.common.registry.registers.FluidRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
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
 * Static tank walls use chunk-baked Voxel V2 parts. The REAL fluid port
 * blocks retain their shared models and capabilities; two purpose-baked
 * machine-only inserts are drawn here inside those same port block spaces.
 * Fluid volumes and the real stored sediment are the only animated visuals.
 * No geometry is synthesized from primitives or regenerated every tick.
 */
public final class WaterSettlingTankRenderer
        implements BlockEntityRenderer<WaterSettlingTankBlockEntity> {
    private static final ResourceLocation INPUT_PORT_INSERT =
            new ResourceLocation("magneticraft2",
                    "multiblock/water_settling_tank_input_port_insert");
    private static final ResourceLocation OUTPUT_PORT_INSERT =
            new ResourceLocation("magneticraft2",
                    "multiblock/water_settling_tank_output_port_insert");

    // Three touching regions produce one U-shaped muddy pool. The two narrow
    // rear arms fill the pockets on either side of the north-facing input.
    // All coordinates are local to the SOUTH-oriented controller.
    private record PoolSection(float minX, float maxX, float minZ, float maxZ) {}

    private static final float POOL_BOTTOM = 1.014F;
    // The real fluid modules have matching 2px bottom plates (Y=1.125).
    // Their in-block water tongues begin just above these metal sills.
    private static final float DIRTY_INLET_SILL = 1.135F;
    private static final float CLEAN_OUTLET_SILL = 1.135F;
    private static final float CLEAN_EAST_EDGE = 1.985F;

    private static final PoolSection[] DIRTY_SECTIONS = {
            new PoolSection(-0.985F, -0.205F, -3.60F, -2.985F),
            new PoolSection(-0.985F, 1.985F, -2.985F, -1.29F),
            new PoolSection(1.205F, 1.985F, -3.60F, -2.985F)
    };
    // The small centered throat is in front of the real inlet's copper
    // ring (which ends at Z=-3.25). X stays between the port's side walls.
    // Keep it separate from the two deeper side pockets: it has a higher
    // metal sill and must not appear until water actually reaches that sill.
    private static final PoolSection DIRTY_INLET =
            new PoolSection(0.135F, 0.865F, -3.22F, -2.985F);
    private static final PoolSection[] DIRTY_WITH_INLET = {
            DIRTY_SECTIONS[0], DIRTY_SECTIONS[1], DIRTY_SECTIONS[2],
            DIRTY_INLET
    };
    private static final float[][] DIRTY_PERIMETER = {
            {-0.985F, -1.29F}, {1.985F, -1.29F},
            {1.985F, -3.60F}, {1.205F, -3.60F},
            {1.205F, -2.985F}, {-0.205F, -2.985F},
            {-0.205F, -3.60F}, {-0.985F, -3.60F}
    };
    private static final float[][] DIRTY_INLET_PERIMETER = {
            {-0.985F, -1.29F}, {1.985F, -1.29F},
            {1.985F, -3.60F}, {1.205F, -3.60F},
            {1.205F, -2.985F}, {0.865F, -2.985F},
            {0.865F, -3.22F}, {0.135F, -3.22F},
            {0.135F, -2.985F}, {-0.205F, -2.985F},
            {-0.205F, -3.60F}, {-0.985F, -3.60F}
    };

    // The main clean pool meets both the east wall and the front retaining
    // lip. At sufficient level a shallow tongue extends through the open
    // center of the real EAST-facing output, ending BEFORE its copper ring.
    private static final PoolSection CLEAN_MAIN =
            new PoolSection(-0.985F, 1.985F, -0.985F, -0.135F);
    private static final PoolSection CLEAN_OUTLET =
            new PoolSection(1.985F, 2.235F, -0.865F, -0.135F);
    private static final PoolSection[] CLEAN_SHALLOW = {CLEAN_MAIN};
    private static final PoolSection[] CLEAN_WITH_OUTLET = {
            CLEAN_MAIN, CLEAN_OUTLET
    };
    private static final float[][] CLEAN_SHALLOW_PERIMETER = {
            {-0.985F, -0.135F}, {1.985F, -0.135F},
            {1.985F, -0.985F}, {-0.985F, -0.985F}
    };
    private static final float[][] CLEAN_OUTLET_PERIMETER = {
            {-0.985F, -0.135F}, {1.985F, -0.135F},
            {2.235F, -0.135F}, {2.235F, -0.865F},
            {1.985F, -0.865F}, {1.985F, -0.985F},
            {-0.985F, -0.985F}
    };

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

        // Both inserts occupy REAL module cells rather than the neighboring
        // required AIR cells. The generic block models still render normally,
        // and their external fluid connections remain untouched.
        drawPortInsert(pose, buffers, light, overlay, tank,
                INPUT_PORT_INSERT, 0D, 1D, -4D);
        drawPortInsert(pose, buffers, light, overlay, tank,
                OUTPUT_PORT_INSERT, 2D, 1D, -1D);

        // Server tanks drive all surfaces. Dirt fills both rear pockets
        // BESIDE the input, not the solid wall returns in its center.
        // Clean water reaches the front lip and, when high enough, the
        // open middle of the east-facing output block.
        if (tank.getDirtyAmount() > 0) {
            renderPool(pose, buffers, light, new FluidStack(
                    FluidRegistry.DIRTY_WATER.get(), tank.getDirtyAmount()),
                    tank.getDirtyFillRatio(), true, 0.67F);
        }
        if (tank.getCleanAmount() > 0) {
            renderPool(pose, buffers, light, new FluidStack(
                    Fluids.WATER, tank.getCleanAmount()),
                    tank.getCleanFillRatio(), false, 0.55F);
        }

        ItemStack sediment = tank.getSedimentStack();
        if (!sediment.isEmpty()) {
            ItemRenderer items = Minecraft.getInstance().getItemRenderer();
            pose.pushPose();
            // Only the actual elevated front-wall sediment output is rendered:
            // the module moved up one block, its stored stack moved with it.
            pose.translate(-0.5D, 1.775D, 0.5D);
            pose.scale(0.8F, 0.8F, 0.8F);
            items.render(sediment, ItemDisplayContext.GROUND, false,
                    pose, buffers, light, overlay,
                    items.getModel(sediment, tank.getLevel(), null, 420));
            pose.popPose();
        }
        pose.popPose();
    }

    /**
     * Draw a pre-baked, static block model IN the occupied fluid port cell.
     * Forge registers both models as additional models on resource reload.
     * This modifies no shared port state, block model, capabilities, or
     * collision, and only runs for a formed Water Settling Tank controller.
     */
    private static void drawPortInsert(PoseStack pose,
            MultiBufferSource buffers, int light, int overlay,
            WaterSettlingTankBlockEntity tank, ResourceLocation modelId,
            double x, double y, double z) {
        BakedModel model = Minecraft.getInstance().getModelManager()
                .getModel(modelId);
        BlockRenderDispatcher renderer = Minecraft.getInstance().getBlockRenderer();

        // A port is several blocks away from the controller. Reusing the
        // controller's packed light value made its BER-only stone cheeks
        // look darker/brighter than the neighboring chunk-baked wall.
        // Read light at the ACTUAL rotated fluid module position instead.
        int dx = (int) x, dy = (int) y, dz = (int) z;
        int rx, rz;
        switch (tank.getFacing()) {
            case WEST -> { rx = -dz; rz = dx; }
            case NORTH -> { rx = -dx; rz = -dz; }
            case EAST -> { rx = dz; rz = -dx; }
            default -> { rx = dx; rz = dz; }
        }
        BlockPos portPos = tank.getBlockPos().offset(rx, dy, rz);
        int portLight = tank.getLevel() == null ? light
                : LevelRenderer.getLightColor(tank.getLevel(), portPos);

        pose.pushPose();
        pose.translate(x, y, z);
        renderer.getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()), tank.getBlockState(),
                model, 1F, 1F, 1F, portLight, overlay);
        pose.popPose();
    }

    /**
     * Connected, tiled X/Z footprints instead of one conservative rectangle.
     * Rectangles only TOUCH along their edges, never overlap in area.
     * One exterior contour draws vertical sides, avoiding fake translucent
     * walls along internal rectangle seams. The texture still tiles once
     * per Minecraft block, even around narrow pockets and the outlet.
     */
    private static void renderPool(PoseStack pose, MultiBufferSource buffers,
            int light, FluidStack fluid, float ratio,
            boolean dirty, float targetOpacity) {
        IClientFluidTypeExtensions client =
                IClientFluidTypeExtensions.of(fluid.getFluid());
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

        // Retain the existing low-weir clearance and server-driven level.
        float top = 1.045F + 0.495F * Math.max(0F, Math.min(1F, ratio));
        boolean extendsIntoInlet = dirty && top > DIRTY_INLET_SILL + 0.005F;
        boolean extendsIntoOutlet = !dirty && top > CLEAN_OUTLET_SILL + 0.005F;
        PoolSection[] sections = dirty ?
                (extendsIntoInlet ? DIRTY_WITH_INLET : DIRTY_SECTIONS) :
                (extendsIntoOutlet ? CLEAN_WITH_OUTLET : CLEAN_SHALLOW);
        float[][] contour = dirty ?
                (extendsIntoInlet ? DIRTY_INLET_PERIMETER : DIRTY_PERIMETER) :
                (extendsIntoOutlet ? CLEAN_OUTLET_PERIMETER
                                   : CLEAN_SHALLOW_PERIMETER);

        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        PoseStack.Pose matrix = pose.last();
        for (PoolSection section : sections) {
            // Native 1-block sprite tiling; crop UVs only at each edge.
            for (int tileZ=(int)Math.floor(section.minZ());
                    tileZ<Math.ceil(section.maxZ()); tileZ++) {
                for (int tileX=(int)Math.floor(section.minX());
                        tileX<Math.ceil(section.maxX()); tileX++) {
                    float x0=Math.max(tileX,section.minX());
                    float x1=Math.min(tileX+1F,section.maxX());
                    float z0=Math.max(tileZ,section.minZ());
                    float z1=Math.min(tileZ+1F,section.maxZ());
                    if (x1<=x0 || z1<=z0) continue;
                    float u0=u(sprite,x0-tileX),u1=u(sprite,x1-tileX);
                    float v0=v(sprite,z0-tileZ),v1=v(sprite,z1-tileZ);
                    vertex(consumer,matrix,x0,top,z0,u0,v0,
                            r,g,b,alpha,light,0F,1F,0F);
                    vertex(consumer,matrix,x0,top,z1,u0,v1,
                            r,g,b,alpha,light,0F,1F,0F);
                    vertex(consumer,matrix,x1,top,z1,u1,v1,
                            r,g,b,alpha,light,0F,1F,0F);
                    vertex(consumer,matrix,x1,top,z0,u1,v0,
                            r,g,b,alpha,light,0F,1F,0F);
                }
            }
        }
        renderPerimeter(consumer, matrix, sprite, contour, top,
                dirty, extendsIntoInlet, alpha, r, g, b, light);
    }

    /**
     * The perimeter is ordered so its exterior normal is (-dz,+dx).
     * Only exterior vertical faces are emitted, never the internal joins
     * between the rear pockets/main basin or the clean outlet tongue.
     */
    private static void renderPerimeter(VertexConsumer consumer,
            PoseStack.Pose matrix, TextureAtlasSprite sprite,
            float[][] contour, float top, boolean dirty,
            boolean extendsIntoInlet, float alpha,
            float r, float g, float b, int light) {
        float sideAlpha=alpha*0.62F;
        float sr=r*0.83F,sg=g*0.83F,sb=b*0.83F;
        for (int edge=0;edge<contour.length;edge++) {
            float[] a=contour[edge],p=contour[(edge+1)%contour.length];
            float dx=p[0]-a[0],dz=p[1]-a[1];
            float length=(float)Math.sqrt(dx*dx+dz*dz);
            if (length<=0F) continue;
            float nx=-dz/length,nz=dx/length;
            boolean alongX=Math.abs(dx)>0.0001F;
            float from=alongX?a[0]:a[1],to=alongX?p[0]:p[1];
            float low=Math.min(from,to),high=Math.max(from,to);
            // Inside the real output there is a metal sill at Y=1.125.
            // The tiny clean-water tongue starts ABOVE it; the main basin
            // continues down to the ceramic floor at Y=1.014.
            boolean onInletThroat = dirty && extendsIntoInlet &&
                    Math.min(a[0],p[0])>=DIRTY_INLET.minX()-0.0001F &&
                    Math.max(a[0],p[0])<=DIRTY_INLET.maxX()+0.0001F &&
                    Math.min(a[1],p[1])<DIRTY_INLET.maxZ()-0.0001F;
            boolean onOutletTongue = !dirty && Math.max(a[0],p[0]) >
                    CLEAN_EAST_EDGE + 0.0001F;
            float bottom = onInletThroat ? DIRTY_INLET_SILL :
                    onOutletTongue ? CLEAN_OUTLET_SILL : POOL_BOTTOM;
            if (top<=bottom) continue;
            float v0=v(sprite,0F),v1=v(sprite,top-bottom);
            for (int tile=(int)Math.floor(low);tile<Math.ceil(high);tile++) {
                float lo=Math.max(tile,low),hi=Math.min(tile+1F,high);
                if (hi<=lo) continue;
                float start=from<to?lo:hi,end=from<to?hi:lo;
                float x0=alongX?start:a[0],z0=alongX?a[1]:start;
                float x1=alongX?end:a[0],z1=alongX?a[1]:end;
                quad(consumer,matrix,x0,bottom,z0,x1,bottom,z1,
                        x1,top,z1,x0,top,z0,
                        u(sprite,start-tile),u(sprite,end-tile),v0,v1,
                        sr,sg,sb,sideAlpha,light,nx,0F,nz);
            }
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
