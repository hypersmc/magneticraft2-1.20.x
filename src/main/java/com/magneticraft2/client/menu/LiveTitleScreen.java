package com.magneticraft2.client.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.lwjgl.glfw.GLFW;

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

    private static LivingMenuController.NavigationTarget targetFor(GuiEventListener listener) {
        if (!(listener instanceof AbstractWidget widget) || !widget.active) {
            return null;
        }
        String message = widget.getMessage().getString();
        if (message.equals(Component.translatable("menu.singleplayer").getString())) {
            return LivingMenuController.NavigationTarget.SINGLEPLAYER;
        }
        if (message.equals(Component.translatable("menu.multiplayer").getString())) {
            return LivingMenuController.NavigationTarget.MULTIPLAYER;
        }
        if (message.equals(Component.translatable("menu.online").getString())) {
            return LivingMenuController.NavigationTarget.REALMS;
        }
        if (message.equals(Component.translatable("menu.quit").getString())) {
            return LivingMenuController.NavigationTarget.QUIT;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (GuiEventListener listener : this.children()) {
                if (listener instanceof AbstractWidget widget
                        && widget.active && widget.isMouseOver(mouseX, mouseY)) {
                    LivingMenuController.NavigationTarget target = targetFor(listener);
                    if (target != null) {
                        LivingMenuController.requestNavigation(target);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE) {
            LivingMenuController.NavigationTarget target = targetFor(this.getFocused());
            if (target != null) {
                LivingMenuController.requestNavigation(target);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
