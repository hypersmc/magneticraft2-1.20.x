package com.magneticraft2.client.gui.screen.blueprintmaker;

import com.magneticraft2.client.gui.container.blueprintmaker.blueprintmaker_container;
import com.magneticraft2.common.blockentity.general.blueprintmakerBlockEntity;
import com.magneticraft2.common.magneticraft2;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author JumpWatch on 20-08-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class blueprintmaker_screen extends AbstractContainerScreen<blueprintmaker_container> {
    private ResourceLocation GUI = new ResourceLocation(magneticraft2.MOD_ID + ":textures/gui/blueprintmaker_gui.png");
    private static final Logger LOGGER = LogManager.getLogger("MGC2-stuff");
    private EditBox blueprintNameField;
    private Button saveButtonClient;
    private Button saveButtonServer;
    private Button saveButtonShare;
    private int yOffset = 0;
    private int blocksToDisplay;
    private int maxVisibleBlocks;
    private int x;
    private int y;
    private int scrollbarPosition = 0;
    private boolean isDraggingScrollbar = false;
    private double prevMouseY = 0;
    private boolean triedsavingwithnoname = false;
    public blueprintmaker_screen(blueprintmaker_container container, Inventory inv, Component name) {
        super(container, inv, name);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256){
            this.minecraft.player.closeContainer();
        }
        return !this.blueprintNameField.keyPressed(pKeyCode,pScanCode,pModifiers) && !this.blueprintNameField.canConsumeInput() ? super.keyPressed(pKeyCode,pScanCode,pModifiers) : true;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = leftPos + imageWidth / 2;
        int centerY = topPos + imageHeight / 2;


        blueprintNameField = new EditBox(font, centerX -32 , centerY+31 , 80,  18, Component.translatable("gui.blueprintname"));
        blueprintNameField.setValue(""); // Set initial value
        blueprintNameField.setHint(Component.translatable("gui.blueprintname"));
        this.saveButtonClient = this.addRenderableWidget(Button.builder(Component.translatable("gui.savebutton.client"), this::onSaveButtonClickClient).bounds(centerX+55,centerY+30, 60, 20).build());
        //this.saveButtonServer = this.addRenderableWidget(Button.builder(Component.translatable("gui.savebutton.server"), this::onSaveButtonClickServer).bounds(centerX+55,centerY+30, 40, 20).build());
        this.addRenderableWidget(blueprintNameField);
        this.addWidget(saveButtonClient);
    }
    private void onSaveButtonClickClient(Button button) {
        if (blueprintNameField.getValue().isEmpty()){
            triedsavingwithnoname = true;
        }else {
            blueprintmakerBlockEntity block = menu.getBlueprintmaker();
            block.setBlueprintname(blueprintNameField.getValue());
            block.saveBlueprintClient(menu.getPlayerName());

        }
    }
    private void onSaveButtonClickServer(Button button){
        if (blueprintNameField.getValue().isEmpty()){
            triedsavingwithnoname = true;
        }else {
            blueprintmakerBlockEntity block = menu.getBlueprintmaker();
            block.setBlueprintname(blueprintNameField.getValue());
            block.saveBlueprintServer(menu.getPlayerName());
        }
    }

        @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        this.renderBackground(pGuiGraphics);
        int i = this.leftPos;
        int j = this.topPos;
        int centerX = leftPos + imageWidth / 2;
        int centerY = topPos + imageHeight / 2;
        pGuiGraphics.setColor(1f, 1f, 1f, 1f);
        pGuiGraphics.blit(GUI, leftPos, topPos, 0, 0, imageWidth + 80, imageHeight + 55);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        this.rendercoordinates(pGuiGraphics, i, j);
        this.rendernoName(pGuiGraphics,i,j);
        this.renderBlockItemList(pGuiGraphics,i,j);
        this.renderBlocksInSpace(pGuiGraphics,i,j);
    }

    private void rendercoordinates(GuiGraphics guiGraphics, int x, int y) {
        blueprintmakerBlockEntity block = menu.getBlueprintmaker();
        BlockPos pos1xyz = block.getPos1BlockPos();
        BlockPos pos2xyz = block.getPos2BlockPos();
        if (pos1xyz.equals(BlockPos.of(block.getPos1long())) && !pos1xyz.equals(new BlockPos(0, 0, 0))) {
            guiGraphics.drawString(font, "pos1:", x + 4, y + 73, 4210752, false);
            guiGraphics.drawString(font, "X: " + pos1xyz.getX(), x + 4, y + 83, 4210752, false);
            guiGraphics.drawString(font, "Y: " + pos1xyz.getY(), x + 4, y + 93, 4210752, false);
            guiGraphics.drawString(font, "Z: " + pos1xyz.getZ(), x + 4, y + 103, 4210752, false);

        }
        if (pos2xyz.equals(BlockPos.of(block.getPos2long())) && !pos2xyz.equals(new BlockPos(0, 0, 0))) {
            guiGraphics.drawString(font, "pos2:", x + 43, y+73, 4210752, false);
            guiGraphics.drawString(font, "X: " + pos2xyz.getX(), x + 43, y+83, 4210752, false );
            guiGraphics.drawString(font, "Y: " + pos2xyz.getY(), x + 43, y+93, 4210752, false );
            guiGraphics.drawString(font, "Z: " + pos2xyz.getZ(), x + 43, y+103, 4210752, false );
        }
    }

    private void renderBlockItemList(GuiGraphics guiGraphics, int x, int y) {
        blueprintmakerBlockEntity block = menu.getBlueprintmaker();
        BlockPos pos1xyz = block.getPos1BlockPos();
        BlockPos pos2xyz = block.getPos2BlockPos();

        if (pos1xyz.getX() == -65 && pos1xyz.getY() == -65 && pos1xyz.getZ() == -65 &&
                pos2xyz.getX() == -65 && pos2xyz.getY() == -65 && pos2xyz.getZ() == -65) {
            return; // Don't render anything if both positions are (-65, -65, -65)
        }

        Map<String, Double> blockCountMap = new HashMap<>();

        // Count the occurrence of each block name
        for (BlockPos currentPos : BlockPos.betweenClosed(pos1xyz, pos2xyz)) {
            BlockState state = block.getLevel().getBlockState(currentPos);

            if (!state.isAir()) {
                String blockName = state.getBlock().getName().getString();
                if (isDoorBlock(blockName)) {
                    if (!blockCountMap.containsKey(blockName)) {
                        blockCountMap.put(blockName, 0.5);
                        blocksToDisplay++;
                    }
                }else if (isBedBlock(blockName)){
                    if (!blockCountMap.containsKey(blockName)){
                        blockCountMap.put(blockName, 0.5);
                        blocksToDisplay++;
                    }
                }else {
                    blockCountMap.put(blockName, blockCountMap.getOrDefault(blockName, 0.0) + 1);
                    blocksToDisplay++;
                }
            }
        }

        int yOffset = 0; // Reset yOffset
        int maxVisibleBlocks = 8;
        int blocksToDisplay = blockCountMap.size(); // Total unique blocks to display
        this.blocksToDisplay = blocksToDisplay;
        this.maxVisibleBlocks = maxVisibleBlocks;

        if (blocksToDisplay > maxVisibleBlocks) {
            // Calculate scrollbar position based on user interaction
            int scrollbarHeight = 92; // Adjust height based on your layout
            int maxScroll = blocksToDisplay - maxVisibleBlocks;
            scrollbarPosition = Math.max(0, Math.min(scrollbarPosition, maxScroll));

            // Render scrollbar (a basic example)
            int scrollbarX = x + 240; // Adjust X position based on your layout
            int scrollbarY = y + 6; // Adjust Y position based on your layout

            int scrollbarThumbY = scrollbarY + (scrollbarHeight * scrollbarPosition / maxScroll);
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 10, scrollbarY + scrollbarHeight, 0xFFCCCCCC); // Scrollbar background
            guiGraphics.fill(scrollbarX, scrollbarThumbY, scrollbarX + 10, scrollbarThumbY + 10, 0xFF888888); // Scrollbar thumb
        }

        int renderedBlocks = 0;
        int blocksToRender = Math.min(maxVisibleBlocks, blocksToDisplay - scrollbarPosition);
        Iterator<Map.Entry<String, Double>> iterator = blockCountMap.entrySet().iterator();
        for (int i = 0; i < scrollbarPosition && iterator.hasNext(); i++) {
            iterator.next(); // Skip entries before the scrollbar position
        }
        List<Map.Entry<String, Double>> entries = new ArrayList<>(blockCountMap.entrySet());

        for (int i = scrollbarPosition; i < scrollbarPosition + blocksToRender && i < entries.size(); i++) {
            Map.Entry<String, Double> entry = entries.get(i);
            String blockName = entry.getKey();
            Double count = entry.getValue();

            if (count > 1) {
                blockName = count.toString().replace(".0", "") + "x " + blockName;
            }

            if (blockName.length() > 12) {
                blockName = blockName.substring(0, 12) + "..."; // Truncate the text
            }

            guiGraphics.drawString(font, blockName, x + 160, y + 10 + yOffset, 16777215, false);
            yOffset += 12;
        }
    }



    private void renderBlocksInSpace(GuiGraphics guiGraphics, int x, int y) {
        blueprintmakerBlockEntity block = menu.getBlueprintmaker();
        BlockPos pos1xyz = block.getPos1BlockPos();
        BlockPos pos2xyz = block.getPos2BlockPos();

        if (pos1xyz.getX() == -65 && pos1xyz.getY() == -65 && pos1xyz.getZ() == -65 &&
                pos2xyz.getX() == -65 && pos2xyz.getY() == -65 && pos2xyz.getZ() == -65) {
            return; // Don't render anything if both positions are (-65, -65, -65)
        }

        int blockSize = 6; // Adjust the size of the block
        int blockX = x + 95; // X coordinate relative to GUI position
        int blockY = y + 40; // Y coordinate relative to GUI position

        int yOffset = 0; // Reset yOffset
        int maxVisibleBlocks = 800; // Maximum number of blocks to display
        int blocksRendered = 0; // Counter to keep track of rendered blocks

        // Iterate through the block positions between pos1 and pos2
        for (BlockPos currentPos : BlockPos.betweenClosed(pos1xyz, pos2xyz)) {
            BlockState state = block.getLevel().getBlockState(currentPos);

            if (!state.isAir()) { // Skip air blocks
                // Calculate the relative position of the block in the 3D grid
                int posX = Math.max(pos1xyz.getX(), pos2xyz.getX()) - currentPos.getX();
                int posY = Math.max(pos1xyz.getY(), pos2xyz.getY()) - currentPos.getY();
                int posZ = Math.max(pos1xyz.getZ(), pos2xyz.getZ()) - currentPos.getZ();

                // Calculate the 3D position for rendering
                int renderX = blockX + (posX * blockSize);
                int renderY = blockY + (posY * blockSize);
                int renderZ = 100 + (posZ * blockSize); // Use different z-coordinates to create a 3D effect

                PoseStack poseStack = new PoseStack(); // Create a matrix stack
                poseStack.translate(renderX, renderY, renderZ); // Set the block's position in 3D space

                // Apply a 45-degree rotation along the x-axis
                poseStack.mulPose(Axis.XP.rotationDegrees(45));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90));

                poseStack.scale(blockSize, blockSize, blockSize); // Scale the block

                // Render the block using Minecraft's block rendering
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                        state, // The block state you want to render
                        poseStack, guiGraphics.bufferSource(), 15 << 20, OverlayTexture.NO_OVERLAY // Set light and overlay
                );

                blocksRendered++;
                if (blocksRendered >= maxVisibleBlocks) {
                    break; // Stop rendering if the maximum visible blocks have been reached
                }
            }
        }
    }



    private void rendernoName(GuiGraphics guiGraphics, int x, int y){
        if (triedsavingwithnoname) {
            guiGraphics.drawString(font, "Please enter name :)", x, y, 4210752, false);
            float waiting = (float) Math.sin(menu.getBlueprintmaker().getLevel().getGameTime() / 40.0) * 0.5f + 0.5f;
            if (waiting >= 0.998f) {
                triedsavingwithnoname = false;
                waiting = 0;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(font, "Blueprint maker", 10, 10, 4210752, false);

    }


    ///scrolll

    private boolean isScrollbarVisible() {
        return blocksToDisplay > maxVisibleBlocks;
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isScrollbarVisible()) {
            int maxScroll = blocksToDisplay - maxVisibleBlocks;
            scrollbarPosition = Math.max(0, Math.min(scrollbarPosition - (int) delta, maxScroll)); // Invert delta here
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    private boolean isDoorBlock(String blockName) {
        return blockName.equalsIgnoreCase("Oak Door") || blockName.equalsIgnoreCase("Spruce Door") ||
                blockName.equalsIgnoreCase("Birch Door") || blockName.equalsIgnoreCase("Jungle Door") ||
                blockName.equalsIgnoreCase("Acacia Door") || blockName.equalsIgnoreCase("Dark Oak Door") ||
                blockName.equalsIgnoreCase("Iron Door") || blockName.equalsIgnoreCase("Crimson Door") ||
                blockName.equalsIgnoreCase("Warped Door");
    }
    private boolean isBedBlock(String blockName){
        return blockName.equalsIgnoreCase("White Bed") || blockName.equalsIgnoreCase("Light Gray Bed") ||
                blockName.equalsIgnoreCase("Gray Bed") || blockName.equalsIgnoreCase("Black Bed") ||
                blockName.equalsIgnoreCase("Brown Bed") || blockName.equalsIgnoreCase("Red Bed") ||
                blockName.equalsIgnoreCase("Orange Bed") || blockName.equalsIgnoreCase("Yellow Bed") ||
                blockName.equalsIgnoreCase("Lime Bed") || blockName.equalsIgnoreCase("Green Bed") ||
                blockName.equalsIgnoreCase("Cyan Bed") || blockName.equalsIgnoreCase("Light Blue Bed") ||
                blockName.equalsIgnoreCase("Blue Bed") || blockName.equalsIgnoreCase(" Purple Bed") ||
                blockName.equalsIgnoreCase("Magenta Bed") || blockName.equalsIgnoreCase("Pink Bed");
    }



}