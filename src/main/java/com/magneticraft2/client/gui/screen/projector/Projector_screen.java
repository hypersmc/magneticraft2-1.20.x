package com.magneticraft2.client.gui.screen.projector;

import com.magneticraft2.client.gui.container.projector.Projector_container;
import com.magneticraft2.common.blockentity.general.projectortestBlockEntity;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.systems.Blueprint.json.Blueprint;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Projector_screen extends AbstractContainerScreen<Projector_container> {
    private static final Logger LOGGER = LogManager.getLogger("Magneticraft2 Projector_screen");
    private List<ClickableArea> clickableAreas = new ArrayList<>();

    private ResourceLocation GUI = new ResourceLocation(magneticraft2.MOD_ID + ":textures/gui/projector_gui.png");
    private int mouseClickY = -1;
    private float scrollOffs;
    private boolean scrolling;
    private boolean displayprints;
    private int startIndex;
    public Projector_screen(Projector_container container, Inventory inv, Component name) {
        super(container, inv, name);

    }

    @Override
    public void render(GuiGraphics p_283479_, int p_283661_, int p_281248_, float p_281886_) {
        renderBackground(p_283479_);
        super.render(p_283479_, p_283661_, p_281248_, p_281886_);
        renderTooltip(p_283479_, p_283661_, p_281248_);
    }


    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {

        this.scrolling = false;
        if (this.displayprints) {
            int i = this.leftPos + 52;
            int j = this.topPos + 14;
            int k = this.startIndex + 9;

            for(int l = this.startIndex; l < k; ++l) {
                int i1 = l - this.startIndex;
                double d0 = pMouseX - (double)(i + i1 % 4 * 16);
                double d1 = pMouseY - (double)(j + i1 / 4 * 18);
                if (d0 >= 0.0D && d1 >= 0.0D && d0 < 16.0D && d1 < 18.0D && this.menu.clickMenuButton(this.minecraft.player, l)) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, l);
                    return true;
                }
            }


            i = this.leftPos + 119;
            j = this.topPos + 9;
            if (pMouseX >= (double)i && pMouseX < (double)(i + 12) && pMouseY >= (double)j && pMouseY < (double)(j + 54)) {
                this.scrolling = true;
            }

        }
        for (ClickableArea area : clickableAreas) {
            if (area.isMouseOver((int) pMouseX, (int) pMouseY)) {
                String clickedText = area.getText();
                menu.getprojector().setBlueprint(clickedText);
                break; // Exit the loop after processing one click
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

        @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        this.renderBackground(guiGraphics);
        int i = this.leftPos;
        int j = this.topPos;
        int k = (int)(41.0F * this.scrollOffs);
        guiGraphics.setColor(1f, 1f, 1f, 1f);
        guiGraphics.blit(GUI, leftPos, topPos, 0,0, imageWidth-17, imageHeight);
        guiGraphics.blit(GUI, i + 107, j + 27 + k, 176-5, 0, 12, 15);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int l = this.leftPos + 35;

        int i1 = this.topPos + 27;
        int j1 = this.startIndex + 7;
        this.renderBar(guiGraphics, mouseX, mouseY, l, i1, j1);
        this.renderBlueprints(guiGraphics,j1, l+2, i1, mouseX, mouseY);
        this.renderIfInvalidOrNotText(guiGraphics, l, i1);
    }
    private void renderIfInvalidOrNotText(GuiGraphics pGuiGraphics, int pX, int pY) {

        if (menu.getprojector().getInvalidBlueprint()){
            pGuiGraphics.drawString(font, "Invalid blueprint: ", pX-25, pY+110, 4210752, false );
            pGuiGraphics.drawString(font, menu.getprojector().getInvalidBlueprint() + "", pX + 62, pY+110, 4210752, false );
        }else {
            pGuiGraphics.drawString(font, "Current blueprint: ", pX-25, pY+110, 4210752, false );
            pGuiGraphics.drawString(font, menu.getprojector().getBlueprint() + "", pX + 67, pY+110, 4210752, false );
        }
    }
    private void renderBlueprints(GuiGraphics pGuiGraphics, int pStartIndex, int pX, int pY, int mouseX, int mouseY) {

        List<String> blueprintNames = new ArrayList<>();

        for (int i = this.startIndex; i <= pStartIndex && i < BlueprintRegistry.getRegisteredBlueprintInNumber(); ++i) {
            blueprintNames.add(BlueprintRegistry.getRegisteredBlueprintByNumber(i));
        }
        Collections.sort(blueprintNames, new Comparator<String>() {
            @Override
            public int compare(String name1, String name2) {
                int num1 = extractNumericSuffix(name1);
                int num2 = extractNumericSuffix(name2);
                return Integer.compare(num1, num2);
            }

            private int extractNumericSuffix(String name) {
                String numericPart = name.replaceAll("[^0-9]", "");
                return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
            }
        });

        for (int i = 0; i < blueprintNames.size(); ++i) {
            int j = i + this.startIndex;
            int i1 = pY + i * 12 + 2;
            int textColor = 16777215;

            boolean isMouseOverText = isMouseOver(pX, i1, font.width(blueprintNames.get(i)), font.lineHeight, mouseX, mouseY);

            if (isMouseOverText) {
                textColor = 16777120;

                // Add the clickable area to the list
                clickableAreas.add(new ClickableArea(pX, i1, font.width(blueprintNames.get(i)), font.lineHeight, blueprintNames.get(i)));
            }

            pGuiGraphics.drawString(font, blueprintNames.get(i), pX, i1, textColor, false);
        }
    }


    private boolean isMouseOver(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }


    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (this.scrolling && this.isScrollBarActive()) {
            int i = this.topPos + 14;
            int j = i + 54;
            this.scrollOffs = ((float)pMouseY - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.startIndex = (int)((double)(this.scrollOffs * (float)this.getOffscreenRows()) + 0.5D) * 4;
            return true;
        } else {
            return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        }
    }

    private void renderBar(GuiGraphics p_282733_, int p_282136_, int p_282147_, int p_281987_, int p_281276_, int p_282688_) {
        p_282733_.blit(GUI, leftPos + 107, topPos + 27, 171, 0, 13, 15);

    }


    @Override
    public boolean mouseScrolled(double p_94686_, double p_94687_, double p_94688_) {
        if (this.isScrollBarActive()) {
            int i = this.getOffscreenRows();
            float f = (float)p_94688_ / (float)i;
            this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0F, 1.0F);
            this.startIndex = (int)((double)(this.scrollOffs * (float)i) + 0.5D) * 4;
        }
        return super.mouseScrolled(p_94686_, p_94687_, p_94688_);
    }
    private boolean isScrollBarActive() {
        return this.displayprints && this.menu.getNumRec() > 12;
    }
    protected int getOffscreenRows() {
        return (this.menu.getNumRec() + 4 - 1) / 4 - 3;
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(font, "Blueprint Projector", 10, 10, 4210752, false);
    }
    class ClickableArea {
        private final int x, y, width, height;
        private final String text;

        public ClickableArea(int x, int y, int width, int height, String text) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
        }

        public boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        public String getText() {
            return text;
        }
    }
}
