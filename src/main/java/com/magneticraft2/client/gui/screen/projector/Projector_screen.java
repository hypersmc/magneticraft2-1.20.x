package com.magneticraft2.client.gui.screen.projector;

import com.magneticraft2.client.gui.container.projector.Projector_container;
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

import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Projector_screen extends AbstractContainerScreen<Projector_container> {
    private static final Logger LOGGER = LogManager.getLogger("Magneticraft2 Projector_screen");

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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.scrolling = false;
        if (this.displayprints) {
            int i = this.leftPos + 52;
            int j = this.topPos + 14;
            int k = this.startIndex + 12;
            for(int l = this.startIndex; l < k; ++l) {
                int i1 = l - this.startIndex;
                double d0 = mouseX - (double)(i + i1 % 4 * 16);
                double d1 = mouseY - (double)(j + i1 / 4 * 18);
                if (d0 >= 0.0D && d1 >= 0.0D && d0 < 16.0D && d1 < 18.0D && this.menu.clickMenuButton(this.minecraft.player, l)) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, l);
                    return true;
                }
            }
            i = this.leftPos + 119;
            j = this.topPos + 9;
            if (mouseX >= (double)i && mouseX < (double)(i + 12) && mouseY >= (double)j && mouseY < (double)(j + 54)) {
                this.scrolling = true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        int k = (int)(41.0F * this.scrollOffs);
        guiGraphics.setColor(1f, 1f, 1f, 1f);
        guiGraphics.blit(GUI, leftPos, topPos, 0,0, imageWidth-17, imageHeight);
        guiGraphics.blit(GUI, i + 107, j + 27 + k, 176-5, 0, 12, 15);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int l = this.leftPos + 52;
        this.renderBlueprints(guiGraphics, l, mouseX, mouseY);
        int i1 = this.topPos + 14;
        int j1 = this.startIndex + 12;
        this.renderBar(guiGraphics, mouseX, mouseY, l, i1, j1);
    }
    private void renderBlueprints(GuiGraphics p_281999_, int p_282658_, int p_282563_, int p_283352_) {

        for (int i = 0; i < BlueprintRegistry.getRegisteredBlueprintByNumber(); ++i){
            p_281999_.drawString(font, BlueprintRegistry.getRegisteredBlueprintByName(i), leftPos + 35 , topPos + 27, 4210752, false);

        }
    }


    @Override
    public boolean mouseDragged(double p_97752_, double p_97753_, int p_97754_, double p_97755_, double p_97756_) {
        if (this.scrolling){
            int i = this.topPos + 14;
            int j = i + 54;
            this.scrollOffs = ((float)p_97753_ - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.startIndex = (int)((double)(this.scrollOffs + 0.5D) * 4);
            return true;
        } else {
            return super.mouseDragged(p_97752_, p_97753_, p_97754_, p_97755_, p_97756_);
        }
    }

    private void renderBar(GuiGraphics p_282733_, int p_282136_, int p_282147_, int p_281987_, int p_281276_, int p_282688_) {
        p_282733_.blit(GUI, leftPos + 50, topPos + 27, 176, 0, 12, 15);

    }

//
//    @Override
//    public boolean mouseScrolled(double p_94686_, double p_94687_, double p_94688_) {
//        if (this.isScrollBarActive()) {
//            int i = this.getOffscreenRows();
//            float f = (float)p_94688_ / (float)i;
//            this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0F, 1.0F);
//            this.startIndex = (int)((double)(this.scrollOffs * (float)i) + 0.5D) * 4;
//        }
//        return super.mouseScrolled(p_94686_, p_94687_, p_94688_);
//    }


    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(font, "Blueprint Projector", 10, 10, 4210752, false);
    }
}
