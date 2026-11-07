package com.magneticraft2.client.render.blocks.stage.iron;
import com.magneticraft2.client.render.blocks.stage.copper.FlywheelMesh;
import com.magneticraft2.client.render.blocks.stage.copper.FlywheelBlockEntity_woodRenderer;
import com.magneticraft2.common.blockentity.stage.iron.WroughtIronFlywheelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 2.4-block-diameter wheel: one BER / one Gear V2 node, native-density UVs. */
public final class WroughtIronFlywheelRenderer
        implements BlockEntityRenderer<WroughtIronFlywheelBlockEntity> {
    private static final ResourceLocation IRON=new ResourceLocation(
            "magneticraft2","textures/block/material/dark_work_iron.png");
    private static final ResourceLocation BRONZE=new ResourceLocation(
            "magneticraft2","textures/block/material/bronze_machined.png");
    private static final int SEGMENTS=24,SPOKES=8;
    private static final double RADIUS=1.15D;
    public WroughtIronFlywheelRenderer(BlockEntityRendererProvider.Context ctx){}
    @Override public void render(WroughtIronFlywheelBlockEntity wheel,float partial,
            PoseStack stack,MultiBufferSource buffers,int light,int overlay) {
        if(!wheel.isFormed())return;
        stack.pushPose();
        stack.translate(0.5D,0.5D,0.5D);
        FlywheelBlockEntity_woodRenderer.orientLocalXToAxis(stack,wheel.getGearAxis());
        stack.mulPose(Axis.XP.rotationDegrees(wheel.getVisualRotationDegrees(partial)));
        VertexConsumer iron=buffers.getBuffer(RenderType.entityCutoutNoCull(IRON));
        VertexConsumer bearing=buffers.getBuffer(RenderType.entityCutoutNoCull(BRONZE));
        FlywheelMesh.drawBox(stack,iron,light,0.92D,0.125D,0.125D,true);
        for(int i=0;i<SPOKES;i++){
            stack.pushPose();
            stack.mulPose(Axis.XP.rotationDegrees(i*360F/SPOKES));
            stack.translate(0D,0.54D,0D);
            FlywheelMesh.drawBox(stack,iron,light,0.15D,1.08D,0.12D,true);
            stack.popPose();
        }
        for(int i=0;i<SEGMENTS;i++){
            double theta=(i+0.5D)*Math.PI*2D/SEGMENTS;
            stack.pushPose();
            stack.translate(0D,Math.cos(theta)*RADIUS,Math.sin(theta)*RADIUS);
            stack.mulPose(Axis.XP.rotationDegrees((float)Math.toDegrees(theta)));
            FlywheelMesh.drawBox(stack,iron,light,0.23D,0.17D,0.32D,true);
            stack.popPose();
        }
        for(int i=0;i<SPOKES;i++){
            double theta=i*Math.PI*2D/SPOKES;
            stack.pushPose();
            stack.translate(0D,Math.cos(theta)*RADIUS,Math.sin(theta)*RADIUS);
            stack.mulPose(Axis.XP.rotationDegrees((float)Math.toDegrees(theta)));
            FlywheelMesh.drawBox(stack,iron,light,0.30D,0.20D,0.30D,true);
            stack.popPose();
        }
        FlywheelMesh.drawBox(stack,bearing,light,0.31D,0.37D,0.37D,true);
        stack.popPose();
    }
}
