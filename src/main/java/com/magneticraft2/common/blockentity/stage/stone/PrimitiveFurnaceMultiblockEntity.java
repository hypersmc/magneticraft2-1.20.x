package com.magneticraft2.common.blockentity.stage.stone;

import com.magneticraft2.client.gui.container.primitivefurnace.primitivefurnace_container;
import com.magneticraft2.common.block.general.BlueprintMultiblock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.recipe.stage.stone.primitive_furnace_multiblockrecipe;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.common.utils.MultiBlockProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 13-11-2024
 * @Project mgc2-1.20
* @version 1.0.0
 */
public class PrimitiveFurnaceMultiblockEntity extends BaseBlockEntityMagneticraft2 {
    private String blueprintname = "";
    private boolean formed = false;
    private String repacementmodel = "";
    private MultiblockController controller;
    private int cookTime;
    private boolean iscooking = false;
    private int totalCookTime = 200; // Default cook time, can be adjusted per recipe


    public PrimitiveFurnaceMultiblockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.primitivefurnacemultiblockentity.get(), pos, state);
        menuProvider = this;
    }
    public boolean isFormed() {
        return formed;
    }
    public int getCookTime() {
        return cookTime;
    }
    public boolean isCooking() {
        return iscooking;
    }
    public String getRepacementmodel() {
        return repacementmodel;
    }
    @Override
    protected MultiblockController createMultiblockController() {
        MultiblockStructure structure = identifyMultiblockStructure(level, worldPosition);
        if (structure != null) {
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Structure found!");
            }
            MultiblockController controller = new MultiblockController(structure);
            if (!controller.getFormed() && !formed){
                controller.identifyAndAddModules(level, worldPosition, structure);
                controller.createStructure(level, worldPosition);
                controller.setFormed(true);
                this.controller = controller;
                setMultiblockController(controller);
                formed = true;
                BlockState currentState = level.getBlockState(worldPosition);
                BlockState newState = currentState.setValue(BlueprintMultiblock.IS_FORMED, true);
                level.setBlock(worldPosition, newState, 3);
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                    LOGGER.info("Controller made and the multiblock should be formed: {}", controller.getFormed());
                }
                requestModelDataUpdate();
                return controller;
            }
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Structure already formed!");
            }
            return null;
        } else {
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Structure NOT found!");
            }
            return null;
        }
    }
    public int getAirAmount(){
        if (controller.getFormed()) {
            BlockPos module = controller.getmodulePos("psi_module");
            if (module != null) {
                BellowsMultiblockModuleEntity PSI = (BellowsMultiblockModuleEntity) getThisWorld().getBlockEntity(module);
                return PSI.getStored();
            }
        }
        return -9999;
    }

    @Override
    protected MultiblockStructure identifyMultiblockStructure(Level world, BlockPos pos) {
        for (Multiblock multiblock : MultiblockRegistry.getRegisteredMultiblocks().values()) {
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("Trying Multiblock: {}", multiblock.getName());
            }
            MultiblockStructure structure = multiblock.getStructure();
            if (matchesStructure(world, pos, structure, multiblock)) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                    LOGGER.info("Found multiblock: " + multiblock.getName());
                    LOGGER.info("Replacementmodel: {}", multiblock.getSettings().getReplaceWhenFormed());
                }
                repacementmodel = multiblock.getSettings().getReplaceWhenFormed();
                blueprintname = multiblock.getName();
                return structure;
            }
        }
        return null;
    }

    @Override
    protected void interactableNoGui(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        return;
    }

    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        if (level.isClientSide) {
            return;
        }
        PrimitiveFurnaceMultiblockEntity entity = (PrimitiveFurnaceMultiblockEntity) e.getLevel().getBlockEntity(pos);

        // Retrieve recipe and inputs
        ItemStack input1 = entity.itemHandler.getStackInSlot(0);
        ItemStack input2 = entity.itemHandler.getStackInSlot(1);
        primitive_furnace_multiblockrecipe recipe = entity.getMatchingRecipe(new SimpleContainer(input1, input2), level);

        // Check if there is a valid recipe
        if (recipe == null) {
            entity.cookTime = 0;
            entity.iscooking = false;
            return;
        }

        // Check if there is enough space in output slots before proceeding
        ItemStack currentOutput1 = entity.itemHandler.getStackInSlot(2);
        ItemStack recipeOutput1 = recipe.assemble(new SimpleContainer(input1, input2), null);
        boolean canAddPrimaryOutput = currentOutput1.isEmpty() ||
                (currentOutput1.is(recipeOutput1.getItem()) &&
                        currentOutput1.getCount() + recipeOutput1.getCount() <= currentOutput1.getMaxStackSize());

        ItemStack currentOutput2 = entity.itemHandler.getStackInSlot(3);
        ItemStack recipeOutput2 = recipe.getSecondOutput();
        boolean canAddSecondaryOutput = currentOutput2.isEmpty() ||
                (currentOutput2.is(recipeOutput2.getItem()) &&
                        currentOutput2.getCount() + recipeOutput2.getCount() <= currentOutput2.getMaxStackSize());

        // If there’s not enough space, reset cook time and skip processing
        if (!canAddPrimaryOutput || !canAddSecondaryOutput) {
            entity.cookTime = 0;
            entity.iscooking = false;
            return;
        }

        // Increment cook time if there's space in the output
        entity.totalCookTime = recipe.getCookTime();
        entity.cookTime++;
        entity.iscooking = true;
        // Check if cook time has been met
        if (entity.cookTime >= entity.totalCookTime) {
            // Process primary output
            if (!currentOutput1.isEmpty() && currentOutput1.is(recipeOutput1.getItem())) {
                currentOutput1.grow(recipeOutput1.getCount());
            } else {
                entity.itemHandler.setStackInSlot(2, recipeOutput1);
            }

            // Process secondary output
            if (!currentOutput2.isEmpty() && currentOutput2.is(recipeOutput2.getItem())) {
                currentOutput2.grow(recipeOutput2.getCount());
            } else {
                entity.itemHandler.setStackInSlot(3, recipeOutput2.copy());
            }

            // Consume inputs
            input1.shrink(1);
            input2.shrink(1);

            // Reset cook time
            entity.cookTime = 0;
            entity.iscooking = false;
        }
        entity.setChanged();
        level.sendBlockUpdated(pos, estate, estate, 3);
    }

    @Nullable
    private primitive_furnace_multiblockrecipe getMatchingRecipe(Container container, Level level) {
        return level.getRecipeManager()
                .getRecipeFor(primitive_furnace_multiblockrecipe.Type.INSTANCE, container, level)
                .orElse(null);
    }
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create( this );
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag( pkt.getTag() );
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        saveAdditional(nbtTagCompound);
        getModelData();
        return nbtTagCompound;
    }
    @Override
    public void handleUpdateTag(CompoundTag parentNBTTagCompound)
    {
        load(parentNBTTagCompound);
    }
    @Override
    public CompoundTag sync() {
        level.sendBlockUpdated( worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL );
        CompoundTag tag = super.getUpdateTag();
        loadClientData(tag);
        return null;
    }
    private void loadClientData(CompoundTag tag) {
        tag.putInt("CookTime", this.cookTime);
        tag.putInt("TotalCookTime", this.totalCookTime);
        tag.putBoolean("iscooking", this.iscooking);
        saveMultiblockData(tag, blueprintname, formed, repacementmodel);
    }
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.cookTime = tag.getInt("CookTime");
        this.totalCookTime = tag.getInt("TotalCookTime");
        this.iscooking = tag.getBoolean("iscooking");
        MultiblockPersistentData multiblockData = loadMultiblockData(tag);
        blueprintname = multiblockData.blueprintName();
        formed = multiblockData.formed();
        repacementmodel = multiblockData.replacementModel();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("CookTime", this.cookTime);
        tag.putInt("TotalCookTime", this.totalCookTime);
        tag.putBoolean("iscooking", this.iscooking);
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
        return 4;
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
    public Component getDisplayName() {
        return Component.translatable("screen.magneticraft2.primitivefurnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new primitivefurnace_container(pContainerId,level,getBlockPos(),pPlayerInventory,pPlayer);
    }

    @Override
    public @NotNull ModelData getModelData() {
        ModelData data = super.getModelData();
        data = data.derive().with(MultiBlockProperties.MODEL_NAME, getRepacementmodel()).build();
        return data;
    }
}
