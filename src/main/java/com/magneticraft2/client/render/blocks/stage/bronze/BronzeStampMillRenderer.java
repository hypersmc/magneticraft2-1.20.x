package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeStampMillBlock;
import com.magneticraft2.client.animation.MachineVisualRenderer;
import com.magneticraft2.client.animation.AnimatedBeltLoopRenderer;
import com.magneticraft2.client.animation.AnimatedComponentRenderer;
import com.magneticraft2.client.animation.AnimationDefinition;
import com.magneticraft2.client.animation.AnimationFrame;
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
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Three eccentric stamp heads, one shared Gear V2 phase, no per-tick baked
 * V2 geometry regeneration. World/guide frame is rendered by V2 fillers.
 */
public final class BronzeStampMillRenderer implements BlockEntityRenderer<BronzeStampMillBlockEntity> {
    private static final ResourceLocation ANIMATION =
            new ResourceLocation("magneticraft2", "bronze_stamp_mill");
    // Preserve v1 resource packs containing only the original 18 drivetrain
    // parts. Those packs still render the whole drive in JSON and fall back
    // to Java for ALL stamp heads. Never combine a partial JSON stamp head
    // with a full legacy stamp head, which would duplicate the mechanism.
    private static final Set<String> DRIVE_PARTS = Set.of(
            "gearbox_frame", "cross_bearing", "side_lower_mount",
            "input_coupler", "input_shaft", "input_spindle", "input_bevel",
            "output_bevel", "lower_spindle", "lower_drive_shaft",
            "lower_shaft_tip", "lower_pulley", "upper_pulley",
            "upper_shaft_start", "upper_shaft_zero",
            "upper_shaft_middle", "upper_shaft_end", "upper_shaft_tip");
    private static final Set<String> HEAD_PARTS = Set.of(
            "plate_left", "plate_middle", "plate_right",
            "cam_left", "cam_middle", "cam_right",
            "hammer_left", "hammer_middle", "hammer_right");
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
    // Weak keys avoid retaining unloaded machines. One phase tracker per
    // typed physical input keeps belt UVs continuous across 0/360 wraps.
    // Resource reload changes the animation definition identity, resetting
    // the tracked phase rather than mixing old and new pack contracts.
    private final Map<BronzeStampMillBlockEntity, BeltVisualState> beltTravel =
            new WeakHashMap<>();
    private final BronzeStampMillOreVisuals oreVisuals =
            new BronzeStampMillOreVisuals();

    private static final class BeltVisualState {
        private AnimationDefinition lastScene;
        private final Map<String, BeltTravel> drives = new HashMap<>();

        void scene(AnimationDefinition scene) {
            if (lastScene != scene) {
                lastScene = scene;
                drives.clear();
            }
        }

        double phase(String input, float visualAngle) {
            return drives.computeIfAbsent(input, ignored -> new BeltTravel())
                    .sampleDegrees(visualAngle);
        }
    }

    private static final class BeltTravel {
        private boolean initialized;
        private float previousAngle;
        private double cumulativeDegrees;

        private double sampleDegrees(float angle) {
            if (!initialized) {
                initialized = true;
                previousAngle = angle;
                cumulativeDegrees = angle;
            } else {
                float delta = (angle - previousAngle) % 360F;
                if (delta > 180F) delta -= 360F;
                else if (delta < -180F) delta += 360F;
                previousAngle = angle;
                // Signed, unwrapped DOUBLE shaft phase. The JSON belt's
                // own driving radius turns this into surface distance;
                // no additional clock and no cumulative float precision
                // loss when a world runs for many hours.
                cumulativeDegrees += delta;
            }
            return cumulativeDegrees;
        }
    }

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


        // All 27 moving/static mechanical scene parts now come from JSON.
        // Gear V2 still supplies the two facing-correct shaft phases. The
        // three cams use 0/120/240-degree track phase offsets; the hammers
        // use reusable cosine linear tracks with the ORIGINAL 0.32-block lift.
        // Belt UVs and in-world ore rendering remain specialized Java paths.
        AnimationDefinition definition = MachineVisualRenderer.definition(ANIMATION);
        AnimatedComponentRenderer.SceneResult drawn = null;
        boolean allHeads = definition != null
                && definition.componentNames().containsAll(HEAD_PARTS);
        boolean noHeads = definition != null
                && Collections.disjoint(definition.componentNames(), HEAD_PARTS);
        if (definition != null
                && definition.componentNames().containsAll(DRIVE_PARTS)
                && (allHeads || noHeads)) {
            EnumSet<AnimationDefinition.State> states = EnumSet.of(
                    mill.isProcessing()
                            ? AnimationDefinition.State.RUNNING
                            : AnimationDefinition.State.STOPPED);
            if (mill.needsMaintenance()) {
                states.add(AnimationDefinition.State.WORN);
            }
            drawn = MachineVisualRenderer.renderScene(ANIMATION,
                    new AnimationFrame(Map.of(
                            "mechanical_input", inputAngle,
                            "output_shaft", driveAngle), states,
                            Map.of("stamp_mill.wear_stage",
                                    Math.max(0, Math.min(5, mill.getPartWearStage())))),
                    state, pose, buffers, light, overlay);
        }

        if (drawn == null) {
            // Exact pre-JSON fallback for the whole drivetrain.
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

        // Bronze-rimmed drums rotate with Gear V2.
        part(pose,buffers,light,overlay,PULLEY,
                pulleyX,inputY,lowerZ,Axis.XP,driveAngle);
        part(pose,buffers,light,overlay,PULLEY,
                pulleyX,upperY,lowerZ,Axis.XP,driveAngle);
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

        }

        // Open belt loops now use the same JSON scene and real shaft inputs.
        // Paths are built by BeltPath on pack reload, not during rendering.
        // Old 18-part packs without belt_loops keep their original leather
        // renderer, including its UV tiling, phase and no-stretch behavior.
        AnimationFrame physicalFrame = new AnimationFrame(
                Map.of("mechanical_input", inputAngle,
                        "output_shaft", driveAngle),
                EnumSet.of(mill.isProcessing()
                        ? AnimationDefinition.State.RUNNING
                        : AnimationDefinition.State.STOPPED),
                Map.of("stamp_mill.wear_stage",
                        Math.max(0, Math.min(5, mill.getPartWearStage()))));
        AnimationDefinition beltScene = drawn == null ? null : drawn.definition();
        BeltVisualState driveState = beltTravel.computeIfAbsent(
                mill, ignored -> new BeltVisualState());
        driveState.scene(beltScene);
        Map<String, Float> inputAngles = new HashMap<>();
        boolean useJsonBelt = beltScene != null && !beltScene.beltLoops().isEmpty();
        if (useJsonBelt) {
            // Validate every loop BEFORE emitting any belt vertices.
            for (AnimationDefinition.BeltLoop loop : beltScene.beltLoops().values()) {
                Float angle = MachineVisualRenderer.physicalAngle(
                        ANIMATION, beltScene, loop.input(), physicalFrame);
                if (angle == null || !Float.isFinite(angle)) {
                    useJsonBelt = false;
                    break;
                }
                inputAngles.put(loop.input(), angle);
            }
        }
        if (useJsonBelt) {
            Map<String, Double> unwrapped = new HashMap<>();
            for (Map.Entry<String, AnimationDefinition.BeltLoop> entry
                    : beltScene.beltLoops().entrySet()) {
                AnimationDefinition.BeltLoop loop = entry.getValue();
                double phase = unwrapped.computeIfAbsent(loop.input(),
                        name -> driveState.phase(name, inputAngles.get(name)));
                AnimatedBeltLoopRenderer.render(loop, phase, pose, buffers, light);
            }
        } else {
            // Full, byte-for-byte-compatible legacy belt surface geometry
            // and texture repeat when an old pack omits belt definitions.
            double phase = driveState.phase("output_phase", driveAngle);
            BronzeStampMillBeltRenderer.render(pose, buffers, light,
                    BronzeStampMillBeltRenderer.distanceForDegrees(phase) % 1.0D);
        }


        if (drawn == null || !allHeads) {
            // Original Java path for the three cams/hammer heads and wear
            // plates. This fallback is used together with the old drivetrain
            // whenever the selected JSON scene or required model fails.
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
        } // end complete legacy stamp-head fallback
        // Port-aligned feed, progressive crushing and two real output
        // chutes. The existing functional item ports continue owning
        // all inventory state; no client-side item transport is invented.
        // An older drivetrain-only pack still uses Java hammer travel for
        // ore/dust effects; new full scenes share the sampled JSON travel.
        oreVisuals.render(mill,partial,pose,buffers,light,overlay,
                allHeads ? drawn : null);
        pose.popPose();
    }
}
