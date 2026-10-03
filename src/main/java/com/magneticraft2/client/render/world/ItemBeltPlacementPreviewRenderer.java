package com.magneticraft2.client.render.world;

import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
import com.magneticraft2.common.item.stage.copper.ItemBeltItem;
import com.magneticraft2.common.systems.GEAR.BeltPath;
import com.magneticraft2.common.systems.GEAR.ItemBeltConnectionManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * World-space construction guide for Item Belt placement.
 *
 * Instead of copying Create's ghost-belt presentation, Magneticraft uses a pair
 * of narrow guide rails with regular cross marks. Green means the exact path is
 * placeable, amber means the geometry is valid but the player lacks segments,
 * and red means the path itself is invalid or blocked.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MOD_ID)
public final class ItemBeltPlacementPreviewRenderer {
    private static final float VALID_R = 0.30F;
    private static final float VALID_G = 0.95F;
    private static final float VALID_B = 0.42F;

    private static final float MISSING_R = 1.00F;
    private static final float MISSING_G = 0.72F;
    private static final float MISSING_B = 0.18F;

    private static final float INVALID_R = 0.95F;
    private static final float INVALID_G = 0.22F;
    private static final float INVALID_B = 0.18F;

    private static final float ALPHA = 0.90F;
    private static final double GUIDE_HALF_WIDTH = 0.31D;
    private static final double ENDPOINT_INFLATE = 0.035D;

    private ItemBeltPlacementPreviewRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        ItemStack beltStack = selectedBeltStack(player, level);
        if (beltStack == null) {
            return;
        }

        BlockPos startPos = ItemBeltItem.getSelectedStart(
                beltStack,
                level
        );
        if (startPos == null
                || !(level.getBlockEntity(startPos)
                instanceof ConveyorRollerBlockEntity startRoller)) {
            return;
        }

        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHit)
                || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos endPos = blockHit.getBlockPos();
        if (!(level.getBlockEntity(endPos)
                instanceof ConveyorRollerBlockEntity endRoller)) {
            return;
        }

        ItemBeltItem.PlacementCheck check =
                ItemBeltItem.evaluateConnection(
                        level,
                        startRoller,
                        endRoller
                );

        boolean enoughSegments = player.getAbilities().instabuild
                || check.layout() == null
                || beltStack.getCount()
                >= check.layout().requiredSegments();

        PreviewColor color;
        if (!check.valid()) {
            color = new PreviewColor(
                    INVALID_R,
                    INVALID_G,
                    INVALID_B
            );
        } else if (!enoughSegments) {
            color = new PreviewColor(
                    MISSING_R,
                    MISSING_G,
                    MISSING_B
            );
        } else {
            color = new PreviewColor(
                    VALID_R,
                    VALID_G,
                    VALID_B
            );
        }

        PoseStack pose = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        MultiBufferSource.BufferSource bufferSource =
                minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer =
                bufferSource.getBuffer(RenderType.LINES);

        pose.pushPose();
        pose.translate(
                -cameraPos.x,
                -cameraPos.y,
                -cameraPos.z
        );

        renderEndpoint(
                pose,
                consumer,
                startPos,
                color,
                true
        );
        renderEndpoint(
                pose,
                consumer,
                endPos,
                color,
                false
        );

        BeltPath path = null;
        if (startPos != endPos
                && startRoller.getGearAxis()
                != Direction.Axis.Y
                && startRoller.getGearAxis()
                == endRoller.getGearAxis()) {
            path = BeltPath.create(
                    startPos,
                    endPos,
                    startRoller.getGearAxis(),
                    startRoller.getRollerRadius(),
                    endRoller.getRollerRadius()
            );
        }

        if (path != null) {
            renderGuidePath(
                    pose,
                    consumer,
                    path,
                    color
            );
        } else {
            renderFallbackGuide(
                    pose,
                    consumer,
                    startPos,
                    endPos,
                    startRoller.getGearAxis(),
                    color
            );
        }

        pose.popPose();
    }

    private static ItemStack selectedBeltStack(LocalPlayer player,
                                               ClientLevel level) {
        ItemStack main = player.getMainHandItem();
        if (ItemBeltItem.getSelectedStart(main, level) != null) {
            return main;
        }

        ItemStack offhand = player.getOffhandItem();
        if (ItemBeltItem.getSelectedStart(offhand, level) != null) {
            return offhand;
        }

        return null;
    }

    private static void renderGuidePath(PoseStack pose,
                                        VertexConsumer consumer,
                                        BeltPath path,
                                        PreviewColor color) {
        for (BeltPath.Segment segment : path.segments()) {
            Vec3 width = segment.widthDirection()
                    .normalize()
                    .scale(GUIDE_HALF_WIDTH);

            Vec3 leftFrom = segment.from().subtract(width);
            Vec3 leftTo = segment.to().subtract(width);
            Vec3 rightFrom = segment.from().add(width);
            Vec3 rightTo = segment.to().add(width);

            drawLine(
                    pose,
                    consumer,
                    leftFrom,
                    leftTo,
                    color,
                    ALPHA
            );
            drawLine(
                    pose,
                    consumer,
                    rightFrom,
                    rightTo,
                    color,
                    ALPHA
            );

            if (segment.type() == BeltPath.SegmentType.STRAIGHT) {
                renderCrossMarks(
                        pose,
                        consumer,
                        segment,
                        color
                );
            }
        }
    }

    private static void renderCrossMarks(PoseStack pose,
                                         VertexConsumer consumer,
                                         BeltPath.Segment segment,
                                         PreviewColor color) {
        Vec3 delta = segment.to().subtract(segment.from());
        double length = delta.length();
        if (length < 0.0001D) {
            return;
        }

        Vec3 direction = delta.scale(1.0D / length);
        Vec3 width = segment.widthDirection()
                .normalize()
                .scale(GUIDE_HALF_WIDTH);

        // One cross mark roughly every half block gives the guide a mechanical
        // "construction ruler" look without pretending the final belt already exists.
        int marks = Math.max(1, (int) Math.floor(length * 2.0D));
        for (int i = 0; i <= marks; i++) {
            double distance = length * i / marks;
            Vec3 center = segment.from()
                    .add(direction.scale(distance));

            drawLine(
                    pose,
                    consumer,
                    center.subtract(width),
                    center.add(width),
                    color,
                    ALPHA * 0.70F
            );
        }
    }

    private static void renderFallbackGuide(
            PoseStack pose,
            VertexConsumer consumer,
            BlockPos start,
            BlockPos end,
            Direction.Axis rollerAxis,
            PreviewColor color) {
        Vec3 from = Vec3.atCenterOf(start);
        Vec3 to = Vec3.atCenterOf(end);
        Vec3 delta = to.subtract(from);

        if (delta.lengthSqr() < 0.0001D) {
            return;
        }

        Vec3 widthDirection = axisVector(rollerAxis);
        if (widthDirection.lengthSqr() < 0.0001D) {
            widthDirection = new Vec3(1.0D, 0.0D, 0.0D);
        }

        Vec3 width = widthDirection.normalize()
                .scale(GUIDE_HALF_WIDTH);

        drawLine(
                pose,
                consumer,
                from.subtract(width),
                to.subtract(width),
                color,
                ALPHA
        );
        drawLine(
                pose,
                consumer,
                from.add(width),
                to.add(width),
                color,
                ALPHA
        );

        int marks = Math.max(
                1,
                (int) Math.ceil(from.distanceTo(to) * 2.0D)
        );
        for (int i = 0; i <= marks; i++) {
            Vec3 center = from.lerp(
                    to,
                    i / (double) marks
            );
            drawLine(
                    pose,
                    consumer,
                    center.subtract(width),
                    center.add(width),
                    color,
                    ALPHA * 0.70F
            );
        }
    }

    private static void renderEndpoint(PoseStack pose,
                                       VertexConsumer consumer,
                                       BlockPos pos,
                                       PreviewColor color,
                                       boolean start) {
        AABB box = new AABB(pos)
                .inflate(ENDPOINT_INFLATE);

        float alpha = start
                ? ALPHA * 0.72F
                : ALPHA;

        LevelRenderer.renderLineBox(
                pose,
                consumer,
                box,
                color.r(),
                color.g(),
                color.b(),
                alpha
        );
    }

    private static void drawLine(PoseStack pose,
                                 VertexConsumer consumer,
                                 Vec3 from,
                                 Vec3 to,
                                 PreviewColor color,
                                 float alpha) {
        PoseStack.Pose current = pose.last();

        consumer.vertex(
                        current.pose(),
                        (float) from.x,
                        (float) from.y,
                        (float) from.z
                )
                .color(
                        color.r(),
                        color.g(),
                        color.b(),
                        alpha
                )
                .normal(current.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        consumer.vertex(
                        current.pose(),
                        (float) to.x,
                        (float) to.y,
                        (float) to.z
                )
                .color(
                        color.r(),
                        color.g(),
                        color.b(),
                        alpha
                )
                .normal(current.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static Vec3 axisVector(Direction.Axis axis) {
        if (axis == null) {
            return Vec3.ZERO;
        }

        return switch (axis) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };
    }

    private record PreviewColor(float r,
                                float g,
                                float b) {
    }
}
