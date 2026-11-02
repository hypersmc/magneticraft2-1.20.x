package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeForgeHammerBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalInputModuleBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.MultiblockItemPortBlockEntity;
import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.recipe.multiblock.MultiblockRecipeHandler;
import com.magneticraft2.common.systems.material.HotBloomWorkpiece;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
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
import com.magneticraft2.common.systems.maintenance.ReplaceableMachinePart;
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
import java.util.Locale;

/**
 * Bronze III powered forge: Gear V2-driven, recipe-defined batches, with a
 * replaceable forging die. No new shaft physics or screen/inventory GUI.
 * The first recipe makes Copper Plates in bulk; hot metal remains a later
 * extension, not a fake heat requirement for room-temperature copper work.
 */
public final class BronzeForgeHammerBlockEntity extends BaseBlockEntityMagneticraft2
        implements MultiblockVoxelHost {
    private static final String PREFIX = "bronze_forge_hammer_";
    private static final ResourceLocation MACHINE =
            new ResourceLocation("magneticraft2", "bronze_forge_hammer");
    private static final float EPS = 0.01F;

    private boolean formed;
    private String blueprintName = "", replacementModel = "", voxelProfileName = "";
    private Direction matchedFacing = Direction.SOUTH;
    private java.util.List<BlockPos> renderAirAnchors = java.util.List.of();
    @Nullable private VoxelShape collision, selection;
    @Nullable private ResourceLocation activeRecipe;
    private ItemStack activeInput = ItemStack.EMPTY;
    private ItemStack lastInput = ItemStack.EMPTY, lastOutput = ItemStack.EMPTY;
    private int progress;
    private boolean running;
    private final ReplaceableMachinePart forgingDie =
            new ReplaceableMachinePart(ItemRegistry.ITEM_BRONZE_FORGE_DIE::get, 384);

    public BronzeForgeHammerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BRONZE_FORGE_HAMMER_BE.get(), pos, state);
        menuProvider = null;
    }

    public boolean isFormed() { return formed; }
    public boolean isRunning() { return running; }
    public int getWearStage() { return forgingDie.getVisualWearStage(); }
    public float getHammerStroke(float partialTicks) {
        if (!running) return 0F;
        MechanicalInputModuleBlockEntity input = getMechanicalInput();
        if (input == null) return 0F;
        float angle = input.getVisualRotationDegrees(partialTicks);
        // Real mechanical phase, not world time. Stop immediately on stall.
        return (float)(.5D - .5D * Math.cos(Math.toRadians(angle)));
    }
    public ItemStack getWorkpiece() { return itemHandler.getStackInSlot(0); }

    public boolean acceptsRecipeInput(ItemStack stack) {
        return MultiblockRecipeHandler.acceptsInput(level, MACHINE, stack);
    }
    private static boolean validRecipe(@Nullable MultiblockProcessingRecipe recipe) {
        return recipe != null && recipe.getInputCount() > 0
                && !recipe.getOutput().isEmpty()
                && recipe.getByproduct().isEmpty()
                && recipe.getFluidInput().isEmpty()
                && recipe.getFluidOutput().isEmpty()
                && recipe.getMinSpeed() > 0F && recipe.getTorque() > 0F;
    }
    @Nullable private MultiblockProcessingRecipe recipe() {
        return MultiblockRecipeHandler.findRecipe(level, MACHINE,
                itemHandler.getStackInSlot(0));
    }
    private static boolean sameContents(ItemStack a, ItemStack b) {
        return a.getCount() == b.getCount() && ItemStack.isSameItemSameTags(a,b);
    }
    public static <E extends BlockEntity> void serverTick(Level world, BlockPos pos,
            BlockState state, E tile) {
        if (world.isClientSide || !(tile instanceof BronzeForgeHammerBlockEntity hammer)) return;
        hammer.tickForging();
        ItemStack in = hammer.itemHandler.getStackInSlot(0);
        ItemStack out = hammer.itemHandler.getStackInSlot(1);
        if (!sameContents(in, hammer.lastInput) || !sameContents(out, hammer.lastOutput)) {
            hammer.sync();
        }
    }
    private void setRunning(boolean value) {
        if (running == value) return;
        running = value;
        sync();
    }
    private void releaseLoad() {
        if (level != null) GearNetworkManager.getInstance()
                .removeMechanicalLoad(level, worldPosition);
    }
    private void resetBatch() {
        if (progress == 0 && activeRecipe == null && activeInput.isEmpty()) return;
        progress = 0;
        activeRecipe = null;
        activeInput = ItemStack.EMPTY;
        setChanged();
    }
    private void tickForging() {
        if (level == null || !formed) {
            releaseLoad();
            setRunning(false);
            return;
        }
        MultiblockProcessingRecipe recipe = recipe();
        if (!validRecipe(recipe)) {
            resetBatch();
            releaseLoad();
            setRunning(false);
            return;
        }
        ItemStack inputStack = itemHandler.getStackInSlot(0);
        // Only cold copper needs no heat. Consolidation is always hot work,
        // and heat must remain valid throughout every supplied hammer tick.
        // A cooled bloom can be reheated at the existing Refractory Kiln.
        if (HotBloomWorkpiece.requiresHeat(recipe)
                && !HotBloomWorkpiece.isHot(inputStack, level)) {
            resetBatch();
            releaseLoad();
            setRunning(false);
            return;
        }
        if (!recipe.getId().equals(activeRecipe)
                || !ItemStack.isSameItemSameTags(activeInput, inputStack)) {
            activeRecipe = recipe.getId();
            activeInput = inputStack.copy();
            activeInput.setCount(1);
            progress = 0;
            setChanged();
        }
        // No load while output is blocked or the die needs replacement.
        ItemStack output = HotBloomWorkpiece.requiresHeat(recipe)
                ? HotBloomWorkpiece.consolidate(inputStack, recipe.getOutput())
                : recipe.getOutput();
        if (!forgingDie.canOperate() || !itemHandler.insertItem(1, output, true).isEmpty()) {
            releaseLoad();
            setRunning(false);
            return;
        }
        MechanicalInputModuleBlockEntity module = getMechanicalInput();
        GearNode node = module == null ? null : module.getOrCreateGearNode();
        // Qualify the physical gearing with raw RPM. Gear V2 deliberately
        // retains raw speed when a source overloads, while effective speed
        // becomes zero. Using effective speed here deactivated this load on
        // the next tick, cleared the overload, then reactivated it forever.
        // Supply is decided below by the authoritative load evaluator.
        boolean canAttempt = node != null && node.getSpeed() >= recipe.getMinSpeed()
                && node.getTorque() + EPS >= recipe.getTorque();
        GearNetworkManager manager = GearNetworkManager.getInstance();
        manager.setMechanicalLoad(level, worldPosition, module == null ? worldPosition
                        : module.getBlockPos(), recipe.getTorque(), canAttempt);
        if (!canAttempt || !manager.getMechanicalLoadState(level, worldPosition).supplied()) {
            setRunning(false);
            return;
        }
        setRunning(true);
        progress++;
        setChanged();
        if (progress < recipe.getProcessTime()) return;
        // Reserve output before consuming the exact ingredients on the server.
        if (!itemHandler.insertItem(1, output, true).isEmpty()) {
            releaseLoad();
            setRunning(false);
            return;
        }
        ItemStack removed = itemHandler.extractItem(0, recipe.getInputCount(), false);
        if (removed.getCount() != recipe.getInputCount())
            throw new IllegalStateException("Forge Hammer input changed during batch");
        if (!itemHandler.insertItem(1, output, false).isEmpty())
            throw new IllegalStateException("Forge Hammer output reservation lost");
        forgingDie.completeBatch();
        resetBatch();
        releaseLoad();
        setRunning(false);
        sync();
    }
    @Nullable public MechanicalInputModuleBlockEntity getMechanicalInput() {
        if (level == null || !formed) return null;
        BlockPos pos = getMultiblockController() == null ? null :
                getMultiblockController().getmodulePos("mechanical_input");
        if (pos == null) pos = worldPosition.relative(matchedFacing.getOpposite(), 2);
        BlockEntity tile = level.getBlockEntity(pos);
        return tile instanceof MechanicalInputModuleBlockEntity module ? module : null;
    }

    /**
     * Explicit feedback for a GUI-less machine. Copper on the anvil proves only
     * that the item port accepted it, NOT that Gear V2 can supply the demanded
     * RPM and torque. Check the actual server-side module and load state here.
     * Recipe enumeration occurs ONLY on player interaction, not each tick.
     */
    @Override protected void interactableNoGui(BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide || !formed) return;
        if (forgingDie.handleInteraction(player, hand, this::sync)) return;

        ItemStack inputStack = itemHandler.getStackInSlot(0);
        MultiblockProcessingRecipe matching = recipe();
        MultiblockProcessingRecipe candidate = matching;
        if (candidate == null && !inputStack.isEmpty()) {
            for (MultiblockProcessingRecipe possible : world.getRecipeManager()
                    .getAllRecipesFor(MultiblockProcessingRecipe.Type.INSTANCE)) {
                if (possible.isForMachine(MACHINE) && possible.acceptsInputType(inputStack)) {
                    candidate = possible;
                    break;
                }
            }
        }

        MechanicalInputModuleBlockEntity module = getMechanicalInput();
        GearNode node = module == null ? null : module.getOrCreateGearNode();
        GearNetworkManager.MechanicalLoadState load =
                GearNetworkManager.getInstance().getMechanicalLoadState(world, worldPosition);
        float rpm = node == null ? 0F : node.getEffectiveSpeed();
        float torque = node == null ? 0F : node.getTorque();
        float requiredRpm = candidate == null ? 0F : candidate.getMinSpeed();
        float requiredTorque = candidate == null ? 0F : candidate.getTorque();

        String diagnostic;
        if (inputStack.isEmpty()) {
            diagnostic = "empty";
        } else if (candidate == null) {
            diagnostic = "recipe_missing";
        } else if (inputStack.getCount() < candidate.getInputCount()) {
            diagnostic = "need_more";
        } else if (!validRecipe(candidate)) {
            diagnostic = "recipe_unsupported";
        } else if (HotBloomWorkpiece.requiresHeat(candidate)
                && !HotBloomWorkpiece.isHot(inputStack, world)) {
            diagnostic = "bloom_cold";
        } else if (!forgingDie.canOperate()) {
            diagnostic = "die_worn";
        } else if (!itemHandler.insertItem(1, candidate.getOutput(), true).isEmpty()) {
            diagnostic = "output_blocked";
        } else if (node == null) {
            diagnostic = "module_missing";
        } else if (load.active() && load.sourcePos() != null
                && load.totalSourceDemand() > load.sourceTorqueCapacity() + EPS) {
            diagnostic = "source_overload";
        } else if (node.isOverloaded()) {
            diagnostic = "overloaded";
        } else if (rpm < requiredRpm && !load.active()) {
            diagnostic = "insufficient_speed";
        } else if (torque + EPS < requiredTorque) {
            diagnostic = "insufficient_torque";
        } else if (load.active() && load.sourcePos() == null) {
            diagnostic = "source_missing";
        } else if (!load.active()) {
            diagnostic = "load_inactive";
        } else if (!load.supplied()) {
            diagnostic = "load_unsupplied";
        } else {
            diagnostic = running ? "working" : "ready";
        }

        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.bronze_forge_hammer.status",
                progress, matching == null ? 0 : matching.getProcessTime(),
                inputStack.getCount(), itemHandler.getStackInSlot(1).getCount(),
                forgingDie.getRemainingCycles(), forgingDie.getMaximumCycles(),
                running), false);
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.bronze_forge_hammer.diagnostic",
                Component.translatable("message.magneticraft2.bronze_forge_hammer.reason." + diagnostic),
                inputStack.getCount(), candidate == null ? 0 : candidate.getInputCount(),
                String.format(Locale.ROOT, "%.1f", rpm),
                String.format(Locale.ROOT, "%.1f", requiredRpm),
                String.format(Locale.ROOT, "%.1f", torque),
                String.format(Locale.ROOT, "%.1f", requiredTorque),
                load.supplied()), false);
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.bronze_forge_hammer.source",
                String.format(Locale.ROOT, "%.1f", load.sourceEquivalentDemand()),
                String.format(Locale.ROOT, "%.1f", load.totalSourceDemand()),
                String.format(Locale.ROOT, "%.1f", load.sourceTorqueCapacity()),
                load.sourcePos() != null,
                load.active()), false);
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
                    "Cannot form Forge Hammer: invalid Voxel profile {}", voxelProfileName);
            return null;
        }
        MultiblockController controller = new MultiblockController(structure);
        controller.identifyAndAddModules(level, worldPosition, structure);
        if (!controller.createStructure(level, worldPosition)) return null;
        // Existing input/output ports survive formation and keep their BEs.
        for (BlockPos member : controller.getBlockMap().keySet()) {
            if (level.getBlockEntity(member) instanceof MultiblockItemPortBlockEntity port)
                port.linkFormedController(worldPosition);
        }
        controller.setFormed(true);
        setMultiblockController(controller);
        formed = true;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(BronzeForgeHammerBlock.IS_FORMED)) {
            level.setBlock(worldPosition, state
                    .setValue(BronzeForgeHammerBlock.IS_FORMED, true)
                    .setValue(BronzeForgeHammerBlock.FACING, matchedFacing), Block.UPDATE_ALL);
        }
        MultiblockRenderParts.assign(level, worldPosition, controller,
                "bronze_forge_hammer", matchedFacing, renderAirAnchors, voxelProfileName);
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
        progress = Math.max(0, tag.getInt("ForgeProgress"));
        activeRecipe = tag.contains("ForgeRecipe")
                ? ResourceLocation.tryParse(tag.getString("ForgeRecipe")) : null;
        activeInput = ItemStack.of(tag.getCompound("ForgeActiveInput"));
        forgingDie.load(tag.getCompound("ForgeDie"));
        // Never assume stale client RUNNING state is authoritative after reload.
        running = tag.getBoolean("ForgeRunning");
        lastInput = itemHandler.getStackInSlot(0).copy();
        lastOutput = itemHandler.getStackInSlot(1).copy();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveMultiblockData(tag, blueprintName, formed, replacementModel);
        if (!voxelProfileName.isBlank()) tag.putString("v2_voxel_profile", voxelProfileName);
        MultiblockVoxelNbt.write(tag, "v2_voxel_collision", collision);
        MultiblockVoxelNbt.write(tag, "v2_voxel_selection", selection);
        tag.putInt("ForgeProgress", progress);
        tag.putBoolean("ForgeRunning", running);
        if (activeRecipe != null) tag.putString("ForgeRecipe", activeRecipe.toString());
        if (!activeInput.isEmpty()) tag.put("ForgeActiveInput", activeInput.save(new CompoundTag()));
        tag.put("ForgeDie", forgingDie.save());
    }
    @Override public CompoundTag sync() {
        setChanged();
        lastInput = itemHandler.getStackInSlot(0).copy();
        lastOutput = itemHandler.getStackInSlot(1).copy();
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
    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }
    // Module ITEM_HANDLER only. The controller cannot bypass the real ports.
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
    @Override public int invsize() { return 2; }
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
    @Override public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(6.0D);
    }
    @Override public @NotNull ModelData getModelData() {
        return super.getModelData().derive()
                .with(MultiBlockProperties.MODEL_NAME, replacementModel).build();
    }
    @Override public Component getDisplayName() {
        return Component.translatable("block.magneticraft2.bronze_forge_hammer");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }
}
