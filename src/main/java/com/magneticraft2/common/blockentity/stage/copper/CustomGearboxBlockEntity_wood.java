package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.client.gui.container.gearbox.CustomGearboxMenu;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;

/**
 * Player-configurable 3x3x3 wooden gearbox.
 *
 * The internal grid contains shaft and medium wooden gear elements. Axial neighbors
 * couple 1:1, parallel gears mesh as spur gears, and diagonal perpendicular gears mesh
 * as miter/bevel pairs. For now every internal gear is an 8-tooth wooden gear, so all
 * ratios are 1:1; the graph still tracks direction signs per external face so larger
 * ratio components can be added later without replacing the housing format.
 */
public class CustomGearboxBlockEntity_wood extends GearBlockEntity implements MenuProvider {
    public static final int GRID_SIZE = 3;
    public static final int CELL_COUNT = GRID_SIZE * GRID_SIZE * GRID_SIZE;

    private static final Direction[] PORT_ORDER = {
            Direction.WEST,
            Direction.EAST,
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH
    };

    private final InternalComponent[] components =
            new InternalComponent[CELL_COUNT];

    private boolean graphDirty = true;
    private final int[] cachedCellSigns = new int[CELL_COUNT];
    private final int[] cachedCellPhaseSteps = new int[CELL_COUNT];
    private final EnumMap<Direction, Integer> cachedPortSigns =
            new EnumMap<>(Direction.class);
    private final EnumSet<Direction> cachedActivePorts =
            EnumSet.noneOf(Direction.class);
    @Nullable
    private Direction cachedReferencePort;
    private boolean cachedGraphValid = true;

    public CustomGearboxBlockEntity_wood(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.CUSTOM_GEARBOX_BE_WOOD.get(), pos, state);
        Arrays.fill(components, InternalComponent.EMPTY);
        Arrays.fill(cachedCellPhaseSteps, -1);
    }

    public static <E extends BlockEntity> void serverTick(Level level,
                                                           BlockPos pos,
                                                           BlockState state,
                                                           E blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (blockEntity instanceof CustomGearboxBlockEntity_wood gearbox) {
            gearbox.serverTickGear();
        }
    }

    @Override
    public int getGearTeeth() {
        return 8;
    }

    @Override
    public float getGearMaxTorque() {
        return 8.0F;
    }

    @Override
    public boolean isShaftLike() {
        return true;
    }

    @Override
    public boolean supportsExternalGearMesh() {
        return false;
    }

    @Override
    public Direction.Axis getGearAxis() {
        Direction reference = getReferencePort();
        return reference == null
                ? Direction.Axis.X
                : reference.getAxis();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "screen.magneticraft2.custom_gearbox"
        );
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId,
                                            Inventory inventory,
                                            Player player) {
        return new CustomGearboxMenu(
                containerId,
                level,
                worldPosition,
                inventory,
                player
        );
    }

    public InternalComponent getComponent(int index) {
        if (index < 0 || index >= CELL_COUNT) {
            return InternalComponent.EMPTY;
        }
        return components[index];
    }

    public InternalComponent getComponent(int x, int y, int z) {
        return getComponent(index(x, y, z));
    }

    public boolean setComponentFromPlayer(ServerPlayer player,
                                          int cellIndex,
                                          InternalComponent next) {
        if (player == null
                || cellIndex < 0
                || cellIndex >= CELL_COUNT
                || next == null) {
            return false;
        }

        InternalComponent current = components[cellIndex];
        if (current == next) {
            return true;
        }

        ComponentCost oldCost = ComponentCost.of(current);
        ComponentCost newCost = ComponentCost.of(next);

        if (!player.getAbilities().instabuild
                && oldCost != newCost
                && newCost != ComponentCost.NONE) {
            Item required = newCost.getItem();
            if (!consumeOne(player, required)) {
                player.displayClientMessage(
                        Component.translatable(
                                newCost == ComponentCost.GEAR
                                        ? "message.magneticraft2.custom_gearbox_need_gear"
                                        : "message.magneticraft2.custom_gearbox_need_shaft"
                        ),
                        true
                );
                return false;
            }
        }

        if (!player.getAbilities().instabuild
                && oldCost != newCost
                && oldCost != ComponentCost.NONE) {
            ItemStack refund = new ItemStack(oldCost.getItem());
            if (!player.getInventory().add(refund)) {
                player.drop(refund, false);
            }
        }

        components[cellIndex] = next;
        onInternalLayoutChanged();
        return true;
    }

    private boolean consumeOne(ServerPlayer player, Item required) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(required)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    public void cycleComponent(int cellIndex, int delta) {
        if (cellIndex < 0 || cellIndex >= CELL_COUNT || delta == 0) {
            return;
        }

        InternalComponent current = components[cellIndex];
        InternalComponent[] values = InternalComponent.values();
        int next = Math.floorMod(
                current.ordinal() + Integer.signum(delta),
                values.length
        );

        components[cellIndex] = values[next];
        onInternalLayoutChanged();
    }

    public List<ItemStack> extractInstalledComponents() {
        int shafts = 0;
        int gears = 0;

        for (int i = 0; i < CELL_COUNT; i++) {
            ComponentCost cost = ComponentCost.of(components[i]);
            if (cost == ComponentCost.SHAFT) {
                shafts++;
            } else if (cost == ComponentCost.GEAR) {
                gears++;
            }

            components[i] = InternalComponent.EMPTY;
        }

        graphDirty = true;
        setChanged();

        List<ItemStack> drops = new ArrayList<>();
        if (shafts > 0) {
            drops.add(new ItemStack(
                    ItemRegistry.ITEM_SHAFT_WOOD.get(),
                    shafts
            ));
        }
        if (gears > 0) {
            drops.add(new ItemStack(
                    ItemRegistry.ITEM_GEAR_MEDIUM_WOOD.get(),
                    gears
            ));
        }

        return drops;
    }

    public void setComponent(int cellIndex, InternalComponent component) {
        if (cellIndex < 0 || cellIndex >= CELL_COUNT || component == null) {
            return;
        }

        if (components[cellIndex] == component) {
            return;
        }

        components[cellIndex] = component;
        onInternalLayoutChanged();
    }

    private void onInternalLayoutChanged() {
        graphDirty = true;
        setChanged();

        if (level != null) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS
            );

            if (!level.isClientSide) {
                updateGearNetwork();
            }
        }
    }

    @Nullable
    public Direction getReferencePort() {
        rebuildGraphCacheIfNeeded();
        return cachedReferencePort;
    }

    public boolean isGraphValid() {
        rebuildGraphCacheIfNeeded();
        return cachedGraphValid;
    }

    public EnumSet<Direction> getActivePorts() {
        rebuildGraphCacheIfNeeded();
        return cachedActivePorts.clone();
    }

    public boolean isPortActive(Direction port) {
        rebuildGraphCacheIfNeeded();
        return cachedGraphValid
                && cachedActivePorts.contains(port);
    }

    /**
     * Rotation sign of this port relative to the gearbox's reference port.
     *
     * +1 means the port uses the same Gear V2 positive-axis direction as the reference;
     * -1 means reversed; 0 means the port is not connected to the active internal graph.
     */
    public int getPortDirectionSign(Direction port) {
        rebuildGraphCacheIfNeeded();
        if (!cachedGraphValid) {
            return 0;
        }
        return cachedPortSigns.getOrDefault(port, 0);
    }

    public int getComponentDirectionSign(int cellIndex) {
        rebuildGraphCacheIfNeeded();
        if (!cachedGraphValid
                || cellIndex < 0
                || cellIndex >= CELL_COUNT) {
            return 0;
        }
        return cachedCellSigns[cellIndex];
    }

    /**
     * Static half-tooth phase used only for rendering. Every internal wooden
     * gear has 8 teeth, so one tooth pitch is 45 degrees and a meshing neighbor
     * must be offset by half of that: 22.5 degrees.
     */
    public float getComponentVisualPhaseDegrees(int cellIndex) {
        rebuildGraphCacheIfNeeded();
        if (!cachedGraphValid
                || cellIndex < 0
                || cellIndex >= CELL_COUNT) {
            return 0.0F;
        }

        return cachedCellPhaseSteps[cellIndex] == 1
                ? 22.5F
                : 0.0F;
    }

    public boolean isBevelGear(int cellIndex) {
        if (!getComponent(cellIndex).isGear()) {
            return false;
        }

        for (InternalEdge edge : getInternalEdges(cellIndex)) {
            InternalComponent neighbor = getComponent(edge.neighborIndex());
            if (neighbor.isGear()
                    && neighbor.getAxis()
                    != getComponent(cellIndex).getAxis()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Bit mask describing which side(s) of this gear's shaft contain a
     * perpendicular bevel mesh. Bit 0 = negative axis side, bit 1 = positive
     * axis side. The renderer uses this to point the bevel face toward the
     * actual shaft intersection instead of leaning every gear the same way.
     */
    public int getBevelSideMask(int cellIndex) {
        InternalComponent component = getComponent(cellIndex);
        if (!component.isGear() || component.getAxis() == null) {
            return 0;
        }

        int[] xyz = coordinates(cellIndex);
        int mask = 0;

        for (InternalEdge edge : getInternalEdges(cellIndex)) {
            InternalComponent neighbor = getComponent(edge.neighborIndex());
            if (!neighbor.isGear()
                    || neighbor.getAxis() == component.getAxis()) {
                continue;
            }

            int[] other = coordinates(edge.neighborIndex());
            int along = componentAlongAxis(
                    other[0] - xyz[0],
                    other[1] - xyz[1],
                    other[2] - xyz[2],
                    component.getAxis()
            );

            if (along < 0) {
                mask |= 1;
            } else if (along > 0) {
                mask |= 2;
            }
        }

        return mask;
    }

    private void rebuildGraphCacheIfNeeded() {
        if (!graphDirty) {
            return;
        }

        graphDirty = false;
        cachedReferencePort = null;
        cachedGraphValid = true;
        cachedActivePorts.clear();
        cachedPortSigns.clear();
        Arrays.fill(cachedCellSigns, 0);
        Arrays.fill(cachedCellPhaseSteps, -1);

        Direction bestReference = null;
        int bestReachablePortCount = -1;
        boolean conflictingPortComponent = false;

        for (Direction candidate : PORT_ORDER) {
            int candidateCell = getPortCellIndex(candidate);
            if (!componentCanExposePort(
                    components[candidateCell],
                    candidate)) {
                continue;
            }

            GraphTraversal traversal = traverseFrom(candidateCell);
            if (!traversal.valid()) {
                conflictingPortComponent = true;
                continue;
            }

            int reachablePorts = countReachablePorts(
                    traversal.signs()
            );

            if (reachablePorts > bestReachablePortCount) {
                bestReachablePortCount = reachablePorts;
                bestReference = candidate;
            }
        }

        if (conflictingPortComponent) {
            cachedGraphValid = false;
            return;
        }

        if (bestReference == null) {
            return;
        }

        int referenceCell = getPortCellIndex(bestReference);
        GraphTraversal finalTraversal = traverseFrom(referenceCell);
        if (!finalTraversal.valid()) {
            cachedGraphValid = false;
            return;
        }

        cachedReferencePort = bestReference;
        System.arraycopy(
                finalTraversal.signs(),
                0,
                cachedCellSigns,
                0,
                CELL_COUNT
        );
        System.arraycopy(
                finalTraversal.phaseSteps(),
                0,
                cachedCellPhaseSteps,
                0,
                CELL_COUNT
        );

        for (Direction port : PORT_ORDER) {
            int cell = getPortCellIndex(port);
            if (!componentCanExposePort(components[cell], port)) {
                continue;
            }

            int sign = cachedCellSigns[cell];
            if (sign == 0) {
                continue;
            }

            cachedActivePorts.add(port);
            cachedPortSigns.put(port, sign);
        }
    }

    private GraphTraversal traverseFrom(int startCell) {
        int[] signs = new int[CELL_COUNT];
        int[] phaseSteps = new int[CELL_COUNT];
        Arrays.fill(phaseSteps, -1);

        Queue<Integer> queue = new ArrayDeque<>();
        signs[startCell] = 1;
        phaseSteps[startCell] = 0;
        queue.add(startCell);

        boolean valid = true;

        while (!queue.isEmpty()) {
            int current = queue.poll();
            int currentSign = signs[current];
            int currentPhaseStep = phaseSteps[current];

            for (InternalEdge edge : getInternalEdges(current)) {
                int neighborIndex = edge.neighborIndex();
                int expectedSign = currentSign * edge.directionSign();
                int expectedPhaseStep = currentPhaseStep
                        ^ (isGearMeshEdge(current, neighborIndex) ? 1 : 0);

                int existingSign = signs[neighborIndex];
                int existingPhaseStep = phaseSteps[neighborIndex];

                if (existingSign == 0) {
                    signs[neighborIndex] = expectedSign;
                    phaseSteps[neighborIndex] = expectedPhaseStep;
                    queue.add(neighborIndex);
                } else {
                    if (existingSign != expectedSign) {
                        valid = false;
                    }
                    if (existingPhaseStep != expectedPhaseStep) {
                        // Even if the rotation signs happen to close, an odd
                        // tooth-mesh loop cannot keep every 8T pair interleaved.
                        valid = false;
                    }
                }
            }
        }

        return new GraphTraversal(signs, phaseSteps, valid);
    }

    private boolean isGearMeshEdge(int firstIndex,
                                   int secondIndex) {
        InternalComponent first = getComponent(firstIndex);
        InternalComponent second = getComponent(secondIndex);

        if (!first.isGear() || !second.isGear()) {
            return false;
        }

        if (first.getAxis() != second.getAxis()) {
            // Any valid perpendicular gear edge is a bevel/miter mesh.
            return true;
        }

        int[] firstPos = coordinates(firstIndex);
        int[] secondPos = coordinates(secondIndex);
        Direction.Axis deltaAxis = nonZeroAxis(
                secondPos[0] - firstPos[0],
                secondPos[1] - firstPos[1],
                secondPos[2] - firstPos[2]
        );

        // Same-axis neighbors along their shaft are rigidly coupled. A
        // same-axis neighbor beside the shaft is a spur-gear mesh.
        return deltaAxis != null
                && deltaAxis != first.getAxis();
    }

    private int countReachablePorts(int[] signs) {
        int count = 0;

        for (Direction port : PORT_ORDER) {
            int cell = getPortCellIndex(port);
            if (signs[cell] != 0
                    && componentCanExposePort(components[cell], port)) {
                count++;
            }
        }

        return count;
    }

    private List<InternalEdge> getInternalEdges(int index) {
        InternalComponent current = getComponent(index);
        if (current == InternalComponent.EMPTY) {
            return List.of();
        }

        int[] xyz = coordinates(index);
        List<InternalEdge> edges = new ArrayList<>();

        for (int other = 0; other < CELL_COUNT; other++) {
            if (other == index) {
                continue;
            }

            InternalComponent neighbor = getComponent(other);
            if (neighbor == InternalComponent.EMPTY) {
                continue;
            }

            int[] otherXyz = coordinates(other);
            int dx = otherXyz[0] - xyz[0];
            int dy = otherXyz[1] - xyz[1];
            int dz = otherXyz[2] - xyz[2];

            int sign = internalConnectionSign(
                    current,
                    neighbor,
                    dx,
                    dy,
                    dz
            );

            if (sign != 0) {
                edges.add(new InternalEdge(other, sign));
            }
        }

        return edges;
    }

    private int internalConnectionSign(InternalComponent first,
                                       InternalComponent second,
                                       int dx,
                                       int dy,
                                       int dz) {
        int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);

        if (first.getAxis() == second.getAxis()
                && manhattan == 1) {
            Direction.Axis deltaAxis = nonZeroAxis(dx, dy, dz);

            // Same axle: shaft/gear hubs couple directly.
            if (deltaAxis == first.getAxis()) {
                return 1;
            }

            // Parallel medium gears one cell apart mesh as ordinary spur gears.
            if (first.isGear() && second.isGear()) {
                return -1;
            }

            return 0;
        }

        if (!first.isGear()
                || !second.isGear()
                || first.getAxis() == second.getAxis()) {
            return 0;
        }

        // Perpendicular miter gears live on diagonal cells so their two shaft axes
        // intersect at the corner between them.
        if (manhattan != 2
                || Math.abs(dx) > 1
                || Math.abs(dy) > 1
                || Math.abs(dz) > 1) {
            return 0;
        }

        int firstDelta = componentAlongAxis(
                dx,
                dy,
                dz,
                first.getAxis()
        );
        int secondDelta = componentAlongAxis(
                dx,
                dy,
                dz,
                second.getAxis()
        );

        Direction.Axis thirdAxis = thirdAxis(
                first.getAxis(),
                second.getAxis()
        );
        int thirdDelta = componentAlongAxis(
                dx,
                dy,
                dz,
                thirdAxis
        );

        if (Math.abs(firstDelta) != 1
                || Math.abs(secondDelta) != 1
                || thirdDelta != 0) {
            return 0;
        }

        return Integer.signum(firstDelta)
                * Integer.signum(secondDelta);
    }

    @Nullable
    private Direction.Axis nonZeroAxis(int dx, int dy, int dz) {
        if (dx != 0 && dy == 0 && dz == 0) {
            return Direction.Axis.X;
        }
        if (dy != 0 && dx == 0 && dz == 0) {
            return Direction.Axis.Y;
        }
        if (dz != 0 && dx == 0 && dy == 0) {
            return Direction.Axis.Z;
        }
        return null;
    }

    private int componentAlongAxis(int dx,
                                   int dy,
                                   int dz,
                                   Direction.Axis axis) {
        return switch (axis) {
            case X -> dx;
            case Y -> dy;
            case Z -> dz;
        };
    }

    private Direction.Axis thirdAxis(Direction.Axis first,
                                     Direction.Axis second) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis != first && axis != second) {
                return axis;
            }
        }
        return Direction.Axis.Z;
    }

    private boolean componentCanExposePort(InternalComponent component,
                                           Direction port) {
        return component != InternalComponent.EMPTY
                && component.getAxis() == port.getAxis();
    }

    public static int getPortCellIndex(Direction direction) {
        return switch (direction) {
            case WEST -> index(0, 1, 1);
            case EAST -> index(2, 1, 1);
            case DOWN -> index(1, 0, 1);
            case UP -> index(1, 2, 1);
            case NORTH -> index(1, 1, 0);
            case SOUTH -> index(1, 1, 2);
        };
    }

    public static int index(int x, int y, int z) {
        return x + y * GRID_SIZE + z * GRID_SIZE * GRID_SIZE;
    }

    public static int[] coordinates(int index) {
        int z = index / 9;
        int remainder = index % 9;
        int y = remainder / 3;
        int x = remainder % 3;
        return new int[]{x, y, z};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        byte[] data = new byte[CELL_COUNT];
        for (int i = 0; i < CELL_COUNT; i++) {
            data[i] = (byte) components[i].ordinal();
        }

        tag.putByteArray("InternalComponents", data);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        Arrays.fill(components, InternalComponent.EMPTY);
        byte[] data = tag.getByteArray("InternalComponents");
        InternalComponent[] values = InternalComponent.values();

        for (int i = 0; i < Math.min(data.length, CELL_COUNT); i++) {
            int ordinal = Byte.toUnsignedInt(data[i]);
            if (ordinal >= 0 && ordinal < values.length) {
                components[i] = values[ordinal];
            }
        }

        graphDirty = true;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net,
                             ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    private enum ComponentCost {
        NONE,
        SHAFT,
        GEAR;

        static ComponentCost of(InternalComponent component) {
            if (component == null || component == InternalComponent.EMPTY) {
                return NONE;
            }
            return component.isGear() ? GEAR : SHAFT;
        }

        Item getItem() {
            return this == GEAR
                    ? ItemRegistry.ITEM_GEAR_MEDIUM_WOOD.get()
                    : ItemRegistry.ITEM_SHAFT_WOOD.get();
        }
    }

    public enum InternalComponent {
        EMPTY(null, false, "."),
        SHAFT_X(Direction.Axis.X, false, "SX"),
        SHAFT_Y(Direction.Axis.Y, false, "SY"),
        SHAFT_Z(Direction.Axis.Z, false, "SZ"),
        GEAR_X(Direction.Axis.X, true, "GX"),
        GEAR_Y(Direction.Axis.Y, true, "GY"),
        GEAR_Z(Direction.Axis.Z, true, "GZ");

        @Nullable
        private final Direction.Axis axis;
        private final boolean gear;
        private final String shortName;

        InternalComponent(@Nullable Direction.Axis axis,
                          boolean gear,
                          String shortName) {
            this.axis = axis;
            this.gear = gear;
            this.shortName = shortName;
        }

        @Nullable
        public Direction.Axis getAxis() {
            return axis;
        }

        public boolean isGear() {
            return gear;
        }

        public String getShortName() {
            return shortName;
        }
    }

    private record InternalEdge(int neighborIndex,
                                int directionSign) {
    }

    private record GraphTraversal(int[] signs,
                                  int[] phaseSteps,
                                  boolean valid) {
    }
}
