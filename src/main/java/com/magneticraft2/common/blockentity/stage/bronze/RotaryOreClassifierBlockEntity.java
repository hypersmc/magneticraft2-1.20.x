package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.RotaryOreClassifierBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalInputModuleBlockEntity;
import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.recipe.multiblock.MultiblockRecipeHandler;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.systems.maintenance.ReplaceableMachinePart;
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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Compact JSON-formed Rotary Ore Classifier with Gear V2 power, two conserved output fractions and a serviceable rotating screen.
 */
public class RotaryOreClassifierBlockEntity
        extends BaseBlockEntityMagneticraft2 implements MultiblockVoxelHost {

    private static final String MULTIBLOCK_PREFIX =
            "rotary_ore_classifier_";
    private static final ResourceLocation RECIPE_MACHINE =
            new ResourceLocation(
                    magneticraft2.MOD_ID,
                    "rotary_ore_classifier"
            );
    private static final float EPSILON = 0.01F;

    private String blueprintName = "";
    private String replacementModel = "";
    private boolean formed = false;
    private Direction matchedFacing = Direction.SOUTH;
    private String voxelProfileName = "";
    private java.util.List<BlockPos> renderAirAnchors=java.util.List.of();
    @Nullable private net.minecraft.world.phys.shapes.VoxelShape collision;
    @Nullable private net.minecraft.world.phys.shapes.VoxelShape selection;
    @Override public void setMultiblockVoxelShapes(
            net.minecraft.world.phys.shapes.VoxelShape collision,
            net.minecraft.world.phys.shapes.VoxelShape selection) {
        this.collision=collision;this.selection=selection;sync();
    }
    @Nullable public net.minecraft.world.phys.shapes.VoxelShape getLocalVoxelCollision(){return collision;}
    @Nullable public net.minecraft.world.phys.shapes.VoxelShape getLocalVoxelSelection(){return selection!=null?selection:collision;}


    private int processTime = 0;
    private int totalProcessTime = 180;
    private boolean processing = false;
    // A batch chooses its optional byproduct once. Never reroll on a stall.
    @Nullable
    private ResourceLocation activeRecipeId;
    private boolean pendingByproduct = false;
    private final ReplaceableMachinePart classifierScreen =
            new ReplaceableMachinePart(
                    ItemRegistry.ITEM_CLASSIFIER_SCREEN::get,
                    384
            );

    // Processing state is not network-synced every tick. Predict the visual
    // progress client-side between start/stop sync points, matching the Washer.
    private double clientProcessSyncTime = Double.NaN;
    private float clientProcessSyncProgress = 0.0F;
    // Client-only display cache. Do not scan every datapack recipe on every
    // frame merely to decide whether a queued stack remains on the input.
    @Nullable private ResourceLocation visualCountRecipe;
    private int visualCountForRecipe = 1;

    private ItemStack lastSyncedInput =
            ItemStack.EMPTY;
    private ItemStack lastSyncedOutput =
            ItemStack.EMPTY;
    private ItemStack lastSyncedByproduct =
            ItemStack.EMPTY;

    public RotaryOreClassifierBlockEntity(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .ROTARY_ORE_CLASSIFIER_BE
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
    
    /**
     * Visual-only state. Stalled batches still have ore inside the screen:
     * do not teleport their workpieces back to the input when power or an
     * output is blocked. The server alone owns and changes the inventory.
     */
    public boolean hasVisibleWorkpiece() {
        return formed
                && activeRecipeId != null
                && processTime > 0
                && !getInputStack().isEmpty();
    }

    public boolean needsMaintenance() {
        return classifierScreen.needsAttention();
    }

    public boolean isPartWornOut() {
        return !classifierScreen.canOperate();
    }

    public int getPartWearStage() {
        return classifierScreen.getVisualWearStage();
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

    /**
     * Used by the physical Item Input module (manual and automated).
     * Invalid material must not occupy the one processing input slot.
     */
    public boolean acceptsRecipeInput(ItemStack stack) {
        return MultiblockRecipeHandler.acceptsInput(level, RECIPE_MACHINE, stack);
    }

    public int getActiveInputCount() {
        MultiblockProcessingRecipe recipe = getMatchingRecipe();
        return recipe == null ? 1 : recipe.getInputCount();
    }

    /** One recipe lookup per active batch type, not one per rendered frame. */
    public int getVisualBatchInputCount() {
        if (activeRecipeId == null) {
            return 1;
        }
        if (!activeRecipeId.equals(visualCountRecipe)) {
            visualCountRecipe = activeRecipeId;
            visualCountForRecipe = getActiveInputCount();
        }
        return visualCountForRecipe;
    }

    public ItemStack getProcessingPrimaryPreview() {
        MultiblockProcessingRecipe recipe = getMatchingRecipe();
        return recipe == null ? ItemStack.EMPTY : recipe.getOutput();
    }

    public ItemStack getProcessingByproductPreview() {
        if (!pendingByproduct) {
            return ItemStack.EMPTY;
        }

        MultiblockProcessingRecipe recipe = getMatchingRecipe();
        return recipe == null ? ItemStack.EMPTY : recipe.getByproduct();
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

    /**
     * Lightweight effects for a DRY, mechanically-powered sieve.
     * Runs only on the client and at most once per ten ticks per machine.
     * No particles/sounds when idle, stalled, unformed or too far away.
     */
    public static <E extends BlockEntity> void clientTick(
            Level level,
            BlockPos pos,
            BlockState state,
            E blockEntity) {
        if (!level.isClientSide
                || !(blockEntity instanceof RotaryOreClassifierBlockEntity classifier)
                || !classifier.formed
                || !classifier.processing
                || classifier.getInputStack().isEmpty()) {
            return;
        }

        long clock = level.getGameTime();
        int stagger = Math.floorMod(pos.hashCode(), 40);
        boolean particleTick = (clock + stagger) % 10L == 0L;
        boolean soundTick = (clock + stagger) % 40L == 0L;
        if ((!particleTick && !soundTick)
                || level.getNearestPlayer(
                    pos.getX() + 0.5D, pos.getY() + 1.5D,
                    pos.getZ() + 0.5D, 18.0D, false) == null) {
            return;
        }

        float progress = classifier.getVisualProcessProgress(0.0F);
        if (progress < 0.18F || progress > 0.96F) {
            return;
        }

        if (particleTick) {
            // Fine chips pass through the lower portion of the rotating mesh
            // into the stationary collection pan. No smoke/sparks/water.
            double localX = 0.5D + (level.random.nextDouble() - 0.5D) * 0.36D;
            double localZ = -1.65D + progress * 2.05D
                    + (level.random.nextDouble() - 0.5D) * 0.35D;
            double[] worldXZ = classifier.worldParticleXZ(localX, localZ);
            if (level.random.nextInt(3) == 0) {
                level.addParticle(new BlockParticleOption(
                                ParticleTypes.FALLING_DUST, Blocks.GRAVEL.defaultBlockState()),
                        worldXZ[0], pos.getY() + 0.97D, worldXZ[1],
                        0.0D, -0.012D, 0.0D);
            } else {
                level.addParticle(new DustParticleOptions(
                                new Vector3f(0.55F, 0.47F, 0.36F), 0.55F),
                        worldXZ[0], pos.getY() + 0.89D, worldXZ[1],
                        (level.random.nextDouble() - 0.5D) * 0.012D,
                        -0.012D, (level.random.nextDouble() - 0.5D) * 0.012D);
            }
        }

        if (soundTick) {
            level.playLocalSound(
                    pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                    SoundEvents.GRAVEL_STEP, SoundSource.BLOCKS,
                    0.15F, 0.86F + level.random.nextFloat() * 0.10F, false);
        }
    }

    /**
     * Matches the controller-centred BER orientation, including all four
     * rotated JSON formation layouts. Coordinates use the local SOUTH
     * model's block-corner origin, not the centre of an AIR filler block.
     */
    private double[] worldParticleXZ(double x, double z) {
        return switch (getFacing()) {
            case NORTH -> new double[]{
                    worldPosition.getX() + 1.0D - x,
                    worldPosition.getZ() + 1.0D - z};
            case WEST -> new double[]{
                    worldPosition.getX() + 1.0D - z,
                    worldPosition.getZ() + x};
            case EAST -> new double[]{
                    worldPosition.getX() + z,
                    worldPosition.getZ() + 1.0D - x};
            default -> new double[]{
                    worldPosition.getX() + x,
                    worldPosition.getZ() + z};
        };
    }

    public static <E extends BlockEntity> void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            E blockEntity) {
        if (level.isClientSide
                || !(blockEntity
                instanceof RotaryOreClassifierBlockEntity mill)) {
            return;
        }

        mill.tickProcessing();
        mill.syncInventoryVisualStateIfChanged();
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
            clearBatch();
            removeMechanicalLoad();
            setProcessing(false);
            return;
        }

        MultiblockProcessingRecipe recipe = getMatchingRecipe();
        MechanicalInputModuleBlockEntity input = getMechanicalInput();

        if (!classifierScreen.canOperate()) {
            removeMechanicalLoad();
            setProcessing(false);
            return;
        }

        if (recipe == null) {
            clearBatch();
            removeMechanicalLoad();
            setProcessing(false);
            return;
        }

        // Changing the recipe or returning after a completed batch starts a
        // new one. A stalled batch keeps its progress and result selection.
        if (!recipe.getId().equals(activeRecipeId)) {
            activeRecipeId = recipe.getId();
            processTime = 0;
            totalProcessTime = recipe.getProcessTime();
            ItemStack possibleByproduct = recipe.getByproduct();
            pendingByproduct = !possibleByproduct.isEmpty()
                    && level.random.nextFloat() < recipe.getByproductChance();
            setChanged();
        }

        ItemStack output = recipe.getOutput();
        ItemStack byproduct = recipe.getByproduct();

        // Only reserve the secondary port if THIS batch will use it.
        // A full byproduct slot must not block batches producing no waste.
        if (!canAccept(1, output)
                || (pendingByproduct && !canAccept(2, byproduct))) {
            removeMechanicalLoad();
            setProcessing(false);
            return;
        }

        GearNode inputNode = input == null
                ? null
                : input.getOrCreateGearNode();

        boolean canAttempt = inputNode != null
                && inputNode.getEffectiveSpeed() >= recipe.getMinSpeed()
                && inputNode.getTorque() + EPSILON >= recipe.getTorque();

        setMechanicalLoad(input, recipe.getTorque(), canAttempt);

        GearNetworkManager.MechanicalLoadState loadState =
                GearNetworkManager.getInstance()
                        .getMechanicalLoadState(level, worldPosition);

        if (!canAttempt || !loadState.supplied()) {
            setProcessing(false);
            return;
        }

        setProcessing(true);
        processTime++;
        setChanged();

        if (processTime < totalProcessTime) {
            return;
        }

        // The output slots have already been checked for capacity; perform
        // the recipe exactly once, with no rerolls or item loss on overflow.
        itemHandler.extractItem(0, recipe.getInputCount(), false);
        insertOutput(1, output);
        if (pendingByproduct) {
            insertOutput(2, byproduct);
        }

        classifierScreen.completeBatch();
        activeRecipeId = null;
        pendingByproduct = false;
        processTime = 0;
        processing = false;
        sync();
    }

    private void clearBatch() {
        if (processTime == 0 && activeRecipeId == null
                && !pendingByproduct) {
            return;
        }

        processTime = 0;
        totalProcessTime = 180;
        activeRecipeId = null;
        pendingByproduct = false;
        setChanged();
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
    public MechanicalInputModuleBlockEntity
    getMechanicalInput() {
        if (level == null
                || !formed) {
            return null;
        }

        BlockPos inputPos = null;

        if (getMultiblockController() != null) {
            inputPos =
                    getMultiblockController()
                            .getmodulePos(
                                    "mechanical_input"
                            );
        }

        // Elevated rear-center Gear V2 module, three cells behind and one up.
        if (inputPos == null) {
            inputPos = worldPosition.relative(getFacing().getOpposite(), 3).above();
        }

        BlockEntity blockEntity =
                level.getBlockEntity(inputPos);

        return blockEntity
                instanceof MechanicalInputModuleBlockEntity input
                ? input
                : null;
    }

    public Direction getFacing() {
        BlockState state = getBlockState();

        return state.hasProperty(
                RotaryOreClassifierBlock.FACING
        )
                ? state.getValue(
                        RotaryOreClassifierBlock.FACING
                )
                : Direction.SOUTH;
    }

    public float getDriveRotationDegrees(
            float partialTicks) {
        MechanicalInputModuleBlockEntity input =
                getMechanicalInput();

        return input == null
                ? 0.0F
                : input.getVisualRotationDegrees(
                        partialTicks
                );
    }

    /** Shared Gear V2 phase drives the real screen drum; no fabricated spin when stopped. */
    public float getDrumRotationDegrees(float partialTicks) {
        Direction facing = getFacing();
        float sign = facing == Direction.NORTH || facing == Direction.WEST ? -1F : 1F;
        return sign * getDriveRotationDegrees(partialTicks);
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

        if (structure == null || !MultiblockVoxelProfiles.exists(voxelProfileName)) {
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
                RotaryOreClassifierBlock.IS_FORMED
        )) {
            BlockState formedState =
                    state.setValue(
                            RotaryOreClassifierBlock.IS_FORMED,
                            true
                    );

            if (formedState.hasProperty(
                    RotaryOreClassifierBlock.FACING
            )) {
                formedState =
                        formedState.setValue(
                                RotaryOreClassifierBlock.FACING,
                                matchedFacing
                        );
            }

            level.setBlock(
                    worldPosition,
                    formedState,
                    Block.UPDATE_ALL
            );
        }

        MultiblockRenderParts.assign(level,worldPosition,controller,
                "rotary_ore_classifier",matchedFacing,renderAirAnchors,voxelProfileName);
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
            voxelProfileName=multiblock.getSettings().getVoxelProfile();
            renderAirAnchors=multiblock.getSettings().getRenderAirAnchors();

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
                        RotaryOreClassifierBlock.FACING
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
        // The classifier screen is a visible, replaceable part separate from item I/O.
        if (classifierScreen.handleInteraction(player, hand, () -> sync())) {
            return;
        }

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
                        : 180;
        processing =
                tag.getBoolean("Processing");
        activeRecipeId = tag.contains("ActiveRecipe")
                ? ResourceLocation.tryParse(tag.getString("ActiveRecipe"))
                : null;
        pendingByproduct = tag.getBoolean("PendingByproduct");
        classifierScreen.load(tag.getCompound("Maintenance"));

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

        MultiblockPersistentData data =
                loadMultiblockData(tag);
        blueprintName =
                data.blueprintName();
        formed =
                data.formed();
        replacementModel =
                data.replacementModel();
        voxelProfileName=tag.getString("v2_voxel_profile");
        collision=MultiblockVoxelNbt.read(tag,"v2_voxel_collision");
        selection=MultiblockVoxelNbt.read(tag,"v2_voxel_selection");
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
        if (activeRecipeId != null) {
            tag.putString("ActiveRecipe", activeRecipeId.toString());
        }
        tag.putBoolean("PendingByproduct", pendingByproduct);
        tag.put("Maintenance", classifierScreen.save());
        if(!voxelProfileName.isBlank())tag.putString("v2_voxel_profile",voxelProfileName);
        MultiblockVoxelNbt.write(tag,"v2_voxel_collision",collision);
        MultiblockVoxelNbt.write(tag,"v2_voxel_selection",selection);

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
        return new AABB(worldPosition).inflate(4.0D);
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
                "block.magneticraft2.rotary_ore_classifier"
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
