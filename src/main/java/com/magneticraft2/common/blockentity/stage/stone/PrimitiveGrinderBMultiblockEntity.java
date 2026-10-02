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
        if (!formed) {
            updateMechanicalState(null);
            setCrushing(false);
            return;
        }

        SimpleContainer input = new SimpleContainer(itemHandler.getStackInSlot(0));
        primitive_grinder_multiblockrecipe recipe = getMatchingRecipe(input, level);
        if (recipe == null) {
            crushtime = 0;
            totalCrushTime = 200;
            updateMechanicalState(null);
            setCrushing(false);
            setChanged();
            return;
        }

        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        if (!canAcceptOutput(result)) {
            updateMechanicalState(findMechanicalInput(level));
            setCrushing(false);
            return;
        }

        GearNode mechanicalInput = findMechanicalInput(level);
        updateMechanicalState(mechanicalInput);

        if (mechanicalInput == null) {
            // Mechanical progress pauses instead of resetting when the crank stops.
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
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(PrimitiveGrinderBMultiblock.FACING)
                ? state.getValue(PrimitiveGrinderBMultiblock.FACING)
                : Direction.NORTH;

        // The authored NORTH model has its mechanical bearing on the left side.
        return facing.getCounterClockWise();
    }

    public BlockPos getMechanicalInputPosition() {
        // The axle enters the upper grinding assembly, not the stone base.
        return worldPosition.above().relative(getMechanicalInputDirection());
    }

    @Nullable
    private GearNode findMechanicalInput(Level level) {
        GearNetworkManager network = GearNetworkManager.getInstance();
        Direction inputDirection = getMechanicalInputDirection();
        GearNode node = network.getGear(getMechanicalInputPosition(), level);

        if (node == null
                || !node.isShaftLike()
                || node.getAxis() != inputDirection.getAxis()
                || node.isOverloaded()
                || node.getSpeed() < MIN_MECHANICAL_SPEED
                || node.getTorque() < REQUIRED_TORQUE) {
            return null;
        }

        return node;
    }

    private void updateMechanicalState(@Nullable GearNode node) {
        mechanicalSpeed = node == null ? 0.0F : node.getSpeed();
        mechanicalTorque = node == null ? 0.0F : node.getTorque();
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
        saveMultiblockData(tag, blueprintname, formed, repacementmodel);
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
