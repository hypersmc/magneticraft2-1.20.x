package com.magneticraft2.client.gui.screen.gearbox;

import com.magneticraft2.client.gui.container.gearbox.CustomGearboxMenu;
import com.magneticraft2.common.blockentity.stage.copper.CustomGearboxBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.CustomGearboxBlockEntity_wood.InternalComponent;
import com.magneticraft2.common.systems.mgc2Network;
import com.magneticraft2.common.systems.networking.CustomGearboxEditPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.EnumSet;

/**
 * Player-facing editor for the custom gearbox's 3x3x3 internal assembly.
 *
 * One north/south layer is shown at a time. The left side is intentionally
 * spatial and large enough to read at normal GUI scale; the right side is a
 * simple part palette. Right-clicking a cell removes its installed part.
 */
public class CustomGearboxScreen extends AbstractContainerScreen<CustomGearboxMenu> {
    private static final int BASE_WIDTH = 372;
    private static final int BASE_HEIGHT = 272;
    private static final int SCREEN_MARGIN = 12;

    private static final int CELL_SIZE = 36;
    private static final int GRID_PIXELS = CELL_SIZE * 3;
    private static final int GRID_LEFT = 30;
    private static final int GRID_TOP = 96;

    private static final int PALETTE_LEFT = 166;
    private static final int PALETTE_TOP = 66;
    private static final int PALETTE_WIDTH = 184;
    private static final int PALETTE_HEIGHT = 21;
    private static final int PALETTE_GAP = 4;

    private static final int LAYER_BUTTON_Y = 50;
    private static final int LAYER_BUTTON_WIDTH = 22;
    private static final int LAYER_BUTTON_HEIGHT = 18;

    private static final InternalComponent[] PALETTE = {
            InternalComponent.EMPTY,
            InternalComponent.SHAFT_X,
            InternalComponent.SHAFT_Y,
            InternalComponent.SHAFT_Z,
            InternalComponent.GEAR_X,
            InternalComponent.GEAR_Y,
            InternalComponent.GEAR_Z
    };

    private int selectedSlice = 1;
    private InternalComponent selectedComponent = InternalComponent.SHAFT_X;
    private float uiScale = 1.0F;

    public CustomGearboxScreen(CustomGearboxMenu menu,
                               Inventory inventory,
                               Component title) {
        super(menu, inventory, title);
        imageWidth = BASE_WIDTH;
        imageHeight = BASE_HEIGHT;
    }

    @Override
    protected void init() {
        float availableWidth = Math.max(
                1.0F,
                width - SCREEN_MARGIN * 2.0F
        );
        float availableHeight = Math.max(
                1.0F,
                height - SCREEN_MARGIN * 2.0F
        );

        uiScale = Math.min(
                1.0F,
                Math.min(
                        availableWidth / BASE_WIDTH,
                        availableHeight / BASE_HEIGHT
                )
        );

        imageWidth = Math.max(
                1,
                Math.round(BASE_WIDTH * uiScale)
        );
        imageHeight = Math.max(
                1,
                Math.round(BASE_HEIGHT * uiScale)
        );

        super.init();
    }

    @Override
    public void render(GuiGraphics graphics,
                       int mouseX,
                       int mouseY,
                       float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderHoverTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics,
                            float partialTick,
                            int mouseX,
                            int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(
                leftPos,
                topPos,
                0.0F
        );
        graphics.pose().scale(
                uiScale,
                uiScale,
                1.0F
        );

        int left = 0;
        int top = 0;

        graphics.fill(
                left,
                top,
                left + BASE_WIDTH,
                top + BASE_HEIGHT,
                0xEE211A13
        );
        graphics.fill(
                left + 4,
                top + 4,
                left + BASE_WIDTH - 4,
                top + BASE_HEIGHT - 4,
                0xFF35291E
        );

        graphics.drawString(
                font,
                Component.translatable("screen.magneticraft2.custom_gearbox"),
                left + 12,
                top + 10,
                0xFFF0D7A4,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("screen.magneticraft2.custom_gearbox_help"),
                left + 12,
                top + 27,
                0xFFB9A98E,
                false
        );

        CustomGearboxBlockEntity_wood gearbox = menu.getGearbox();
        if (gearbox == null) {
            graphics.drawString(
                    font,
                    "Gearbox unavailable",
                    left + 12,
                    top + 52,
                    0xFFFF5555,
                    false
            );
            graphics.pose().popPose();
            return;
        }

        renderSectionFrames(graphics, left, top);
        renderLayerSelector(graphics, left, top);
        renderGrid(graphics, gearbox, left, top);
        renderPalette(graphics, left, top);
        renderStatus(graphics, gearbox, left, top);

        graphics.pose().popPose();
    }

    private void renderSectionFrames(GuiGraphics graphics,
                                     int left,
                                     int top) {
        drawScaledString(
                graphics,
                "INTERNAL LAYOUT",
                left + 20,
                top + 72,
                0xFFE5C78F,
                0.85F
        );
        drawScaledString(
                graphics,
                "PARTS",
                left + PALETTE_LEFT,
                top + 43,
                0xFFE5C78F,
                0.85F
        );

        graphics.fill(
                left + 18,
                top + 68,
                left + 151,
                top + 222,
                0xFF6D5943
        );
        graphics.fill(
                left + 19,
                top + 69,
                left + 150,
                top + 221,
                0xFF2A241E
        );

        graphics.fill(
                left + 156,
                top + 61,
                left + 360,
                top + 242,
                0xFF6D5943
        );
        graphics.fill(
                left + 157,
                top + 62,
                left + 359,
                top + 241,
                0xFF2A241E
        );
    }

    private void renderLayerSelector(GuiGraphics graphics,
                                     int left,
                                     int top) {
        int previousX = left + GRID_LEFT;
        int nextX = left + GRID_LEFT
                + GRID_PIXELS
                - LAYER_BUTTON_WIDTH;

        drawButton(
                graphics,
                previousX,
                top + LAYER_BUTTON_Y,
                LAYER_BUTTON_WIDTH,
                LAYER_BUTTON_HEIGHT,
                "<",
                false
        );
        drawButton(
                graphics,
                nextX,
                top + LAYER_BUTTON_Y,
                LAYER_BUTTON_WIDTH,
                LAYER_BUTTON_HEIGHT,
                ">",
                false
        );

        String layerName = switch (selectedSlice) {
            case 0 -> "NORTH";
            case 2 -> "SOUTH";
            default -> "CENTER";
        };

        drawScaledCenteredString(
                graphics,
                "Layer " + (selectedSlice + 1) + " / 3  " + layerName,
                left + GRID_LEFT + GRID_PIXELS / 2,
                top + 56,
                0xFFE5C78F,
                0.68F
        );

        drawScaledCenteredString(
                graphics,
                "UP",
                left + GRID_LEFT + GRID_PIXELS / 2,
                top + GRID_TOP - 11,
                0xFF9D8E78,
                0.85F
        );
        drawScaledCenteredString(
                graphics,
                "DOWN",
                left + GRID_LEFT + GRID_PIXELS / 2,
                top + GRID_TOP + GRID_PIXELS + 5,
                0xFF9D8E78,
                0.85F
        );
        drawScaledString(
                graphics,
                "W",
                left + GRID_LEFT - 10,
                top + GRID_TOP + GRID_PIXELS / 2 - 3,
                0xFF9D8E78,
                0.85F
        );
        drawScaledString(
                graphics,
                "E",
                left + GRID_LEFT + GRID_PIXELS + 5,
                top + GRID_TOP + GRID_PIXELS / 2 - 3,
                0xFF9D8E78,
                0.85F
        );
    }

    private void renderGrid(GuiGraphics graphics,
                            CustomGearboxBlockEntity_wood gearbox,
                            int left,
                            int top) {
        EnumSet<Direction> activePorts = gearbox.getActivePorts();

        for (int displayY = 0; displayY < 3; displayY++) {
            int y = 2 - displayY;

            for (int x = 0; x < 3; x++) {
                int screenX = left + GRID_LEFT + x * CELL_SIZE;
                int screenY = top + GRID_TOP + displayY * CELL_SIZE;
                int index = CustomGearboxBlockEntity_wood.index(
                        x,
                        y,
                        selectedSlice
                );
                InternalComponent component = gearbox.getComponent(index);

                boolean portCell = isExternalPortCell(
                        x,
                        y,
                        selectedSlice
                );
                boolean activePortCell = portCell
                        && isActivePortCell(gearbox, activePorts, index);

                int border = activePortCell
                        ? 0xFF79CE62
                        : portCell
                        ? 0xFF8B704D
                        : 0xFF66513C;

                graphics.fill(
                        screenX,
                        screenY,
                        screenX + CELL_SIZE - 2,
                        screenY + CELL_SIZE - 2,
                        border
                );
                graphics.fill(
                        screenX + 2,
                        screenY + 2,
                        screenX + CELL_SIZE - 4,
                        screenY + CELL_SIZE - 4,
                        0xFF211C18
                );

                if (portCell) {
                    graphics.drawString(
                            font,
                            "P",
                            screenX + 4,
                            screenY + 4,
                            activePortCell ? 0xFF8FE277 : 0xFF9A8162,
                            false
                    );
                }

                renderComponentIcon(
                        graphics,
                        component,
                        screenX,
                        screenY
                );
            }
        }
    }

    private void renderComponentIcon(GuiGraphics graphics,
                                     InternalComponent component,
                                     int x,
                                     int y) {
        if (component == InternalComponent.EMPTY) {
            return;
        }

        Direction.Axis axis = component.getAxis();
        int iconColor = component.isGear()
                ? 0xFFD6A34F
                : 0xFFC4874E;

        int centerX = x + (CELL_SIZE - 2) / 2;
        int centerY = y + (CELL_SIZE - 2) / 2;

        if (component.isGear()) {
            graphics.fill(
                    centerX - 10,
                    centerY - 4,
                    centerX + 11,
                    centerY + 5,
                    iconColor
            );
            graphics.fill(
                    centerX - 4,
                    centerY - 10,
                    centerX + 5,
                    centerY + 11,
                    iconColor
            );
            graphics.fill(
                    centerX - 8,
                    centerY - 8,
                    centerX + 9,
                    centerY + 9,
                    iconColor
            );
        } else if (axis == Direction.Axis.X) {
            graphics.fill(
                    centerX - 12,
                    centerY - 3,
                    centerX + 13,
                    centerY + 4,
                    iconColor
            );
        } else if (axis == Direction.Axis.Y) {
            graphics.fill(
                    centerX - 3,
                    centerY - 12,
                    centerX + 4,
                    centerY + 13,
                    iconColor
            );
        } else {
            graphics.fill(
                    centerX - 7,
                    centerY - 7,
                    centerX + 8,
                    centerY + 8,
                    iconColor
            );
            graphics.fill(
                    centerX - 3,
                    centerY - 3,
                    centerX + 4,
                    centerY + 4,
                    0xFF211C18
            );
        }

        graphics.drawCenteredString(
                font,
                axis == null ? "" : axis.getName().toUpperCase(),
                centerX,
                centerY - 4,
                0xFFFFFFFF
        );
    }

    private void renderPalette(GuiGraphics graphics,
                               int left,
                               int top) {
        for (int i = 0; i < PALETTE.length; i++) {
            InternalComponent component = PALETTE[i];
            int y = top + PALETTE_TOP
                    + i * (PALETTE_HEIGHT + PALETTE_GAP);

            drawButton(
                    graphics,
                    left + PALETTE_LEFT,
                    y,
                    PALETTE_WIDTH,
                    PALETTE_HEIGHT,
                    componentLabel(component),
                    component == selectedComponent
            );
        }

        drawScaledString(
                graphics,
                "X = WEST-EAST",
                left + PALETTE_LEFT,
                top + 247,
                0xFF9D8E78,
                0.82F
        );
        drawScaledString(
                graphics,
                "Y = DOWN-UP   Z = NORTH-SOUTH",
                left + PALETTE_LEFT,
                top + 257,
                0xFF9D8E78,
                0.82F
        );
    }

    private void renderStatus(GuiGraphics graphics,
                              CustomGearboxBlockEntity_wood gearbox,
                              int left,
                              int top) {
        EnumSet<Direction> activePorts = gearbox.getActivePorts();

        String ports = activePorts.isEmpty()
                ? "none"
                : activePorts.stream()
                .map(direction -> direction.getName().toUpperCase())
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");

        graphics.drawString(
                font,
                "Active: " + ports,
                left + 18,
                top + 228,
                activePorts.isEmpty() ? 0xFFB9A98E : 0xFFB9DCA6,
                false
        );

        graphics.drawString(
                font,
                gearbox.isGraphValid()
                        ? "Network: valid"
                        : "Network: conflict",
                left + 18,
                top + 241,
                gearbox.isGraphValid() ? 0xFF91D67B : 0xFFFF6666,
                false
        );

        String layerPorts;
        if (selectedSlice == 0) {
            layerPorts = "Port cell: NORTH center";
        } else if (selectedSlice == 2) {
            layerPorts = "Port cell: SOUTH center";
        } else {
            layerPorts = "Port cells: W / E / UP / DOWN";
        }

        drawScaledString(
                graphics,
                layerPorts,
                left + 18,
                top + 254,
                0xFFB9A98E,
                0.82F
        );
    }

    private void drawScaledString(GuiGraphics graphics,
                                  String text,
                                  float x,
                                  float y,
                                  int color,
                                  float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(
                font,
                text,
                0,
                0,
                color,
                false
        );
        graphics.pose().popPose();
    }

    private void drawScaledCenteredString(GuiGraphics graphics,
                                          String text,
                                          float centerX,
                                          float y,
                                          int color,
                                          float scale) {
        float width = font.width(text) * scale;
        drawScaledString(
                graphics,
                text,
                centerX - width * 0.5F,
                y,
                color,
                scale
        );
    }

    private void drawButton(GuiGraphics graphics,
                            int x,
                            int y,
                            int width,
                            int height,
                            String label,
                            boolean selected) {
        int border = selected ? 0xFF79CE62 : 0xFF6D5943;
        int fill = selected ? 0xFF496638 : 0xFF2A241E;

        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                border
        );
        graphics.fill(
                x + 1,
                y + 1,
                x + width - 1,
                y + height - 1,
                fill
        );

        graphics.drawCenteredString(
                font,
                label,
                x + width / 2,
                y + (height - 8) / 2,
                selected ? 0xFFFFFFFF : 0xFFE7D6B8
        );
    }

    private String componentLabel(InternalComponent component) {
        return switch (component) {
            case EMPTY -> "Empty";
            case SHAFT_X -> "Shaft X     West - East";
            case SHAFT_Y -> "Shaft Y     Down - Up";
            case SHAFT_Z -> "Shaft Z     North - South";
            case GEAR_X -> "Gear X      West - East";
            case GEAR_Y -> "Gear Y      Down - Up";
            case GEAR_Z -> "Gear Z      North - South";
        };
    }

    @Override
    protected void renderLabels(GuiGraphics graphics,
                                int mouseX,
                                int mouseY) {
        // Everything is positioned in renderBg; this menu has no inventory slots.
    }

    @Override
    public boolean mouseClicked(double mouseX,
                                double mouseY,
                                int button) {
        if (button == 0 && clickLayerSelector(mouseX, mouseY)) {
            return true;
        }

        if (button == 0 && clickPalette(mouseX, mouseY)) {
            return true;
        }

        int cell = getHoveredCell(mouseX, mouseY);
        if (cell >= 0 && (button == 0 || button == 1)) {
            InternalComponent target = button == 1
                    ? InternalComponent.EMPTY
                    : selectedComponent;

            mgc2Network.CHANNEL.sendToServer(
                    new CustomGearboxEditPacket(
                            menu.getBlockEntityPos(),
                            cell,
                            target.ordinal()
                    )
            );
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickLayerSelector(double mouseX,
                                       double mouseY) {
        double localX = toLocalX(mouseX);
        double localY = toLocalY(mouseY);

        int y = LAYER_BUTTON_Y;
        int previousX = GRID_LEFT;
        int nextX = GRID_LEFT
                + GRID_PIXELS
                - LAYER_BUTTON_WIDTH;

        if (inside(
                localX,
                localY,
                previousX,
                y,
                LAYER_BUTTON_WIDTH,
                LAYER_BUTTON_HEIGHT
        )) {
            selectedSlice = Math.floorMod(selectedSlice - 1, 3);
            return true;
        }

        if (inside(
                localX,
                localY,
                nextX,
                y,
                LAYER_BUTTON_WIDTH,
                LAYER_BUTTON_HEIGHT
        )) {
            selectedSlice = Math.floorMod(selectedSlice + 1, 3);
            return true;
        }

        return false;
    }

    private boolean clickPalette(double mouseX,
                                 double mouseY) {
        double localX = toLocalX(mouseX);
        double localY = toLocalY(mouseY);
        int x = PALETTE_LEFT;

        for (int i = 0; i < PALETTE.length; i++) {
            int y = PALETTE_TOP
                    + i * (PALETTE_HEIGHT + PALETTE_GAP);

            if (inside(
                    localX,
                    localY,
                    x,
                    y,
                    PALETTE_WIDTH,
                    PALETTE_HEIGHT
            )) {
                selectedComponent = PALETTE[i];
                return true;
            }
        }

        return false;
    }

    private int getHoveredCell(double mouseX,
                               double mouseY) {
        double localX = toLocalX(mouseX);
        double localY = toLocalY(mouseY);

        for (int displayY = 0; displayY < 3; displayY++) {
            for (int x = 0; x < 3; x++) {
                int screenX = GRID_LEFT + x * CELL_SIZE;
                int screenY = GRID_TOP
                        + displayY * CELL_SIZE;

                if (inside(
                        localX,
                        localY,
                        screenX,
                        screenY,
                        CELL_SIZE - 2,
                        CELL_SIZE - 2
                )) {
                    int y = 2 - displayY;
                    return CustomGearboxBlockEntity_wood.index(
                            x,
                            y,
                            selectedSlice
                    );
                }
            }
        }

        return -1;
    }

    private void renderHoverTooltip(GuiGraphics graphics,
                                    int mouseX,
                                    int mouseY) {
        CustomGearboxBlockEntity_wood gearbox = menu.getGearbox();
        if (gearbox == null) {
            return;
        }

        int cell = getHoveredCell(mouseX, mouseY);
        if (cell >= 0) {
            InternalComponent component = gearbox.getComponent(cell);
            int[] xyz = CustomGearboxBlockEntity_wood.coordinates(cell);

            String text = component == InternalComponent.EMPTY
                    ? "Empty cell"
                    : componentLabel(component);

            graphics.renderTooltip(
                    font,
                    Component.literal(
                            text
                                    + "  ["
                                    + xyz[0]
                                    + ", "
                                    + xyz[1]
                                    + ", "
                                    + xyz[2]
                                    + "]"
                    ),
                    mouseX,
                    mouseY
            );
        }
    }

    private double toLocalX(double mouseX) {
        return (mouseX - leftPos) / uiScale;
    }

    private double toLocalY(double mouseY) {
        return (mouseY - topPos) / uiScale;
    }

    private boolean inside(double mouseX,
                           double mouseY,
                           int x,
                           int y,
                           int width,
                           int height) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    private boolean isExternalPortCell(int x,
                                       int y,
                                       int z) {
        return ((x == 0 || x == 2) && y == 1 && z == 1)
                || ((y == 0 || y == 2) && x == 1 && z == 1)
                || ((z == 0 || z == 2) && x == 1 && y == 1);
    }

    private boolean isActivePortCell(CustomGearboxBlockEntity_wood gearbox,
                                     EnumSet<Direction> activePorts,
                                     int index) {
        for (Direction direction : activePorts) {
            if (CustomGearboxBlockEntity_wood.getPortCellIndex(direction)
                    == index) {
                return true;
            }
        }
        return false;
    }
}
