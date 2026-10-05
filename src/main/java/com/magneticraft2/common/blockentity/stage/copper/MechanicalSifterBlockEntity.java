package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalSifterBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.recipe.multiblock.MultiblockRecipeHandler;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JSON-multiblock controller for the crank-driven Mechanical Sifter.
 */
public class MechanicalSifterBlockEntity
        extends BaseBlockEntityMagneticraft2 {

    private static final String MULTIBLOCK_PREFIX =
            "mechanical_sifter_";
    private static final ResourceLocation RECIPE_MACHINE =
            new ResourceLocation(
                    magneticraft2.MOD_ID,
                    "mechanical_sifter"
            );
    private static final float EPSILON = 0.01F;

    private String blueprintName = "";
    private String replacementModel = "";
    private boolean formed = false;
    private Direction matchedFacing = Direction.SOUTH;

    private int processTime = 0;
    private int totalProcessTime = 160;
    private boolean processing = false;

    // Processing state is not network-synced every tick. Predict the visual
    // progress client-side between start/stop sync points, matching the Washer.
    private float clientProcessSyncTime = Float.NaN;
    private float clientProcessSyncProgress = 0.0F;

    private ItemStack lastSyncedInput =
            ItemStack.EMPTY;
    private ItemStack lastSyncedOutput =
            ItemStack.EMPTY;
    private ItemStack lastSyncedByproduct =
            ItemStack.EMPTY;

    public MechanicalSifterBlockEntity(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .MECHANICAL_SIFTER_BE
                        .get(),
                pos,
                state
        );
    }

    public boolean isFormed() {
        return formed;
    }

    public boolean isProcessing() {
        return processing;
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

        float now =
                level.getGameTime()
                        + partialTicks;

        if (Float.isNaN(
                clientProcessSyncTime)) {
            clientProcessSyncTime = now;
            clientProcessSyncProgress = base;
        }

        float elapsed =
                Math.max(
                        0.0F,
                        now - clientProcessSyncTime
                );

        float predicted =
                clientProcessSyncProgress
                        + elapsed
                        / Math.max(
                                1.0F,
                                totalProcessTime
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
                instanceof MechanicalSifterBlockEntity sifter)) {
            return;
        }

        sifter.tickProcessing();
        sifter.syncInventoryVisualStateIfChanged();
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

        MultiblockProcessingRecipe recipe =
                getMatchingRecipe();
        CrankBlockEntity_wood crank =
                getConnectedCrank();

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

        boolean canAttempt =
                crank != null
                        && crank.getOrCreateGearNode()
                        .getEffectiveSpeed()
                        >= recipe.getMinSpeed()
                        && crank.getOrCreateGearNode()
                        .getTorque()
                        + EPSILON
                        >= recipe.getTorque();

        setMechanicalLoad(
                crank,
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

    private void setMechanicalLoad(
            @Nullable CrankBlockEntity_wood crank,
            float torque,
            boolean active) {
        if (level == null) {
            return;
        }

        GearNetworkManager.getInstance()
                .setMechanicalLoad(
                        level,
                        worldPosition,
                        crank == null
                                ? worldPosition
                                : crank.getBlockPos(),
                        Math.max(0.0F, torque),
                        active && crank != null
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
    public CrankBlockEntity_wood
    getConnectedCrank() {
        if (level == null || !formed) {
            return null;
        }

        Direction facing =
                getFacing();

        BlockPos crankPos =
                worldPosition.relative(
                        facing.getOpposite()
                );

        BlockEntity blockEntity =
                level.getBlockEntity(crankPos);

        if (blockEntity
                instanceof CrankBlockEntity_wood crank
                && crank.getRodOutputPos()
                .equals(worldPosition)) {
            return crank;
        }

        return null;
    }

    public Direction getFacing() {
        BlockState state = getBlockState();

        return state.hasProperty(
                MechanicalSifterBlock.FACING
        )
                ? state.getValue(
                        MechanicalSifterBlock.FACING
                )
                : Direction.SOUTH;
    }

    public float getShakeOffset(
            float partialTicks) {
        CrankBlockEntity_wood crank =
                getConnectedCrank();

        return crank == null
                ? 0.0F
                : (crank.getStrokeProgress(
                        partialTicks
                ) - 0.5F) * 0.24F;
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
                MechanicalSifterBlock.IS_FORMED
        )) {
            BlockState formedState =
                    state.setValue(
                            MechanicalSifterBlock.IS_FORMED,
                            true
                    );

            if (formedState.hasProperty(
                    MechanicalSifterBlock.FACING
            )) {
                formedState =
                        formedState.setValue(
                                MechanicalSifterBlock.FACING,
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
                        MechanicalSifterBlock.FACING
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

            // Slot 0 belongs to the dedicated Item Input module. Do
            // not let controller/output interaction pull raw feed material.
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

        if (level != null
                && level.isClientSide) {
            if (processing) {
                clientProcessSyncTime =
                        level.getGameTime();
                clientProcessSyncProgress =
                        getProcessProgress();
            } else {
                clientProcessSyncTime =
                        Float.NaN;
                clientProcessSyncProgress =
                        getProcessProgress();
            }
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

        saveMultiblockData(
                tag,
                blueprintName,
                formed,
                replacementModel
        );
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
                "block.magneticraft2.mechanical_sifter"
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
