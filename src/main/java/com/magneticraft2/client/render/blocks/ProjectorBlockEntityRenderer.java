package com.magneticraft2.client.render.blocks;

import com.magneticraft2.common.blockentity.general.projectortestBlockEntity;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.Blueprint.json.Blueprint;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintData;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintRegistry;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

import static com.magneticraft2.common.block.general.BaseBlockMagneticraft2.FACING;

/**
 * @author JumpWatch on 17-08-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class ProjectorBlockEntityRenderer implements BlockEntityRenderer<projectortestBlockEntity> {
    private static final Logger LOGGER = LogManager.getLogger("Projectorrender");
    private static final float SQUARE_SIZE = 5.0f; // Size of the projected square
    private static final float SQUARE_OFFSET = 0.5f - SQUARE_SIZE / 2.0f; // Offset from block center
    private static final int NO_OVERLAY = OverlayTexture.NO_OVERLAY;

    private final Minecraft minecraft = Minecraft.getInstance();
    public ProjectorBlockEntityRenderer(BlockEntityRendererProvider.Context context){

    }
    @Override
    public void render(projectortestBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        Direction blockFacing = pBlockEntity.getBlockState().getValue(FACING);
        int offset = (int) (SQUARE_OFFSET + 1); // Offset by 1 block away
        Vec3 projectionOffset = Vec3.ZERO;

        if (blockFacing == Direction.EAST) {
            projectionOffset = new Vec3(-offset , 0, -0.5);
        } else if (blockFacing == Direction.NORTH) {
            projectionOffset = new Vec3(-0.5, 0, -offset);
        } else if (blockFacing == Direction.SOUTH) {
            projectionOffset = new Vec3(-0.5, 0, offset - 1);
        } else if (blockFacing == Direction.WEST) {
            projectionOffset = new Vec3(offset -1, 0, -0.5);
        }

        // Get the blueprint
        Blueprint blueprint = BlueprintRegistry.getRegisteredBlueprint("magneticraft2", pBlockEntity.getBlueprint());
        if (blueprint != null) {
            pBlockEntity.setInvalidBlueprint(false);

            BlueprintStructure structure = blueprint.getStructure();
            Map<String, List<List<String>>> layout = structure.getLayout();
            int[] dimensions = structure.getDimensions();

            // Initialize the base translation position
            float baseX = (float) projectionOffset.x();
            float baseY = (float) projectionOffset.y();
            float baseZ = (float) projectionOffset.z();

            // Render the blueprint blocks
            for (int layerIndex = 0; layerIndex < dimensions[2]; layerIndex++) {
                String layerName = "layer" + (layerIndex + 1);
                if (layout.containsKey(layerName)) {
                    List<List<String>> layer = layout.get(layerName);

                    // Apply translation to the rendering position for the current layer
                    pPoseStack.pushPose();
                    pPoseStack.translate(baseX + projectionOffset.x(), baseY + projectionOffset.y(), baseZ + projectionOffset.z());

                    // Render each block in the layer
                    for (int row = 0; row < dimensions[0]; row++) {
                        for (int col = 0; col < dimensions[1]; col++) {
                            String value = layer.get(row).get(col);
                            Block block = structure.getBlocks().get(value);
                            if (block != null) {
                                // Render the block using Minecraft's block rendering
                                BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
                                pPoseStack.pushPose(); // Push a new matrix to isolate scaling
                                pPoseStack.translate(0.125f, 0.125f, 0.125f); // Center the block
                                pPoseStack.scale(0.75f, 0.75f, 0.75f); // Scale the block down
                                blockRenderer.getModelRenderer().renderModel(pPoseStack.last(), pBuffer.getBuffer(RenderType.solid()), block.getStateDefinition().any(), blockRenderer.getBlockModel(block.getStateDefinition().any()), 1.0F, 1.0F, 1.0F, pPackedLight, OverlayTexture.NO_OVERLAY);
                                pPoseStack.popPose(); // Pop the scaling matrix
                            }
                            pPoseStack.translate(1.0, 0.0, 0.0); // Move to the next position in the row
                        }
                        pPoseStack.translate(-dimensions[1], 0.0, 1.0); // Move to the next row, reset horizontal position
                    }

                    pPoseStack.popPose();

                    // Move the base translation position upwards
                    baseY += 1.5f; // Use the original layer offset to maintain spacing
                }
            }

            /// TODO: 20-08-2023 Light effect from the projector itself.

        } else {
            pBlockEntity.setInvalidBlueprint(true);
        }
    }





    @Override
    public boolean shouldRender(projectortestBlockEntity pBlockEntity, Vec3 pCameraPos) {
        return true;
    }

    @Override
    public boolean shouldRenderOffScreen(projectortestBlockEntity pBlockEntity) {
        return true;
    }
}
