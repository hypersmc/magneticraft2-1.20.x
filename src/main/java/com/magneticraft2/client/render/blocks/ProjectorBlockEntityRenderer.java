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
    private static final Logger LOGGER = LogManager.getLogger("Projector_render");
    private static final float SQUARE_SIZE = 5.0f; // Size of the projected square
    private static final float SQUARE_OFFSET = 0.5f - SQUARE_SIZE / 2.0f; // Offset from the block center
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


        // Get the blueprint
        Blueprint blueprint = BlueprintRegistry.getRegisteredBlueprint("magneticraft2", pBlockEntity.getBlueprint());
        if (blueprint != null) {
            pBlockEntity.setInvalidBlueprint(false);

            BlueprintStructure structure = blueprint.getStructure();
            Map<String, List<List<String>>> layout = structure.getLayout();
            int[] dimensions = structure.getDimensions();
            double halfWidth = dimensions[0] / 2.0;
            double halfDepth = dimensions[2] / 2.0;

// Centering offset: adjust based on blueprint size
            Vec3 centeringOffset = new Vec3(-halfWidth + 0.5, 0, -halfDepth + 0.5);

// Directional offset: place the projection in front of the projector
            Direction blockFacing = pBlockEntity.getBlockState().getValue(FACING);
            Vec3 facingOffset = Vec3.ZERO;

            switch (blockFacing) {
                case EAST -> facingOffset = new Vec3(1.5, 0, 0.5);    // Move 1 block east
                case WEST -> facingOffset = new Vec3(-1.0, 0, 0);   // Move 1 block west
                case NORTH -> facingOffset = new Vec3(0.5, 0, -1.0);  // Move 1 block north
                case SOUTH -> facingOffset = new Vec3(0, 0, 1.0);   // Move 1 block south
            }

// Combine centering and directional offsets
            Vec3 projectionOffset = centeringOffset.add(facingOffset);

            // Initialize the base translation position
            float baseX = (float) projectionOffset.x();
            float baseY = (float) projectionOffset.y();
            float baseZ = (float) projectionOffset.z();
            if (!pBlockEntity.getRenderingoutline()) {
                renderOutline(pPoseStack, pBuffer, dimensions, projectionOffset);
            } else {
                // Render the blueprint blocks
                for (int layerIndex = 0; layerIndex < dimensions[1]; layerIndex++) { // Changed dimensions[2] to dimensions[1] for correct layer depth
                    String layerName = "layer" + (layerIndex + 1);
                    if (layout.containsKey(layerName)) {
                        List<List<String>> layer = layout.get(layerName);

                        // Check if the dimensions of the layer match the blueprint dimensions
                        int numRows = layer.size();
                        int numCols = numRows > 0 ? layer.get(0).size() : 0;

                        // Adjust the layer layout to fit the dimensions if necessary
                        if (numRows != dimensions[2] || numCols != dimensions[0]) { // Corrected dimension check
                            List<List<String>> adjustedLayer = new ArrayList<>();
                            for (int row = 0; row < dimensions[2]; row++) {
                                List<String> newRow = new ArrayList<>();
                                for (int col = 0; col < dimensions[0]; col++) {
                                    int adjustedRow = row % numRows;
                                    int adjustedCol = col % numCols;
                                    newRow.add(layer.get(adjustedRow).get(adjustedCol));
                                }
                                adjustedLayer.add(newRow);
                            }
                            layer = adjustedLayer;
                        }

                        // Apply translation for the current layer
                        pPoseStack.pushPose();
                        pPoseStack.translate(baseX + projectionOffset.x(), baseY + layerIndex, baseZ + projectionOffset.z()); // Translate by layerIndex to stack layers vertically

                        // Render each block in the layer
                        for (int row = 0; row < dimensions[2]; row++) { // Corrected row count based on dimensions[2] (depth)
                            for (int col = 0; col < dimensions[0]; col++) { // Corrected column count based on dimensions[0] (width)
                                String value = layer.get(row).get(col);
                                Block block = structure.getBlocks().get(value);
                                int color = Minecraft.getInstance().level.getBiome(pBlockEntity.getBlockPos()).get().getGrassColor(pBlockEntity.getBlockPos().getX(),pBlockEntity.getBlockPos().getZ());
                                if (block != null) {
                                    // Render the block using Minecraft's block rendering
                                    BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
                                    pPoseStack.pushPose(); // Isolate transformations for each block
                                    pPoseStack.translate(0.5, 0.5, 0.5); // Center block within its grid cell
                                    pPoseStack.scale(0.75f, 0.75f, 0.75f); // Scale down the block

                                    blockRenderer.getModelRenderer().renderModel(
                                            pPoseStack.last(),
                                            pBuffer.getBuffer(RenderType.cutout()),
                                            block.defaultBlockState(),
                                            blockRenderer.getBlockModel(block.defaultBlockState()),
                                            ((color >> 16) & 0xFF) / 255.0F, // Red component
                                            ((color >> 8) & 0xFF) / 255.0F,  // Green component
                                            (color & 0xFF) / 255.0F,         // Blue component
                                            pPackedLight,
                                            OverlayTexture.NO_OVERLAY
                                    );
                                    pPoseStack.popPose(); // Revert transformations for the next block
                                }
                                pPoseStack.translate(1.0, 0.0, 0.0); // Move to the next block in the row
                            }
                            pPoseStack.translate(-dimensions[0], 0.0, 1.0); // Reset horizontal position and move to the next row
                        }

                        pPoseStack.popPose(); // Restore the original pose for the next layer
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
