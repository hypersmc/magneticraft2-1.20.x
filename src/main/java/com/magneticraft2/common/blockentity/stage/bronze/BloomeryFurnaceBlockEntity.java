package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BloomeryFurnaceBlock;
import com.magneticraft2.common.blockentity.stage.copper.MultiblockItemPortBlockEntity;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.recipe.multiblock.BloomeryRecipe;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressure;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressurizedGas;
import com.magneticraft2.common.systems.PRESSURE.IPressurizedGasHandler;
import com.magneticraft2.common.systems.PRESSURE.LegacyAirPort;
import net.minecraft.world.item.Items;
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

/** Recipe-driven, server-authoritative Bloomery Furnace. Rendering stays independent. */
public final class BloomeryFurnaceBlockEntity extends BaseBlockEntityMagneticraft2
        implements MultiblockVoxelHost {
    private static final String PREFIX = "bloomery_furnace_";
    // K1 balance constants are provisional HEAT units, not degrees Celsius.
    private static final int HEAT_CAPACITY=24000, IGNITION_THRESHOLD=9000;
    private static final int HEAT_GAIN=75, PASSIVE_LOSS=3, HEAT_COST=12;
    private static final int AIR_CAPACITY=8, AIR_INTERVAL=5;
    private static final int FUEL_QUEUE_LIMIT = 16;
    private ItemStack fuel = ItemStack.EMPTY;
    private int burnRemaining, burnTotal, progress;
    @Nullable private ResourceLocation activeRecipeId;
    // Ignore stack count but preserve identity and NBT (including material quality).
    private ItemStack activeInput = ItemStack.EMPTY;
    private ItemStack activeFlux = ItemStack.EMPTY;
    private int airStep;
    private ItemStack lastInput = ItemStack.EMPTY, lastOutput = ItemStack.EMPTY;
    private ItemStack lastFlux = ItemStack.EMPTY, lastSlag = ItemStack.EMPTY;
    private final LazyOptional<IPressurizedGasHandler> gasInput =
            LazyOptional.of(() -> new LegacyAirPort(pressureHandler));
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

    public BloomeryFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BLOOMERY_FURNACE_BE.get(), pos, state);
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
                    "Cannot form Bloomery Furnace: missing Voxel V2 profile {}", voxelProfileName);
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
        if (state.hasProperty(BloomeryFurnaceBlock.IS_FORMED)) {
            level.setBlock(worldPosition, state
                    .setValue(BloomeryFurnaceBlock.IS_FORMED, true)
                    .setValue(BloomeryFurnaceBlock.FACING, matchedFacing), Block.UPDATE_ALL);
        }
        // The JSON controller uses its own block-local baked model.
        // Every filler receives its generated part ID and a collision snapshot.
        MultiblockRenderParts.assign(level, worldPosition, controller,
                "bloomery_furnace", matchedFacing, renderAirAnchors, voxelProfileName);
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

    public boolean acceptsIngredientSlot(int slot, ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        for (BloomeryRecipe recipe : level.getRecipeManager()
                .getAllRecipesFor(BloomeryRecipe.Type.INSTANCE))
            if (recipe.accepts(slot, stack)) return true;
        return false;
    }

    @Nullable private BloomeryRecipe findRecipe() {
        if (level == null) return null;
        for (BloomeryRecipe recipe : level.getRecipeManager()
                .getAllRecipesFor(BloomeryRecipe.Type.INSTANCE))
            if (recipe.matchesSlots(itemHandler)) return recipe;
        return null;
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
        if (world.isClientSide || !(tile instanceof BloomeryFurnaceBlockEntity kiln)) return;
        kiln.tickKiln();
        ItemStack in = kiln.itemHandler.getStackInSlot(0);
        ItemStack out = kiln.itemHandler.getStackInSlot(1);
        if (!sameStack(in, kiln.lastInput) || !sameStack(out, kiln.lastOutput)
                || !sameStack(kiln.itemHandler.getStackInSlot(3),kiln.lastFlux)
                || !sameStack(kiln.itemHandler.getStackInSlot(2),kiln.lastSlag))
            kiln.sync();
    }

    private void tickKiln() {
        if (level == null || !formed) return;
        int heat=Math.max(0,Math.min(HEAT_CAPACITY,heatHandler.getHeatStored()));
        boolean changed=false, notify=false;
        // Burning charcoal continues burning even when output backs up.
        if (burnRemaining>0) {
            burnRemaining--;
            heat=Math.min(HEAT_CAPACITY,heat+HEAT_GAIN);
            changed=true;
        }
        int cooled=Math.max(0,heat-PASSIVE_LOSS);
        if (cooled!=heat) changed=true;
        heat=cooled;
        BloomeryRecipe recipe=findRecipe();
        if (recipe==null) {
            if (progress!=0 || activeRecipeId!=null) {
                progress=0;activeRecipeId=null;activeInput=ItemStack.EMPTY;
                activeFlux=ItemStack.EMPTY;airStep=0;changed=true;notify=true;
            }
        } else {
            ItemStack ore=itemHandler.getStackInSlot(BloomeryRecipe.ORE_SLOT);
            ItemStack flux=itemHandler.getStackInSlot(BloomeryRecipe.FLUX_SLOT);
            if (!recipe.getId().equals(activeRecipeId)
                    || !ItemStack.isSameItemSameTags(ore,activeInput)
                    || !ItemStack.isSameItemSameTags(flux,activeFlux)) {
                activeRecipeId=recipe.getId();
                activeInput=ore.copy();activeInput.setCount(1);
                activeFlux=flux.copy();activeFlux.setCount(1);
                progress=0;airStep=0;changed=true;notify=true;
            }
            ItemStack bloom=recipe.bloom(),slag=recipe.slag();
            // Two *guaranteed* outputs. Reserve both before starting new fuel.
            boolean outputsFit=itemHandler.insertItem(BloomeryRecipe.BLOOM_SLOT,bloom,true).isEmpty()
                    && itemHandler.insertItem(BloomeryRecipe.SLAG_SLOT,slag,true).isEmpty();
            if (outputsFit) {
                if (burnRemaining==0 && !fuel.isEmpty()) {
                    int time=fuelTime(fuel);
                    if (time>0) {
                        fuel.shrink(1);if (fuel.isEmpty())fuel=ItemStack.EMPTY;
                        burnRemaining=time;burnTotal=time;
                        changed=true;notify=true;
                    }
                }
                // A fresh unit of actual supplied air is required every
                // AIR_INTERVAL ticks. A stalled blower can only use the
                // finite air inventory; this is not a free power source.
                if (burnRemaining>0 && heat>=IGNITION_THRESHOLD+HEAT_COST) {
                    boolean available=airStep!=0 || pressureHandler.getPressureStored()>0;
                    if (available) {
                        if (airStep==0 && pressureHandler.extractPressure(1,false)!=1)
                            throw new IllegalStateException("Bloomery air reservation lost");
                        airStep=(airStep+1)%AIR_INTERVAL;
                        heat-=HEAT_COST;progress++;changed=true;
                        if (progress>=recipe.processTime()) {
                            if (!itemHandler.insertItem(BloomeryRecipe.BLOOM_SLOT,bloom,true).isEmpty()
                                    || !itemHandler.insertItem(BloomeryRecipe.SLAG_SLOT,slag,true).isEmpty())
                                throw new IllegalStateException("Bloomery output reservation lost");
                            ItemStack removedOre=itemHandler.extractItem(BloomeryRecipe.ORE_SLOT,recipe.oreCount(),false);
                            ItemStack removedFlux=itemHandler.extractItem(BloomeryRecipe.FLUX_SLOT,recipe.fluxCount(),false);
                            if (removedOre.getCount()!=recipe.oreCount()
                                    || removedFlux.getCount()!=recipe.fluxCount())
                                throw new IllegalStateException("Bloomery inputs changed during commit");
                            if (!itemHandler.insertItem(BloomeryRecipe.BLOOM_SLOT,bloom,false).isEmpty()
                                    || !itemHandler.insertItem(BloomeryRecipe.SLAG_SLOT,slag,false).isEmpty())
                                throw new IllegalStateException("Bloomery output changed during commit");
                            progress=0;airStep=0;activeRecipeId=null;
                            activeInput=ItemStack.EMPTY;activeFlux=ItemStack.EMPTY;
                            notify=true;
                        }
                    }
                }
            }
        }
        if (heatHandler.getHeatStored()!=heat) {heatHandler.setHeat(heat);changed=true;}
        if (changed) {
            setChanged();
            if (notify || level.getGameTime()%20L==0L)sync();
        }
    }

    @Override protected void interactableNoGui(BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide || !formed) return;
        ItemStack held=player.getItemInHand(hand);
        if (held.isEmpty() && player.isShiftKeyDown() && !fuel.isEmpty()) {
            ItemStack returned=fuel.copy();fuel=ItemStack.EMPTY;
            player.getInventory().add(returned);
            if (!returned.isEmpty())player.drop(returned,false);
            sync();return;
        }
        if (!held.isEmpty()) {
            boolean valid=held.is(Items.CHARCOAL)
                    && fuel.getCount()<FUEL_QUEUE_LIMIT
                    && (fuel.isEmpty() || ItemStack.isSameItemSameTags(fuel,held));
            if (valid) {
                if (fuel.isEmpty()) {fuel=held.copy();fuel.setCount(1);}
                else fuel.grow(1);
                if (!player.getAbilities().instabuild)held.shrink(1);
                sync();
            }
            player.displayClientMessage(Component.literal(valid
                    ? "Bloomery: charcoal added" : "Bloomery accepts only charcoal fuel"),true);
            return;
        }
        BloomeryRecipe recipe=findRecipe();
        player.displayClientMessage(Component.literal(
                "Bloomery: fuel "+fuel.getCount()+", burning "+burnRemaining
                +", HEAT "+heatHandler.getHeatStored()+"/"+HEAT_CAPACITY
                +", AIR "+pressureHandler.getPressureStored()+"/"+AIR_CAPACITY
                +", progress "+progress+"/"+(recipe==null?0:recipe.processTime())
                +", flux "+itemHandler.getStackInSlot(BloomeryRecipe.FLUX_SLOT).getCount()),true);
    }

    @Override public void invalidateCaps() { super.invalidateCaps();gasInput.invalidate(); }

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
        fuel = ItemStack.of(tag.getCompound("BloomeryFuel"));
        burnTotal = Math.max(0, tag.getInt("BloomeryBurnTotal"));
        burnRemaining = Math.max(0, Math.min(burnTotal, tag.getInt("BloomeryBurnRemaining")));
        progress = Math.max(0, tag.getInt("BloomeryProgress"));
        airStep=Math.max(0,Math.min(AIR_INTERVAL-1,tag.getInt("BloomeryAirStep")));
        activeFlux=ItemStack.of(tag.getCompound("BloomeryActiveFlux"));
        activeRecipeId = tag.contains("BloomeryRecipe")
                ? ResourceLocation.tryParse(tag.getString("BloomeryRecipe")) : null;
        activeInput = ItemStack.of(tag.getCompound("BloomeryActiveInput"));
        lastInput = itemHandler.getStackInSlot(0).copy();
        lastOutput = itemHandler.getStackInSlot(1).copy();
        lastFlux=itemHandler.getStackInSlot(3).copy();lastSlag=itemHandler.getStackInSlot(2).copy();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveMultiblockData(tag, blueprintName, formed, replacementModel);
        if (!voxelProfileName.isBlank()) tag.putString("v2_voxel_profile", voxelProfileName);
        MultiblockVoxelNbt.write(tag, "v2_voxel_collision", localVoxelCollision);
        MultiblockVoxelNbt.write(tag, "v2_voxel_selection", localVoxelSelection);
        tag.putInt("BloomeryBurnTotal", burnTotal);
        tag.putInt("BloomeryBurnRemaining", burnRemaining);
        tag.putInt("BloomeryProgress", progress);
        tag.putInt("BloomeryAirStep",airStep);
        if (!activeFlux.isEmpty())tag.put("BloomeryActiveFlux",activeFlux.save(new CompoundTag()));
        if (!fuel.isEmpty()) tag.put("BloomeryFuel", fuel.save(new CompoundTag()));
        if (activeRecipeId != null) tag.putString("BloomeryRecipe", activeRecipeId.toString());
        if (!activeInput.isEmpty()) tag.put("BloomeryActiveInput", activeInput.save(new CompoundTag()));
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
        lastFlux = itemHandler.getStackInSlot(3).copy();
        lastSlag = itemHandler.getStackInSlot(2).copy();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return getUpdateTag();
    }

    // Genuine input, primary and byproduct modules own item I/O. Exposing the
    // base controller-wide handler here would bypass the K1 input lock.
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return LazyOptional.empty();
        if (cap == CapabilityPressure.PRESSURE)
            return formed && side == matchedFacing
                    ? super.getCapability(cap,side) : LazyOptional.empty();
        if (cap == CapabilityPressurizedGas.GAS)
            return formed && side == matchedFacing ? gasInput.cast() : LazyOptional.empty();
        return super.getCapability(cap, side);
    }

    @Override public int capacityE() { return 0; }
    @Override public int maxtransferE() { return 0; }
    @Override public int capacityH() { return HEAT_CAPACITY; }
    @Override public int maxtransferH() { return 0; }
    @Override public int capacityW() { return 0; }
    @Override public int maxtransferW() { return 0; }
    @Override public int capacityF() { return 0; }
    @Override public int tanks() { return 0; }
    @Override public int invsize() { return 4; }
    @Override public int capacityP() { return AIR_CAPACITY; }
    @Override public int maxtransferP() { return 1; }

    @Override public boolean itemcape() { return true; }
    @Override public boolean energycape() { return false; }
    @Override public boolean heatcape() { return true; }
    @Override public boolean wattcape() { return false; }
    @Override public boolean fluidcape() { return false; }
    @Override public boolean pressurecape() { return true; }

    // K1 HEAT comes only from actual charcoal combustion; AIR is separate.
    @Override public boolean HeatCanReceive() { return false; }
    @Override public boolean HeatCanSend() { return false; }
    @Override public boolean WattCanReceive() { return false; }
    @Override public boolean WattCanSend() { return false; }
    @Override public boolean EnergyCanReceive() { return false; }
    @Override public boolean EnergyCanSend() { return false; }
    @Override public boolean PressureCanReceive() { return true; }
    @Override public boolean PressureCanSend() { return false; }

    @Override public Level getThisWorld() { return level; }
    @Override public BlockPos getThisPosition() { return worldPosition; }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(5.0D); }
    @Override public @NotNull ModelData getModelData() {
        return super.getModelData().derive()
                .with(MultiBlockProperties.MODEL_NAME, replacementModel).build();
    }
    @Override public Component getDisplayName() {
        return Component.translatable("block.magneticraft2.bloomery_furnace");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }
}
