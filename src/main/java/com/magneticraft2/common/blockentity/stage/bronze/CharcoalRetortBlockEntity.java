package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.CharcoalRetortBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.copper.MultiblockItemPortBlockEntity;
import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.recipe.multiblock.MultiblockRecipeHandler;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Server-authoritative retort cycle. Logs use REAL item port slot 0;
 * charcoal and tar are independently extractable from slots 1 and 2.
 * The firebox fuel queue is separate from port inventory and cannot be
 * mistaken for pyrolysis feed. Every byproduct is reserved before input is spent.
 */
public final class CharcoalRetortBlockEntity extends BaseBlockEntityMagneticraft2
        implements MultiblockVoxelHost {
    private static final ResourceLocation MACHINE =
            new ResourceLocation("magneticraft2", "charcoal_retort");
    private static final String PREFIX = "charcoal_retort_";
    private static final int FUEL_QUEUE_LIMIT = 16;

    private String blueprintName = "";
    private String replacementModel = "";
    private String voxelProfileName = "";
    private List<BlockPos> airAnchors = List.of();
    private Direction matchedFacing = Direction.SOUTH;
    private boolean formed;
    @Nullable private VoxelShape collision, selection;
    private ItemStack fuel = ItemStack.EMPTY;
    private ItemStack lastIn = ItemStack.EMPTY, lastOut = ItemStack.EMPTY, lastTar = ItemStack.EMPTY;
    private int burnRemaining, burnTotal, progress;
    private boolean processing;
    @Nullable private ResourceLocation activeRecipeId;

    public CharcoalRetortBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.CHARCOAL_RETORT_BE.get(), pos, state);
    }

    public boolean isFormed() { return formed; }
    public boolean isProcessing() { return processing; }
    public int getProgress() { return progress; }
    public int getBurnRemaining() { return burnRemaining; }
    public ItemStack getFuelDisplay() { return fuel.copy(); }
    public ItemStack getLogDisplay() { return itemHandler.getStackInSlot(0).copy(); }
    public ItemStack getCharcoalDisplay() { return itemHandler.getStackInSlot(1).copy(); }
    public ItemStack getTarDisplay() { return itemHandler.getStackInSlot(2).copy(); }
    public boolean acceptsRecipeInput(ItemStack stack) {
        return MultiblockRecipeHandler.acceptsInput(level, MACHINE, stack);
    }

    @Override public void setMultiblockVoxelShapes(VoxelShape physical, VoxelShape selectable) {
        collision = physical;
        selection = selectable;
        sync();
    }
    @Nullable public VoxelShape getLocalVoxelCollision() { return collision; }
    @Nullable public VoxelShape getLocalVoxelSelection() { return selection == null ? collision : selection; }

    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos,
            BlockState state, E tile) {
        if (level.isClientSide || !(tile instanceof CharcoalRetortBlockEntity retort)) return;
        retort.tickRetort();
        ItemStack a = retort.itemHandler.getStackInSlot(0);
        ItemStack b = retort.itemHandler.getStackInSlot(1);
        ItemStack c = retort.itemHandler.getStackInSlot(2);
        if (!sameStack(a, retort.lastIn) || !sameStack(b, retort.lastOut)
                || !sameStack(c, retort.lastTar)) retort.sync();
    }

    private static boolean sameStack(ItemStack a, ItemStack b) {
        return (a.isEmpty() && b.isEmpty()) ||
                (a.getCount() == b.getCount() && ItemStack.isSameItemSameTags(a, b));
    }

    @Nullable private MultiblockProcessingRecipe findRecipe() {
        return MultiblockRecipeHandler.findRecipe(level, MACHINE, itemHandler.getStackInSlot(0));
    }

    private static boolean validRecipe(@Nullable MultiblockProcessingRecipe recipe) {
        return recipe != null && recipe.getInputCount() > 0
                && !recipe.getOutput().isEmpty() && !recipe.getByproduct().isEmpty()
                && recipe.getByproductChance() == 1F
                && recipe.getTorque() == 0F && recipe.getMinSpeed() == 0F
                && recipe.getFluidInput().isEmpty() && recipe.getFluidOutput().isEmpty();
    }

    private static int fuelTime(ItemStack stack) {
        if (stack.isEmpty() || !stack.getCraftingRemainingItem().isEmpty()) return 0;
        // Tar recovered by this machine can be burned here immediately; other
        // furnace-compatible solid fuels use the same authoritative Forge lookup.
        if (stack.is(ItemRegistry.ITEM_WOOD_TAR.get())) return 200;
        return Math.max(0, ForgeHooks.getBurnTime(stack, RecipeType.SMELTING));
    }

    private void tickRetort() {
        if (level == null) return;
        if (!formed) {
            updateLit(false);
            setProcessing(false);
            return;
        }

        // Burning fuel keeps burning even if an output becomes blocked.
        if (burnRemaining > 0) {
            burnRemaining--;
            setChanged();
        }
        MultiblockProcessingRecipe recipe = findRecipe();
        if (!validRecipe(recipe)) {
            if (progress != 0 || activeRecipeId != null) {
                progress = 0;
                activeRecipeId = null;
                setChanged();
            }
            setProcessing(false);
            updateLit(burnRemaining > 0);
            return;
        }

        if (!recipe.getId().equals(activeRecipeId)) {
            activeRecipeId = recipe.getId();
            progress = 0;
            setChanged();
        }

        // Check ALL ports before burning a new fuel item or consuming feed.
        ItemStack result = recipe.getOutput(), tar = recipe.getByproduct();
        if (!itemHandler.insertItem(1, result, true).isEmpty()
                || !itemHandler.insertItem(2, tar, true).isEmpty()) {
            setProcessing(false);
            updateLit(burnRemaining > 0);
            return;
        }

        if (burnRemaining <= 0) {
            if (fuel.isEmpty() || fuelTime(fuel) <= 0) {
                setProcessing(false);
                updateLit(false);
                return;
            }
            burnTotal = fuelTime(fuel);
            burnRemaining = burnTotal;
            fuel.shrink(1);
            if (fuel.isEmpty()) fuel = ItemStack.EMPTY;
            sync();
        }
        updateLit(true);
        setProcessing(true);
        progress++;
        setChanged();
        if (progress < recipe.getProcessTime()) {
            if (level.getGameTime() % 20L == 0L) sync();
            return;
        }

        // Commit only after capacity checks, exactly once per complete batch.
        ItemStack removed = itemHandler.extractItem(0, recipe.getInputCount(), false);
        if (removed.getCount() != recipe.getInputCount()) {
            throw new IllegalStateException("Retort feed changed during a completed batch");
        }
        if (!itemHandler.insertItem(1, result, false).isEmpty()
                || !itemHandler.insertItem(2, tar, false).isEmpty()) {
            throw new IllegalStateException("Retort reserved outputs were not available");
        }
        progress = 0;
        activeRecipeId = null;
        setProcessing(false);
        sync();
    }

    private void setProcessing(boolean next) {
        if (processing == next) return;
        processing = next;
        sync();
    }
    private void updateLit(boolean next) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof CharcoalRetortBlock
                && state.getValue(CharcoalRetortBlock.LIT) != next)
            level.setBlock(worldPosition, state.setValue(CharcoalRetortBlock.LIT, next), Block.UPDATE_CLIENTS);
    }

    @Override protected void interactableNoGui(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return;
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            boolean valid = fuelTime(held) > 0 && fuel.getCount() < FUEL_QUEUE_LIMIT
                    && (fuel.isEmpty() || ItemStack.isSameItemSameTags(fuel, held));
            if (valid) {
                if (fuel.isEmpty()) {
                    fuel = held.copy();
                    fuel.setCount(1);
                } else fuel.grow(1);
                if (!player.getAbilities().instabuild) held.shrink(1);
                sync();
            }
            player.displayClientMessage(Component.translatable(
                    valid ? "message.magneticraft2.charcoal_retort.fuel_added"
                            : "message.magneticraft2.charcoal_retort.fuel_rejected"), true);
            return;
        }
        if (player.isShiftKeyDown() && !fuel.isEmpty()) {
            ItemStack removed = fuel;
            fuel = ItemStack.EMPTY;
            if (!player.getInventory().add(removed)) player.drop(removed, false);
            sync();
            return;
        }
        MultiblockProcessingRecipe recipe = findRecipe();
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.charcoal_retort.status", fuel.getCount(),
                burnRemaining, itemHandler.getStackInSlot(0).getCount(),
                itemHandler.getStackInSlot(1).getCount(), itemHandler.getStackInSlot(2).getCount(),
                progress, recipe == null ? 0 : recipe.getProcessTime()), true);
    }

    @Override protected @Nullable MultiblockController createMultiblockController() {
        if (level == null || level.isClientSide || formed) return null;
        MultiblockStructure structure = identifyMultiblockStructure(level, worldPosition);
        if (structure == null || !MultiblockVoxelProfiles.exists(voxelProfileName)) return null;
        MultiblockController controller = new MultiblockController(structure);
        controller.identifyAndAddModules(level, worldPosition, structure);
        if (!controller.createStructure(level, worldPosition)) return null;
        // These remain REAL item module blocks. Link only this Retort's
        // ports to its controller so formed-state models AND slot forwarding
        // resolve correctly; other multiblock controllers are unchanged.
        for (BlockPos member : controller.getBlockMap().keySet()) {
            if (level.getBlockEntity(member) instanceof MultiblockItemPortBlockEntity port) {
                port.linkRetortController(worldPosition);
            }
        }
        controller.setFormed(true);
        setMultiblockController(controller);
        formed = true;
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof CharcoalRetortBlock) {
            level.setBlock(worldPosition, state.setValue(CharcoalRetortBlock.IS_FORMED, true)
                    .setValue(CharcoalRetortBlock.FACING, matchedFacing), Block.UPDATE_ALL);
        }
        MultiblockRenderParts.assign(level, worldPosition, controller,
                "charcoal_retort", matchedFacing, airAnchors, voxelProfileName);
        requestModelDataUpdate();
        sync();
        return controller;
    }

    @Override protected @Nullable MultiblockStructure identifyMultiblockStructure(Level level, BlockPos pos) {
        for (Multiblock mb : MultiblockRegistry.getRegisteredMultiblocks().values()) {
            if (!mb.getName().startsWith(PREFIX)
                    || !matchesStructure(level, pos, mb.getStructure(), mb)) continue;
            blueprintName = mb.getName();
            replacementModel = mb.getSettings().getReplaceWhenFormed();
            voxelProfileName = mb.getSettings().getVoxelProfile();
            airAnchors = mb.getSettings().getRenderAirAnchors();
            matchedFacing = fromName(blueprintName);
            return mb.getStructure();
        }
        return null;
    }
    private static Direction fromName(String name) {
        if (name.endsWith("_north")) return Direction.NORTH;
        if (name.endsWith("_east")) return Direction.EAST;
        if (name.endsWith("_west")) return Direction.WEST;
        return Direction.SOUTH;
    }

    @Override public void onDestroy(Level level) {
        if (!level.isClientSide && !fuel.isEmpty()) {
            ItemStack dropped = fuel.copy();
            fuel = ItemStack.EMPTY;
            Block.popResource(level, worldPosition, dropped);
        }
        super.onDestroy(level);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        MultiblockPersistentData data = loadMultiblockData(tag);
        blueprintName = data.blueprintName();
        formed = data.formed();
        replacementModel = data.replacementModel();
        matchedFacing = fromName(blueprintName);
        voxelProfileName = tag.getString("v2_voxel_profile");
        collision = MultiblockVoxelNbt.read(tag, "v2_voxel_collision");
        selection = MultiblockVoxelNbt.read(tag, "v2_voxel_selection");
        progress = Math.max(0, tag.getInt("RetortProgress"));
        processing = tag.getBoolean("RetortProcessing");
        fuel = ItemStack.of(tag.getCompound("RetortFuel"));
        burnTotal = Math.max(0, tag.getInt("RetortBurnTotal"));
        burnRemaining = Math.max(0, Math.min(burnTotal, tag.getInt("RetortBurnRemaining")));
        activeRecipeId = tag.contains("RetortRecipe")
                ? ResourceLocation.tryParse(tag.getString("RetortRecipe")) : null;
        lastIn = itemHandler.getStackInSlot(0).copy();
        lastOut = itemHandler.getStackInSlot(1).copy();
        lastTar = itemHandler.getStackInSlot(2).copy();
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveMultiblockData(tag, blueprintName, formed, replacementModel);
        if (!voxelProfileName.isBlank()) tag.putString("v2_voxel_profile", voxelProfileName);
        MultiblockVoxelNbt.write(tag, "v2_voxel_collision", collision);
        MultiblockVoxelNbt.write(tag, "v2_voxel_selection", selection);
        tag.putInt("RetortProgress", progress);
        tag.putBoolean("RetortProcessing", processing);
        tag.putInt("RetortBurnTotal", burnTotal);
        tag.putInt("RetortBurnRemaining", burnRemaining);
        if (!fuel.isEmpty()) tag.put("RetortFuel", fuel.save(new CompoundTag()));
        if (activeRecipeId != null) tag.putString("RetortRecipe", activeRecipeId.toString());
    }

    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag nbt = new CompoundTag();
        saveAdditional(nbt);
        return nbt;
    }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }
    @Override public CompoundTag sync() {
        setChanged();
        lastIn = itemHandler.getStackInSlot(0).copy();
        lastOut = itemHandler.getStackInSlot(1).copy();
        lastTar = itemHandler.getStackInSlot(2).copy();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        return getUpdateTag();
    }

    // The real input/output module block entities own all item automation.
    @Override public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return LazyOptional.empty();
        return super.getCapability(capability, side);
    }

    @Override public int capacityE() { return 0; }
    @Override public int maxtransferE() { return 0; }
    @Override public int capacityH() { return 0; }
    @Override public int maxtransferH() { return 0; }
    @Override public int capacityW() { return 0; }
    @Override public int maxtransferW() { return 0; }
    @Override public int capacityF() { return 0; }
    @Override public int tanks() { return 0; }
    @Override public int invsize() { return 3; }
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
        return new AABB(worldPosition).inflate(4.0D);
    }
    @Override public @NotNull ModelData getModelData() {
        return super.getModelData().derive()
                .with(MultiBlockProperties.MODEL_NAME, replacementModel).build();
    }
    @Override public Component getDisplayName() {
        return Component.translatable("block.magneticraft2.charcoal_retort");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }
}
