package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeIndustrialAirBlowerBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalInputModuleBlockEntity;
import com.magneticraft2.common.blockentity.stage.stone.BellowsMultiblockModuleEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockRenderParts;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelHost;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelNbt;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelProfiles;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressure;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressurizedGas;
import com.magneticraft2.common.systems.PRESSURE.GasMedium;
import com.magneticraft2.common.systems.PRESSURE.IPressurizedGasHandler;
import com.magneticraft2.common.systems.PRESSURE.LegacyAirPort;
import com.magneticraft2.common.systems.PRESSURE.PressurizedPipeNetwork;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.PRESSURE.IPressureStorage;
import com.magneticraft2.common.utils.MultiBlockProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;

/**
 * Bronze IV K1 industrial combustion-air blower. Uses Mechanical Power torque/load and
 * the existing pressure capability used by the Primitive Furnace bellows module.
 *
 * Air is a buffered, integer unit (NOT Forge fluid gas, HEAT or a new network).
 * Only the front face of the formed controller exports an output-only capability.
 * Its outlet may feed a directly adjacent receiver or a bounded passive bronze
 * air-duct network. This is pressure-unit transport, not full gas physics.
 */
public final class BronzeIndustrialAirBlowerBlockEntity extends BaseBlockEntityMagneticraft2
        implements MultiblockVoxelHost {
    private static final String PREFIX = "bronze_industrial_air_blower_";
    public static final int AIR_CAPACITY = 16;
    public static final int TRANSFER_RATE = 1;
    public static final int PUMP_INTERVAL = 5;
    public static final float MIN_RPM = 16.0F;
    public static final float TORQUE_DEMAND = 5.0F;
    private static final float EPS = 0.01F;
    private boolean formed, running;
    private String blueprintName = "", replacementModel = "", voxelProfileName = "";
    private Direction matchedFacing = Direction.SOUTH;
    private java.util.List<BlockPos> renderAirAnchors = java.util.List.of();
    @Nullable private VoxelShape collision, selection;
    private int pumpProgress;
    // Share the same per-game-tick extraction budget across automatic transfer
    // and integrations querying the Forge capability repeatedly.
    private long lastOutputTick = Long.MIN_VALUE;
    private int outputThisTick;
    private int nextReceiver; // Round-robin duct branch selection, not persistent pressure.

    // Isolated, directional send-only view of internal mutable pressure storage.
    // The storage itself may receive pressure to account for generated air, but
    // callers must NEVER inject free air back into the blower.
    private final LazyOptional<IPressureStorage> output = LazyOptional.of(() -> new IPressureStorage() {
        @Override public int receivePressure(int units, boolean simulate) { return 0; }
        @Override public int extractPressure(int units, boolean simulate) {
            return drainAir(units, simulate);
        }
        @Override public int getPressureStored() { return formed ? pressureHandler.getPressureStored() : 0; }
        @Override public int getMaxPressureStored() { return AIR_CAPACITY; }
        @Override public boolean canSend() { return formed; }
        @Override public boolean canReceive() { return false; }
        @Override public boolean setSend(boolean val) { return val && formed; }
        @Override public boolean setReceive(boolean val) { return !val; }
    });

    // Typed AIR port for future pneumatic consumers; the existing pressure
    // capability remains available to old integrations and saved machinery.
    private final LazyOptional<IPressurizedGasHandler> gasOutput =
            LazyOptional.of(() -> new LegacyAirPort(output.orElseThrow(
                    () -> new IllegalStateException("Missing blower air capability"))));

    public BronzeIndustrialAirBlowerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BRONZE_INDUSTRIAL_AIR_BLOWER_BE.get(), pos, state);
        menuProvider = null;
    }

    public boolean isFormed() { return formed; }
    public boolean isRunning() { return running; }
    public int getAirStored() { return pressureHandler.getPressureStored(); }
    public float getRotorAngle(float partial) {
        MechanicalInputModuleBlockEntity module = getMechanicalInput();
        return module == null ? 0F : module.getVisualRotationDegrees(partial);
    }

    public static <E extends BlockEntity> void serverTick(Level world, BlockPos pos,
            BlockState state, E tile) {
        if (world.isClientSide || !(tile instanceof BronzeIndustrialAirBlowerBlockEntity blower)) return;
        blower.tickBlower();
    }
    private void setRunning(boolean active) {
        if (running == active) return;
        running = active;
        sync();
    }
    private void releaseLoad() {
        if (level != null) GearNetworkManager.getInstance().removeMechanicalLoad(level, worldPosition);
    }
    @Nullable public MechanicalInputModuleBlockEntity getMechanicalInput() {
        if (level == null || !formed) return null;
        BlockPos pos = getMultiblockController() == null ? null
                : getMultiblockController().getmodulePos("mechanical_input");
        if (pos == null) pos = worldPosition.relative(matchedFacing.getOpposite(), 2);
        BlockEntity tile = level.getBlockEntity(pos);
        return tile instanceof MechanicalInputModuleBlockEntity module ? module : null;
    }

    private int drainAir(int units, boolean simulate) {
        if (level == null || !formed || units <= 0) return 0;
        long tick = level.getGameTime();
        int spent = lastOutputTick == tick ? outputThisTick : 0;
        int permitted = Math.min(units, Math.max(0, TRANSFER_RATE - spent));
        if (permitted <= 0) return 0;
        int got = pressureHandler.extractPressure(permitted, simulate);
        if (got > 0 && !simulate) {
            if (lastOutputTick != tick) {
                lastOutputTick = tick;
                outputThisTick = 0;
            }
            outputThisTick += got;
            setChanged();
        }
        return got;
    }

    private void transferAir() {
        if (level == null || !formed || drainAir(TRANSFER_RATE, true) == 0) return;
        BlockPos next = worldPosition.relative(matchedFacing);
        if (!level.hasChunkAt(next)) return;
        // The physical outlet may contact a pressure-capable furnace directly
        // (old K1 saves remain valid) or an independent passive air-duct graph.
        if (level.getBlockState(next).is(BlockRegistry.BRONZE_AIR_DUCT.get())) {
            // Bounded BFS every five ticks, not every tick/frame. No duct BE,
            // active pipe tickers or world-wide network scans.
            if (level.getGameTime() % PUMP_INTERVAL != 0) return;
            IPressureStorage send = output.orElse(null);
            if (send != null) {
                PressurizedPipeNetwork.Transfer result = PressurizedPipeNetwork.transfer(
                        level, next, worldPosition, new LegacyAirPort(send), nextReceiver);
                nextReceiver = result.nextReceiver();
            }
            return;
        }
        BlockEntity neighbor = level.getBlockEntity(next);
        if (neighbor == null) return;
        // Never deliver to the old bellows module unless it belongs to an
        // actually formed Primitive Furnace structure.
        if (neighbor instanceof BellowsMultiblockModuleEntity bellows
                && !bellows.isFormedModule()) return;
        IPressurizedGasHandler receiver = PressurizedPipeNetwork.port(
                neighbor, matchedFacing.getOpposite(), GasMedium.AIR);
        IPressureStorage send = output.orElse(null);
        if (send != null && receiver != null)
            PressurizedPipeNetwork.transferDirect(new LegacyAirPort(send), receiver, 0);
    }

    private void tickBlower() {
        if (level == null || !formed) {
            releaseLoad();
            setRunning(false);
            pumpProgress = 0;
            return;
        }
        // Buffered air is still available after drive stops. No free air can be
        // produced by passive neighbor queries or on the client.
        transferAir();
        MechanicalInputModuleBlockEntity module = getMechanicalInput();
        GearNode node = module == null ? null : module.getOrCreateGearNode();
        boolean room = pressureHandler.getPressureStored() < AIR_CAPACITY;
        // Qualify raw speed to prevent Mechanical Power overload on/off oscillation.
        boolean canAttempt = room && node != null && node.getSpeed() >= MIN_RPM
                && node.getTorque() + EPS >= TORQUE_DEMAND;
        GearNetworkManager manager = GearNetworkManager.getInstance();
        manager.setMechanicalLoad(level, worldPosition,
                module == null ? worldPosition : module.getBlockPos(),
                TORQUE_DEMAND, canAttempt);
        boolean supplied = canAttempt && manager.getMechanicalLoadState(level, worldPosition).supplied();
        setRunning(supplied);
        if (!supplied) {
            pumpProgress = 0;
            return;
        }
        if (++pumpProgress < PUMP_INTERVAL) {
            setChanged();
            if (level.getGameTime() % 20L == 0L) sync();
            return;
        }
        pumpProgress = 0;
        int gained = pressureHandler.receivePressure(1, false);
        if (gained > 0) {
            transferAir();
            setChanged();
            if (level.getGameTime() % 20L == 0L) sync();
        }
    }

    @Override protected void interactableNoGui(BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide || !formed) return;
        MechanicalInputModuleBlockEntity module = getMechanicalInput();
        GearNode node = module == null ? null : module.getOrCreateGearNode();
        GearNetworkManager.MechanicalLoadState load =
                GearNetworkManager.getInstance().getMechanicalLoadState(world, worldPosition);
        BlockPos next = worldPosition.relative(matchedFacing);
        boolean receiverPresent = world.hasChunkAt(next)
                && (world.getBlockState(next).is(BlockRegistry.BRONZE_AIR_DUCT.get())
                    ? !PressurizedPipeNetwork.receivers(world, next, worldPosition,
                            GasMedium.AIR).isEmpty()
                    : world.getBlockEntity(next) != null
                      && world.getBlockEntity(next)
                         .getCapability(CapabilityPressure.PRESSURE, matchedFacing.getOpposite())
                         .map(IPressureStorage::canReceive).orElse(false));
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.bronze_industrial_air_blower.status",
                running, pressureHandler.getPressureStored(), AIR_CAPACITY,
                pumpProgress, PUMP_INTERVAL, receiverPresent), false);
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.bronze_industrial_air_blower.diagnostic",
                String.format(Locale.ROOT, "%.1f", node == null ? 0F : node.getSpeed()),
                String.format(Locale.ROOT, "%.1f", MIN_RPM),
                String.format(Locale.ROOT, "%.1f", node == null ? 0F : node.getTorque()),
                String.format(Locale.ROOT, "%.1f", TORQUE_DEMAND),
                load.supplied(), load.active()), false);
    }

    @Override protected @Nullable MultiblockStructure identifyMultiblockStructure(Level world,
            BlockPos pos) {
        for (Multiblock mb : MultiblockRegistry.getRegisteredMultiblocks().values()) {
            if (!mb.getName().startsWith(PREFIX)) continue;
            MultiblockStructure structure = mb.getStructure();
            if (!matchesStructure(world, pos, structure, mb)) continue;
            blueprintName = mb.getName();
            matchedFacing = facingFromName(blueprintName);
            replacementModel = mb.getSettings().getReplaceWhenFormed();
            voxelProfileName = mb.getSettings().getVoxelProfile();
            renderAirAnchors = mb.getSettings().getRenderAirAnchors();
            return structure;
        }
        return null;
    }
    private static Direction facingFromName(String name) {
        if (name.endsWith("_north")) return Direction.NORTH;
        if (name.endsWith("_west")) return Direction.WEST;
        if (name.endsWith("_east")) return Direction.EAST;
        return Direction.SOUTH;
    }
    @Override protected @Nullable MultiblockController createMultiblockController() {
        if (level == null || level.isClientSide || formed) return null;
        MultiblockStructure structure = identifyMultiblockStructure(level, worldPosition);
        if (structure == null) return null;
        if (!MultiblockVoxelProfiles.exists(voxelProfileName)) {
            org.apache.logging.log4j.LogManager.getLogger("Magneticraft2Voxels").error(
                    "Cannot form Air Blower: invalid Voxel profile {}", voxelProfileName);
            return null;
        }
        MultiblockController controller = new MultiblockController(structure);
        controller.identifyAndAddModules(level, worldPosition, structure);
        if (!controller.createStructure(level, worldPosition)) return null;
        controller.setFormed(true);
        setMultiblockController(controller);
        formed = true;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(BronzeIndustrialAirBlowerBlock.IS_FORMED)) {
            level.setBlock(worldPosition, state
                    .setValue(BronzeIndustrialAirBlowerBlock.IS_FORMED, true)
                    .setValue(BronzeIndustrialAirBlowerBlock.FACING, matchedFacing), Block.UPDATE_ALL);
        }
        MultiblockRenderParts.assign(level, worldPosition, controller,
                "bronze_industrial_air_blower", matchedFacing, renderAirAnchors, voxelProfileName);
        requestModelDataUpdate();
        sync();
        return controller;
    }
    @Override public void onDestroy(Level world) {
        releaseLoad();
        super.onDestroy(world);
    }
    @Override public void setMultiblockVoxelShapes(VoxelShape coll, VoxelShape sel) {
        collision = coll; selection = sel; sync();
    }
    @Nullable public VoxelShape getLocalVoxelCollision() { return collision; }
    @Nullable public VoxelShape getLocalVoxelSelection() {
        return selection == null ? collision : selection;
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        MultiblockPersistentData data = loadMultiblockData(tag);
        blueprintName = data.blueprintName();
        replacementModel = data.replacementModel();
        formed = data.formed();
        matchedFacing = facingFromName(blueprintName);
        voxelProfileName = tag.getString("v2_voxel_profile");
        collision = MultiblockVoxelNbt.read(tag, "v2_voxel_collision");
        selection = MultiblockVoxelNbt.read(tag, "v2_voxel_selection");
        pressureHandler.setPressure(Math.max(0, Math.min(AIR_CAPACITY, pressureHandler.getPressureStored())));
        pumpProgress = Math.max(0, Math.min(PUMP_INTERVAL - 1, tag.getInt("AirPumpProgress")));
        lastOutputTick = Long.MIN_VALUE;
        outputThisTick = 0;
        running = false; // Never restore an assumed mechanical supply after reload.
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveMultiblockData(tag, blueprintName, formed, replacementModel);
        if (!voxelProfileName.isBlank()) tag.putString("v2_voxel_profile", voxelProfileName);
        MultiblockVoxelNbt.write(tag, "v2_voxel_collision", collision);
        MultiblockVoxelNbt.write(tag, "v2_voxel_selection", selection);
        tag.putInt("AirPumpProgress", pumpProgress);
    }
    @Override public CompoundTag sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        return getUpdateTag();
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag(); saveAdditional(tag); return tag;
    }
    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(Connection connection,
            ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        output.invalidate();
        gasOutput.invalidate();
    }
    @Override public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityPressure.PRESSURE)
            return formed && side == matchedFacing ? output.cast() : LazyOptional.empty();
        if (cap == CapabilityPressurizedGas.GAS)
            return formed && side == matchedFacing ? gasOutput.cast() : LazyOptional.empty();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return LazyOptional.empty();
        return super.getCapability(cap, side);
    }

    @Override public int capacityE() { return 0; }
    @Override public int maxtransferE() { return 0; }
    @Override public int capacityH() { return 0; }
    @Override public int maxtransferH() { return 0; }
    @Override public int capacityW() { return 0; }
    @Override public int maxtransferW() { return 0; }
    @Override public int capacityF() { return 0; }
    @Override public int tanks() { return 0; }
    @Override public int invsize() { return 0; }
    @Override public int capacityP() { return AIR_CAPACITY; }
    @Override public int maxtransferP() { return TRANSFER_RATE; }
    @Override public boolean itemcape() { return false; }
    @Override public boolean energycape() { return false; }
    @Override public boolean heatcape() { return false; }
    @Override public boolean wattcape() { return false; }
    @Override public boolean fluidcape() { return false; }
    @Override public boolean pressurecape() { return true; }
    @Override public boolean HeatCanReceive() { return false; }
    @Override public boolean HeatCanSend() { return false; }
    @Override public boolean WattCanReceive() { return false; }
    @Override public boolean WattCanSend() { return false; }
    @Override public boolean EnergyCanReceive() { return false; }
    @Override public boolean EnergyCanSend() { return false; }
    @Override public boolean PressureCanReceive() { return true; } // Internal pump only; proxy denies external receive.
    @Override public boolean PressureCanSend() { return true; }
    @Override public Level getThisWorld() { return level; }
    @Override public BlockPos getThisPosition() { return worldPosition; }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(5D); }
    @Override public @NotNull ModelData getModelData() {
        return super.getModelData().derive()
                .with(MultiBlockProperties.MODEL_NAME, replacementModel).build();
    }
    @Override public Component getDisplayName() {
        return Component.translatable("block.magneticraft2.bronze_industrial_air_blower");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }
}
