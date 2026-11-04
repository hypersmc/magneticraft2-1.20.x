package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.client.animation.AnimationDefinition;
import com.magneticraft2.client.animation.AnimationFrame;
import com.magneticraft2.client.animation.AnimatedComponentRenderer;
import com.magneticraft2.client.animation.MachineVisualRenderer;
import com.magneticraft2.common.block.stage.bronze.BronzeForgeHammerBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeForgeHammerBlockEntity;
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

import java.util.Map;
import java.util.Set;

/** Only the ram and current workpiece are dynamic. V2 renders the fixed frame. */
public final class BronzeForgeHammerRenderer
        implements BlockEntityRenderer<BronzeForgeHammerBlockEntity> {
    private static final ResourceLocation RAM =
            new ResourceLocation("magneticraft2", "multiblock/bronze_forge_hammer_ram");
    private static final ResourceLocation WORKPIECE_VISUAL =
            new ResourceLocation("magneticraft2", "bronze_forge_hammer");
    private static final Set<AnimationDefinition.State> RUNNING =
            Set.of(AnimationDefinition.State.RUNNING);
    private static final Set<AnimationDefinition.State> STOPPED =
            Set.of(AnimationDefinition.State.STOPPED);

    public BronzeForgeHammerRenderer(BlockEntityRendererProvider.Context ignored) {}

    @Override public int getViewDistance() { return 64; }

    @Override public void render(BronzeForgeHammerBlockEntity hammer, float partial,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!hammer.isFormed()) return;
        Direction facing = hammer.getBlockState().getValue(BronzeForgeHammerBlock.FACING);
        float yaw = switch (facing) {
            case WEST -> -90F;
            case NORTH -> 180F;
            case EAST -> 90F;
            default -> 0F;
        };
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D, 0D, -.5D);

        // One die, one Gear V2-driven vertical stroke. The model is registered
        // at resource load and remains fixed across frames; no voxel rebuild.
        pose.pushPose();
        pose.translate(0D, (32D - 10D * hammer.getHammerStroke(partial)) / 16D, -1D);
        BakedModel ram = Minecraft.getInstance().getModelManager().getModel(RAM);
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        dispatcher.getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()),
                BlockRegistry.BRONZE_FORGE_HAMMER.get().defaultBlockState(),
                ram, 1F, 1F, 1F, light, overlay);
        pose.popPose();

        ItemStack workpiece = hammer.getWorkpiece();
        // Scene-driven real ItemStack rendering is opt-in. The unchanged
        // original Java workpiece is the complete fallback on missing or
        // invalid pack scenes; the ram stays the accepted Gear V2 renderer.
        AnimationFrame frame = new AnimationFrame(
                Map.of(), hammer.isRunning() ? RUNNING : STOPPED, Map.of(),
                Map.of("process", hammer.getVisualProgress(partial)),
                Map.of("workpiece", workpiece,
                       "output", hammer.getFinishedWorkpiece()));
        AnimatedComponentRenderer.SceneResult scene =
                MachineVisualRenderer.renderScene(WORKPIECE_VISUAL, frame,
                        hammer.getBlockState(), pose, buffers, light, overlay);
        AnimationDefinition.Component itemComponent = scene == null
                ? null : scene.definition().component("workpiece");
        boolean jsonItem = itemComponent != null && itemComponent.itemVisual() != null;
        if (!jsonItem && !workpiece.isEmpty()) {
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            pose.pushPose();
            // The center of the 5x4x3 anvil, NOT the controller/port surface.
            pose.translate(8D/16D, 24.5D/16D, -9D/16D);
            pose.mulPose(Axis.XP.rotationDegrees(90F));
            pose.scale(.42F,.42F,.42F);
            itemRenderer.render(workpiece, ItemDisplayContext.NONE, false,
                    pose, buffers, light, overlay,
                    itemRenderer.getModel(workpiece, hammer.getLevel(), null, 889));
            pose.popPose();
        }
        pose.popPose();
    }
}
