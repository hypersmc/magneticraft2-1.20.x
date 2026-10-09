package com.magneticraft2.client.render.blocks.stage.stone;

import com.magneticraft2.common.block.stage.stone.Primitive_anvilBlock;
import com.magneticraft2.common.blockentity.stage.stone.Primitive_anvilEntity;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PrimitiveAnvilBlockEntityRenderer implements BlockEntityRenderer<Primitive_anvilEntity> {
    public PrimitiveAnvilBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Primitive_anvilEntity blockEntity,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight,
                       int packedOverlay) {
        ItemStack stack = blockEntity.getStoredItem();
        if (stack.isEmpty()) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, blockEntity.getLevel(), null, 0);

        Direction facing = blockEntity.getBlockState().hasProperty(Primitive_anvilBlock.FACING)
                ? blockEntity.getBlockState().getValue(Primitive_anvilBlock.FACING)
                : Direction.SOUTH;

        boolean finishedPlate = stack.is(ItemRegistry.ITEM_COPPER_PLATE.get());

        poseStack.pushPose();
        poseStack.translate(0.5D, finishedPlate ? 1.015D : 1.028D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingYaw(facing)));

        if (finishedPlate) {
            // The finished plate has a small 3D model authored flat in the XZ plane.
            // Keep it low and broad so the visual reads as a hammered workpiece.
            poseStack.scale(0.62F, 0.62F, 0.62F);
        } else if (model.isGui3d()) {
            poseStack.scale(0.44F, 0.44F, 0.44F);
        } else {
            // Generated items such as vanilla ingots are authored in the XY plane.
            // Lay them flat on the anvil face.
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.50F, 0.50F, 0.50F);
        }

        itemRenderer.render(
                stack,
                ItemDisplayContext.NONE,
                false,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                model
        );

        poseStack.popPose();
    }

    private static float getFacingYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case EAST -> 270.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }
}
