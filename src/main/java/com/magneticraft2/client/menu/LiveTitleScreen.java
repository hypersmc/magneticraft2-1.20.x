package com.magneticraft2.client.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Retains vanilla's actual title-menu widgets but renders them over the
 * existing live client level, without drawing the panorama.
 */
final class LiveTitleScreen extends TitleScreen {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Minecraft's GameRenderer already renders the real ClientLevel behind Screen.
        // Do not call TitleScreen.render(): it draws the panorama over that world.
        graphics.fillGradient(0, 0, this.width, this.height, 0x30000000, 0x50000000);
        graphics.drawCenteredString(this.font, "Magneticraft2", this.width / 2, 28, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Living Main Menu - real world simulation", this.width / 2, 43, 0xE0E0E0);
        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        // A pause here would stop the real server-side machines.
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        leaveIfNavigating();
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        leaveIfNavigating();
        return handled;
    }

    private void leaveIfNavigating() {
        // The original TitleScreen buttons can navigate to other screens.
        // Disconnect the private menu world before those screens can be used.
        if (this.minecraft != null) {
            Screen destination = this.minecraft.screen;
            if (destination != this) {
                LivingMenuController.leaveMenuFor(destination);
            }
        }
    }
}
