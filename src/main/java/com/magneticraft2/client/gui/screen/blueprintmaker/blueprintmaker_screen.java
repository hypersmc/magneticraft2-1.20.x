package com.magneticraft2.client.gui.screen.blueprintmaker;

import com.magneticraft2.client.gui.container.blueprintmaker.blueprintmaker_container;
import com.magneticraft2.common.blockentity.general.blueprintmakerBlockEntity;
import com.magneticraft2.common.magneticraft2;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JumpWatch on 20-08-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class blueprintmaker_screen extends AbstractContainerScreen<blueprintmaker_container> {
    private ResourceLocation GUI = new ResourceLocation(magneticraft2.MOD_ID + ":textures/gui/blueprintmaker_gui.png");
    private static final Logger LOGGER = LogManager.getLogger("MGC2-stuff");
    private EditBox blueprintNameField;
    private Button saveButton;
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


        blueprintNameField = new EditBox(font, centerX -20 , centerY+31 , 68,  18, Component.translatable("gui.blueprintname"));
        blueprintNameField.setValue(""); // Set initial value
        blueprintNameField.setHint(Component.translatable("gui.blueprintname"));
        this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.savebutton"), this::onSaveButtonClick).bounds(centerX+55,centerY+30, 40, 20).build());
        this.addRenderableWidget(blueprintNameField);
        this.addWidget(saveButton);
    }
    private void onSaveButtonClick(Button button) {
        if (blueprintNameField.getValue().isEmpty()){
            triedsavingwithnoname = true;
        }else {
            blueprintmakerBlockEntity block = menu.getBlueprintmaker();
            block.setBlueprintname(blueprintNameField.getValue());
            block.saveBlueprint();

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
        this.x = x;
        this.y = y;
        if (pos1xyz.getX() == -65 && pos1xyz.getY() == -65 && pos1xyz.getZ() == -65 &&
                pos2xyz.getX() == -65 && pos2xyz.getY() == -65 && pos2xyz.getZ() == -65) {
            return; // Don't render anything if both positions are (-65, -65, -65)
        }

        int blocksToDisplay = 0;
        List<String> blockNames = new ArrayList<>();
        yOffset = 0; // Reset yOffset

        for (int xIndex = Math.min(pos1xyz.getX(), pos2xyz.getX()); xIndex <= Math.max(pos1xyz.getX(), pos2xyz.getX()); xIndex++) {
            for (int yIndex = Math.min(pos1xyz.getY(), pos2xyz.getY()); yIndex <= Math.max(pos1xyz.getY(), pos2xyz.getY()); yIndex++) {
                for (int zIndex = Math.min(pos1xyz.getZ(), pos2xyz.getZ()); zIndex <= Math.max(pos1xyz.getZ(), pos2xyz.getZ()); zIndex++) {
                    BlockPos currentPos = new BlockPos(xIndex, yIndex, zIndex);
                    BlockState state = block.getLevel().getBlockState(currentPos);

                    if (!state.isAir()) { // Skip air blocks
                        blocksToDisplay++;
                        this.blocksToDisplay = blocksToDisplay;
                        blockNames.add(state.getBlock().getName().getString());
                    }
                }
            }
        }

        int maxVisibleBlocks = 8;
        this.maxVisibleBlocks = maxVisibleBlocks;

        // Calculate totalHeight based on blocksToDisplay and maxVisibleBlocks
        int totalHeight = Math.min(blocksToDisplay, maxVisibleBlocks) * 12;

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

        // Render the visible block names based on yOffset and scrollbar position
        int blocksToRender = Math.min(maxVisibleBlocks, blocksToDisplay - scrollbarPosition);
        for (int i = scrollbarPosition; i < scrollbarPosition + blocksToRender; i++) {
            String blockName = blockNames.get(i);
            if (blockName.length() > 12) {
                blockName = blockName.substring(0, 12) + "..."; // Truncate the text
            }
            guiGraphics.drawString(font, blockName, x + 160, y + 10 + yOffset, 16777215, false);
            yOffset += 12;
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
            scrollbarPosition = Math.max(0, Math.min(scrollbarPosition + (int) delta, maxScroll));
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }



}