package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzePrecisionBoringMachineBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzePrecisionBoringMachineBlockEntity;
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
 * Only two short supported shaft sections and the clamped workpiece are
 * rendered dynamically. They use cached baked models; no entities or
 * per-frame voxel geometry. The REAL rear mechanical input's formed block
 * is invisible, hence a small shaft coupling is rendered in its cell.
 */
public final class BronzePrecisionBoringMachineRenderer
        implements BlockEntityRenderer<BronzePrecisionBoringMachineBlockEntity> {
    private static final ResourceLocation COUPLING =
            new ResourceLocation("magneticraft2", "multiblock/bronze_precision_boring_machine_coupling");
    private static final ResourceLocation SPINDLE =
            new ResourceLocation("magneticraft2", "multiblock/bronze_precision_boring_machine_spindle");

    public BronzePrecisionBoringMachineRenderer(BlockEntityRendererProvider.Context ignored) {}

    @Override public int getViewDistance() { return 64; }

    @Override public void render(BronzePrecisionBoringMachineBlockEntity borer, float partial,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!borer.isFormed()) return;
        Direction facing = borer.getBlockState().getValue(BronzePrecisionBoringMachineBlock.FACING);
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

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher blocks = mc.getBlockRenderer();
        BlockState state = BlockRegistry.BRONZE_PRECISION_BORING_MACHINE.get().defaultBlockState();
        float inputPhase = borer.getBoringSpindleAngle(partial);

        // Actual rear Gear V2 input cell: local z=-32..-16, axis y=8px.
        // The static housed gear train at z=-16 lifts the tool axis to y=20px.
        drawShaft(pose, buffers, blocks, mc.getModelManager().getModel(COUPLING),
                state, inputPhase, 8D, -24D, light, overlay);
        // One gear pair reverses rotation. The tool occupies z=-16..0 and
        // terminates immediately behind the stationary clamped casting.
        drawShaft(pose, buffers, blocks, mc.getModelManager().getModel(SPINDLE),
                state, -inputPhase, 20D, -8D, light, overlay);

        ItemStack casting = borer.getWorkpiece();
        if (!casting.isEmpty()) {
            ItemRenderer items = mc.getItemRenderer();
            pose.pushPose();
            // The rough bearing is held UP at the boring axis, NOT floating
            // above the machine. Its front vice and cradle are static V2 parts.
            pose.translate(.5D, 20D / 16D, 4D / 16D);
            pose.mulPose(Axis.XP.rotationDegrees(90F));
            pose.scale(.58F, .58F, .58F);
            items.render(casting, ItemDisplayContext.NONE, false,
                    pose, buffers, light, overlay,
                    items.getModel(casting, borer.getLevel(), null, 417));
            pose.popPose();
        }
        pose.popPose();
    }

    private static void drawShaft(PoseStack pose, MultiBufferSource buffers,
            BlockRenderDispatcher blocks, BakedModel model, BlockState state,
            float angle, double centerYpx, double centerZpx, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5D, centerYpx / 16D, centerZpx / 16D);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-.5D, -.5D, -.5D);
        blocks.getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()), state, model,
                1F, 1F, 1F, light, overlay);
        pose.popPose();
    }
}
