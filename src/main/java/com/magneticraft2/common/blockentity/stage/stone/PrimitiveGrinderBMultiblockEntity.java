package com.magneticraft2.common.blockentity.stage.stone;

import com.magneticraft2.common.block.general.BlueprintMultiblock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.recipe.stage.stone.primitive_grinder_multiblockrecipe;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrimitiveGrinderBMultiblockEntity extends BaseBlockEntityMagneticraft2 {
    private String blueprintname = "";
    private boolean formed = false;
    private String repacementmodel = "";
    private MultiblockController controller;

    private int crushtime;
    private boolean crushing = false;
    private int totalCrushTime = 200;

    public PrimitiveGrinderBMultiblockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.primitivegrinderbmultiblockentity.get(), pos, state);
    }

    public boolean isFormed() {
        return formed;
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

    }

    @Nullable
    private primitive_grinder_multiblockrecipe getMatchingRecipe(Container container, Level level) {
        return level.getRecipeManager()
                .getRecipeFor(primitive_grinder_multiblockrecipe.Type.INSTANCE, container, level)
                .orElse(null);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create( this );
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag( pkt.getTag() );
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbtTagCompound = new CompoundTag();
        saveAdditional(nbtTagCompound);
        getModelData();
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    private void loadClientData(CompoundTag tag) {
        tag.putInt("CrushTime", this.crushtime);
        tag.putInt("TotalCrushTime", this.totalCrushTime);
        tag.putBoolean("isCrushing", this.crushing);
        saveMultiblockData(tag, blueprintname, formed, repacementmodel);
    }
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.crushtime = tag.getInt("CrushTime");
        this.totalCrushTime = tag.getInt("TotalCrushTime");
        this.crushing = tag.getBoolean("isCrushing");
        MultiblockPersistentData multiblockData = loadMultiblockData(tag);
        blueprintname = multiblockData.blueprintName();
        formed = multiblockData.formed();
        repacementmodel = multiblockData.replacementModel();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("CrushTime", this.crushtime);
        tag.putInt("TotalCrushTime", this.totalCrushTime);
        tag.putBoolean("isCrushing", this.crushing);
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
        level.sendBlockUpdated( worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL );
        CompoundTag tag = super.getUpdateTag();
        loadClientData(tag);
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.magneticraft2.primitivegrinder");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return null;
    }
    @Override
    public @NotNull ModelData getModelData() {
        ModelData data = super.getModelData();
        data = data.derive().with(MultiBlockProperties.MODEL_NAME, getRepacementmodel()).build();
        return data;
    }
}
