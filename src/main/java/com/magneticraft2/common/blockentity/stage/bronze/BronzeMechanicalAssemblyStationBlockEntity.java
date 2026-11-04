package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeMechanicalAssemblyStationBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalInputModuleBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.MultiblockItemPortBlockEntity;
import com.magneticraft2.common.recipe.multiblock.MechanicalAssemblyRecipe;
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
import com.magneticraft2.common.utils.MultiBlockProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
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

/**
 * Bronze III: one real four-slot input port, one real output and an existing
 * mechanical-input module. There is no fabricated crafting shortcut: the
 * controller consumes exactly four recipe-indexed ingredients after completing
 * a server-authoritative mechanical load cycle.
 */
public final class BronzeMechanicalAssemblyStationBlockEntity
        extends BaseBlockEntityMagneticraft2 implements MultiblockVoxelHost {
    private static final String PREFIX = "bronze_mechanical_assembly_station_";
    private static final float EPS = 0.01F;
    public static final int OUTPUT_SLOT = MechanicalAssemblyRecipe.INPUT_SLOTS;

    private boolean formed, running;
    private String blueprintName = "", replacementModel = "", voxelProfileName = "";
    private Direction matchedFacing = Direction.SOUTH;
    private java.util.List<BlockPos> renderAirAnchors = java.util.List.of();
    @Nullable private VoxelShape collision, selection;
    @Nullable private ResourceLocation activeRecipe;
    private final ItemStack[] activeInputs = {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };
    private int progress;

    public BronzeMechanicalAssemblyStationBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BRONZE_MECHANICAL_ASSEMBLY_STATION_BE.get(), pos, state);
        menuProvider = null;
    }
    public boolean isFormed() { return formed; }
    public boolean isRunning() { return running; }
    public int getProgress() { return progress; }

    public boolean acceptsIngredientSlot(int slot, ItemStack candidate) {
        if (slot < 0 || slot >= MechanicalAssemblyRecipe.INPUT_SLOTS
                || candidate.isEmpty() || level == null) return false;
        for (MechanicalAssemblyRecipe recipe : level.getRecipeManager()
                .getAllRecipesFor(MechanicalAssemblyRecipe.Type.INSTANCE))
            if (recipe.accepts(slot, candidate)) return true;
        return false;
    }

    @Nullable private MechanicalAssemblyRecipe findRecipe() {
        if (level == null) return null;
        for (MechanicalAssemblyRecipe recipe : level.getRecipeManager()
                .getAllRecipesFor(MechanicalAssemblyRecipe.Type.INSTANCE))
            if (recipe.matchesSlots(itemHandler)) return recipe;
        return null;
    }
    private void resetProgress() {
        progress = 0;
        activeRecipe = null;
        for (int i = 0; i < activeInputs.length; i++) activeInputs[i] = ItemStack.EMPTY;
        setChanged();
    }
    private void setRunning(boolean active) {
        if (running == active) return;
        running = active;
        sync();
    }
    private void releaseLoad() {
        if (level != null) GearNetworkManager.getInstance().removeMechanicalLoad(level, worldPosition);
    }
    @Nullable private MechanicalInputModuleBlockEntity getMechanicalInput() {
        if (level == null || !formed) return null;
        BlockPos pos = getMultiblockController() == null ? null
                : getMultiblockController().getmodulePos("mechanical_input");
        if (pos == null) pos = worldPosition.relative(matchedFacing.getOpposite(), 4);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof MechanicalInputModuleBlockEntity module ? module : null;
    }

    public static <E extends BlockEntity> void serverTick(Level world, BlockPos pos,
            BlockState state, E entity) {
        if (world.isClientSide || !(entity instanceof BronzeMechanicalAssemblyStationBlockEntity station))
            return;
        station.tickAssembly();
    }
    private void tickAssembly() {
        if (!formed || level == null) {
            releaseLoad();
            setRunning(false);
            return;
        }
        MechanicalAssemblyRecipe recipe = findRecipe();
        if (recipe == null) {
            if (activeRecipe != null || progress != 0) resetProgress();
            releaseLoad();
            setRunning(false);
            return;
        }
        boolean inputChanged = !recipe.getId().equals(activeRecipe);
        for (int i = 0; i < MechanicalAssemblyRecipe.INPUT_SLOTS; i++) {
            if (!ItemStack.isSameItemSameTags(activeInputs[i], itemHandler.getStackInSlot(i)))
                inputChanged = true;
        }
        if (inputChanged) {
            activeRecipe = recipe.getId();
            progress = 0;
            for (int i = 0; i < MechanicalAssemblyRecipe.INPUT_SLOTS; i++) {
                activeInputs[i] = itemHandler.getStackInSlot(i).copy();
                activeInputs[i].setCount(1);
            }
            setChanged();
        }

        ItemStack output = recipe.output();
        if (!itemHandler.insertItem(OUTPUT_SLOT, output, true).isEmpty()) {
            releaseLoad();
            setRunning(false);
            return;
        }
        MechanicalInputModuleBlockEntity module = getMechanicalInput();
        GearNode node = module == null ? null : module.getOrCreateGearNode();
        boolean canAttempt = node != null && node.getSpeed() >= recipe.minSpeed()
                && node.getTorque() + EPS >= recipe.torque();
        GearNetworkManager power = GearNetworkManager.getInstance();
        power.setMechanicalLoad(level, worldPosition,
                module == null ? worldPosition : module.getBlockPos(),
                recipe.torque(), canAttempt);
        if (!canAttempt || !power.getMechanicalLoadState(level, worldPosition).supplied()) {
            setRunning(false);
            return;
        }
        setRunning(true);
        progress++;
        setChanged();
        if (progress < recipe.processTime()) {
            if (level.getGameTime() % 20L == 0) sync();
            return;
        }

        // Revalidate all items and the destination on the same server tick.
        // Rejected output never consumes even a single ingredient.
        if (!recipe.matchesSlots(itemHandler)
                || !itemHandler.insertItem(OUTPUT_SLOT, output, true).isEmpty()) {
            releaseLoad();
            setRunning(false);
            return;
        }
        for (int i = 0; i < MechanicalAssemblyRecipe.INPUT_SLOTS; i++) {
            int count = recipe.part(i).count();
            ItemStack taken = itemHandler.extractItem(i, count, false);
            if (taken.getCount() != count)
                throw new IllegalStateException("Assembly input changed after reservation");
        }
        if (!itemHandler.insertItem(OUTPUT_SLOT, output, false).isEmpty())
            throw new IllegalStateException("Assembly result changed after reservation");
        resetProgress();
        releaseLoad();
        setRunning(false);
        sync();
    }

    @Override protected void interactableNoGui(BlockState state, Level world,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide || !formed) return;
        MechanicalAssemblyRecipe recipe = findRecipe();
        MechanicalInputModuleBlockEntity module = getMechanicalInput();
        GearNode node = module == null ? null : module.getOrCreateGearNode();
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.bronze_mechanical_assembly_station.status",
                recipe == null ? "no matching recipe" : recipe.getId().toString(),
                progress, recipe == null ? 0 : recipe.processTime(),
                node == null ? 0 : node.getSpeed(),
                node == null ? 0 : node.getTorque(), running), true);
    }

    @Override protected @Nullable MultiblockStructure identifyMultiblockStructure(Level world,
            BlockPos pos) {
        for (Multiblock blueprint : MultiblockRegistry.getRegisteredMultiblocks().values()) {
            if (!blueprint.getName().startsWith(PREFIX)) continue;
            MultiblockStructure structure = blueprint.getStructure();
            if (!matchesStructure(world, pos, structure, blueprint)) continue;
            blueprintName = blueprint.getName();
            replacementModel = blueprint.getSettings().getReplaceWhenFormed();
            voxelProfileName = blueprint.getSettings().getVoxelProfile();
            renderAirAnchors = blueprint.getSettings().getRenderAirAnchors();
            matchedFacing = blueprintName.endsWith("_west") ? Direction.WEST
                    : blueprintName.endsWith("_north") ? Direction.NORTH
                    : blueprintName.endsWith("_east") ? Direction.EAST : Direction.SOUTH;
            return structure;
        }
        return null;
    }
    @Override protected @Nullable MultiblockController createMultiblockController() {
        if (level == null || level.isClientSide || formed) return null;
        MultiblockStructure structure = identifyMultiblockStructure(level, worldPosition);
        if (structure == null || !MultiblockVoxelProfiles.exists(voxelProfileName)) return null;
        MultiblockController controller = new MultiblockController(structure);
        controller.identifyAndAddModules(level, worldPosition, structure);
        if (!controller.createStructure(level, worldPosition)) return null;
        for (BlockPos member : controller.getBlockMap().keySet())
            if (level.getBlockEntity(member) instanceof MultiblockItemPortBlockEntity port)
                port.linkFormedController(worldPosition);
        controller.setFormed(true);
        setMultiblockController(controller);
        formed = true;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(BronzeMechanicalAssemblyStationBlock.IS_FORMED))
            level.setBlock(worldPosition, state
                    .setValue(BronzeMechanicalAssemblyStationBlock.IS_FORMED, true)
                    .setValue(BronzeMechanicalAssemblyStationBlock.FACING, matchedFacing),
                    Block.UPDATE_ALL);
        MultiblockRenderParts.assign(level, worldPosition, controller,
                "bronze_mechanical_assembly_station", matchedFacing,
                renderAirAnchors, voxelProfileName);
        requestModelDataUpdate();
        sync();
        return controller;
    }

    @Override public void onDestroy(Level world) {
        releaseLoad();
        super.onDestroy(world);
    }
    @Override public void setMultiblockVoxelShapes(VoxelShape coll, VoxelShape sel) {
        collision = coll;
        selection = sel;
        sync();
    }
    @Nullable public VoxelShape getLocalVoxelCollision() { return collision; }
    @Nullable public VoxelShape getLocalVoxelSelection() { return selection == null ? collision : selection; }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        MultiblockPersistentData data = loadMultiblockData(tag);
        blueprintName = data.blueprintName();
        formed = data.formed();
        replacementModel = data.replacementModel();
        voxelProfileName = tag.getString("v2_voxel_profile");
        matchedFacing = blueprintName.endsWith("_west") ? Direction.WEST
                : blueprintName.endsWith("_north") ? Direction.NORTH
                : blueprintName.endsWith("_east") ? Direction.EAST : Direction.SOUTH;
        collision = MultiblockVoxelNbt.read(tag, "v2_voxel_collision");
        selection = MultiblockVoxelNbt.read(tag, "v2_voxel_selection");
        progress = Math.max(0, tag.getInt("AssemblyProgress"));
        activeRecipe = ResourceLocation.tryParse(tag.getString("AssemblyRecipe"));
        for (int i = 0; i < activeInputs.length; i++)
            activeInputs[i] = ItemStack.of(tag.getCompound("AssemblyInput" + i));
        running = false;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveMultiblockData(tag, blueprintName, formed, replacementModel);
        if (!voxelProfileName.isBlank()) tag.putString("v2_voxel_profile", voxelProfileName);
        MultiblockVoxelNbt.write(tag, "v2_voxel_collision", collision);
        MultiblockVoxelNbt.write(tag, "v2_voxel_selection", selection);
        tag.putInt("AssemblyProgress", progress);
        if (activeRecipe != null) tag.putString("AssemblyRecipe", activeRecipe.toString());
        for (int i = 0; i < activeInputs.length; i++)
            if (!activeInputs[i].isEmpty())
                tag.put("AssemblyInput" + i, activeInputs[i].save(new CompoundTag()));
    }
    @Override public CompoundTag sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        return getUpdateTag();
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }
    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(Connection c, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }
    @Override public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap, @Nullable Direction side) {
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
    @Override public int invsize() { return OUTPUT_SLOT + 1; }
    @Override public int capacityP() { return 0; }
    @Override public int maxtransferP() { return 0; }
    @Override public boolean itemcape() { return true; }
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
    @Override public Level getThisWorld() { return level; }
    @Override public BlockPos getThisPosition() { return worldPosition; }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(5D); }
    @Override public @NotNull ModelData getModelData() {
        return super.getModelData().derive()
                .with(MultiBlockProperties.MODEL_NAME, replacementModel).build();
    }
    @Override public Component getDisplayName() {
        return Component.translatable("block.magneticraft2.bronze_mechanical_assembly_station");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }
}
