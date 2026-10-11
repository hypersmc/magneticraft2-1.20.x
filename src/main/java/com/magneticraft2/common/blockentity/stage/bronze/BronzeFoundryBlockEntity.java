package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeFoundryBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockRenderParts;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelProfiles;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelNbt;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelHost;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Formable Foundry shell. Heating and molten transfer are explicitly NOT active yet. */
public final class BronzeFoundryBlockEntity extends BaseBlockEntityMagneticraft2
        implements MultiblockVoxelHost {
    private static final String PREFIX = "bronze_foundry_";
    private String blueprintName = "";
    private String replacementModel = "";
    private boolean formed;
    private Direction matchedFacing = Direction.SOUTH;
    // Copied from the matching JSON settings only for this formation attempt.
    private java.util.List<BlockPos> renderAirAnchors = java.util.List.of();
    private String voxelProfileName = "";
    @Nullable private VoxelShape localVoxelCollision;
    @Nullable private VoxelShape localVoxelSelection;
    private final BronzeFoundryThermalCore thermal = new BronzeFoundryThermalCore(this);

    @Override
    public void setMultiblockVoxelShapes(VoxelShape collision, VoxelShape selection) {
        this.localVoxelCollision = collision;
        this.localVoxelSelection = selection;
        sync();
    }

    @Nullable
    public VoxelShape getLocalVoxelCollision() { return localVoxelCollision; }

    @Nullable
    public VoxelShape getLocalVoxelSelection() {
        return localVoxelSelection != null ? localVoxelSelection : localVoxelCollision;
    }

    public BronzeFoundryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BRONZE_FOUNDRY_BE.get(), pos, state);
        menuProvider = null;
    }

    public boolean isFormed() { return formed; }

    public BronzeFoundryThermalCore getThermal() { return thermal; }

    public static void serverTick(Level world, BlockPos pos, BlockState state, BronzeFoundryBlockEntity be) {
        if (!world.isClientSide && be.formed) be.thermal.tick(world);
    }

    @Override
    public void onDestroy(Level world) {
        thermal.salvage(world);
        super.onDestroy(world);
    }

    @Override
    protected @Nullable MultiblockController createMultiblockController() {
        if (level == null || level.isClientSide || formed) return null;
        MultiblockStructure structure = identifyMultiblockStructure(level, worldPosition);
        if (structure == null) return null;
        // Refuse missing/broken profile before replacing any in-world bricks.
        if (!voxelProfileName.isBlank() && !MultiblockVoxelProfiles.exists(voxelProfileName)) {
            org.apache.logging.log4j.LogManager.getLogger("Magneticraft2Voxels").error(
                    "Cannot form Bronze Foundry: missing Voxel V2 profile {}", voxelProfileName);
            return null;
        }
        MultiblockController controller = new MultiblockController(structure);
        controller.identifyAndAddModules(level, worldPosition, structure);
        if (!controller.createStructure(level, worldPosition)) return null;
        controller.setFormed(true);
        setMultiblockController(controller);
        formed = true;

        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(BronzeFoundryBlock.IS_FORMED)) {
            level.setBlock(worldPosition, state
                    .setValue(BronzeFoundryBlock.IS_FORMED, true)
                    .setValue(BronzeFoundryBlock.FACING, matchedFacing), Block.UPDATE_ALL);
        }
        // The JSON controller uses its own block-local baked model.
        // Every filler receives its generated part ID and a collision snapshot.
        MultiblockRenderParts.assign(level, worldPosition, controller,
                "bronze_foundry", matchedFacing, renderAirAnchors, voxelProfileName);
        requestModelDataUpdate();
        sync();
        return controller;
    }

    @Override
    protected @Nullable MultiblockStructure identifyMultiblockStructure(Level world, BlockPos pos) {
        for (Multiblock multiblock : MultiblockRegistry.getRegisteredMultiblocks().values()) {
            if (!multiblock.getName().startsWith(PREFIX)) continue;
            MultiblockStructure structure = multiblock.getStructure();
            if (!matchesStructure(world, pos, structure, multiblock)) continue;
            blueprintName = multiblock.getName();
            replacementModel = multiblock.getSettings().getReplaceWhenFormed();
            matchedFacing = facingFromName(blueprintName);
            renderAirAnchors = multiblock.getSettings().getRenderAirAnchors();
            voxelProfileName = multiblock.getSettings().getVoxelProfile();
            return structure;
        }
        return null;
    }

    private static Direction facingFromName(String name) {
        if (name.endsWith("_north")) return Direction.NORTH;
        if (name.endsWith("_east")) return Direction.EAST;
        if (name.endsWith("_west")) return Direction.WEST;
        return Direction.SOUTH;
    }

    @Override
    protected void interactableNoGui(BlockState state, Level world, BlockPos pos,
                                     Player player, InteractionHand hand, BlockHitResult hit) {
        // Interaction zones are authored independently of collision.
        // These are feedback-only until the heating/pouring systems exist.
        String zone = MultiblockVoxelProfiles.hitZone(voxelProfileName, matchedFacing,
                worldPosition, hit.getLocation());
        if (zone == null) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft2.bronze_foundry.prototype"), true);
            return;
        }
        // Both the controller and V2 filler proxies forward the same hit
        // location; no invisible GUI or camera-dependent state.
        switch (zone) {
            case "firebox" -> thermal.useFirebox(player, player.getItemInHand(hand));
            case "crucible" -> thermal.useCrucible(player, player.getItemInHand(hand));
            case "tap" -> thermal.useTap(player);
            default -> player.displayClientMessage(Component.translatable(
                    "message.magneticraft2.bronze_foundry.prototype"), true);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        MultiblockPersistentData data = loadMultiblockData(tag);
        blueprintName = data.blueprintName();
        formed = data.formed();
        replacementModel = data.replacementModel();
        matchedFacing = facingFromName(blueprintName);
        voxelProfileName = tag.getString("v2_voxel_profile");
        localVoxelCollision = MultiblockVoxelNbt.read(tag, "v2_voxel_collision");
        localVoxelSelection = MultiblockVoxelNbt.read(tag, "v2_voxel_selection");
        thermal.load(tag.getCompound("FoundryThermal"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveMultiblockData(tag, blueprintName, formed, replacementModel);
        if (!voxelProfileName.isBlank()) tag.putString("v2_voxel_profile", voxelProfileName);
        MultiblockVoxelNbt.write(tag, "v2_voxel_collision", localVoxelCollision);
        MultiblockVoxelNbt.write(tag, "v2_voxel_selection", localVoxelSelection);
        CompoundTag thermalData = new CompoundTag();
        thermal.save(thermalData);
        tag.put("FoundryThermal", thermalData);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }

    @Override
    public CompoundTag sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return getUpdateTag();
    }

    @Override public int capacityE() { return 0; }
    @Override public int maxtransferE() { return 0; }
    @Override public int capacityH() {
        return Magneticraft2ConfigCommon.GENERAL.FoundryHeatCapacity.get();
    }
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
    @Override public boolean heatcape() { return true; }
    @Override public boolean wattcape() { return false; }
    @Override public boolean fluidcape() { return false; }
    @Override public boolean pressurecape() { return false; }

    // The first foundry has a solid-fuel firebox; no external heat injection.
    @Override public boolean HeatCanReceive() { return false; }
    @Override public boolean HeatCanSend() { return false; }
    @Override public boolean WattCanReceive() { return false; }
    @Override public boolean WattCanSend() { return false; }
    @Override public boolean EnergyCanReceive() { return false; }
    @Override public boolean EnergyCanSend() { return false; }
    @Override public boolean PressureCanReceive() { return false; }
    @Override public boolean PressureCanSend() { return false; }

    @Override public Level getThisWorld() { return level; }
    @Override public BlockPos getThisPosition() { return worldPosition; }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(3.0D); }
    @Override public @NotNull ModelData getModelData() {
        return super.getModelData().derive()
                .with(MultiBlockProperties.MODEL_NAME, replacementModel).build();
    }
    @Override public Component getDisplayName() {
        return Component.translatable("block.magneticraft2.bronze_foundry");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }
}
