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
 * Compact 3-slice editor for the custom gearbox's 3x3x3 internal assembly.
 *
 * Left click cycles forward:
 *   Empty -> Shaft X/Y/Z -> Gear X/Y/Z -> Empty
 *
 * Right click cycles backwards.
 */
public class CustomGearboxScreen extends AbstractContainerScreen<CustomGearboxMenu> {
    private static final int CELL_SIZE = 18;
    private static final int SLICE_WIDTH = CELL_SIZE * 3;
    private static final int SLICE_GAP = 18;
    private static final int GRID_TOP = 53;
    private static final int GRID_LEFT = 16;

    public CustomGearboxScreen(CustomGearboxMenu menu,
                               Inventory inventory,
                               Component title) {
        super(menu, inventory, title);
        imageWidth = 250;
        imageHeight = 190;
    }

    @Override
    public void render(GuiGraphics graphics,
                       int mouseX,
                       int mouseY,
                       float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
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

        CustomGearboxBlockEntity_wood gearbox = menu.getGearbox();

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.magneticraft2.custom_gearbox"
                ),
                left + 10,
                top + 10,
                0xFFF0D7A4,
                false
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "screen.magneticraft2.custom_gearbox_help"
                ),
                left + 10,
                top + 25,
                0xFFB9A98E,
                false
        );

        if (gearbox == null) {
            graphics.drawString(
                    font,
                    "Gearbox unavailable",
                    left + 10,
                    top + 43,
                    0xFFFF5555,
                    false
            );
            return;
        }

        String[] sliceNames = {
                "NORTH slice",
                "CENTER slice",
                "SOUTH slice"
        };

        EnumSet<Direction> activePorts = gearbox.getActivePorts();

        for (int z = 0; z < 3; z++) {
            int sliceX = left + GRID_LEFT
                    + z * (SLICE_WIDTH + SLICE_GAP);

            graphics.drawString(
                    font,
                    sliceNames[z],
                    sliceX,
                    top + 42,
                    0xFFE5C78F,
                    false
            );

            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    int screenX = sliceX + x * CELL_SIZE;
                    int screenY = top + GRID_TOP
                            + (2 - y) * CELL_SIZE;

                    int index =
                            CustomGearboxBlockEntity_wood.index(x, y, z);
                    InternalComponent component =
                            gearbox.getComponent(index);

                    boolean portCell = isExternalPortCell(x, y, z);
                    boolean activePortCell =
                            portCell && isActivePortCell(
                                    gearbox,
                                    activePorts,
                                    index
                            );

                    int border = activePortCell
                            ? 0xFF7FCB62
                            : portCell
                            ? 0xFF8B704D
                            : 0xFF594838;

                    int fill = component == InternalComponent.EMPTY
                            ? 0xFF2A241E
                            : component.isGear()
                            ? 0xFF80612C
                            : 0xFF5D4229;

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
                            fill
                    );

                    int textColor = component == InternalComponent.EMPTY
                            ? 0xFF7D7264
                            : 0xFFFFFFFF;

                    graphics.drawCenteredString(
                            font,
                            component.getShortName(),
                            screenX + (CELL_SIZE - 1) / 2,
                            screenY + 5,
                            textColor
                    );
                }
            }
        }

        Direction reference = gearbox.getReferencePort();
        String referenceText = reference == null
                ? "Reference: none"
                : "Reference: " + reference.getName().toUpperCase();

        graphics.drawString(
                font,
                referenceText,
                left + 10,
                top + 119,
                0xFFF0D7A4,
                false
        );

        String portsText = activePorts.isEmpty()
                ? "Active ports: none"
                : "Active ports: " + activePorts.stream()
                .map(direction -> direction.getName().toUpperCase()
                        + (gearbox.getPortDirectionSign(direction) < 0
                        ? "(-)"
                        : "(+)"))
                .reduce((a, b) -> a + " " + b)
                .orElse("none");

        graphics.drawString(
                font,
                portsText,
                left + 10,
                top + 134,
                0xFFB9DCA6,
                false
        );

        if (!gearbox.isGraphValid()) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "screen.magneticraft2.custom_gearbox_conflict"
                    ),
                    left + 10,
                    top + 149,
                    0xFFFF6666,
                    false
            );
        } else {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "screen.magneticraft2.custom_gearbox_rules"
                    ),
                    left + 10,
                    top + 149,
                    0xFFB9A98E,
                    false
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics,
                                int mouseX,
                                int mouseY) {
        // All text is positioned in renderBg because this screen has no inventory slots.
    }

    @Override
    public boolean mouseClicked(double mouseX,
                                double mouseY,
                                int button) {
        int cell = getHoveredCell(mouseX, mouseY);
        if (cell >= 0 && (button == 0 || button == 1)) {
            mgc2Network.CHANNEL.sendToServer(
                    new CustomGearboxEditPacket(
                            menu.getBlockEntityPos(),
                            cell,
                            button == 0 ? 1 : -1
                    )
            );
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getHoveredCell(double mouseX, double mouseY) {
        for (int z = 0; z < 3; z++) {
            int sliceX = leftPos + GRID_LEFT
                    + z * (SLICE_WIDTH + SLICE_GAP);

            for (int displayY = 0; displayY < 3; displayY++) {
                for (int x = 0; x < 3; x++) {
                    int screenX = sliceX + x * CELL_SIZE;
                    int screenY = topPos + GRID_TOP
                            + displayY * CELL_SIZE;

                    if (mouseX >= screenX
                            && mouseX < screenX + CELL_SIZE - 1
                            && mouseY >= screenY
                            && mouseY < screenY + CELL_SIZE - 1) {
                        int y = 2 - displayY;
                        return CustomGearboxBlockEntity_wood.index(
                                x,
                                y,
                                z
                        );
                    }
                }
            }
        }

        return -1;
    }

    private boolean isExternalPortCell(int x, int y, int z) {
        return (x == 0 || x == 2) && y == 1 && z == 1
                || (y == 0 || y == 2) && x == 1 && z == 1
                || (z == 0 || z == 2) && x == 1 && y == 1;
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
