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
    private static final ResourceLocation STRIKE_PLATE =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_strike_plate");
    private static final ResourceLocation CAM =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_cam");
    // Only the legacy EXTERIOR trim. The reusable formed input module owns
    // the moving wood rotor and bronze bearing faces inside this block.
    private static final ResourceLocation INPUT_FRAME_DETAIL =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_input_frame_detail");

    // Render-only powertrain, mechanically driven by the formed input module.
    // X shafts and Z input each have correctly aligned baked geometry.
    private static final ResourceLocation DRIVE_SHAFT =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_shaft");
    private static final ResourceLocation DRIVE_SHAFT_START =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_shaft_start");
    private static final ResourceLocation SHAFT_TIP =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_shaft_tip");
    private static final ResourceLocation INPUT_SHAFT =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_input_shaft");
    private static final ResourceLocation BEVEL_Z =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_bevel");
    private static final ResourceLocation BEVEL_X =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_bevel_x");

    private static final ResourceLocation INPUT_SPINDLE =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_input_spindle");
    private static final ResourceLocation INPUT_COUPLER =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_input_coupler");
    private static final ResourceLocation LOWER_SPINDLE =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_lower_spindle");
    private static final ResourceLocation GEARBOX_FRAME =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_gearbox_frame");
    private static final ResourceLocation CROSS_BEARING =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_cross_bearing");
    private static final ResourceLocation SIDE_LOWER_MOUNT =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_side_lower_mount");
    private static final ResourceLocation PULLEY =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_pulley");
    private static final ResourceLocation BELT_STRAIGHT =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_belt_straight");
    private static final ResourceLocation BELT_UPPER =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_belt_upper");
    private static final ResourceLocation BELT_WRAP =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_belt_wrap");

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

        // Restore the Stamp Mill's accepted corner posts and dark iron
        // input-frame rails, clipped to clear the actual module housing.
        // The real module BER still owns all central bronze bearing and
        // moving wooden-rotor geometry: no duplicate surfaces.
        pose.pushPose();
        pose.translate(0D,0D,-2D);
        renderModel(pose,buffers,light,overlay,
                Minecraft.getInstance().getModelManager().getModel(INPUT_FRAME_DETAIL));
        pose.popPose();

        // The formed input module renders its own wood shaft and bearing.
        // Only the drive extension OUTSIDE its real 1x1 block is rendered here.
        final float inputAngle=mill.getStampInputRotationDegrees(partial);
        final float driveAngle=mill.getStampCrankRotationDegrees(partial);
        // All coordinates are in controller-relative model pixels.
        // Two shaft axes intersect at (8,8,-10), but the actual steel
        // spindles STOP short of each other and exchange torque through
        // two 8-tooth bronze bevel wheels in the forward (+X,+Z) quadrant.
        // Both teeth and gearbox brackets are drawn separately from V2.
        final double inputX=8D/16D, inputY=8D/16D;
        final double lowerZ=-10D/16D, upperY=38.5D/16D;
        final double pulleyX=47.2D/16D;
        // Input is synchronized with the real module's +world-axis rotor.
        // At the (+X,+Z) tooth contact, X and Z opposite angular signs
        // give matching tangential velocities (a true 1:1 bevel drive).


        // Static bronze/wood gearbox is connected to the frame's two
        // underside girders, with midshaft and side bearing supports.
        part(pose,buffers,light,overlay,GEARBOX_FRAME,
                inputX,inputY,lowerZ,null,0F);
        part(pose,buffers,light,overlay,CROSS_BEARING,
                25D/16D,inputY,lowerZ,null,0F);
        part(pose,buffers,light,overlay,SIDE_LOWER_MOUNT,
                pulleyX,inputY,lowerZ,null,0F);

        // Wood-to-iron coupling at the front of the ACTUAL module cell.
        // Its hollow center leaves the thin rotating steel extension clear.
        part(pose,buffers,light,overlay,INPUT_COUPLER,
                inputX,inputY,-16D/16D,Axis.ZP,inputAngle);
        // Steel begins at z=-16, and never occupies the formed input block.
        part(pose,buffers,light,overlay,INPUT_SHAFT,
                inputX,inputY,-18D/16D,Axis.ZP,inputAngle);
        part(pose,buffers,light,overlay,INPUT_SPINDLE,
                inputX,inputY,lowerZ,Axis.ZP,inputAngle);
        // Both gears share the same theoretical axes intersection as
        // their rotation pivot. Their tooth rings are deliberately
        // OFFSET along their shafts and meet with small clearance.
        part(pose,buffers,light,overlay,BEVEL_Z,
                inputX,inputY,lowerZ,Axis.ZP,inputAngle);
        part(pose,buffers,light,overlay,BEVEL_X,
                inputX,inputY,lowerZ,Axis.XP,driveAngle);

        // X output hub starts at x=10.5px (not x=8), so the perpendicular
        // input shaft never intersects it. Segments join at x=24 and x=40;
        // the last tip ends at the outside pulley x=47.2.
        part(pose,buffers,light,overlay,LOWER_SPINDLE,
                16D/16D,inputY,lowerZ,Axis.XP,driveAngle);
        part(pose,buffers,light,overlay,DRIVE_SHAFT,
                32D/16D,inputY,lowerZ,Axis.XP,driveAngle);
        part(pose,buffers,light,overlay,SHAFT_TIP,
                40D/16D,inputY,lowerZ,Axis.XP,driveAngle);

        // Wide, dark, closed leather loop wrapped over smooth bronze-rimmed
        // wooden drums. The lower/upper centers (8,38.5) and shaft phases
        // are unchanged. Leather does not rotate as a rigid pulley mesh;
        // the two half-rounds remain fixed while both drums spin.
        part(pose,buffers,light,overlay,PULLEY,
                pulleyX,inputY,lowerZ,Axis.XP,driveAngle);
        part(pose,buffers,light,overlay,PULLEY,
                pulleyX,upperY,lowerZ,Axis.XP,driveAngle);
        part(pose,buffers,light,overlay,BELT_STRAIGHT,
                pulleyX,8D/16D,lowerZ,null,0F);
        part(pose,buffers,light,overlay,BELT_UPPER,
                pulleyX,24D/16D,lowerZ,null,0F);
        // The lower half-round uses the model's upper semicircle flipped.
        part(pose,buffers,light,overlay,BELT_WRAP,
                pulleyX,inputY,lowerZ,Axis.XP,180F);
        part(pose,buffers,light,overlay,BELT_WRAP,
                pulleyX,upperY,lowerZ,null,0F);

        // One continuous drive axis through all four fixed crank pillows.
        // Four 16px lengths [-24..40] and a short [40..47.2] end spindle.
        // Every upper shaft segment and eccentric uses the signed output
        // phase of the bevel pair, with no extra reversal in the open belt.
        for(int x=-16;x<=32;x+=16){
            // The left exposed end is capped once; shared segment joints
            // have NO coplanar end faces, avoiding moving axle flicker.
            part(pose,buffers,light,overlay,x==-16?DRIVE_SHAFT_START:DRIVE_SHAFT,
                    x/16D,upperY,lowerZ,Axis.XP,driveAngle);
        }
        part(pose,buffers,light,overlay,SHAFT_TIP,
                40D/16D,upperY,lowerZ,Axis.XP,driveAngle);

        BakedModel stamp=Minecraft.getInstance().getModelManager().getModel(
                mill.getPartWearStage()>=2?WORN_HAMMER:HAMMER);
        BakedModel cam=Minecraft.getInstance().getModelManager().getModel(CAM);
        // Each hammer is centered over one complete 1x1 I/O module pocket.
        int[] stations={-8,8,24};
        for(int i=0;i<stations.length;i++) {
            // Three industrial wear plates make the crushing stations
            // legible. Their upper surfaces remain below stopped hammer
            // clearance: y=23.05..23.58 under the shoe at y=24.
            part(pose,buffers,light,overlay,STRIKE_PLATE,
                    stations[i]/16D,23D/16D,-10D/16D,null,0F);
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
            pose.mulPose(Axis.XP.rotationDegrees(driveAngle+120F*i));
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
