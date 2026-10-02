package com.magneticraft2.common.blockentity.stage.stone;

import com.magneticraft2.common.block.stage.stone.PrimitiveGrinderBMultiblock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.recipe.stage.stone.primitive_grinder_multiblockrecipe;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrimitiveGrinderBMultiblockEntity extends BaseBlockEntityMagneticraft2 {
    public static final float MIN_MECHANICAL_SPEED = 20.0F;
    public static final float REQUIRED_TORQUE = 4.0F;

    private String blueprintname = "";
    private boolean formed = false;
    private String repacementmodel = "";
    private MultiblockController controller;

    private int crushtime = 0;
    private boolean crushing = false;
    private int totalCrushTime = 200;
    private float mechanicalSpeed = 0.0F;
    private float mechanicalTorque = 0.0F;
    private float mechanicalRotationDegrees = 0.0F;
    private int mechanicalDirectionMultiplier = 1;
    private boolean mechanicalInputConnected = false;
    private boolean mechanicalInputOverloaded = false;
    private boolean mechanicalLoadActive = false;
    private boolean mechanicalLoadSupplied = false;
    private float mechanicalSourceEquivalentDemand = 0.0F;
    private float mechanicalTotalSourceDemand = 0.0F;
    private float mechanicalSourceTorqueCapacity = 0.0F;

    private float clientMechanicalRotationDegrees = 0.0F;
    private float lastClientMechanicalVisualTime = Float.NaN;

    public PrimitiveGrinderBMultiblockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.primitivegrinderbmultiblockentity.get(), pos, state);
    }

    public boolean isFormed() {
        return formed;
    }

    public String getRepacementmodel() {
        return repacementmodel;
    }

    public boolean isCrushing() {
        return crushing;
    }

    public int getCrushTime() {
        return crushtime;
    }

    public int getTotalCrushTime() {
        return totalCrushTime;
    }

    public float getMechanicalSpeed() {
        return mechanicalSpeed;
    }

    public float getMechanicalTorque() {
        return mechanicalTorque;
    }

    public boolean isMechanicalInputConnected() {
        return mechanicalInputConnected;
    }

    public boolean isMechanicalInputOverloaded() {
        return mechanicalInputOverloaded;
    }

    public int getMechanicalDirectionMultiplier() {
        return mechanicalDirectionMultiplier;
    }

    public boolean isMechanicalLoadActive() {
        return mechanicalLoadActive;
    }

    public boolean isMechanicalLoadSupplied() {
        return mechanicalLoadSupplied;
    }

    public float getMechanicalSourceEquivalentDemand() {
        return mechanicalSourceEquivalentDemand;
    }

    public float getMechanicalTotalSourceDemand() {
        return mechanicalTotalSourceDemand;
    }

    public float getMechanicalSourceTorqueCapacity() {
        return mechanicalSourceTorqueCapacity;
    }

    public boolean hasRequiredMechanicalPower() {
        boolean basePowerAvailable = mechanicalInputConnected
                && !mechanicalInputOverloaded
                && mechanicalSpeed >= MIN_MECHANICAL_SPEED
                && mechanicalTorque >= REQUIRED_TORQUE;
        return basePowerAvailable && (!mechanicalLoadActive || mechanicalLoadSupplied);
    }

    public float getMechanicalVisualRotationDegrees(float partialTicks) {
        Level currentLevel = getLevel();
        if (currentLevel == null) {
            return normalizeDegrees(clientMechanicalRotationDegrees);
        }

        float currentVisualTime = currentLevel.getGameTime() + partialTicks;
        if (Float.isNaN(lastClientMechanicalVisualTime)) {
            clientMechanicalRotationDegrees = mechanicalRotationDegrees;
            lastClientMechanicalVisualTime = currentVisualTime;
            return normalizeDegrees(clientMechanicalRotationDegrees);
        }

        float deltaTicks = currentVisualTime - lastClientMechanicalVisualTime;
        lastClientMechanicalVisualTime = currentVisualTime;
        if (deltaTicks < 0.0F) {
            deltaTicks = 0.0F;
        } else if (deltaTicks > 20.0F) {
            deltaTicks = 20.0F;
        }

        if (!mechanicalInputOverloaded && mechanicalSpeed > 0.01F) {
            float degreesPerTick = mechanicalSpeed * 360.0F / 1200.0F;
            clientMechanicalRotationDegrees += degreesPerTick * deltaTicks * mechanicalDirectionMultiplier;
        }

        return normalizeDegrees(clientMechanicalRotationDegrees);
    }

    public ItemStack getInputStack() {
        return itemHandler.getStackInSlot(0);
    }

    public ItemStack getOutputStack() {
        return itemHandler.getStackInSlot(1);
    }

    @Override
    protected MultiblockController createMultiblockController() {
        MultiblockStructure structure = identifyMultiblockStructure(level, worldPosition);
        if (structure == null) {
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Primitive Grinder structure NOT found");
            }
            return null;
        }

        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            LOGGER.info("Primitive Grinder structure found");
        }

        MultiblockController newController = new MultiblockController(structure);
        if (!newController.getFormed() && !formed) {
            newController.identifyAndAddModules(level, worldPosition, structure);
            newController.createStructure(level, worldPosition);
            newController.setFormed(true);
            controller = newController;
            setMultiblockController(newController);
            formed = true;

            BlockState currentState = level.getBlockState(worldPosition);
            if (currentState.hasProperty(PrimitiveGrinderBMultiblock.IS_FORMED)) {
                level.setBlock(
                        worldPosition,
                        currentState.setValue(PrimitiveGrinderBMultiblock.IS_FORMED, true),
                        Block.UPDATE_ALL
                );
            }

            requestModelDataUpdate();
            sync();
            return newController;
        }

        return null;
    }

    @Override
    protected MultiblockStructure identifyMultiblockStructure(Level world, BlockPos pos) {
        for (Multiblock multiblock : MultiblockRegistry.getRegisteredMultiblocks().values()) {
            MultiblockStructure structure = multiblock.getStructure();
            if (matchesStructure(world, pos, structure, multiblock)) {
                repacementmodel = multiblock.getSettings().getReplaceWhenFormed();
                blueprintname = multiblock.getName();
                return structure;
            }
        }
        return null;
    }

    @Override
    protected void interactableNoGui(BlockState state,
                                     Level level,
                                     BlockPos pos,
                                     Player player,
                                     InteractionHand hand,
                                     BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        if (held.isEmpty()) {
            ItemStack extracted = itemHandler.extractItem(1, 64, false);
            if (extracted.isEmpty()) {
                extracted = itemHandler.extractItem(0, 64, false);
            }

            if (!extracted.isEmpty()) {
                if (!player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
                sync();
            }
            return;
        }

        ItemStack single = held.copy();
        single.setCount(1);

        SimpleContainer probe = new SimpleContainer(single);
        if (getMatchingRecipe(probe, level) == null) {
            return;
        }

        ItemStack remainder = itemHandler.insertItem(0, single, false);
        if (remainder.isEmpty()) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            sync();
        }
    }

    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState state, E blockEntity) {
        if (level.isClientSide || !(blockEntity instanceof PrimitiveGrinderBMultiblockEntity grinder)) {
            return;
        }

        grinder.tickProcessing(level);
    }

    private void tickProcessing(Level level) {
        GearNetworkManager network = GearNetworkManager.getInstance();

        if (!formed) {
            updateMechanicalLoad(level, false);
            updateMechanicalState(null);
            setCrushing(false);
            return;
        }

        // Mechanical state is independent of recipes. A connected shaft should visibly
        // drive the Grinder even while it is idle or when the current item cannot process.
        GearNode mechanicalInput = findConnectedMechanicalInput(level);
        updateMechanicalState(mechanicalInput);

        SimpleContainer input = new SimpleContainer(itemHandler.getStackInSlot(0));
        primitive_grinder_multiblockrecipe recipe = getMatchingRecipe(input, level);
        if (recipe == null) {
            updateMechanicalLoad(level, false);
            crushtime = 0;
            totalCrushTime = 200;
            setCrushing(false);
            setChanged();
            return;
        }

        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        if (!canAcceptOutput(result)) {
            updateMechanicalLoad(level, false);
            setCrushing(false);
            return;
        }

        // A valid pending job is a real 4T load on the connected shaft. Registering it
        // can overload the complete source network, so refresh the input state afterwards.
        updateMechanicalLoad(level, true);
        mechanicalInput = findConnectedMechanicalInput(level);
        updateMechanicalState(mechanicalInput);

        GearNetworkManager.MechanicalLoadState loadState =
                network.getMechanicalLoadState(level, worldPosition);

        if (mechanicalInput == null
                || mechanicalInput.getSpeed() < MIN_MECHANICAL_SPEED
                || !loadState.supplied()) {
            // Progress pauses rather than resetting when the crank stops or the aggregate
            // network demand exceeds what the source/gearing can actually provide.
            setCrushing(false);
            return;
        }

        totalCrushTime = Math.max(1, recipe.getCrushtime());
        setCrushing(true);
        crushtime++;
        setChanged();

        if (crushtime < totalCrushTime) {
            return;
        }

        itemHandler.extractItem(0, 1, false);
        itemHandler.insertItem(1, result, false);
        crushtime = 0;
        setCrushing(false);
        sync();
    }

    public Direction getMechanicalInputDirection() {
        // The current Grinder model is authored with its input on the WEST/left side.
        // Keep this explicit until the formed model itself becomes directional.
        return Direction.WEST;
    }

    public BlockPos getMechanicalInputPosition() {
        // The shaft enters the centerline of the stone base.
        return worldPosition.relative(getMechanicalInputDirection());
    }

    @Nullable
    private GearNode findConnectedMechanicalInput(Level level) {
        GearNetworkManager network = GearNetworkManager.getInstance();
        Direction inputDirection = getMechanicalInputDirection();
        GearNode node = network.getGear(getMechanicalInputPosition(), level);

        if (node == null
                || !node.isShaftLike()
                || node.getAxis() != inputDirection.getAxis()) {
            return null;
        }

        return node;
    }

    private boolean hasRequiredMechanicalPower(@Nullable GearNode node) {
        return node != null
                && !node.isOverloaded()
                && node.getSpeed() >= MIN_MECHANICAL_SPEED
                && node.getTorque() >= REQUIRED_TORQUE;
    }

    private void updateMechanicalLoad(Level level, boolean active) {
        GearNetworkManager network = GearNetworkManager.getInstance();
        network.setMechanicalLoad(
                level,
                worldPosition,
                getMechanicalInputPosition(),
                REQUIRED_TORQUE,
                active
        );

        GearNetworkManager.MechanicalLoadState state =
                network.getMechanicalLoadState(level, worldPosition);

        boolean changed = mechanicalLoadActive != state.active()
                || mechanicalLoadSupplied != state.supplied()
                || Math.abs(mechanicalSourceEquivalentDemand - state.sourceEquivalentDemand()) > 0.01F
                || Math.abs(mechanicalTotalSourceDemand - state.totalSourceDemand()) > 0.01F
                || Math.abs(mechanicalSourceTorqueCapacity - state.sourceTorqueCapacity()) > 0.01F;

        mechanicalLoadActive = state.active();
        mechanicalLoadSupplied = state.supplied();
        mechanicalSourceEquivalentDemand = state.sourceEquivalentDemand();
        mechanicalTotalSourceDemand = state.totalSourceDemand();
        mechanicalSourceTorqueCapacity = state.sourceTorqueCapacity();

        if (changed) {
            sync();
        }
    }

    private void updateMechanicalState(@Nullable GearNode node) {
        float newSpeed = node == null ? 0.0F : node.getSpeed();
        float newTorque = node == null ? 0.0F : node.getTorque();
        float newRotation = node == null ? mechanicalRotationDegrees : node.getRotationDegrees();
        int newDirection = node == null ? mechanicalDirectionMultiplier : node.getDirectionMultiplier();
        boolean newConnected = node != null;
        boolean newOverloaded = node != null && node.isOverloaded();

        boolean changed = Math.abs(mechanicalSpeed - newSpeed) > 0.01F
                || Math.abs(mechanicalTorque - newTorque) > 0.01F
                || mechanicalDirectionMultiplier != newDirection
                || mechanicalInputConnected != newConnected
                || mechanicalInputOverloaded != newOverloaded;

        mechanicalSpeed = newSpeed;
        mechanicalTorque = newTorque;
        mechanicalRotationDegrees = newRotation;
        mechanicalDirectionMultiplier = newDirection;
        mechanicalInputConnected = newConnected;
        mechanicalInputOverloaded = newOverloaded;

        if (changed) {
            sync();
        }
    }

    private float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0F;
        return normalized < 0.0F ? normalized + 360.0F : normalized;
    }

    private boolean canAcceptOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }

        ItemStack current = itemHandler.getStackInSlot(1);
        if (current.isEmpty()) {
            return result.getCount() <= itemHandler.getSlotLimit(1);
        }

        if (!ItemStack.isSameItemSameTags(current, result)) {
            return false;
        }

        int limit = Math.min(itemHandler.getSlotLimit(1), current.getMaxStackSize());
        return current.getCount() + result.getCount() <= limit;
    }

    private void setCrushing(boolean crushing) {
        if (this.crushing == crushing) {
            return;
        }
        this.crushing = crushing;
        sync();
    }

    @Nullable
    private primitive_grinder_multiblockrecipe getMatchingRecipe(SimpleContainer container, Level level) {
        return level.getRecipeManager()
                .getRecipeFor(primitive_grinder_multiblockrecipe.Type.INSTANCE, container, level)
                .orElse(null);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        crushtime = tag.getInt("CrushTime");
        totalCrushTime = tag.contains("TotalCrushTime") ? tag.getInt("TotalCrushTime") : 200;
        crushing = tag.getBoolean("isCrushing");
        mechanicalSpeed = tag.getFloat("MechanicalSpeed");
        mechanicalTorque = tag.getFloat("MechanicalTorque");
        mechanicalRotationDegrees = tag.getFloat("MechanicalRotationDegrees");
        mechanicalDirectionMultiplier = tag.getInt("MechanicalDirectionMultiplier") < 0 ? -1 : 1;
        mechanicalInputConnected = tag.getBoolean("MechanicalInputConnected");
        mechanicalInputOverloaded = tag.getBoolean("MechanicalInputOverloaded");
        mechanicalLoadActive = tag.getBoolean("MechanicalLoadActive");
        mechanicalLoadSupplied = tag.getBoolean("MechanicalLoadSupplied");
        mechanicalSourceEquivalentDemand = tag.getFloat("MechanicalSourceEquivalentDemand");
        mechanicalTotalSourceDemand = tag.getFloat("MechanicalTotalSourceDemand");
        mechanicalSourceTorqueCapacity = tag.getFloat("MechanicalSourceTorqueCapacity");
        clientMechanicalRotationDegrees = mechanicalRotationDegrees;
        lastClientMechanicalVisualTime = Float.NaN;

        MultiblockPersistentData multiblockData = loadMultiblockData(tag);
        blueprintname = multiblockData.blueprintName();
        formed = multiblockData.formed();
        repacementmodel = multiblockData.replacementModel();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("CrushTime", crushtime);
        tag.putInt("TotalCrushTime", totalCrushTime);
        tag.putBoolean("isCrushing", crushing);
        tag.putFloat("MechanicalSpeed", mechanicalSpeed);
        tag.putFloat("MechanicalTorque", mechanicalTorque);
        tag.putFloat("MechanicalRotationDegrees", mechanicalRotationDegrees);
        tag.putInt("MechanicalDirectionMultiplier", mechanicalDirectionMultiplier);
        tag.putBoolean("MechanicalInputConnected", mechanicalInputConnected);
        tag.putBoolean("MechanicalInputOverloaded", mechanicalInputOverloaded);
        tag.putBoolean("MechanicalLoadActive", mechanicalLoadActive);
        tag.putBoolean("MechanicalLoadSupplied", mechanicalLoadSupplied);
        tag.putFloat("MechanicalSourceEquivalentDemand", mechanicalSourceEquivalentDemand);
        tag.putFloat("MechanicalTotalSourceDemand", mechanicalTotalSourceDemand);
        tag.putFloat("MechanicalSourceTorqueCapacity", mechanicalSourceTorqueCapacity);
        saveMultiblockData(tag, blueprintname, formed, repacementmodel);
    }

    @Override
    public void onDestroy(Level level) {
        if (!level.isClientSide) {
            GearNetworkManager.getInstance().removeMechanicalLoad(level, worldPosition);
        }
        super.onDestroy(level);
    }

    @Override
    public int capacityE() {
        return 0;
    }

    @Override
    public int maxtransferE() {
        return 0;
    }

    @Override
    public int capacityH() {
        return 0;
    }

    @Override
    public int maxtransferH() {
        return 0;
    }

    @Override
    public int capacityW() {
        return 0;
    }

    @Override
    public int maxtransferW() {
        return 0;
    }

    @Override
    public int capacityF() {
        return 0;
    }

    @Override
    public int tanks() {
        return 0;
    }

    @Override
    public int invsize() {
        return 2;
    }

    @Override
    public int capacityP() {
        return 0;
    }

    @Override
    public int maxtransferP() {
        return 0;
    }

    @Override
    public boolean itemcape() {
        return true;
    }

    @Override
    public boolean energycape() {
        return false;
    }

    @Override
    public boolean heatcape() {
        return false;
    }

    @Override
    public boolean wattcape() {
        return false;
    }

    @Override
    public boolean fluidcape() {
        return false;
    }

    @Override
    public boolean pressurecape() {
        return false;
    }

    @Override
    public boolean HeatCanReceive() {
        return false;
    }

    @Override
    public boolean HeatCanSend() {
        return false;
    }

    @Override
    public boolean WattCanReceive() {
        return false;
    }

    @Override
    public boolean WattCanSend() {
        return false;
    }

    @Override
    public boolean EnergyCanReceive() {
        return false;
    }

    @Override
    public boolean EnergyCanSend() {
        return false;
    }

    @Override
    public boolean PressureCanReceive() {
        return false;
    }

    @Override
    public boolean PressureCanSend() {
        return false;
    }

    @Override
    public Level getThisWorld() {
        return level;
    }

    @Override
    public BlockPos getThisPosition() {
        return worldPosition;
    }

    @Override
    public CompoundTag sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.magneticraft2.primitivegrinder");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    @Override
    public @NotNull ModelData getModelData() {
        return super.getModelData()
                .derive()
                .with(MultiBlockProperties.MODEL_NAME, getRepacementmodel())
                .build();
    }
}
