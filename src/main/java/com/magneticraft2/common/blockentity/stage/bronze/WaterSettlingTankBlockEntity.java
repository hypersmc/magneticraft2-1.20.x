package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.WaterSettlingTankBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.FluidRegistry;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Two real tanks and one captured-sediment output.  No fake item inventory,
 * no Gear V2 load: sedimentation is passive, requires TIME and free outputs.
 *
 * Recipe data controls each batch. The controller only accepts fluid-only
 * multiblock_processing recipes for magneticraft2:water_settling_tank.
 */
public final class WaterSettlingTankBlockEntity
        extends BaseBlockEntityMagneticraft2 implements MultiblockVoxelHost {
    private static final String PREFIX = "water_settling_tank_";
    private static final ResourceLocation MACHINE =
            new ResourceLocation("magneticraft2", "water_settling_tank");
    public static final int DIRTY_CAPACITY = 12000;
    public static final int CLEAN_CAPACITY = 12000;

    private String blueprintName = "";
    private String replacementModel = "";
    private String voxelProfileName = "";
    private List<BlockPos> renderAirAnchors = List.of();
    private boolean formed;
    private Direction matchedFacing = Direction.SOUTH;
    private int processTime;
    private boolean processing;
    @Nullable private ResourceLocation activeRecipeId;
    @Nullable private VoxelShape collision, selection;
    private ItemStack lastOutput = ItemStack.EMPTY;

    private final FluidTank dirty = new FluidTank(DIRTY_CAPACITY,
            fluid -> fluid.getFluid() == FluidRegistry.DIRTY_WATER.get()) {
        @Override protected void onContentsChanged() { sync(); }
    };
    private final FluidTank clean = new FluidTank(CLEAN_CAPACITY,
            fluid -> fluid.getFluid() == Fluids.WATER) {
        @Override protected void onContentsChanged() { sync(); }
    };
    private LazyOptional<IFluidHandler> fluidCapability =
            LazyOptional.of(() -> new SettlingFluidHandler());

    public WaterSettlingTankBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.WATER_SETTLING_TANK_BE.get(), pos, state);
    }

    public boolean isFormed() { return formed; }
    public boolean isProcessing() { return processing; }
    public int getDirtyAmount() { return dirty.getFluidAmount(); }
    public int getCleanAmount() { return clean.getFluidAmount(); }
    public float getDirtyFillRatio() { return dirty.getFluidAmount() / (float) DIRTY_CAPACITY; }
    public float getCleanFillRatio() { return clean.getFluidAmount() / (float) CLEAN_CAPACITY; }
    public ItemStack getSedimentStack() { return itemHandler.getStackInSlot(1); }
    public int getProcessTime() { return processTime; }
    public int getProcessDuration() {
        MultiblockProcessingRecipe recipe = currentRecipe();
        return recipe == null ? 0 : recipe.getProcessTime();
    }

    @Override
    public void setMultiblockVoxelShapes(VoxelShape physical, VoxelShape selectable) {
        collision = physical;
        selection = selectable;
        sync();
    }
    @Nullable public VoxelShape getLocalVoxelCollision() { return collision; }
    @Nullable public VoxelShape getLocalVoxelSelection() {
        return selection == null ? collision : selection;
    }

    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos,
            BlockState state, E tile) {
        if (level.isClientSide || !(tile instanceof WaterSettlingTankBlockEntity tank))
            return;
        tank.tickSettling();
        ItemStack shown = tank.getSedimentStack();
        if (!sameVisual(shown, tank.lastOutput)) {
            tank.lastOutput = shown.copy();
            tank.sync();
        }
    }

    private static boolean sameVisual(ItemStack a, ItemStack b) {
        return (a.isEmpty() && b.isEmpty()) ||
                (a.getCount() == b.getCount() && ItemStack.isSameItemSameTags(a, b));
    }

    private MultiblockProcessingRecipe findRecipe() {
        if (level == null) return null;
        for (MultiblockProcessingRecipe recipe : level.getRecipeManager()
                .getAllRecipesFor(MultiblockProcessingRecipe.Type.INSTANCE)) {
            if (isSettlingRecipe(recipe)
                    && dirty.getFluidAmount() >= recipe.getFluidInput().getAmount())
                return recipe;
        }
        return null;
    }

    private static boolean isSettlingRecipe(MultiblockProcessingRecipe recipe) {
        return recipe.isForMachine(MACHINE) && recipe.getInputCount() == 0
                && !recipe.getFluidInput().isEmpty()
                && recipe.getFluidInput().getFluid() == FluidRegistry.DIRTY_WATER.get()
                && !recipe.getFluidOutput().isEmpty()
                && recipe.getFluidOutput().getFluid() == Fluids.WATER
                && !recipe.getOutput().isEmpty()
                && recipe.getTorque() == 0F && recipe.getMinSpeed() == 0F
                && recipe.getFluidOutput().getAmount() < recipe.getFluidInput().getAmount();
    }

    @Nullable
    private MultiblockProcessingRecipe currentRecipe() {
        if (level == null || activeRecipeId == null) return null;
        return level.getRecipeManager().byKey(activeRecipeId)
                .filter(r -> r instanceof MultiblockProcessingRecipe)
                .map(r -> (MultiblockProcessingRecipe) r)
                .filter(WaterSettlingTankBlockEntity::isSettlingRecipe)
                .orElse(null);
    }

    private void tickSettling() {
        if (!formed || level == null) {
            processTime = 0; processing = false; activeRecipeId = null;
            return;
        }
        MultiblockProcessingRecipe recipe = currentRecipe();
        if (activeRecipeId != null && recipe == null) {
            // The recipe changed or was removed on datapack reload.
            processTime = 0;
            activeRecipeId = null;
        }
        if (recipe == null) {
            recipe = findRecipe();
            if (recipe == null) {
                setProcessing(false);
                return;
            }
            activeRecipeId = recipe.getId();
            processTime = 0;
            setChanged();
        }

        FluidStack required = recipe.getFluidInput();
        FluidStack recovered = recipe.getFluidOutput();
        ItemStack sediment = recipe.getOutput();
        boolean ready = dirty.getFluidAmount() >= required.getAmount()
                && dirty.getFluid().isFluidEqual(required)
                && clean.fill(recovered, IFluidHandler.FluidAction.SIMULATE)
                    == recovered.getAmount()
                && itemHandler.insertItem(1, sediment.copy(), true).isEmpty();
        if (!ready) {
            setProcessing(false);
            return; // Keep unfinished progress and materials on every blockage.
        }

        setProcessing(true);
        ++processTime;
        setChanged();
        if (processTime < recipe.getProcessTime()) return;

        // Same server tick, after simulating ALL outputs. Consume exactly
        // one batch, then commit clean fluid and captured wet sediment.
        FluidStack drained = dirty.drain(required, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != required.getAmount())
            throw new IllegalStateException("Settling batch drained incorrectly");
        clean.fill(recovered, IFluidHandler.FluidAction.EXECUTE);
        ItemStack remainder = itemHandler.insertItem(1, sediment.copy(), false);
        if (!remainder.isEmpty())
            throw new IllegalStateException("Settling batch exceeded sediment capacity");
        processTime = 0;
        activeRecipeId = null;
        processing = false;
        sync();
    }

    private void setProcessing(boolean value) {
        if (processing == value) return;
        processing = value;
        sync();
    }

    @Override
    protected void interactableNoGui(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return;
        // Real output inventory extraction belongs to the primary item port.
        // This controller provides status only; it never creates bonus items.
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.water_settling_tank.status",
                getDirtyAmount(), DIRTY_CAPACITY, getCleanAmount(), CLEAN_CAPACITY,
                getSedimentStack().getCount(),
                processing ? getProcessTime() : 0, getProcessDuration()), true);
    }

    @Override
    protected MultiblockController createMultiblockController() {
        MultiblockStructure structure = identifyMultiblockStructure(level, worldPosition);
        if (structure == null || !MultiblockVoxelProfiles.exists(voxelProfileName))
            return null;
        MultiblockController controller = new MultiblockController(structure);
        controller.identifyAndAddModules(level, worldPosition, structure);
        if (!controller.createStructure(level, worldPosition)) return null;
        controller.setFormed(true);
        setMultiblockController(controller);
        formed = true;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(WaterSettlingTankBlock.IS_FORMED)) {
            BlockState updated = state
                    .setValue(WaterSettlingTankBlock.IS_FORMED, true)
                    .setValue(WaterSettlingTankBlock.FACING, matchedFacing);
            level.setBlock(worldPosition, updated, Block.UPDATE_ALL);
        }
        MultiblockRenderParts.assign(level, worldPosition, controller,
                "water_settling_tank", matchedFacing, renderAirAnchors, voxelProfileName);
        requestModelDataUpdate();
        sync();
        return controller;
    }

    @Override
    protected MultiblockStructure identifyMultiblockStructure(Level world, BlockPos pos) {
        for (Multiblock multiblock : MultiblockRegistry.getRegisteredMultiblocks().values()) {
            if (!multiblock.getName().startsWith(PREFIX)) continue;
            if (!matchesStructure(world, pos, multiblock.getStructure(), multiblock))
                continue;
            blueprintName = multiblock.getName();
            replacementModel = multiblock.getSettings().getReplaceWhenFormed();
            voxelProfileName = multiblock.getSettings().getVoxelProfile();
            renderAirAnchors = multiblock.getSettings().getRenderAirAnchors();
            matchedFacing = facingFromMultiblockName(blueprintName);
            return multiblock.getStructure();
        }
        return null;
    }

    private Direction facingFromMultiblockName(String name) {
        if (name.endsWith("_north")) return Direction.NORTH;
        if (name.endsWith("_west")) return Direction.WEST;
        if (name.endsWith("_east")) return Direction.EAST;
        return Direction.SOUTH;
    }

    private final class SettlingFluidHandler implements IFluidHandler {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? dirty.getFluid() :
                    tank == 1 ? clean.getFluid() : FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int tank) {
            return tank == 0 ? DIRTY_CAPACITY : tank == 1 ? CLEAN_CAPACITY : 0;
        }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 && dirty.isFluidValid(stack);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            return formed ? dirty.fill(resource, action) : 0;
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return formed ? clean.drain(resource, action) : FluidStack.EMPTY;
        }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return formed ? clean.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER)
            return fluidCapability.cast();
        // Only the real output module may extract sediment. The controller
        // must not become a second all-slots item automation port.
        if (capability == ForgeCapabilities.ITEM_HANDLER)
            return LazyOptional.empty();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }
    @Override public void reviveCaps() {
        super.reviveCaps();
        fluidCapability = LazyOptional.of(() -> new SettlingFluidHandler());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        processTime = tag.getInt("SettlingProgress");
        processing = tag.getBoolean("Settling");
        activeRecipeId = tag.contains("SettlingRecipe")
                ? ResourceLocation.tryParse(tag.getString("SettlingRecipe")) : null;
        if (tag.contains("DirtyTank")) dirty.readFromNBT(tag.getCompound("DirtyTank"));
        if (tag.contains("CleanTank")) clean.readFromNBT(tag.getCompound("CleanTank"));
        MultiblockPersistentData data = loadMultiblockData(tag);
        blueprintName = data.blueprintName();
        formed = data.formed();
        replacementModel = data.replacementModel();
        voxelProfileName = tag.getString("v2_voxel_profile");
        collision = MultiblockVoxelNbt.read(tag, "v2_voxel_collision");
        selection = MultiblockVoxelNbt.read(tag, "v2_voxel_selection");
        lastOutput = getSedimentStack().copy();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("SettlingProgress", processTime);
        tag.putBoolean("Settling", processing);
        if (activeRecipeId != null) tag.putString("SettlingRecipe", activeRecipeId.toString());
        tag.put("DirtyTank", dirty.writeToNBT(new CompoundTag()));
        tag.put("CleanTank", clean.writeToNBT(new CompoundTag()));
        if (!voxelProfileName.isBlank())
            tag.putString("v2_voxel_profile", voxelProfileName);
        MultiblockVoxelNbt.write(tag, "v2_voxel_collision", collision);
        MultiblockVoxelNbt.write(tag, "v2_voxel_selection", selection);
        saveMultiblockData(tag, blueprintName, formed, replacementModel);
    }
    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }
    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }
    @Override public CompoundTag sync() {
        setChanged();
        lastOutput = getSedimentStack().copy();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(),
                getBlockState(), Block.UPDATE_ALL);
        return getUpdateTag();
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
        return new AABB(worldPosition).inflate(5.0D);
    }
    @Override public @NotNull ModelData getModelData() {
        return super.getModelData().derive()
                .with(MultiBlockProperties.MODEL_NAME, replacementModel).build();
    }
    @Override public Component getDisplayName() {
        return Component.translatable("block.magneticraft2.water_settling_tank");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id,
            Inventory inventory, Player player) { return null; }
}
