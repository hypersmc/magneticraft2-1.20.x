package com.magneticraft2.client.render.blocks;

import com.magneticraft2.common.blockentity.general.projectortestBlockEntity;
import com.magneticraft2.common.systems.Blueprint.json.Blueprint;
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
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import java.util.ArrayList;
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
    //TODO unused feature for now!
    private int xoffset = 0;
    private int yoffset = 10;
    private int zoffset = 0;
    private int rotation = 0;

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
            if (!pBlockEntity.getRenderingoutline()) {
                renderOutline(pPoseStack, pBuffer, dimensions, projectionOffset);
            } else {
                // Render the blueprint blocks
                for (int layerIndex = 0; layerIndex < dimensions[2]; layerIndex++) {
                    String layerName = "layer" + (layerIndex + 1);
                    if (layout.containsKey(layerName)) {
                        List<List<String>> layer = layout.get(layerName);

                        // Check if the dimensions of the layer match the blueprint dimensions
                        int numRows = layer.size();
                        int numCols = numRows > 0 ? layer.get(0).size() : 0;

                        // If dimensions don't match, adjust the layer layout to fit the dimensions
                        if (numRows != dimensions[0] || numCols != dimensions[1]) {
                            List<List<String>> adjustedLayer = new ArrayList<>();
                            for (int row = 0; row < dimensions[0]; row++) {
                                List<String> newRow = new ArrayList<>();
                                for (int col = 0; col < dimensions[1]; col++) {
                                    int adjustedRow = row % numRows;
                                    int adjustedCol = col % numCols;
                                    newRow.add(layer.get(adjustedRow).get(adjustedCol));
                                }
                                adjustedLayer.add(newRow);
                            }
                            layer = adjustedLayer;
                        }

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
                                    blockRenderer.getModelRenderer().renderModel(pPoseStack.last(), pBuffer.getBuffer(RenderType.cutout()), block.defaultBlockState(), blockRenderer.getBlockModel(block.getStateDefinition().any()), 1.0F, 1.0F, 1.0F, pPackedLight, OverlayTexture.NO_OVERLAY);
                                    pPoseStack.popPose(); // Pop the scaling matrix
                                }
                                pPoseStack.translate(1.0, 0.0, 0.0); // Move to the next position in the row
                            }
                            pPoseStack.translate(-dimensions[1], 0.0, 1.0); // Move to the next row, reset horizontal position
                        }

                        pPoseStack.popPose();

                        // Move the base translation position upwards
                        baseY += 1f; // Use the original layer offset to maintain spacing
                    }
                }

            }
        }else{
            pBlockEntity.setInvalidBlueprint(true);
        }
    }
    private void renderOutline(PoseStack pPoseStack, MultiBufferSource pBuffer, int[] dimensions, Vec3 projectionOffset) {
        // Calculate the corner points of the outline
        Vec3 startPoint = new Vec3(0, 0, 0);
        Vec3 endPoint = new Vec3(dimensions[0], -dimensions[1], dimensions[2]);
        float baseX = (float) projectionOffset.x();
        float baseY = (float) projectionOffset.y();
        float baseZ = (float) projectionOffset.z();
        // Apply translation and scaling to match the projected position
        pPoseStack.pushPose();
        pPoseStack.translate(baseX + projectionOffset.x(), baseY + projectionOffset.y(), baseZ + projectionOffset.z());
        pPoseStack.scale(1.0F, -1.0F, 1.0F); // Invert Y axis for correct rendering

        // Render the outline using Minecraft's debug renderer
        DebugRenderer.renderFilledBox(pPoseStack, pBuffer, startPoint.x(), startPoint.y(), startPoint.z(), endPoint.x(), endPoint.y(), endPoint.z(), 1.0F, 1.0F, 1.0F, 0.25F);

        pPoseStack.popPose();
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
