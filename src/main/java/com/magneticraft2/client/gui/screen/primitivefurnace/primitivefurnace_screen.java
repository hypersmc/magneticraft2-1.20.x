package com.magneticraft2.client.gui.screen.primitivefurnace;

import com.magneticraft2.client.gui.container.primitivefurnace.primitivefurnace_container;
import com.magneticraft2.common.blockentity.stage.stone.PrimitiveFurnaceMultiblockEntity;
import com.magneticraft2.common.magneticraft2;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * @author JumpWatch on 14-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class primitivefurnace_screen extends AbstractContainerScreen<primitivefurnace_container> {
    private ResourceLocation GUI = new ResourceLocation(magneticraft2.MOD_ID + ":textures/gui/primitive_furnace_gui.png");

    public primitivefurnace_screen(primitivefurnace_container  container, Inventory inv, Component name) {
        super(container, inv, name);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        this.renderBackground(pGuiGraphics);
        int i = this.leftPos;
        int j = this.topPos;
        int centerX = leftPos + imageWidth / 2;
        int centerY = topPos + imageHeight / 2;
        pGuiGraphics.setColor(1f, 1f, 1f, 1f);
        pGuiGraphics.blit(GUI, leftPos, topPos, 0, 0, imageWidth, imageHeight + 55);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        this.renderPSI(pGuiGraphics, i, j);
    }
    private void renderPSI(GuiGraphics guiGraphics, int x, int y) {
        PrimitiveFurnaceMultiblockEntity block = menu.getMultiblockEntity();
        int PSI = block.getAirAmount();
        guiGraphics.drawString(font, "PSI: " + PSI, x + 4, y + 43, 4210752, false);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

}
