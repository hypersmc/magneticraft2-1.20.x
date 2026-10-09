package com.magneticraft2.client.render.world;

import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import com.magneticraft2.common.item.stage.copper.ItemBeltItem;
import com.magneticraft2.common.item.stage.copper.LeatherBeltItem;
import com.magneticraft2.common.systems.GEAR.BeltPath;
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
 * Magneticraft belt-placement construction guide.
 *
 * This intentionally does not render a ghost copy of the final belt. Instead it
 * draws two "survey rails" with regular cross marks along the proposed route:
 *
 * green = placeable now
 * amber = valid route, but not enough belt segments
 * red   = invalid/blocked route
 *
 * Both the Leather Drive Belt and the wide Item Belt use the same visual
 * language while keeping their own placement rules.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MOD_ID)
public final class BeltPlacementPreviewRenderer {
    private static final PreviewColor VALID =
            new PreviewColor(0.30F, 0.95F, 0.42F);
    private static final PreviewColor MISSING =
            new PreviewColor(1.00F, 0.72F, 0.18F);
    private static final PreviewColor INVALID =
            new PreviewColor(0.95F, 0.22F, 0.18F);

    private static final float ALPHA = 0.90F;
    private static final double ITEM_GUIDE_HALF_WIDTH = 0.31D;
    private static final double DRIVE_GUIDE_HALF_WIDTH = 0.11D;
    private static final double ENDPOINT_INFLATE = 0.035D;

    private BeltPlacementPreviewRenderer() {
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

        Preview preview = buildPreview(
                minecraft,
                level,
                player
        );
        if (preview == null) {
            return;
        }

        PreviewColor color = !preview.valid()
                ? INVALID
                : preview.enoughSegments()
                ? VALID
                : MISSING;

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
                preview.start(),
                color,
                true
        );
        renderEndpoint(
                pose,
                consumer,
                preview.end(),
                color,
                false
        );

        if (preview.path() != null) {
            renderGuidePath(
                    pose,
                    consumer,
                    preview.path(),
                    preview.guideHalfWidth(),
                    color
            );
        } else {
            renderFallbackGuide(
                    pose,
                    consumer,
                    preview.start(),
                    preview.end(),
                    preview.axis(),
                    preview.guideHalfWidth(),
                    color
            );
        }

        pose.popPose();
    }

    private static Preview buildPreview(Minecraft minecraft,
                                        ClientLevel level,
                                        LocalPlayer player) {
        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHit)
                || hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        ItemStack main = player.getMainHandItem();
        Preview mainPreview = buildPreviewForStack(
                level,
                player,
                main,
                blockHit.getBlockPos()
        );
        if (mainPreview != null) {
            return mainPreview;
        }

        return buildPreviewForStack(
                level,
                player,
                player.getOffhandItem(),
                blockHit.getBlockPos()
        );
    }

    private static Preview buildPreviewForStack(ClientLevel level,
                                                LocalPlayer player,
                                                ItemStack stack,
                                                BlockPos endPos) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        BlockPos itemStart = ItemBeltItem.getSelectedStart(
                stack,
                level
        );
        if (itemStart != null) {
            return buildItemBeltPreview(
                    level,
                    player,
                    stack,
                    itemStart,
                    endPos
            );
        }

        BlockPos leatherStart = LeatherBeltItem.getSelectedStart(
                stack,
                level
        );
        if (leatherStart != null) {
            return buildLeatherBeltPreview(
                    level,
                    player,
                    stack,
                    leatherStart,
                    endPos
            );
        }

        return null;
    }

    private static Preview buildItemBeltPreview(
            ClientLevel level,
            LocalPlayer player,
            ItemStack stack,
            BlockPos startPos,
            BlockPos endPos) {
        if (!(level.getBlockEntity(startPos)
                instanceof ConveyorRollerBlockEntity start)
                || !(level.getBlockEntity(endPos)
                instanceof ConveyorRollerBlockEntity end)) {
            return null;
        }

        ItemBeltItem.PlacementCheck check =
                ItemBeltItem.evaluateConnection(
                        level,
                        start,
                        end
                );

        int requiredSegments = check.layout() == null
                ? 0
                : check.layout().requiredSegments();

        boolean enough = player.getAbilities().instabuild
                || requiredSegments == 0
                || stack.getCount() >= requiredSegments;

        BeltPath path = createPathWhenPossible(
                startPos,
                endPos,
                start.getGearAxis(),
                end.getGearAxis(),
                start.getRollerRadius(),
                end.getRollerRadius()
        );

        return new Preview(
                startPos,
                endPos,
                start.getGearAxis(),
                path,
                ITEM_GUIDE_HALF_WIDTH,
                check.valid(),
                enough
        );
    }

    private static Preview buildLeatherBeltPreview(
            ClientLevel level,
            LocalPlayer player,
            ItemStack stack,
            BlockPos startPos,
            BlockPos endPos) {
        if (!(level.getBlockEntity(startPos)
                instanceof PulleyBlockEntity_wood start)
                || !(level.getBlockEntity(endPos)
                instanceof PulleyBlockEntity_wood end)) {
            return null;
        }

        LeatherBeltItem.PlacementCheck check =
                LeatherBeltItem.evaluateConnection(
                        level,
                        start,
                        end
                );

        int requiredSegments = LeatherBeltItem.requiredSegments(
                startPos,
                endPos
        );
        boolean enough = player.getAbilities().instabuild
                || stack.getCount() >= requiredSegments;

        BeltPath path = createPathWhenPossible(
                startPos,
                endPos,
                start.getGearAxis(),
                end.getGearAxis(),
                start.getPulleyRadius(),
                end.getPulleyRadius()
        );

        return new Preview(
                startPos,
                endPos,
                start.getGearAxis(),
                path,
                DRIVE_GUIDE_HALF_WIDTH,
                check.valid(),
                enough
        );
    }

    private static BeltPath createPathWhenPossible(
            BlockPos start,
            BlockPos end,
            Direction.Axis startAxis,
            Direction.Axis endAxis,
            double startRadius,
            double endRadius) {
        if (start.equals(end)
                || startAxis == null
                || startAxis != endAxis) {
            return null;
        }

        return BeltPath.create(
                start,
                end,
                startAxis,
                startRadius,
                endRadius
        );
    }

    private static void renderGuidePath(PoseStack pose,
                                        VertexConsumer consumer,
                                        BeltPath path,
                                        double guideHalfWidth,
                                        PreviewColor color) {
        for (BeltPath.Segment segment : path.segments()) {
            Vec3 width = segment.widthDirection()
                    .normalize()
                    .scale(guideHalfWidth);

            drawLine(
                    pose,
                    consumer,
                    segment.from().subtract(width),
                    segment.to().subtract(width),
                    color,
                    ALPHA
            );
            drawLine(
                    pose,
                    consumer,
                    segment.from().add(width),
                    segment.to().add(width),
                    color,
                    ALPHA
            );

            if (segment.type() == BeltPath.SegmentType.STRAIGHT) {
                renderCrossMarks(
                        pose,
                        consumer,
                        segment,
                        guideHalfWidth,
                        color
                );
            }
        }
    }

    private static void renderCrossMarks(PoseStack pose,
                                         VertexConsumer consumer,
                                         BeltPath.Segment segment,
                                         double guideHalfWidth,
                                         PreviewColor color) {
        Vec3 delta = segment.to().subtract(segment.from());
        double length = delta.length();
        if (length < 0.0001D) {
            return;
        }

        Vec3 direction = delta.scale(1.0D / length);
        Vec3 width = segment.widthDirection()
                .normalize()
                .scale(guideHalfWidth);

        int marks = Math.max(
                1,
                (int) Math.floor(length * 2.0D)
        );

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
            Direction.Axis axis,
            double guideHalfWidth,
            PreviewColor color) {
        Vec3 from = Vec3.atCenterOf(start);
        Vec3 to = Vec3.atCenterOf(end);
        if (from.distanceToSqr(to) < 0.0001D) {
            return;
        }

        Vec3 widthDirection = axisVector(axis);
        Vec3 width = widthDirection
                .normalize()
                .scale(guideHalfWidth);

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
        AABB box = new AABB(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() + 1.0D,
                pos.getY() + 1.0D,
                pos.getZ() + 1.0D
        ).inflate(ENDPOINT_INFLATE);

        LevelRenderer.renderLineBox(
                pose,
                consumer,
                box,
                color.r(),
                color.g(),
                color.b(),
                start ? ALPHA * 0.72F : ALPHA
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
                .normal(
                        current.normal(),
                        0.0F,
                        1.0F,
                        0.0F
                )
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
                .normal(
                        current.normal(),
                        0.0F,
                        1.0F,
                        0.0F
                )
                .endVertex();
    }

    private static Vec3 axisVector(Direction.Axis axis) {
        if (axis == null) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }

        return switch (axis) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };
    }

    private record Preview(
            BlockPos start,
            BlockPos end,
            Direction.Axis axis,
            BeltPath path,
            double guideHalfWidth,
            boolean valid,
            boolean enoughSegments) {
    }

    private record PreviewColor(float r,
                                float g,
                                float b) {
    }
}
