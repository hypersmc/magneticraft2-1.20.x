package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalOreWasherBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.recipe.stage.copper.MechanicalOreWasherRecipe;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
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
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Controller logic for the JSON-defined 3x2x3 Ore Washer.
 */
public class MechanicalOreWasherBlockEntity
        extends BaseBlockEntityMagneticraft2 {

    private static final String MULTIBLOCK_PREFIX =
            "mechanical_ore_washer_";
    private static final float EPSILON = 0.01F;

    private String blueprintName = "";
    private String replacementModel = "";
    private boolean formed = false;

    private int processTime = 0;
    private int totalProcessTime = 160;
    private boolean processing = false;
    private boolean hasWater = false;

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
                findWaterSupply();

        if (newWater != hasWater) {
            hasWater = newWater;
            sync();
        }

        MechanicalOreWasherRecipe recipe =
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

        boolean canAttempt =
                hasWater
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
                1,
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

    private boolean findWaterSupply() {
        if (level == null) {
            return false;
        }

        Direction facing =
                getBlockState()
                        .getValue(
                                MechanicalOreWasherBlock.FACING
                        );
        Direction side =
                facing.getClockWise();

        BlockPos machineMiddle =
                worldPosition.relative(facing);

        // The trough is open on both sides. A source placed beside either side
        // of the centre section supplies the washer.
        for (Direction direction :
                new Direction[]{side, side.getOpposite()}) {
            BlockPos waterPos =
                    machineMiddle
                            .relative(direction, 2);

            if (level.getFluidState(waterPos)
                    .isSource()
                    && level.getFluidState(waterPos)
                    .is(
                            net.minecraft.tags.FluidTags.WATER
                    )) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private MechanicalOreWasherRecipe
    getMatchingRecipe() {
        if (level == null
                || itemHandler
                        .getStackInSlot(0)
                        .isEmpty()) {
            return null;
        }

        return level.getRecipeManager()
                .getRecipeFor(
                        MechanicalOreWasherRecipe
                                .Type.INSTANCE,
                        new SimpleContainer(
                                itemHandler
                                        .getStackInSlot(0)
                        ),
                        level
                )
                .orElse(null);
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
            level.setBlock(
                    worldPosition,
                    state.setValue(
                            MechanicalOreWasherBlock.IS_FORMED,
                            true
                    ),
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
        Direction facing =
                getBlockState().getValue(
                        MechanicalOreWasherBlock.FACING
                );
        String expectedName =
                MULTIBLOCK_PREFIX
                        + facing.getName();

        for (Multiblock multiblock :
                MultiblockRegistry
                        .getRegisteredMultiblocks()
                        .values()) {
            if (!multiblock.getName()
                    .equals(expectedName)) {
                continue;
            }

            MultiblockStructure structure =
                    multiblock.getStructure();

            if (matchesStructure(
                    world,
                    pos,
                    structure,
                    multiblock
            )) {
                blueprintName =
                        multiblock.getName();
                replacementModel =
                        multiblock.getSettings()
                                .getReplaceWhenFormed();
                return structure;
            }
        }

        return null;
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

            if (extracted.isEmpty()) {
                extracted =
                        itemHandler.extractItem(
                                0,
                                64,
                                false
                        );
            }

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
                level.getRecipeManager()
                        .getRecipeFor(
                                MechanicalOreWasherRecipe
                                        .Type.INSTANCE,
                                new SimpleContainer(single),
                                level
                        )
                        .isPresent();

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
                .inflate(3.0D);
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
