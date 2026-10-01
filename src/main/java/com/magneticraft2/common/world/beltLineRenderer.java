package com.magneticraft2.common.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 11-01-2024
 * @Project mgc2-1.20
* @version 1.0.0
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MOD_ID)
public class beltLineRenderer {

    // --- Tuning ---
    private static final double MIN_RADIUS = 3.5;      // blocks; lower allows tighter turns
    private static final double HANDLE_SCALE = 0.5;    // control handle length = HANDLE_SCALE * distance, clamped [2..12]
    private static final int   MIN_SAMPLES  = 16;
    private static final int   MAX_SAMPLES  = 160;
    private static final float LINE_ALPHA   = 0.85f;   // visibility
    private static final float LINE_WIDTH   = 0.045f;  // fake thickness (draws multiple lines offset)

    // Simple record for curve evaluation
    private record CurveSample(Vec3 p, Vec3 t) {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Draw after translucent so it sits on top of the world nicely
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || stack.getTag() == null || !stack.getTag().contains("connectedbelt")) return;

        BlockPos start = BlockPos.of(stack.getTag().getLong("connectedbelt"));
        BlockPos end   = player.blockPosition();

        // You can replace these with actual block facings from your belt blocks
        Direction startFacing = Direction.NORTH;             // TODO: real facing from your start block, if needed
        Vec3 startDir = Vec3.atLowerCornerOf(startFacing.getNormal());
        Vec3 endDir   = player.getLookAngle();               // Or match startFacing for straight runs

        BeltPreview preview = new BeltPreview(level, start, end, startDir, endDir, MIN_RADIUS);

        // Nothing to draw?
        if (preview.points.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();

        pose.pushPose();
        pose.translate(-camPos.x, -camPos.y, -camPos.z);

        float r = preview.valid ? 0.30f : 0.95f;
        float g = preview.valid ? 0.95f : 0.25f;
        float b = 0.30f;
        float a = LINE_ALPHA;

        // Render a "thicker" polyline by drawing a few offset lines
        renderThickPolyline(event, pose, preview.points, LINE_WIDTH, r, g, b, a);

        // Optional: an AABB guide for quick visual bounds (comment out if not wanted)
        //drawBounds(event, pose, preview.bounds, r, g, b, 0.25f);

        pose.popPose();
    }

    // ---------------------------
    // Preview builder and math
    // ---------------------------
    private static final class BeltPreview {
        final List<Vec3> points = new ArrayList<>();
        final AABB bounds;
        final boolean valid;

        BeltPreview(Level level, BlockPos start, BlockPos end, Vec3 startDir, Vec3 endDir, double minRadius) {
            Vec3 P0 = Vec3.atCenterOf(start).add(0, 0.25, 0);
            Vec3 P3 = Vec3.atCenterOf(end).add(0, 0.25, 0);

            double dist = P0.distanceTo(P3);
            if (dist < 0.001) {
                this.bounds = new AABB(P0, P3);
                this.valid = false;
                return;
            }

            double d = Mth.clamp(dist * HANDLE_SCALE, 2.0, 12.0);
            Vec3 P1 = P0.add(startDir.normalize().scale(d));
            Vec3 P2 = P3.subtract(endDir.normalize().scale(d));

            int N = Mth.clamp((int)(dist * 6), MIN_SAMPLES, MAX_SAMPLES);

            // sample curve
            List<Vec3> tmp = new ArrayList<>(N + 1);
            for (int i = 0; i <= N; i++) {
                double u = i / (double) N;
                tmp.add(bezier(P0, P1, P2, P3, u).p());
            }
            this.points.addAll(tmp);

            // compute bounds
            AABB bb = new AABB(P0, P3);
            for (Vec3 p : tmp) bb = bb.minmax(new AABB(p, p));
            this.bounds = bb.inflate(0.5);

            // curvature validation
            boolean ok = true;
            for (int i = 1; i < tmp.size() - 1; i++) {
                double kappa = curvature(tmp.get(i - 1), tmp.get(i), tmp.get(i + 1)); // 1/R
                if (kappa > 1.0 / minRadius) { ok = false; break; }
            }
            // quick collision validation (skip every other sample)
            if (ok) {
                for (int i = 0; i < tmp.size(); i += 2) {
                    BlockPos p = BlockPos.containing(tmp.get(i));
                    if (!level.isEmptyBlock(p) && !level.getBlockState(p).canBeReplaced()) { ok = false; break; }
                }
            }
            this.valid = ok;
        }
    }

    private static CurveSample bezier(Vec3 P0, Vec3 P1, Vec3 P2, Vec3 P3, double u) {
        double v = 1.0 - u;
        double b0 = v * v * v;
        double b1 = 3 * v * v * u;
        double b2 = 3 * v * u * u;
        double b3 = u * u * u;
        Vec3 p = new Vec3(
                b0 * P0.x + b1 * P1.x + b2 * P2.x + b3 * P3.x,
                b0 * P0.y + b1 * P1.y + b2 * P2.y + b3 * P3.y,
                b0 * P0.z + b1 * P1.z + b2 * P2.z + b3 * P3.z
        );
        // derivative (not used here directly, but handy if you want arrowheads)
        Vec3 t = new Vec3(
                3 * v * v * (P1.x - P0.x) + 6 * v * u * (P2.x - P1.x) + 3 * u * u * (P3.x - P2.x),
                3 * v * v * (P1.y - P0.y) + 6 * v * u * (P2.y - P1.y) + 3 * u * u * (P3.y - P2.y),
                3 * v * v * (P1.z - P0.z) + 6 * v * u * (P2.z - P1.z) + 3 * u * u * (P3.z - P2.z)
        );
        return new CurveSample(p, t);
    }

    // Discrete curvature: κ ≈ Δθ / Δs across three points
    private static double curvature(Vec3 a, Vec3 b, Vec3 c) {
        Vec3 v1 = b.subtract(a);
        Vec3 v2 = c.subtract(b);
        double s1 = v1.length();
        double s2 = v2.length();
        if (s1 < 1e-4 || s2 < 1e-4) return 0.0;
        double cos = v1.normalize().dot(v2.normalize());
        cos = Mth.clamp(cos, -1.0, 1.0);
        double dTheta = Math.acos(cos);
        double ds = 0.5 * (s1 + s2);
        return ds < 1e-4 ? 0.0 : dTheta / ds; // κ = 1/R
    }

    // ---------------------------
    // Rendering helpers
    // ---------------------------

    /** Draw a thicker-looking polyline by drawing multiple offset lines around the center line. */
    private static void renderThickPolyline(RenderLevelStageEvent event, PoseStack pose, List<Vec3> pts,
                                            float halfWidth, float r, float g, float b, float a) {
        // Core center line
        renderPolylineLines(event, pose, pts, r, g, b, a);

        // Two slight offsets (left/right) perpendicular to world up to fake thickness
        // This keeps it simple without custom shaders.
        final Vec3 UP = new Vec3(0, 1, 0);
        List<Vec3> left = new ArrayList<>(pts.size());
        List<Vec3> right = new ArrayList<>(pts.size());

        for (int i = 0; i < pts.size(); i++) {
            Vec3 p = pts.get(i);
            // tangent approx using neighbors
            Vec3 t;
            if (i == 0) t = pts.get(1).subtract(p);
            else if (i == pts.size() - 1) t = p.subtract(pts.get(i - 1));
            else t = pts.get(i + 1).subtract(pts.get(i - 1));
            t = t.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : t.normalize();
            Vec3 side = t.cross(UP);
            if (side.lengthSqr() < 1e-6) side = new Vec3(1, 0, 0); // fallback if parallel with UP
            side = side.normalize().scale(halfWidth);

            left.add(p.add(side));
            right.add(p.subtract(side));
        }

        float sideAlpha = a * 0.8f;
        renderPolylineLines(event, pose, left,  r, g, b, sideAlpha);
        renderPolylineLines(event, pose, right, r, g, b, sideAlpha);
    }

    /** Render simple line segments through the polyline points. */
    private static void renderPolylineLines(RenderLevelStageEvent event, PoseStack pose, List<Vec3> pts,
                                            float r, float g, float b, float a) {
        if (pts.size() < 2) return;

        // Optional: quick frustum in WORLD space (no extra camera translate!)
        AABB worldBounds = aabbOf(pts).inflate(0.25);
        if (!event.getFrustum().isVisible(worldBounds)) return;
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bufferSource.getBuffer(RenderType.LINES);
        PoseStack.Pose last = pose.last();

        for (int i = 0; i < pts.size() - 1; i++) {
            Vec3 p0 = pts.get(i);
            Vec3 p1 = pts.get(i + 1);

            vc.vertex(last.pose(), (float)p0.x, (float)p0.y, (float)p0.z)
                    .color(r, g, b, a)
                    .normal(last.normal(), 0, 1, 0)
                    .endVertex();

            vc.vertex(last.pose(), (float)p1.x, (float)p1.y, (float)p1.z)
                    .color(r, g, b, a)
                    .normal(last.normal(), 0, 1, 0)
                    .endVertex();
        }
    }

    private static void drawBounds(RenderLevelStageEvent event, PoseStack pose, AABB box,
                                   float r, float g, float b, float a) {
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bufferSource.getBuffer(RenderType.LINES);
        LevelRenderer.renderLineBox(pose, vc, box, r, g, b, a);
    }


    private static AABB aabbOf(List<Vec3> pts) {
        AABB bb = new AABB(pts.get(0), pts.get(0));

        for (Vec3 p : pts) bb = bb.minmax(new AABB(p, p));
        return bb;
    }
}