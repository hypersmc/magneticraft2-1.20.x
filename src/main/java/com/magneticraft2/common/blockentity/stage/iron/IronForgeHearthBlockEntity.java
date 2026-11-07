package com.magneticraft2.common.blockentity.stage.iron;

import com.magneticraft2.common.block.stage.iron.IronForgeHearthBlock;
import com.magneticraft2.common.blockentity.stage.copper.MultiblockItemPortBlockEntity;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.systems.material.HotBloomWorkpiece;
import com.magneticraft2.common.recipe.multiblock.MultiblockRecipeHandler;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Recipe-driven, server-authoritative Iron Forge Hearth. Rendering stays independent. */
public final class IronForgeHearthBlockEntity extends BaseBlockEntityMagneticraft2
        implements MultiblockVoxelHost {
    private static final String PREFIX = "iron_forge_hearth_";
    private static final ResourceLocation MACHINE = new ResourceLocation("magneticraft2", "iron_forge_hearth");
    private static final int FUEL_QUEUE_LIMIT = 16;
    private ItemStack fuel = ItemStack.EMPTY;
    private int burnRemaining, burnTotal, progress;
    @Nullable private ResourceLocation activeRecipeId;
    // Ignore stack count but preserve identity and NBT (including material quality).
    private ItemStack activeInput = ItemStack.EMPTY;
    private ItemStack lastInput = ItemStack.EMPTY, lastOutput = ItemStack.EMPTY;
    private String blueprintName = "";
    private String replacementModel = "";
    private boolean formed;
    private Direction matchedFacing = Direction.SOUTH;
    // Copied from the matching JSON settings only for this formation attempt.
    private java.util.List<BlockPos> renderAirAnchors = java.util.List.of();
    private String voxelProfileName = "";
    @Nullable private VoxelShape localVoxelCollision;
    @Nullable private VoxelShape localVoxelSelection;

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

    public IronForgeHearthBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.IRON_FORGE_HEARTH_BE.get(), pos, state);
        menuProvider = null;
    }

    public boolean isFormed() { return formed; }

    @Override
    protected @Nullable MultiblockController createMultiblockController() {
        if (level == null || level.isClientSide || formed) return null;
        MultiblockStructure structure = identifyMultiblockStructure(level, worldPosition);
        if (structure == null) return null;
        // Refuse missing/broken profile before replacing any in-world bricks.
        if (!voxelProfileName.isBlank() && !MultiblockVoxelProfiles.exists(voxelProfileName)) {
            org.apache.logging.log4j.LogManager.getLogger("Magneticraft2Voxels").error(
                    "Cannot form Iron Forge Hearth: missing Voxel V2 profile {}", voxelProfileName);
            return null;
        }
        MultiblockController controller = new MultiblockController(structure);
        controller.identifyAndAddModules(level, worldPosition, structure);
        if (!controller.createStructure(level, worldPosition)) return null;
        // Keep both genuine module BEs linked for their port skins and item handlers.
        // This is the same formed-port linkage already used by the Retort.
        for (BlockPos member : controller.getBlockMap().keySet()) {
            if (level.getBlockEntity(member) instanceof MultiblockItemPortBlockEntity port) {
                port.linkFormedController(worldPosition);
            }
        }
        controller.setFormed(true);
        setMultiblockController(controller);
        formed = true;

        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(IronForgeHearthBlock.IS_FORMED)) {
            level.setBlock(worldPosition, state
                    .setValue(IronForgeHearthBlock.IS_FORMED, true)
                    .setValue(IronForgeHearthBlock.FACING, matchedFacing), Block.UPDATE_ALL);
        }
        // The JSON controller uses its own block-local baked model.
        // Every filler receives its generated part ID and a collision snapshot.
        MultiblockRenderParts.assign(level, worldPosition, controller,
                "iron_forge_hearth", matchedFacing, renderAirAnchors, voxelProfileName);
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

    public boolean acceptsRecipeInput(ItemStack stack) {
        return MultiblockRecipeHandler.acceptsInput(level, MACHINE, stack);
    }

    private static boolean validRecipe(@Nullable MultiblockProcessingRecipe recipe) {
        return recipe != null && recipe.getInputCount() == 4
                && recipe.getOutput().getCount() == 4
                && HotBloomWorkpiece.isReheatRecipe(recipe)
                && recipe.getByproduct().isEmpty()
                && recipe.getMinSpeed() == 0F && recipe.getTorque() == 0F
                && recipe.getFluidInput().isEmpty() && recipe.getFluidOutput().isEmpty();
    }

    @Nullable private MultiblockProcessingRecipe findRecipe() {
        return MultiblockRecipeHandler.findRecipe(level, MACHINE, itemHandler.getStackInSlot(0));
    }

    private static int fuelTime(ItemStack stack) {
        if (stack.isEmpty() || !stack.getCraftingRemainingItem().isEmpty()) return 0;
        return Math.max(0, ForgeHooks.getBurnTime(stack, RecipeType.SMELTING));
    }

    private static boolean sameStack(ItemStack a, ItemStack b) {
        return (a.isEmpty() && b.isEmpty()) ||
                (a.getCount() == b.getCount() && ItemStack.isSameItemSameTags(a, b));
    }

    public static <E extends BlockEntity> void serverTick(Level world, BlockPos pos,
            BlockState state, E tile) {
        if (world.isClientSide || !(tile instanceof IronForgeHearthBlockEntity kiln)) return;
        kiln.tickHearth();
        ItemStack in = kiln.itemHandler.getStackInSlot(0);
        ItemStack out = kiln.itemHandler.getStackInSlot(1);
        if (!sameStack(in, kiln.lastInput) || !sameStack(out, kiln.lastOutput))
            kiln.sync();
    }

    private void tickHearth() {
        if (level == null || !formed) return;
        int capacity = heatHandler.getMaxHeatStored();
        int heat = Math.max(0, Math.min(capacity, heatHandler.getHeatStored()));
        int threshold = Magneticraft2ConfigCommon.GENERAL.HearthHeatingThreshold.get();
        int cost = Magneticraft2ConfigCommon.GENERAL.HearthHeatingHeatPerTick.get();
        int gain = Magneticraft2ConfigCommon.GENERAL.HearthHeatPerFuelTick.get();
        int loss = Magneticraft2ConfigCommon.GENERAL.HearthPassiveHeatLoss.get();
        boolean changed = false, notify = false;

        // An ignited fuel item continues burning even if the output fills.
        if (burnRemaining > 0) {
            burnRemaining--;
            heat = (int) Math.min((long) capacity, (long) heat + gain);
            changed = true;
        }
        int cooled = Math.max(0, heat - loss);
        if (cooled != heat) changed = true;
        heat = cooled;

        MultiblockProcessingRecipe recipe = findRecipe();
        ItemStack candidate = itemHandler.getStackInSlot(0);
        // If a datapack replaces our batch recipe with a material-conversion
        // recipe, do not produce hot wrought stock or skip thermal accounting.
        if (!validRecipe(recipe) || !candidate.is(recipe.getOutput().getItem())
                || threshold > capacity || cost > capacity) {
            if (progress != 0 || activeRecipeId != null || !activeInput.isEmpty()) {
                progress = 0;
                activeRecipeId = null;
                activeInput = ItemStack.EMPTY;
                changed = true;
                notify = true;
            }
        } else {
            if (!recipe.getId().equals(activeRecipeId)
                    || !ItemStack.isSameItemSameTags(activeInput, candidate)) {
                activeRecipeId = recipe.getId();
                activeInput = candidate.copy();
                activeInput.setCount(1);
                progress = 0;
                changed = true;
                notify = true;
            }
            ItemStack product = recipe.getOutput();
            // Reheating an Iron Bloom, Wrought Iron Ingot or Iron Bar
            // requires real fuel, HEAT and a dedicated datapack recipe. The timestamp is
            // authoritative world game-time, not client cosmetic state.
            // Use the same tagged product for BOTH reservation and commit.
            if (HotBloomWorkpiece.isReheatRecipe(recipe)) {
                product = HotBloomWorkpiece.heatInKiln(candidate, product, level);
            }
            // Reserve output before new ignition, firing progress or input removal.
            boolean outputFits = itemHandler.insertItem(1, product, true).isEmpty();
            if (outputFits) {
                if (burnRemaining <= 0 && (long) heat < (long) threshold + cost && !fuel.isEmpty()) {
                    int time = fuelTime(fuel);
                    if (time > 0) {
                        fuel.shrink(1);
                        if (fuel.isEmpty()) fuel = ItemStack.EMPTY;
                        burnRemaining = time;
                        burnTotal = time;
                        changed = true;
                        notify = true;
                    }
                }
                if (heat >= threshold && heat >= cost) {
                    heat -= cost;
                    progress++;
                    changed = true;
                    if (progress >= recipe.getProcessTime()) {
                        // No output can be emitted twice; this tick commits once.
                        if (!itemHandler.insertItem(1, product, true).isEmpty())
                            throw new IllegalStateException("Hearth output reservation lost");
                        ItemStack removed = itemHandler.extractItem(0, recipe.getInputCount(), false);
                        if (removed.getCount() != recipe.getInputCount())
                            throw new IllegalStateException("Hearth input changed during firing");
                        if (!itemHandler.insertItem(1, product, false).isEmpty())
                            throw new IllegalStateException("Hearth output became unavailable");
                        activeRecipeId = null;
                        activeInput = ItemStack.EMPTY;
                        progress = 0;
                        notify = true;
                    }
                }
            }
        }
        if (heat != heatHandler.getHeatStored()) {
            heatHandler.setHeat(heat);
            changed = true;
        }
        if (changed) {
            setChanged();
            if (notify || level.getGameTime() % 20L == 0L) sync();
        }
    }

    @Override protected void interactableNoGui(BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide || !formed) return;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() && player.isShiftKeyDown() && !fuel.isEmpty()) {
            ItemStack returned = fuel.copy();
            fuel = ItemStack.EMPTY;
            // Return only any remainder; do not duplicate queued fuel.
            if (!player.getInventory().add(returned) && !returned.isEmpty())
                player.drop(returned, false);
            sync();
            return;
        }
        if (!held.isEmpty()) {
            boolean ok = fuelTime(held) > 0 && fuel.getCount() < FUEL_QUEUE_LIMIT
                    && (fuel.isEmpty() || ItemStack.isSameItemSameTags(fuel, held));
            if (ok) {
                if (fuel.isEmpty()) {
                    fuel = held.copy();
                    fuel.setCount(1);
                } else fuel.grow(1);
                if (!player.getAbilities().instabuild) held.shrink(1);
                sync();
            }
            player.displayClientMessage(Component.translatable(
                    ok ? "message.magneticraft2.iron_forge_hearth.fuel_added"
                       : "message.magneticraft2.iron_forge_hearth.fuel_rejected"), true);
            return;
        }
        MultiblockProcessingRecipe recipe = findRecipe();
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.iron_forge_hearth.status",
                fuel.getCount(), burnRemaining, heatHandler.getHeatStored(),
                heatHandler.getMaxHeatStored(), progress,
                recipe == null ? 0 : recipe.getProcessTime(),
                itemHandler.getStackInSlot(0).getCount(),
                itemHandler.getStackInSlot(1).getCount()), true);
    }

    @Override public void onDestroy(Level world) {
        if (!world.isClientSide && !fuel.isEmpty()) {
            Block.popResource(world, worldPosition, fuel.copy());
            fuel = ItemStack.EMPTY;
        }
        super.onDestroy(world);
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
        fuel = ItemStack.of(tag.getCompound("HearthFuel"));
        burnTotal = Math.max(0, tag.getInt("HearthBurnTotal"));
        burnRemaining = Math.max(0, Math.min(burnTotal, tag.getInt("HearthBurnRemaining")));
        progress = Math.max(0, tag.getInt("HearthProgress"));
        activeRecipeId = tag.contains("HearthRecipe")
                ? ResourceLocation.tryParse(tag.getString("HearthRecipe")) : null;
        activeInput = ItemStack.of(tag.getCompound("HearthActiveInput"));
        lastInput = itemHandler.getStackInSlot(0).copy();
        lastOutput = itemHandler.getStackInSlot(1).copy();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveMultiblockData(tag, blueprintName, formed, replacementModel);
        if (!voxelProfileName.isBlank()) tag.putString("v2_voxel_profile", voxelProfileName);
        MultiblockVoxelNbt.write(tag, "v2_voxel_collision", localVoxelCollision);
        MultiblockVoxelNbt.write(tag, "v2_voxel_selection", localVoxelSelection);
        tag.putInt("HearthBurnTotal", burnTotal);
        tag.putInt("HearthBurnRemaining", burnRemaining);
        tag.putInt("HearthProgress", progress);
        if (!fuel.isEmpty()) tag.put("HearthFuel", fuel.save(new CompoundTag()));
        if (activeRecipeId != null) tag.putString("HearthRecipe", activeRecipeId.toString());
        if (!activeInput.isEmpty()) tag.put("HearthActiveInput", activeInput.save(new CompoundTag()));
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
        lastInput = itemHandler.getStackInSlot(0).copy();
        lastOutput = itemHandler.getStackInSlot(1).copy();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return getUpdateTag();
    }

    // The two genuine module blocks own all item I/O. Exposing the
    // base controller-wide handler here would bypass the K1 input lock.
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return LazyOptional.empty();
        return super.getCapability(cap, side);
    }

    @Override public int capacityE() { return 0; }
    @Override public int maxtransferE() { return 0; }
    @Override public int capacityH() {
        return Magneticraft2ConfigCommon.GENERAL.HearthHeatCapacity.get();
    }
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
    @Override public boolean heatcape() { return true; }
    @Override public boolean wattcape() { return false; }
    @Override public boolean fluidcape() { return false; }
    @Override public boolean pressurecape() { return false; }

    // No external HEAT transfer in K2; only the solid-fuel firebox supplies HEAT.
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
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(5.0D); }
    @Override public @NotNull ModelData getModelData() {
        return super.getModelData().derive()
                .with(MultiBlockProperties.MODEL_NAME, replacementModel).build();
    }
    @Override public Component getDisplayName() {
        return Component.translatable("block.magneticraft2.iron_forge_hearth");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }
}
