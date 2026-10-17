package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeStampMillBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeStampMillBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Three eccentric stamp heads, one shared Gear V2 phase, no per-tick baked
 * V2 geometry regeneration. World/guide frame is rendered by V2 fillers.
 */
public final class BronzeStampMillRenderer implements BlockEntityRenderer<BronzeStampMillBlockEntity> {
    private static final ResourceLocation HAMMER =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_hammer");
    private static final ResourceLocation WORN_HAMMER =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_hammer_worn");
    private static final ResourceLocation MODULE =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_input_frame");
    private static final ResourceLocation CAM =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_cam");

    private static final ResourceLocation DRIVE_SHAFT =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_shaft");
    private static final ResourceLocation SHAFT_TIP =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_shaft_tip");
    private static final ResourceLocation BEVEL =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_bevel");
    private static final ResourceLocation PULLEY =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_pulley");
    private static final ResourceLocation BELT_STRAIGHT =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_belt_straight");
    private static final ResourceLocation BELT_WRAP =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_belt_wrap");
    private static final ResourceLocation BEARING =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_bearing");
    private static final ResourceLocation COLLAR =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_collar");

    private static void part(PoseStack pose,MultiBufferSource buffers,int light,int overlay,
                             ResourceLocation model,double x,double y,double z,
                             Axis axis,float degrees) {
        pose.pushPose();
        pose.translate(x,y,z);
        if(axis!=null)pose.mulPose(axis.rotationDegrees(degrees));
        pose.translate(-.5D,-.5D,-.5D);
        renderModel(pose,buffers,light,overlay,
                Minecraft.getInstance().getModelManager().getModel(model));
        pose.popPose();
    }

    public BronzeStampMillRenderer(BlockEntityRendererProvider.Context context) {}
    private static void renderModel(PoseStack pose,MultiBufferSource buffers,int light,int overlay,BakedModel model) {
        BlockRenderDispatcher blocks=Minecraft.getInstance().getBlockRenderer();
        blocks.getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.cutout()),
                BlockRegistry.BRONZE_STAMP_MILL.get().defaultBlockState(),model,
                1F,1F,1F,light,overlay);
    }
    @Override public int getViewDistance(){return 64;}
    @Override public void render(BronzeStampMillBlockEntity mill,float partial,PoseStack pose,
                                 MultiBufferSource buffers,int light,int overlay) {
        if(!mill.isFormed())return;
        BlockState state=mill.getBlockState();
        Direction facing=state.getValue(BronzeStampMillBlock.FACING);
        float yaw=switch(facing) {
            case WEST -> -90F;case NORTH -> 180F;case EAST -> 90F;default -> 0F;
        };
        pose.pushPose();
        pose.translate(.5D,0D,.5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D,0D,-.5D);

        // The actual formed input module at (0,0,-2) is not a filler.
        // Render a compact bearing casing right over its hidden block state.
        pose.pushPose();
        pose.translate(0D,0D,-2D);
        renderModel(pose,buffers,light,overlay,
                Minecraft.getInstance().getModelManager().getModel(MODULE));
        pose.popPose();

        float shaftAngle=mill.getDriveRotationDegrees(partial);

        // Physical transmission, from the real rear-center Gear V2 shaft:
        // rear Z shaft -> bevel pair -> X crossshaft -> right-side twin
        // pulleys and vertical belt -> upper X crankshaft. Both 1:1 bevel
        // and pulley stages share Gear V2's speed; the Z axis reverses
        // direction at the bevel pair. No independent animation clock.
        // The input rotor and rear bevel live inside the functional input
        // module at (0,0,-2), without baking static faces into that cell.
        part(pose,buffers,light,overlay,DRIVE_SHAFT,
                .5D,.5D,-1.50D,Axis.ZP,shaftAngle);
        part(pose,buffers,light,overlay,BEVEL,
                .5D,.5D,-1.125D,Axis.ZP,shaftAngle);
        part(pose,buffers,light,overlay,BEVEL,
                .5D,.5D,-.875D,Axis.XP,-shaftAngle);
        // The transverse drive line follows X beneath the supported trough.
        // It remains outside all three front-facing functional item ports.
        for(int station=-1;station<=2;station++){
            part(pose,buffers,light,overlay,DRIVE_SHAFT,
                    station+.5D,.5D,-.875D,Axis.XP,-shaftAngle);
        }
        // Distinct bearing collar makes the drive actually look attached.
        part(pose,buffers,light,overlay,COLLAR,
                2.55D,.5D,-.875D,null,0F);
        // Right-side belt train: lower pulley on crossshaft, upper pulley
        // on the crankshaft, with nonrotating taut belt flights.
        part(pose,buffers,light,overlay,PULLEY,
                2.97D,.5D,-.625D,Axis.XP,-shaftAngle);
        part(pose,buffers,light,overlay,PULLEY,
                2.97D,2.40625D,-.625D,Axis.XP,-shaftAngle);
        // Belt flights run from the lower pulley to the upper pulley.
        for(int segment=0;segment<2;segment++){
            part(pose,buffers,light,overlay,BELT_STRAIGHT,
                    2.97D,.5D+segment,-.625D,null,0F);
        }
        part(pose,buffers,light,overlay,BELT_WRAP,
                2.97D,.5D,-.625D,Axis.XP,-shaftAngle);
        part(pose,buffers,light,overlay,BELT_WRAP,
                2.97D,2.40625D,-.625D,Axis.XP,-shaftAngle);
        // A continuous rotating shaft is visible between all four
        // original fixed pillow bearings; unlike the old static mesh.
        for(int station=-2;station<=2;station++){
            part(pose,buffers,light,overlay,DRIVE_SHAFT,
                    station+.5D,2.40625D,-.625D,Axis.XP,-shaftAngle);
        }

        BakedModel stamp=Minecraft.getInstance().getModelManager().getModel(
                mill.getPartWearStage()>=2?WORN_HAMMER:HAMMER);
        BakedModel cam=Minecraft.getInstance().getModelManager().getModel(CAM);
        // Each hammer is centered over one complete 1x1 I/O module pocket.
        int[] stations={-8,8,24};
        for(int i=0;i<stations.length;i++) {
            // The narrow shoe fits BETWEEN the bronze trough lips and
            // rides inside both vertically anchored guide rails.
            pose.pushPose();
            pose.translate((stations[i]-8D)/16D,
                    24D/16D+mill.getHammerLift(partial,i),
                    (-10D-8D)/16D);
            renderModel(pose,buffers,light,overlay,stamp);
            pose.popPose();

            // One crank per stamp station; phase stagger matches hammer travel.
            pose.pushPose();
            pose.translate(stations[i]/16D,38.5D/16D,-10D/16D);
            pose.mulPose(Axis.XP.rotationDegrees(shaftAngle+120F*i));
            pose.translate(-.5D,-.5D,-.5D);
            renderModel(pose,buffers,light,overlay,cam);
            pose.popPose();
        }
        ItemRenderer items=Minecraft.getInstance().getItemRenderer();
        ItemStack feed=mill.getInputStack();
        if(!feed.isEmpty()){
            pose.pushPose();
            // Display incoming ore on top of the actual crushing plate,
            // not below the tray inside the support frame.
            pose.translate(.5D,24D/16D,-10D/16D);
            pose.scale(.55F,.55F,.55F);
            BakedModel model=items.getModel(feed,mill.getLevel(),null,721);
            items.render(feed,ItemDisplayContext.GROUND,false,pose,buffers,light,overlay,model);
            pose.popPose();
        }
        pose.popPose();
    }
}
