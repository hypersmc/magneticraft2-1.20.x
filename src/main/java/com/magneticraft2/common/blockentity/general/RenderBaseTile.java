package com.magneticraft2.common.blockentity.general;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * @author JumpWatch on 01-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public abstract class RenderBaseTile<T extends BlockEntity> implements BlockEntityRenderer<T> {
    public double xPos = 0D;
    public double zPos = 0D;

    public RenderBaseTile(BlockEntityRendererProvider.Context rendererDispatcherIn) {}
    @Override
    public abstract void render(T te, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay);
}
