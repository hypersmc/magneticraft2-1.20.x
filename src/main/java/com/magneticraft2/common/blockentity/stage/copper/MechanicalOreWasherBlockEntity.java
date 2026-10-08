package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalOreWasherBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.recipe.multiblock.MultiblockRecipeHandler;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.FluidRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
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
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Controller logic for the JSON-defined 3x2x3 Ore Washer.
 */
public class MechanicalOreWasherBlockEntity
        extends BaseBlockEntityMagneticraft2 {

    private static final String MULTIBLOCK_PREFIX =
            "mechanical_ore_washer_";
    private static final ResourceLocation RECIPE_MACHINE =
            new ResourceLocation(
                    magneticraft2.MOD_ID,
                    "mechanical_ore_washer"
            );
    private static final float EPSILON = 0.01F;

    private String blueprintName = "";
    private String replacementModel = "";
    private boolean formed = false;
    private Direction matchedFacing = Direction.SOUTH;

    private int processTime = 0;
    private int totalProcessTime = 160;
    private boolean processing = false;
    private boolean hasWater = false;

    public static final int WATER_CAPACITY = 4000;
    public static final int DIRTY_WATER_CAPACITY = 4000;

    private final FluidTank waterTank =
            new FluidTank(
                    WATER_CAPACITY,
                    stack -> stack.getFluid() == Fluids.WATER
            ) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                    sync();
                }
            };

    private final FluidTank dirtyWaterTank =
            new FluidTank(
                    DIRTY_WATER_CAPACITY,
                    stack -> stack.getFluid()
                            == FluidRegistry.DIRTY_WATER.get()
            ) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                    sync();
                }
            };

    private LazyOptional<IFluidHandler> fluidCapability =
            LazyOptional.of(() -> new WasherFluidHandler());

    // Client-only visual accumulator for the internal leather drive belt.
    private double clientBeltTravelDistance = 0.0D;
    private double lastClientBeltVisualTime = Double.NaN;

    // Processing is synced only when state changes, not every tick. The client
    // predicts progress between those sync points for smooth one-way item travel.
    private double clientProcessSyncTime = Double.NaN;
    private float clientProcessSyncProgress = 0.0F;

    // The shared base ItemStackHandler only marks the block entity dirty when
    // automation changes a slot. These snapshots let the Washer detect those
    // external changes (especially Transfer Arm extraction) and push a visual
    // update to clients without syncing every tick.
    private ItemStack lastSyncedInput =
            ItemStack.EMPTY;
    private ItemStack lastSyncedOutput =
            ItemStack.EMPTY;
    private ItemStack lastSyncedByproduct =
            ItemStack.EMPTY;

    public MechanicalOreWasherBlockEntity(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .MECHANICAL_ORE_WASHER_BE
                        .get(),
                pos,
                state
        );
    }

    public boolean isFormed() {
        return formed;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public boolean isProcessing() {
        return processing;
    }

    public boolean hasWaterSupply() {
        return hasWater;
    }

    public int getWaterAmount() {
        return waterTank.getFluidAmount();
    }

    public int getWaterCapacity() {
        return waterTank.getCapacity();
    }

    public float getWaterFillRatio() {
        return waterTank.getCapacity() <= 0
                ? 0.0F
                : Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                waterTank.getFluidAmount()
                                        / (float) waterTank.getCapacity()
                        )
                );
    }

    public int getDirtyWaterAmount() {
        return dirtyWaterTank.getFluidAmount();
    }

    public int getDirtyWaterCapacity() {
        return dirtyWaterTank.getCapacity();
    }

    public ItemStack getInputStack() {
        return itemHandler.getStackInSlot(0);
    }

    public ItemStack getOutputStack() {
        return itemHandler.getStackInSlot(1);
    }

    public ItemStack getByproductStack() {
        return itemHandler.getStackInSlot(2);
    }

    public float getProcessProgress() {
        return totalProcessTime <= 0
                ? 0.0F
                : Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                processTime
                                        / (float) totalProcessTime
                        )
                );
    }

    public float getVisualProcessProgress(
            float partialTicks) {
        float base =
                getProcessProgress();

        if (!processing
                || level == null
                || !level.isClientSide) {
            return base;
        }

        double now =
                (double) level.getGameTime()
                        + (double) partialTicks;

        if (Double.isNaN(
                clientProcessSyncTime)) {
            clientProcessSyncTime = now;
            clientProcessSyncProgress = base;
        }

        double elapsed =
                Math.max(
                        0.0D,
                        now - clientProcessSyncTime
                );

        float predicted =
                (float) (
                        clientProcessSyncProgress
                                + elapsed
                                / Math.max(
                                        1.0D,
                                        (double) totalProcessTime
                                )
                );

        return Math.max(
                0.0F,
                Math.min(
                        1.0F,
                        predicted
                )
        );
    }


    public static <E extends BlockEntity> void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            E blockEntity) {
        if (level.isClientSide
                || !(blockEntity
                instanceof MechanicalOreWasherBlockEntity washer)) {
            return;
        }

        washer.tickProcessing();
        washer.syncInventoryVisualStateIfChanged();
    }

    private void syncInventoryVisualStateIfChanged() {
        if (level == null
                || level.isClientSide
                || itemHandler == null) {
            return;
        }

        ItemStack input =
                itemHandler.getStackInSlot(0);
        ItemStack output =
                itemHandler.getStackInSlot(1);
        ItemStack byproduct =
                itemHandler.getStackInSlot(2);

        if (sameVisualStack(
                input,
                lastSyncedInput
        )
                && sameVisualStack(
                output,
                lastSyncedOutput
        )
                && sameVisualStack(
                byproduct,
                lastSyncedByproduct
        )) {
            return;
        }

        sync();
    }

    private boolean sameVisualStack(
            ItemStack first,
            ItemStack second) {
        if (first.isEmpty()
                && second.isEmpty()) {
            return true;
        }

        return first.getCount()
                == second.getCount()
                && ItemStack.isSameItemSameTags(
                        first,
                        second
                );
    }

    private void captureInventoryVisualState() {
        if (itemHandler == null) {
            lastSyncedInput =
                    ItemStack.EMPTY;
            lastSyncedOutput =
                    ItemStack.EMPTY;
            lastSyncedByproduct =
                    ItemStack.EMPTY;
            return;
        }

        lastSyncedInput =
                itemHandler
                        .getStackInSlot(0)
                        .copy();
        lastSyncedOutput =
                itemHandler
                        .getStackInSlot(1)
                        .copy();
        lastSyncedByproduct =
                itemHandler
                        .getStackInSlot(2)
                        .copy();
    }

    private void tickProcessing() {
        if (level == null) {
            return;
        }

        if (!formed) {
            removeMechanicalLoad();
            setProcessing(false);
            return;
        }

        boolean newWater =
                waterTank.getFluidAmount() > 0;

        if (newWater != hasWater) {
            hasWater = newWater;
            sync();
        }

        MultiblockProcessingRecipe recipe =
                getMatchingRecipe();

        MechanicalInputModuleBlockEntity input =
                getMechanicalInput();

        if (recipe == null) {
            removeMechanicalLoad();
            processTime = 0;
            totalProcessTime = 160;
            setProcessing(false);
            return;
        }

        ItemStack output = recipe.getOutput();
        ItemStack byproduct = recipe.getByproduct();

        if (!canAccept(1, output)
                || (!byproduct.isEmpty()
                && !canAccept(2, byproduct))) {
            removeMechanicalLoad();
            setProcessing(false);
            return;
        }

        GearNode inputNode =
                input == null
                        ? null
                        : input.getOrCreateGearNode();

        FluidStack requiredFluid =
                recipe.getFluidInput();
        FluidStack producedFluid =
                recipe.getFluidOutput();

        boolean fluidInputReady =
                requiredFluid.isEmpty()
                        || (!waterTank.getFluid().isEmpty()
                        && waterTank.getFluid()
                        .isFluidEqual(requiredFluid)
                        && waterTank.getFluidAmount()
                        >= requiredFluid.getAmount());

        boolean fluidOutputReady =
                producedFluid.isEmpty()
                        || dirtyWaterTank.fill(
                                producedFluid,
                                IFluidHandler.FluidAction.SIMULATE
                        ) == producedFluid.getAmount();

        boolean canAttempt =
                fluidInputReady
                        && fluidOutputReady
                        && inputNode != null
                        && inputNode.getEffectiveSpeed()
                        >= recipe.getMinSpeed()
                        && inputNode.getTorque()
                        + EPSILON
                        >= recipe.getTorque();

        setMechanicalLoad(
                input,
                recipe.getTorque(),
                canAttempt
        );

        GearNetworkManager.MechanicalLoadState loadState =
                GearNetworkManager.getInstance()
                        .getMechanicalLoadState(
                                level,
                                worldPosition
                        );

        if (!canAttempt
                || !loadState.supplied()) {
            setProcessing(false);
            return;
        }

        totalProcessTime =
                recipe.getProcessTime();
        setProcessing(true);
        processTime++;
        setChanged();

        if (processTime
                < totalProcessTime) {
            return;
        }

        itemHandler.extractItem(
                0,
                recipe.getInputCount(),
                false
        );

        if (!requiredFluid.isEmpty()) {
            waterTank.drain(
                    requiredFluid,
                    IFluidHandler.FluidAction.EXECUTE
            );
        }

        if (!producedFluid.isEmpty()) {
            dirtyWaterTank.fill(
                    producedFluid,
                    IFluidHandler.FluidAction.EXECUTE
            );
        }

        insertOutput(1, output);

        if (!byproduct.isEmpty()
                && level.random.nextFloat()
                < recipe.getByproductChance()) {
            insertOutput(
                    2,
                    byproduct
            );
        }

        processTime = 0;
        setProcessing(false);
        sync();
    }

    @Nullable
    public MechanicalInputModuleBlockEntity
    getMechanicalInput() {
        if (level == null
                || getMultiblockController() == null) {
            return null;
        }

        BlockPos inputPos =
                getMultiblockController()
                        .getmodulePos(
                                "mechanical_input"
                        );

        if (inputPos == null) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(inputPos);

        return blockEntity
                instanceof MechanicalInputModuleBlockEntity input
                ? input
                : null;
    }

    public float getMechanicalVisualRotationDegrees(
            float partialTicks) {
        MechanicalInputModuleBlockEntity input =
                getMechanicalInput();

        return input == null
                ? 0.0F
                : input.getVisualRotationDegrees(
                        partialTicks
                );
    }

    public double getMechanicalVisualBeltTravelDistance(
            float partialTicks,
            double inputPulleyRadius) {
        MechanicalInputModuleBlockEntity input =
                getMechanicalInput();

        if (input == null || level == null) {
            return clientBeltTravelDistance;
        }

        double currentVisualTime =
                (double) level.getGameTime()
                        + (double) partialTicks;

        if (Double.isNaN(lastClientBeltVisualTime)) {
            lastClientBeltVisualTime = currentVisualTime;
            return clientBeltTravelDistance;
        }

        double deltaTicks =
                currentVisualTime - lastClientBeltVisualTime;
        lastClientBeltVisualTime = currentVisualTime;

        if (deltaTicks < 0.0F) {
            deltaTicks = 0.0F;
        } else if (deltaTicks > 20.0F) {
            deltaTicks = 20.0F;
        }

        float rpm =
                input.isClientOverloaded()
                        ? 0.0F
                        : input.getClientSpeed();

        if (Math.abs(rpm) > EPSILON) {
            double circumference =
                    Math.PI * 2.0D * inputPulleyRadius;
            double blocksPerTick =
                    (rpm / 1200.0D) * circumference;

            clientBeltTravelDistance -=
                    blocksPerTick
                            * deltaTicks
                            * input.getDirectionMultiplier();

            // Keep this accumulator genuinely continuous. It now drives both
            // the tiled belt texture and the driven pulley/drum angle, so a
            // texture-repeat modulo would create a mechanical phase jump.
        }

        return clientBeltTravelDistance;
    }

    private void setMechanicalLoad(
            @Nullable MechanicalInputModuleBlockEntity input,
            float torque,
            boolean active) {
        if (level == null) {
            return;
        }

        GearNetworkManager.getInstance()
                .setMechanicalLoad(
                        level,
                        worldPosition,
                        input == null
                                ? worldPosition
                                : input.getBlockPos(),
                        Math.max(0.0F, torque),
                        active && input != null
                );
    }

    private void removeMechanicalLoad() {
        if (level != null) {
            GearNetworkManager.getInstance()
                    .removeMechanicalLoad(
                            level,
                            worldPosition
                    );
        }
    }

    @Nullable
    private MultiblockProcessingRecipe
    getMatchingRecipe() {
        return MultiblockRecipeHandler.findRecipe(
                level,
                RECIPE_MACHINE,
                itemHandler.getStackInSlot(0)
        );
    }

    private boolean canAccept(
            int slot,
            ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        ItemStack current =
                itemHandler.getStackInSlot(slot);

        if (current.isEmpty()) {
            return stack.getCount()
                    <= itemHandler
                    .getSlotLimit(slot);
        }

        if (!ItemStack
                .isSameItemSameTags(
                        current,
                        stack
                )) {
            return false;
        }

        int limit =
                Math.min(
                        itemHandler.getSlotLimit(slot),
                        current.getMaxStackSize()
                );

        return current.getCount()
                + stack.getCount()
                <= limit;
    }

    private void insertOutput(
            int slot,
            ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack current =
                itemHandler.getStackInSlot(slot);

        if (current.isEmpty()) {
            itemHandler.setStackInSlot(
                    slot,
                    stack.copy()
            );
            return;
        }

        ItemStack grown =
                current.copy();
        grown.grow(stack.getCount());
        itemHandler.setStackInSlot(
                slot,
                grown
        );
    }

    @Override
    protected MultiblockController
    createMultiblockController() {
        MultiblockStructure structure =
                identifyMultiblockStructure(
                        level,
                        worldPosition
                );

        if (structure == null) {
            return null;
        }

        MultiblockController controller =
                new MultiblockController(structure);

        controller.identifyAndAddModules(
                level,
                worldPosition,
                structure
        );

        if (!controller.createStructure(
                level,
                worldPosition
        )) {
            return null;
        }

        controller.setFormed(true);
        setMultiblockController(controller);
        formed = true;

        BlockState state =
                level.getBlockState(
                        worldPosition
                );

        if (state.hasProperty(
                MechanicalOreWasherBlock.IS_FORMED
        )) {
            BlockState formedState =
                    state.setValue(
                            MechanicalOreWasherBlock.IS_FORMED,
                            true
                    );

            if (formedState.hasProperty(
                    MechanicalOreWasherBlock.FACING
            )) {
                formedState =
                        formedState.setValue(
                                MechanicalOreWasherBlock.FACING,
                                matchedFacing
                        );
            }

            level.setBlock(
                    worldPosition,
                    formedState,
                    Block.UPDATE_ALL
            );
        }

        requestModelDataUpdate();
        sync();
        return controller;
    }

    @Override
    protected MultiblockStructure
    identifyMultiblockStructure(
            Level world,
            BlockPos pos) {
        // Match the physical structure first, exactly like the older
        // Magneticraft multiblocks do. The controller's placement-facing must
        // not decide which layout is legal; the built structure decides that.
        //
        // This is important for Patchouli/Visualize: a player can rotate the
        // projected structure independently of how the controller block happened
        // to be facing when it was placed.
        for (Multiblock multiblock :
                MultiblockRegistry
                        .getRegisteredMultiblocks()
                        .values()) {
            if (!multiblock.getName()
                    .startsWith(
                            MULTIBLOCK_PREFIX
                    )) {
                continue;
            }

            MultiblockStructure structure =
                    multiblock.getStructure();

            if (!matchesStructure(
                    world,
                    pos,
                    structure,
                    multiblock
            )) {
                continue;
            }

            blueprintName =
                    multiblock.getName();
            replacementModel =
                    multiblock.getSettings()
                            .getReplaceWhenFormed();
            matchedFacing =
                    facingFromMultiblockName(
                            blueprintName
                    );

            return structure;
        }

        return null;
    }

    private Direction facingFromMultiblockName(
            String name) {
        if (name.endsWith("_north")) {
            return Direction.NORTH;
        }
        if (name.endsWith("_east")) {
            return Direction.EAST;
        }
        if (name.endsWith("_west")) {
            return Direction.WEST;
        }
        if (name.endsWith("_south")) {
            return Direction.SOUTH;
        }

        Direction current =
                getBlockState().getValue(
                        MechanicalOreWasherBlock.FACING
                );

        return current.getAxis().isHorizontal()
                ? current
                : Direction.SOUTH;
    }

    @Override
    protected void interactableNoGui(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        ItemStack held =
                player.getItemInHand(hand);

        if (held.isEmpty()) {
            ItemStack extracted =
                    itemHandler.extractItem(
                            1,
                            64,
                            false
                    );

            if (extracted.isEmpty()) {
                extracted =
                        itemHandler.extractItem(
                                2,
                                64,
                                false
                        );
            }

            // Input is intentionally not a fallback here. The formed
            // machine has a dedicated Item Input module for slot 0; keeping
            // controller/output interaction restricted to slots 1/2 prevents
            // the output side from unexpectedly pulling queued raw material.
            if (!extracted.isEmpty()
                    && !player.getInventory()
                    .add(extracted)) {
                player.drop(
                        extracted,
                        false
                );
            }

            sync();
            return;
        }

        ItemStack single = held.copy();
        single.setCount(1);

        boolean valid =
                MultiblockRecipeHandler.acceptsInput(
                        level,
                        RECIPE_MACHINE,
                        single
                );

        if (!valid) {
            return;
        }

        ItemStack remainder =
                itemHandler.insertItem(
                        0,
                        single,
                        false
                );

        if (remainder.isEmpty()) {
            if (!player.getAbilities()
                    .instabuild) {
                held.shrink(1);
            }

            sync();
        }
    }

    private void setProcessing(
            boolean value) {
        if (processing == value) {
            return;
        }

        processing = value;
        sync();
    }

    @Override
    public void onDestroy(Level level) {
        removeMechanicalLoad();
        super.onDestroy(level);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        processTime =
                tag.getInt("ProcessTime");
        totalProcessTime =
                tag.contains("TotalProcessTime")
                        ? Math.max(
                                1,
                                tag.getInt(
                                        "TotalProcessTime"
                                )
                        )
                        : 160;
        processing =
                tag.getBoolean("Processing");
        hasWater =
                tag.getBoolean("HasWater");

        if (level != null
                && level.isClientSide) {
            if (processing) {
                clientProcessSyncTime =
                        level.getGameTime();
                clientProcessSyncProgress =
                        getProcessProgress();
            } else {
                clientProcessSyncTime =
                        Double.NaN;
                clientProcessSyncProgress =
                        getProcessProgress();
            }
        }

        if (tag.contains("WaterTank")) {
            waterTank.readFromNBT(
                    tag.getCompound("WaterTank")
            );
            hasWater =
                    waterTank.getFluidAmount() > 0;
        }

        if (tag.contains("DirtyWaterTank")) {
            dirtyWaterTank.readFromNBT(
                    tag.getCompound("DirtyWaterTank")
            );
        }

        MultiblockPersistentData data =
                loadMultiblockData(tag);
        blueprintName =
                data.blueprintName();
        formed =
                data.formed();
        replacementModel =
                data.replacementModel();
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putInt(
                "ProcessTime",
                processTime
        );
        tag.putInt(
                "TotalProcessTime",
                totalProcessTime
        );
        tag.putBoolean(
                "Processing",
                processing
        );
        tag.putBoolean(
                "HasWater",
                hasWater
        );
        tag.put(
                "WaterTank",
                waterTank.writeToNBT(
                        new CompoundTag()
                )
        );
        tag.put(
                "DirtyWaterTank",
                dirtyWaterTank.writeToNBT(
                        new CompoundTag()
                )
        );

        saveMultiblockData(
                tag,
                blueprintName,
                formed,
                replacementModel
        );
    }

    private final class WasherFluidHandler
            implements IFluidHandler {

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return waterTank.getFluid();
            }
            if (tank == 1) {
                return dirtyWaterTank.getFluid();
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) {
                return waterTank.getCapacity();
            }
            if (tank == 1) {
                return dirtyWaterTank.getCapacity();
            }
            return 0;
        }

        @Override
        public boolean isFluidValid(
                int tank,
                @NotNull FluidStack stack) {
            return tank == 0
                    && waterTank.isFluidValid(stack);
        }

        @Override
        public int fill(
                FluidStack resource,
                FluidAction action) {
            return waterTank.fill(
                    resource,
                    action
            );
        }

        @Override
        public @NotNull FluidStack drain(
                FluidStack resource,
                FluidAction action) {
            return dirtyWaterTank.drain(
                    resource,
                    action
            );
        }

        @Override
        public @NotNull FluidStack drain(
                int maxDrain,
                FluidAction action) {
            return dirtyWaterTank.drain(
                    maxDrain,
                    action
            );
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCapability.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluidCapability =
                LazyOptional.of(() -> new WasherFluidHandler());
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener>
    getUpdatePacket() {
        return ClientboundBlockEntityDataPacket
                .create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag =
                new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void onDataPacket(
            Connection net,
            ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag =
                packet.getTag();

        if (tag != null) {
            load(tag);
        }
    }

    @Override
    public CompoundTag sync() {
        setChanged();
        captureInventoryVisualState();

        if (level != null) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_ALL
            );
        }

        return getUpdateTag();
    }

    @Override
    public int capacityE() { return 0; }
    @Override
    public int maxtransferE() { return 0; }
    @Override
    public int capacityH() { return 0; }
    @Override
    public int maxtransferH() { return 0; }
    @Override
    public int capacityW() { return 0; }
    @Override
    public int maxtransferW() { return 0; }
    @Override
    public int capacityF() { return 0; }
    @Override
    public int tanks() { return 0; }
    @Override
    public int invsize() { return 3; }
    @Override
    public int capacityP() { return 0; }
    @Override
    public int maxtransferP() { return 0; }

    @Override
    public boolean itemcape() { return true; }
    @Override
    public boolean energycape() { return false; }
    @Override
    public boolean heatcape() { return false; }
    @Override
    public boolean wattcape() { return false; }
    @Override
    public boolean fluidcape() { return false; }
    @Override
    public boolean pressurecape() { return false; }

    @Override
    public boolean HeatCanReceive() { return false; }
    @Override
    public boolean HeatCanSend() { return false; }
    @Override
    public boolean WattCanReceive() { return false; }
    @Override
    public boolean WattCanSend() { return false; }
    @Override
    public boolean EnergyCanReceive() { return false; }
    @Override
    public boolean EnergyCanSend() { return false; }
    @Override
    public boolean PressureCanReceive() { return false; }
    @Override
    public boolean PressureCanSend() { return false; }

    @Override
    public Level getThisWorld() {
        return level;
    }

    @Override
    public BlockPos getThisPosition() {
        return worldPosition;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition)
                .inflate(2.0D);
    }

    @Override
    public @NotNull ModelData getModelData() {
        return super.getModelData()
                .derive()
                .with(
                        MultiBlockProperties.MODEL_NAME,
                        replacementModel
                )
                .build();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.magneticraft2.mechanical_ore_washer"
        );
    }

    @Override
    public @Nullable AbstractContainerMenu
    createMenu(
            int id,
            Inventory inventory,
            Player player) {
        return null;
    }
}
