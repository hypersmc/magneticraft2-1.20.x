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
 * Only one north/south layer is shown at a time. The player first selects a
 * component from the palette and then places it into the grid. Right-clicking
 * a cell clears it. This keeps the editor spatial instead of exposing the
 * internal graph/debug representation.
 */
public class CustomGearboxScreen extends AbstractContainerScreen<CustomGearboxMenu> {
    private static final int CELL_SIZE = 28;
    private static final int GRID_PIXELS = CELL_SIZE * 3;
    private static final int GRID_LEFT = 30;
    private static final int GRID_TOP = 70;

    private static final int PALETTE_LEFT = 132;
    private static final int PALETTE_TOP = 55;
    private static final int PALETTE_WIDTH = 142;
    private static final int PALETTE_HEIGHT = 17;
    private static final int PALETTE_GAP = 2;

    private static final int LAYER_BUTTON_Y = 43;
    private static final int LAYER_BUTTON_SIZE = 18;

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

    public CustomGearboxScreen(CustomGearboxMenu menu,
                               Inventory inventory,
                               Component title) {
        super(menu, inventory, title);
        imageWidth = 292;
        imageHeight = 212;
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
        int left = leftPos;
        int top = topPos;

        graphics.fill(
                left,
                top,
                left + imageWidth,
                top + imageHeight,
                0xEE211A13
        );
        graphics.fill(
                left + 4,
                top + 4,
                left + imageWidth - 4,
                top + imageHeight - 4,
                0xFF35291E
        );

        graphics.drawString(
                font,
                Component.translatable("screen.magneticraft2.custom_gearbox"),
                left + 10,
                top + 10,
                0xFFF0D7A4,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("screen.magneticraft2.custom_gearbox_help"),
                left + 10,
                top + 25,
                0xFFB9A98E,
                false
        );

        CustomGearboxBlockEntity_wood gearbox = menu.getGearbox();
        if (gearbox == null) {
            graphics.drawString(
                    font,
                    "Gearbox unavailable",
                    left + 10,
                    top + 48,
                    0xFFFF5555,
                    false
            );
            return;
        }

        renderLayerSelector(graphics, left, top);
        renderGrid(graphics, gearbox, left, top);
        renderPalette(graphics, left, top);
        renderStatus(graphics, gearbox, left, top);
    }

    private void renderLayerSelector(GuiGraphics graphics,
                                     int left,
                                     int top) {
        int previousX = left + GRID_LEFT;
        int nextX = left + GRID_LEFT + GRID_PIXELS - LAYER_BUTTON_SIZE;

        drawButton(
                graphics,
                previousX,
                top + LAYER_BUTTON_Y,
                LAYER_BUTTON_SIZE,
                16,
                "<",
                false
        );
        drawButton(
                graphics,
                nextX,
                top + LAYER_BUTTON_Y,
                LAYER_BUTTON_SIZE,
                16,
                ">",
                false
        );

        String layerName = switch (selectedSlice) {
            case 0 -> "NORTH";
            case 2 -> "SOUTH";
            default -> "CENTER";
        };

        graphics.drawCenteredString(
                font,
                "Layer " + (selectedSlice + 1) + " / 3  " + layerName,
                left + GRID_LEFT + GRID_PIXELS / 2,
                top + 47,
                0xFFE5C78F
        );

        graphics.drawCenteredString(
                font,
                "UP",
                left + GRID_LEFT + GRID_PIXELS / 2,
                top + GRID_TOP - 10,
                0xFF9D8E78
        );
        graphics.drawCenteredString(
                font,
                "DOWN",
                left + GRID_LEFT + GRID_PIXELS / 2,
                top + GRID_TOP + GRID_PIXELS + 3,
                0xFF9D8E78
        );
        graphics.drawString(
                font,
                "W",
                left + GRID_LEFT - 10,
                top + GRID_TOP + GRID_PIXELS / 2 - 4,
                0xFF9D8E78,
                false
        );
        graphics.drawString(
                font,
                "E",
                left + GRID_LEFT + GRID_PIXELS + 4,
                top + GRID_TOP + GRID_PIXELS / 2 - 4,
                0xFF9D8E78,
                false
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
                        screenX + CELL_SIZE - 1,
                        screenY + CELL_SIZE - 1,
                        border
                );
                graphics.fill(
                        screenX + 2,
                        screenY + 2,
                        screenX + CELL_SIZE - 3,
                        screenY + CELL_SIZE - 3,
                        0xFF251F1A
                );

                if (portCell) {
                    graphics.drawString(
                            font,
                            "P",
                            screenX + 3,
                            screenY + 3,
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

        int centerX = x + CELL_SIZE / 2;
        int centerY = y + CELL_SIZE / 2;

        if (component.isGear()) {
            graphics.fill(
                    centerX - 8,
                    centerY - 3,
                    centerX + 9,
                    centerY + 4,
                    iconColor
            );
            graphics.fill(
                    centerX - 3,
                    centerY - 8,
                    centerX + 4,
                    centerY + 9,
                    iconColor
            );
            graphics.fill(
                    centerX - 6,
                    centerY - 6,
                    centerX + 7,
                    centerY + 7,
                    iconColor
            );
        } else if (axis == Direction.Axis.X) {
            graphics.fill(
                    centerX - 9,
                    centerY - 2,
                    centerX + 10,
                    centerY + 3,
                    iconColor
            );
        } else if (axis == Direction.Axis.Y) {
            graphics.fill(
                    centerX - 2,
                    centerY - 9,
                    centerX + 3,
                    centerY + 10,
                    iconColor
            );
        } else {
            graphics.fill(
                    centerX - 5,
                    centerY - 5,
                    centerX + 6,
                    centerY + 6,
                    iconColor
            );
            graphics.fill(
                    centerX - 2,
                    centerY - 2,
                    centerX + 3,
                    centerY + 3,
                    0xFF251F1A
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
        graphics.drawString(
                font,
                "PLACE PART",
                left + PALETTE_LEFT,
                top + 43,
                0xFFE5C78F,
                false
        );

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

        graphics.drawString(
                font,
                "X = WEST-EAST",
                left + PALETTE_LEFT,
                top + 190,
                0xFF9D8E78,
                false
        );
        graphics.drawString(
                font,
                "Y = DOWN-UP   Z = NORTH-SOUTH",
                left + PALETTE_LEFT,
                top + 200,
                0xFF9D8E78,
                false
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
                "Connected faces: " + ports,
                left + 10,
                top + 166,
                activePorts.isEmpty() ? 0xFFB9A98E : 0xFFB9DCA6,
                false
        );

        graphics.drawString(
                font,
                gearbox.isGraphValid()
                        ? "Mechanical path: valid"
                        : "Mechanical path: conflicting gear loop",
                left + 10,
                top + 179,
                gearbox.isGraphValid() ? 0xFF91D67B : 0xFFFF6666,
                false
        );

        if (selectedSlice == 0 || selectedSlice == 2) {
            graphics.drawString(
                    font,
                    selectedSlice == 0
                            ? "Center cell can expose the NORTH face."
                            : "Center cell can expose the SOUTH face.",
                    left + 10,
                    top + 192,
                    0xFFB9A98E,
                    false
            );
        } else {
            graphics.drawString(
                    font,
                    "Edge-center cells expose WEST / EAST / UP / DOWN.",
                    left + 10,
                    top + 192,
                    0xFFB9A98E,
                    false
            );
        }
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
            case SHAFT_X -> "Shaft X   W-E";
            case SHAFT_Y -> "Shaft Y   D-U";
            case SHAFT_Z -> "Shaft Z   N-S";
            case GEAR_X -> "Gear X    W-E";
            case GEAR_Y -> "Gear Y    D-U";
            case GEAR_Z -> "Gear Z    N-S";
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
        int y = topPos + LAYER_BUTTON_Y;
        int previousX = leftPos + GRID_LEFT;
        int nextX = leftPos + GRID_LEFT
                + GRID_PIXELS
                - LAYER_BUTTON_SIZE;

        if (inside(
                mouseX,
                mouseY,
                previousX,
                y,
                LAYER_BUTTON_SIZE,
                16
        )) {
            selectedSlice = Math.floorMod(selectedSlice - 1, 3);
            return true;
        }

        if (inside(
                mouseX,
                mouseY,
                nextX,
                y,
                LAYER_BUTTON_SIZE,
                16
        )) {
            selectedSlice = Math.floorMod(selectedSlice + 1, 3);
            return true;
        }

        return false;
    }

    private boolean clickPalette(double mouseX,
                                 double mouseY) {
        int x = leftPos + PALETTE_LEFT;

        for (int i = 0; i < PALETTE.length; i++) {
            int y = topPos + PALETTE_TOP
                    + i * (PALETTE_HEIGHT + PALETTE_GAP);

            if (inside(
                    mouseX,
                    mouseY,
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
        for (int displayY = 0; displayY < 3; displayY++) {
            for (int x = 0; x < 3; x++) {
                int screenX = leftPos + GRID_LEFT + x * CELL_SIZE;
                int screenY = topPos + GRID_TOP
                        + displayY * CELL_SIZE;

                if (inside(
                        mouseX,
                        mouseY,
                        screenX,
                        screenY,
                        CELL_SIZE - 1,
                        CELL_SIZE - 1
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
