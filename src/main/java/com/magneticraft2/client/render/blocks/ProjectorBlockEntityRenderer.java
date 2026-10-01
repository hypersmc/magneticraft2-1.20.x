package com.magneticraft2.client.render.blocks;

import com.magneticraft2.common.blockentity.general.projectortestBlockEntity;
import com.magneticraft2.common.systems.Blueprint.json.Blueprint;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintRegistry;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintStructure;
import com.mojang.blaze3d.vertex.PoseStack;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 17-08-2023
 * @Project mgc2-1.20
* @version 1.0.0
 */
public class ProjectorBlockEntityRenderer implements BlockEntityRenderer<projectortestBlockEntity> {
    private static final Logger LOGGER = LogManager.getLogger("Projector_render");
    private static final int NO_OVERLAY = OverlayTexture.NO_OVERLAY;
    private static final float PREVIEW_BLOCK_SCALE = 0.75F;

    private final Minecraft minecraft = Minecraft.getInstance();

    public ProjectorBlockEntityRenderer(BlockEntityRendererProvider.Context context){

    }

    @Override
    public void render(projectortestBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        String blueprintName = pBlockEntity.getBlueprint();
        if (blueprintName == null || blueprintName.isBlank()) {
            return;
        }

        Blueprint blueprint = BlueprintRegistry.getRegisteredBlueprint("magneticraft2", blueprintName);
        if (blueprint == null) {
            pBlockEntity.setInvalidBlueprint(true);
            return;
        }

        pBlockEntity.setInvalidBlueprint(false);

        BlueprintStructure structure = blueprint.getStructure();
        if (structure == null || structure.getDimensions() == null || structure.getDimensions().length < 3) {
            pBlockEntity.setInvalidBlueprint(true);
            return;
        }

        int[] dimensions = structure.getDimensions();
        Direction blockFacing = pBlockEntity.getProjectionDirection();

        if (!pBlockEntity.getRenderingoutline()) {
            renderOutline(pPoseStack, pBuffer, dimensions, blockFacing);
        } else {
            renderBlueprintBlocks(pBlockEntity, pPoseStack, pBuffer, pPackedLight, structure, dimensions, blockFacing);
        }
    }

    private void renderBlueprintBlocks(projectortestBlockEntity pBlockEntity, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, BlueprintStructure structure, int[] dimensions, Direction blockFacing) {
        Map<String, List<List<String>>> layout = structure.getLayout();
        if (layout == null || layout.isEmpty()) {
            return;
        }

        for (int layerIndex = 0; layerIndex < dimensions[1]; layerIndex++) {
            String layerName = "layer" + (layerIndex + 1);
            if (!layout.containsKey(layerName)) {
                continue;
            }

            List<List<String>> layer = normalizeLayer(layout.get(layerName), dimensions);

            for (int row = 0; row < dimensions[2]; row++) {
                for (int col = 0; col < dimensions[0]; col++) {
                    String value = layer.get(row).get(col);
                    Block block = structure.getBlocks().get(value);
                    if (block == null || block.defaultBlockState().isAir()) {
                        continue;
                    }

                    Vec3 cellCenter = getProjectedCellCenter(blockFacing, dimensions[0], col, row);
                    renderPreviewBlock(pBlockEntity, pPoseStack, pBuffer, pPackedLight, block, cellCenter, layerIndex);
                }
            }
        }
    }

    private List<List<String>> normalizeLayer(List<List<String>> layer, int[] dimensions) {
        int width = dimensions[0];
        int depth = dimensions[2];

        if (layer == null || layer.isEmpty()) {
            return createEmptyLayer(width, depth);
        }

        int numRows = layer.size();
        int numCols = numRows > 0 && layer.get(0) != null ? layer.get(0).size() : 0;
        if (numRows == depth && numCols == width) {
            return layer;
        }

        if (numRows <= 0 || numCols <= 0) {
            return createEmptyLayer(width, depth);
        }

        List<List<String>> adjustedLayer = new ArrayList<>();
        for (int row = 0; row < depth; row++) {
            List<String> newRow = new ArrayList<>();
            for (int col = 0; col < width; col++) {
                int adjustedRow = row % numRows;
                int adjustedCol = col % numCols;
                newRow.add(layer.get(adjustedRow).get(adjustedCol));
            }
            adjustedLayer.add(newRow);
        }
        return adjustedLayer;
    }

    private List<List<String>> createEmptyLayer(int width, int depth) {
        List<List<String>> emptyLayer = new ArrayList<>();
        for (int row = 0; row < depth; row++) {
            List<String> newRow = new ArrayList<>();
            for (int col = 0; col < width; col++) {
                newRow.add(" ");
            }
            emptyLayer.add(newRow);
        }
        return emptyLayer;
    }

    private void renderPreviewBlock(projectortestBlockEntity pBlockEntity, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, Block block, Vec3 cellCenter, int layerIndex) {
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        int color = 0xFFFFFF;
        if (minecraft.level != null) {
            color = minecraft.level.getBiome(pBlockEntity.getBlockPos()).get().getGrassColor(pBlockEntity.getBlockPos().getX(), pBlockEntity.getBlockPos().getZ());
        }

        pPoseStack.pushPose();
        pPoseStack.translate(cellCenter.x(), layerIndex + 0.5D, cellCenter.z());
        pPoseStack.scale(PREVIEW_BLOCK_SCALE, PREVIEW_BLOCK_SCALE, PREVIEW_BLOCK_SCALE);
        pPoseStack.translate(-0.5D, -0.5D, -0.5D);

        blockRenderer.getModelRenderer().renderModel(
                pPoseStack.last(),
                pBuffer.getBuffer(RenderType.cutout()),
                block.defaultBlockState(),
                blockRenderer.getBlockModel(block.defaultBlockState()),
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                pPackedLight,
                NO_OVERLAY
        );

        pPoseStack.popPose();
    }

    private void renderOutline(PoseStack pPoseStack, MultiBufferSource pBuffer, int[] dimensions, Direction blockFacing) {
        if (dimensions[0] <= 0 || dimensions[1] <= 0 || dimensions[2] <= 0) {
            return;
        }

        double minX = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (int row = 0; row < dimensions[2]; row++) {
            for (int col = 0; col < dimensions[0]; col++) {
                Vec3 cellCenter = getProjectedCellCenter(blockFacing, dimensions[0], col, row);
                minX = Math.min(minX, cellCenter.x() - 0.5D);
                minZ = Math.min(minZ, cellCenter.z() - 0.5D);
                maxX = Math.max(maxX, cellCenter.x() + 0.5D);
                maxZ = Math.max(maxZ, cellCenter.z() + 0.5D);
            }
        }

        pPoseStack.pushPose();
        DebugRenderer.renderFilledBox(pPoseStack, pBuffer, minX, 0.0D, minZ, maxX, dimensions[1], maxZ, 1.0F, 1.0F, 1.0F, 0.25F);
        pPoseStack.popPose();
    }

    private Vec3 getProjectedCellCenter(Direction blockFacing, int width, int col, int row) {
        Vec3 projectorCenter = new Vec3(0.5D, 0.0D, 0.5D);
        Vec3 forward = new Vec3(blockFacing.getStepX(), 0.0D, blockFacing.getStepZ());
        Vec3 right = new Vec3(-blockFacing.getStepZ(), 0.0D, blockFacing.getStepX());

        int sideOffset = getGridAlignedSideOffset(width, col);
        return projectorCenter
                .add(forward.scale(row + 1.0D))
                .add(right.scale(sideOffset));
    }

    private int getGridAlignedSideOffset(int width, int col) {
        /*
         * A blueprint with an even width cannot be perfectly centered on a single block while
         * staying aligned to Minecraft's block grid. Using half-block offsets makes the preview
         * look centered, but the outline/projection ends up between real block positions.
         *
         * This keeps every projected cell on a real block center. Odd widths still center on the
         * projector's forward line. Even widths are intentionally biased one block to the left
         * of that line, so 2-wide becomes [-1, 0] instead of [-0.5, 0.5].
         */
        return col - (width / 2);
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
