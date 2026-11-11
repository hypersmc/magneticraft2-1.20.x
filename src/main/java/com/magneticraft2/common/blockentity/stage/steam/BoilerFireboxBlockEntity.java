package com.magneticraft2.common.blockentity.stage.steam;

import com.magneticraft2.common.block.stage.steam.BoilerFireboxFrontBlock;
import com.magneticraft2.common.block.stage.steel.SteelBoilerVesselBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.general.Multiblockfiller_tile;
import com.magneticraft2.common.blockentity.stage.steel.SteelBoilerVesselBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockRenderParts;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelHost;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelNbt;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelProfiles;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import com.magneticraft2.common.systems.steam.BoilerAssemblyHost;
import com.magneticraft2.common.systems.steam.BoilerAssemblyRecovery;
import com.magneticraft2.common.systems.steam.BoilerFireboxLayout;
import com.magneticraft2.common.systems.steam.BoilerFireboxInstallation;
import com.magneticraft2.common.utils.MultiBlockProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Separate 18-block furnace assembly; warm-up state remains owned by its linked vessel. */
public final class BoilerFireboxBlockEntity extends BaseBlockEntityMagneticraft2
        implements MultiblockVoxelHost, BoilerAssemblyHost {
    private UUID identity = UUID.randomUUID();
    private boolean formed;
    private String blueprint = "", model = "", profile = "";
    private Direction facing = Direction.SOUTH;
    @Nullable private BlockPos vesselPosition;
    @Nullable private UUID vesselIdentity;
    @Nullable private VoxelShape collision, selection;

    public BoilerFireboxBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BOILER_FIREBOX_BE.get(), pos, state);
        menuProvider = null;
    }

    public boolean isFormed() { return formed; }
    public Direction assemblyFacing() { return facing; }
    @Nullable public BlockPos vesselPosition() { return vesselPosition; }
    @Nullable public UUID vesselIdentity() { return vesselIdentity; }
    @Nullable public VoxelShape localCollision() { return collision; }
    @Nullable public VoxelShape localSelection() { return selection; }

    public void use(Player player) { use(player, InteractionHand.MAIN_HAND); }
    public void use(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide || hand != InteractionHand.MAIN_HAND) return;
        if (!formed) { form(player); return; }
        SteelBoilerVesselBlockEntity vessel = BoilerFireboxInstallation.vesselFromClick(level, worldPosition);
        if (vessel == null) { message(player, "unloaded"); return; }
        vessel.warmup().useFirebox(this, player, hand);
    }
    private void message(@Nullable Player player, String key, Object... args) {
        if (player != null) player.displayClientMessage(
                Component.translatable("message.magneticraft2.firebox_v2." + key, args), true);
    }

    private @Nullable Multiblock blueprintForFacing(Direction direction) {
        String name = "boiler_firebox_" + direction.getName();
        for (Multiblock bp : MultiblockRegistry.getRegisteredMultiblocks().values())
            if (name.equals(bp.getName())) return bp;
        return null;
    }

    @Override protected @Nullable MultiblockStructure identifyMultiblockStructure(Level world, BlockPos pos) {
        Direction direction = getBlockState().getValue(BoilerFireboxFrontBlock.FACING);
        Multiblock bp = blueprintForFacing(direction);
        if (bp == null) return null;
        BlockPos vessel = BoilerFireboxLayout.vessel(pos, direction);
        if (!BoilerFireboxLayout.loaded(world, BoilerFireboxLayout.cells(vessel, direction))) return null;
        return matchesStructure(world, pos, bp.getStructure(), bp) ? bp.getStructure() : null;
    }

    @Override protected @Nullable MultiblockController createMultiblockController() { return form(null); }
    @Override public void onRightClick() { if (!formed) form(null); }

    private @Nullable MultiblockController form(@Nullable Player player) {
        if (!(level instanceof ServerLevel server) || formed) return null;
        Direction direction = getBlockState().getValue(BoilerFireboxFrontBlock.FACING);
        BlockPos vesselPos = BoilerFireboxLayout.vessel(worldPosition, direction);
        List<BlockPos> ownCells = BoilerFireboxLayout.cells(vesselPos, direction);
        if (!server.hasChunkAt(vesselPos) || !BoilerFireboxLayout.loaded(server, ownCells)) {
            message(player, "unloaded"); return null;
        }
        if (!(server.getBlockEntity(vesselPos) instanceof SteelBoilerVesselBlockEntity vessel)
                || !vessel.isFormed()
                || vessel.getBlockState().getValue(SteelBoilerVesselBlock.FACING) != direction) {
            message(player, "vessel"); return null;
        }
        if (!vessel.canLinkBoilerFirebox(worldPosition)) {
            message(player, "already_linked"); return null;
        }
        // All original vessel cells must be loaded before adopting their identity.
        if (!BoilerFireboxLayout.loaded(server, vessel.boilerOriginalBlocks().keySet())
                || !BoilerAssemblyRecovery.attachmentAreaLoaded(server, vesselPos, direction)) {
            message(player, "unloaded"); return null;
        }
        Map<BlockPos, BlockState> vesselOriginals = vessel.boilerOriginalBlocks();
        if (vesselOriginals.size() != 60 || vesselOriginals.values().stream().filter(state -> !state.isAir()).count() != 53) {
            message(player, "vessel"); return null;
        }
        for (Map.Entry<BlockPos, BlockState> entry : vesselOriginals.entrySet()) {
            if (entry.getKey().equals(vesselPos)) continue;
            if (entry.getValue().isAir()) {
                if (!server.getBlockState(entry.getKey()).isAir()) { message(player, "vessel"); return null; }
            } else if (!(server.getBlockEntity(entry.getKey()) instanceof Multiblockfiller_tile filler)
                    || !vesselPos.equals(filler.getControllerPos())) {
                message(player, "vessel"); return null;
            }
        }
        BlockPos oldStack = vesselPos.offset(BoilerFireboxLayout.offset(
                new com.magneticraft2.common.systems.steam.BoilerFireboxPlan.Cell(0, 2, 1), direction));
        if (server.getBlockState(oldStack).is(BlockRegistry.BOILER_FLUE_CHIMNEY.get())) {
            message(player, "legacy_chimney"); return null;
        }
        for (BlockPos approach : List.of(worldPosition.relative(direction), worldPosition.relative(direction).above())) {
            if (!server.hasChunkAt(approach)) { message(player, "unloaded"); return null; }
            if (!server.getBlockState(approach).isAir()) { message(player, "access"); return null; }
        }
        Multiblock bp = blueprintForFacing(direction);
        if (bp == null || !MultiblockVoxelProfiles.exists(bp.getSettings().getVoxelProfile())) {
            message(player, "resources"); return null;
        }
        // Fail before touching the world, including when a data pack changes the footprint.
        MultiblockStructure structure = bp.getStructure();
        BlockPos origin = worldPosition.subtract(findControllerOffset(structure, getBlockState().getBlock()));
        int[] dimensions = structure.getDimensions();
        List<BlockPos> actualCells = new ArrayList<>();
        for (int y = 0; y < dimensions[1]; y++) for (int z = 0; z < dimensions[2]; z++)
            for (int x = 0; x < dimensions[0]; x++) actualCells.add(origin.offset(x, y, z));
        if (!new java.util.HashSet<>(actualCells).equals(new java.util.HashSet<>(ownCells))) {
            message(player, "resources"); return null;
        }
        if (!matchesStructure(server, worldPosition, structure, bp)) {
            message(player, "materials"); return null;
        }
        for (BlockPos pos : ownCells) {
            if (!pos.equals(worldPosition) && server.getBlockEntity(pos) != null) {
                message(player, "resources"); return null; // never absorb another inventory/controller
            }
            if (player != null && (!player.getAbilities().mayBuild || !server.mayInteract(player, pos))) {
                message(player, "protected"); return null;
            }
        }
        List<BlockSnapshot> snapshots = new ArrayList<>(ownCells.size());
        for (BlockPos pos : ownCells) snapshots.add(BlockSnapshot.create(server.dimension(), server, pos));
        MultiblockController candidate = new MultiblockController(structure);
        // No item/fluid modules belong to this inert extension.
        if (!candidate.createStructure(server, worldPosition)) {
            message(player, "materials"); return null;
        }
        if (player != null && ForgeEventFactory.onMultiBlockPlace(player, snapshots, direction)) {
            for (int i = snapshots.size() - 1; i >= 0; i--) snapshots.get(i).restore(true, false);
            candidate.getBlockMap().clear();
            message(player, "protected"); return null;
        }
        identity = UUID.randomUUID();
        vesselPosition = vesselPos.immutable();
        vesselIdentity = vessel.boilerAssemblyId();
        facing = direction;
        blueprint = bp.getName(); model = bp.getSettings().getReplaceWhenFormed();
        profile = bp.getSettings().getVoxelProfile();
        setMultiblockController(candidate);
        candidate.setFormed(true); formed = true;
        server.setBlock(worldPosition, getBlockState().setValue(BoilerFireboxFrontBlock.IS_FORMED, true), Block.UPDATE_ALL);
        MultiblockRenderParts.assign(server, worldPosition, candidate, "boiler_firebox", direction,
                List.of(), profile);
        BoilerAssemblyRecovery.markHost(server, this);
        vessel.linkBoilerFirebox(worldPosition, identity);
        BoilerAssemblyRecovery.markHost(server, vessel);
        BoilerAssemblyRecovery.adoptExistingServiceFittings(server, vessel, direction);
        requestModelDataUpdate(); sync();
        if (player != null) vessel.warmup().status(player);
        return candidate;
    }

    @Override public void setMultiblockVoxelShapes(VoxelShape coll, VoxelShape sel) {
        collision = coll; selection = sel; sync();
    }
    @Override protected void interactableNoGui(BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) { use(player, hand); }
    @Override public void onDestroy(Level world) {
        if (world instanceof ServerLevel server && formed)
            BoilerAssemblyRecovery.request(server, new Link(worldPosition, identity), true, null);
    }
    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) BoilerAssemblyRecovery.wake(server);
    }

    @Override public UUID boilerAssemblyId() { return identity; }
    @Override public BlockPos boilerAssemblyPosition() { return worldPosition; }
    @Override public boolean boilerAssemblyTracked() { return true; }
    @Override public boolean boilerAssemblyFormed() { return formed; }
    @Override public @Nullable Link boilerChild() { return null; }
    @Override public Map<BlockPos, BlockState> boilerOriginalBlocks() {
        return getMultiblockController() == null ? Map.of() : Map.copyOf(getMultiblockController().getBlockMap());
    }
    @Override public Map<BlockPos, BlockState> claimBoilerOriginalBlocks() {
        if (!formed) return Map.of();
        Map<BlockPos, BlockState> originals = new LinkedHashMap<>(boilerOriginalBlocks());
        formed = false;
        if (getMultiblockController() != null) {
            getMultiblockController().setFormed(false);
            getMultiblockController().getBlockMap().clear();
        }
        if (level instanceof ServerLevel server && vesselPosition != null && vesselIdentity != null)
            BoilerAssemblyRecovery.unlinkParent(server, new Link(vesselPosition, vesselIdentity), identity);
        setChanged();
        return originals;
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        MultiblockPersistentData saved = loadMultiblockData(tag);
        blueprint = saved.blueprintName(); model = saved.replacementModel(); formed = saved.formed();
        if (tag.hasUUID("FireboxAssemblyId")) identity = tag.getUUID("FireboxAssemblyId");
        vesselPosition = tag.contains("FireboxVesselPos") ? BlockPos.of(tag.getLong("FireboxVesselPos")) : null;
        vesselIdentity = tag.hasUUID("FireboxVesselId") ? tag.getUUID("FireboxVesselId") : null;
        Direction decoded = Direction.byName(tag.getString("FireboxFacing"));
        facing = decoded != null && decoded.getAxis().isHorizontal() ? decoded : Direction.SOUTH;
        profile = tag.getString("FireboxVoxelProfile");
        collision = MultiblockVoxelNbt.read(tag, "FireboxCollision");
        selection = MultiblockVoxelNbt.read(tag, "FireboxSelection");
        if (level != null && level.isClientSide) requestModelDataUpdate();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveMultiblockData(tag, blueprint, formed, model);
        tag.putInt("FireboxAssemblyVersion", 1);
        tag.putUUID("FireboxAssemblyId", identity);
        tag.putString("FireboxFacing", facing.getName());
        tag.putString("FireboxVoxelProfile", profile);
        if (vesselPosition != null) tag.putLong("FireboxVesselPos", vesselPosition.asLong());
        if (vesselIdentity != null) tag.putUUID("FireboxVesselId", vesselIdentity);
        MultiblockVoxelNbt.write(tag, "FireboxCollision", collision);
        MultiblockVoxelNbt.write(tag, "FireboxSelection", selection);
    }
    @Override public CompoundTag sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        return getUpdateTag();
    }
    @Override public CompoundTag getUpdateTag() { CompoundTag tag = new CompoundTag(); saveAdditional(tag); return tag; }
    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }
    @Override public @NotNull ModelData getModelData() {
        return super.getModelData().derive().with(MultiBlockProperties.MODEL_NAME, model).build();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.magneticraft2.boiler_firebox_front"); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return null; }
    @Override public Level getThisWorld() { return level; }
    @Override public BlockPos getThisPosition() { return worldPosition; }
    @Override public int capacityE() { return 0; }
    @Override public int maxtransferE() { return 0; }
    @Override public int capacityH() { return 0; }
    @Override public int maxtransferH() { return 0; }
    @Override public int capacityW() { return 0; }
    @Override public int maxtransferW() { return 0; }
    @Override public int capacityF() { return 0; }
    @Override public int tanks() { return 0; }
    @Override public int invsize() { return 0; }
    @Override public int capacityP() { return 0; }
    @Override public int maxtransferP() { return 0; }
    @Override public boolean itemcape() { return false; }
    @Override public boolean energycape() { return false; }
    @Override public boolean heatcape() { return false; }
    @Override public boolean wattcape() { return false; }
    @Override public boolean fluidcape() { return false; }
    @Override public boolean pressurecape() { return false; }
    @Override public boolean HeatCanReceive() { return false; }
    @Override public boolean HeatCanSend() { return false; }
    @Override public boolean WattCanReceive() { return false; }
    @Override public boolean WattCanSend() { return false; }
    @Override public boolean EnergyCanReceive() { return false; }
    @Override public boolean EnergyCanSend() { return false; }
    @Override public boolean PressureCanReceive() { return false; }
    @Override public boolean PressureCanSend() { return false; }
}
