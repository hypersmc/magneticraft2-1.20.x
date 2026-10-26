package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeMechanicalLatheBlock;
import com.magneticraft2.client.animation.MachineVisualRenderer;
import com.magneticraft2.client.animation.AnimationDefinition;
import com.magneticraft2.client.animation.AnimationFrame;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeMechanicalLatheBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import com.magneticraft2.common.registry.registers.BlockRegistry;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import java.util.EnumSet;
import java.util.Map;

/** V2 static frame, formed module gearbox, rotating arbor and cross-fed tool.
 * All visible drive axes share the current Gear V2 network rotation. */
public final class BronzeMechanicalLatheRenderer
        implements BlockEntityRenderer<BronzeMechanicalLatheBlockEntity> {
    private static final ResourceLocation CARRIAGE =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_lathe_carriage");
    private static final ResourceLocation CROSS_SLIDE =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_lathe_cross_slide");
    private static final ResourceLocation INPUT_FRAME =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_lathe_input_frame");
    private static final ResourceLocation INPUT_ROTOR =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_lathe_input_rotor");
    private static final ResourceLocation INPUT_OUTPUT_ROTOR =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_lathe_input_output_rotor");
    private static final ResourceLocation ARBOR =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_lathe_arbor");
    private static final ResourceLocation SPINDLE =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_lathe_spindle");
    private static final ResourceLocation CHUCK =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_lathe_chuck");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation("magneticraft2", "bronze_mechanical_lathe");
    public BronzeMechanicalLatheRenderer(BlockEntityRendererProvider.Context ignored){}

    private static void model(PoseStack pose,MultiBufferSource source,int light,int overlay,
                              BakedModel model) {
        BlockRenderDispatcher blocks=Minecraft.getInstance().getBlockRenderer();
        blocks.getModelRenderer().renderModel(pose.last(),source.getBuffer(RenderType.cutout()),
                BlockRegistry.BRONZE_MECHANICAL_LATHE.get().defaultBlockState(),
                model,1F,1F,1F,light,overlay);
    }
    @Override public int getViewDistance(){return 64;}
    @Override public void render(BronzeMechanicalLatheBlockEntity lathe,float partial,
            PoseStack pose,MultiBufferSource source,int light,int overlay){
        if(!lathe.isFormed())return;
        BlockState state=lathe.getBlockState();
        Direction facing=state.getValue(BronzeMechanicalLatheBlock.FACING);
        float yaw=switch(facing){
            case WEST->-90F;case NORTH->180F;case EAST->90F;default->0F;
        };
        pose.pushPose();
        pose.translate(.5D,0,.5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D,0,-.5D);
        float inputAngle=lathe.getInputRotation(partial);
        float angle=lathe.getSpindleRotation(partial);
        ItemRenderer items=Minecraft.getInstance().getItemRenderer();

        // The south blueprint places the formed (hidden) Gear V2 module two
        // cells behind the controller, centered at x=8, y=8, z=-24 pixels.
        // The two real shaft axes meet INSIDE this gearbox: incoming Z, output X.
        pose.pushPose();
        pose.translate(0D, 0D, -2D);
        model(pose,source,light,overlay,
                Minecraft.getInstance().getModelManager().getModel(INPUT_FRAME));
        pose.popPose();

        // One coherent machine sample drives every JSON component. A pack can
        // add children, alternate models and state-conditional decorations
        // without introducing any more hardcoded draw calls.
        EnumSet<AnimationDefinition.State> states = EnumSet.of(lathe.isRunning()
                ? AnimationDefinition.State.RUNNING : AnimationDefinition.State.STOPPED);
        if (lathe.isInputOverloadedForAnimation()) {
            states.add(AnimationDefinition.State.OVERLOADED);
        }
        if (lathe.isCutterWornForAnimation()) {
            states.add(AnimationDefinition.State.WORN);
        }
        AnimationFrame frame = new AnimationFrame(
                Map.of("mechanical_input", inputAngle, "spindle", angle), states);
        AnimationDefinition drawn = MachineVisualRenderer.render(
                ANIMATION, frame, state, pose, source, light, overlay);

        // Backward compatibility: the already-shipped single-rotor resource
        // pack omits input_output_rotor, so only that part uses legacy Java.
        // An invalid model aborts the entire JSON scene BEFORE drawing, then
        // both legacy rotor paths are used.
        if (drawn == null || drawn.component("input_rotor") == null) {
            pose.pushPose();
            pose.translate(0D, 0D, -2D);
            pose.translate(.5D, .5D, .5D);
            pose.mulPose(Axis.ZP.rotationDegrees(inputAngle));
            pose.translate(-.5D, -.5D, -.5D);
            model(pose,source,light,overlay,
                    Minecraft.getInstance().getModelManager().getModel(INPUT_ROTOR));
            pose.popPose();
        }

        if (drawn == null || drawn.component("input_output_rotor") == null) {
            pose.pushPose();
            pose.translate(0D, 0D, -2D);
            pose.translate(.5D,.5D,.5D);
            pose.mulPose(Axis.XP.rotationDegrees(-inputAngle));
            pose.translate(-.5D,-.5D,-.5D);
            model(pose,source,light,overlay,
                    Minecraft.getInstance().getModelManager().getModel(INPUT_OUTPUT_ROTOR));
            pose.popPose();
        }

        // The continuous spindle is seated inside the LEFT headstock bearing;
        // the clutch stops it in place when machining is idle.
        pose.pushPose();
        pose.translate(-4.5D/16D,22D/16D,-10D/16D);
        pose.mulPose(Axis.XP.rotationDegrees(angle));
        pose.translate(-.5D,-.5D,-.5D);
        model(pose,source,light,overlay,
                Minecraft.getInstance().getModelManager().getModel(CHUCK));
        model(pose,source,light,overlay,
                Minecraft.getInstance().getModelManager().getModel(SPINDLE));
        pose.popPose();

        ItemStack mounted=lathe.getOutput().isEmpty()
                ?lathe.getWorkpiece():lathe.getOutput();
        if(!mounted.isEmpty()){
            // Retaining collars appear only with a mounted casting.
            // The main spindle itself is always rendered with the chuck.
            pose.pushPose();
            pose.translate(-4.5D/16D,22D/16D,-10D/16D);
            pose.mulPose(Axis.XP.rotationDegrees(angle));
            pose.translate(-.5D,-.5D,-.5D);
            model(pose,source,light,overlay,
                    Minecraft.getInstance().getModelManager().getModel(ARBOR));
            pose.popPose();
            pose.pushPose();
            pose.translate(3D/16D,22D/16D,-10D/16D);
            pose.mulPose(Axis.XP.rotationDegrees(angle));
            pose.mulPose(Axis.ZP.rotationDegrees(90F));
            pose.scale(.52F,.52F,.52F);
            BakedModel workModel=items.getModel(mounted,lathe.getLevel(),null,883);
            items.render(mounted,ItemDisplayContext.NONE,false,pose,source,light,overlay,workModel);
            pose.popPose();
        }

        float feed=lathe.getToolFeed();
        float traverse=lathe.getCutTraverse();
        float cutDepth=lathe.getCuttingDepthPixels();

        // Bed saddle: longitudinal X travel only, so its runners remain
        // centered on the two guide rails for the entire machining cycle.
        pose.pushPose();
        // Start the tool near the chuck at the beginning of the cutting phase;
        // traverse only the mounted casting, not the empty bed.
        double saddleX=(-3D-2.5D*traverse)/16D;
        pose.translate(saddleX,17D/16D,-6D/16D);
        model(pose,source,light,overlay,
                Minecraft.getInstance().getModelManager().getModel(CARRIAGE));

        // Cross-slide travels radially toward the spindle, independently of
        // the bed saddle. The cutter shares this transform and stays clamped.
        pose.pushPose();
        // Geared castings need less inward travel than narrow bearings.
        // Only the upper slide feeds in Z; the bed saddle remains on rails.
        pose.translate(0D,0D,-cutDepth*feed/16D);
        model(pose,source,light,overlay,
                Minecraft.getInstance().getModelManager().getModel(CROSS_SLIDE));
        ItemStack bit=lathe.getCutter();
        if(!bit.isEmpty()){
            pose.pushPose();
            // Resting bit is withdrawn from the workpiece; the controlled
            // cross feed brings its edge in rather than cutting through it.
            pose.translate(7D/16D,7D/16D,1D/16D);
            pose.mulPose(Axis.XP.rotationDegrees(-90F));
            pose.scale(.28F,.28F,.28F);
            BakedModel bitModel=items.getModel(bit,lathe.getLevel(),null,884);
            items.render(bit,ItemDisplayContext.NONE,false,
                    pose,source,light,overlay,bitModel);
            pose.popPose();
        }
        pose.popPose(); // cross-slide
        pose.popPose(); // saddle
        pose.popPose();
    }
}
