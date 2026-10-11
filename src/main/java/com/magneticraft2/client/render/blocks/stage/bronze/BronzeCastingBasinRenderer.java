package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.blockentity.stage.bronze.BronzeCastingBasinBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Static basin is chunk-baked. Only mold, pouring and output are dynamic. */
public final class BronzeCastingBasinRenderer
        implements BlockEntityRenderer<BronzeCastingBasinBlockEntity> {
    private static final ResourceLocation BRONZE =
            new ResourceLocation("minecraft","block/raw_copper_block");

    public BronzeCastingBasinRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(BronzeCastingBasinBlockEntity basin,float partialTicks,
                       PoseStack pose,MultiBufferSource sources,int light,int overlay) {
        ItemStack result=basin.displayOutput();
        if (!result.isEmpty()) {
            renderItem(basin,result,.50D,.72D,.50D,.57F,991,pose,sources,light,overlay);
        } else if (basin.isCooling()) {
            // Low, flat surface inside the pan, not a full glowing block.
            TextureAtlasSprite sprite=Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(BRONZE);
            float progress=1F-basin.getDirectFillTicks()
                    /(float) BronzeCastingBasinBlockEntity.DIRECT_FILL_DURATION;
            float extent=basin.getDirectFillTicks()>0
                    ? 1.25F+2.25F*Math.max(0F,progress) : 3.5F;
            float x0=(8F-extent)/16F,x1=(8F+extent)/16F;
            float z0=(8F-extent)/16F,z1=(8F+extent)/16F,y=7.5F/16F;
            VertexConsumer buf=sources.getBuffer(RenderType.translucent());
            PoseStack.Pose m=pose.last();
            float size=extent*2F;
            quad(buf,m,x0,y,z0,sprite.getU(0),sprite.getV(0));
            quad(buf,m,x0,y,z1,sprite.getU(0),sprite.getV(size));
            quad(buf,m,x1,y,z1,sprite.getU(size),sprite.getV(size));
            quad(buf,m,x1,y,z0,sprite.getU(size),sprite.getV(0));
        } else {
            renderItem(basin,basin.displayMold(),.50D,.59D,.50D,.55F,992,
                    pose,sources,light,overlay);
        }
    }

    private static void quad(VertexConsumer v,PoseStack.Pose pose,
                             float x,float y,float z,float u,float texV) {
        v.vertex(pose.pose(),x,y,z).color(1F,.55F,.18F,1F).uv(u,texV)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0)
                .normal(pose.normal(),0,1,0).endVertex();
    }

    private static void renderItem(BronzeCastingBasinBlockEntity basin,ItemStack stack,
                                   double x,double y,double z,float scale,int seed,
                                   PoseStack pose,MultiBufferSource sources,int light,int overlay) {
        if(stack.isEmpty())return;
        ItemStack display=stack.copy();
        display.setCount(1);
        pose.pushPose();
        pose.translate(x,y,z);
        ItemRenderer renderer=Minecraft.getInstance().getItemRenderer();
        BakedModel model=renderer.getModel(display,basin.getLevel(),null,seed);
        pose.scale(scale,scale,scale);
        renderer.render(display,ItemDisplayContext.NONE,false,pose,sources,light,overlay,model);
        pose.popPose();
    }
}
