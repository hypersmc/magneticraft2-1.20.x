package com.magneticraft2.common.blockentity.stage.stone;

import com.magneticraft2.common.block.general.BlueprintMultiblock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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

public class PrimitiveStorageCellarMultiblockEntity extends BaseBlockEntityMagneticraft2 {
    private String blueprintname = "";
    private String repacementmodel = "";
    private boolean formed = false;
    private MultiblockController controller;

    public boolean isFormed() {
        return formed;
    }
    public PrimitiveStorageCellarMultiblockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(BlockEntityRegistry.storagecellarblockentity.get(), pWorldPosition, pBlockState);
    }
    public String getMBblueprintname() {
        return blueprintname;
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
        return 48;
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

    //just so i got what needs to save when quit game


    @Override
    public Component getDisplayName() {
        return null;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return null;
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
    public CompoundTag sync() {
        level.sendBlockUpdated( worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL );
        CompoundTag tag = super.getUpdateTag();
        loadClientData(tag);
        return null;
    }
    private void loadClientData(CompoundTag tag) {
        blueprintname = tag.getString("blueprintname");
        tag.putString("blueprintname", blueprintname);
        tag.putBoolean("formed", formed);
        tag.putString("repacementmodel", repacementmodel);
        if (getMultiblockController() != null) {
            tag.put("MultiblockController", getMultiblockController().saveToNBT());
            CompoundTag structureTag = getMultiblockController().getStructure().saveToNBT();
            tag.put("MultiblockStructure", structureTag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        blueprintname = tag.getString("BlueprintName");
        formed = tag.getBoolean("Formed");
        repacementmodel = tag.getString("Repacementmodel");
        if (tag.contains("MultiblockStructure")) {
            // Extract data for dimensions, layout, and blocks from the NBT tag
            CompoundTag structureTag = tag.getCompound("MultiblockStructure");

            // Retrieve dimensions
            ListTag dimensionsList = structureTag.getList("dimensions", 3); // Assuming each dimension is an integer
            int[] dimensions = new int[dimensionsList.size()];
            for (int i = 0; i < dimensionsList.size(); i++) {
                dimensions[i] = dimensionsList.getInt(i);
            }

            // Retrieve layout
            Map<String, List<List<String>>> layout = new HashMap<>();
            CompoundTag layoutTag = structureTag.getCompound("layout");
            for (String layerKey : layoutTag.getAllKeys()) {
                List<List<String>> layerList = new ArrayList<>();
                ListTag layerData = layoutTag.getList(layerKey, 9); // Assuming each row is stored as a ListTag
                for (int j = 0; j < layerData.size(); j++) {
                    List<String> rowList = new ArrayList<>();
                    ListTag rowData = layerData.getList(j);
                    for (int k = 0; k < rowData.size(); k++) {
                        rowList.add(rowData.getString(k));
                    }
                    layerList.add(rowList);
                }
                layout.put(layerKey, layerList);
            }

            // Retrieve blocks
            Map<String, Block> blocks = new HashMap<>();
            CompoundTag blocksTag = structureTag.getCompound("blocks");
            for (String blockKey : blocksTag.getAllKeys()) {
                Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(blocksTag.getString(blockKey)));
                if (block != null) {
                    blocks.put(blockKey, block);
                }
            }

            // Now construct the MultiblockStructure with the loaded data
            MultiblockStructure structure = new MultiblockStructure(dimensions, layout, blocks);

            // Now initialize MultiblockController with the loaded structure
            if (tag.contains("MultiblockController")) {
                MultiblockController multiblockController = new MultiblockController(structure);
                multiblockController.loadFromNBT(tag.getCompound("MultiblockController"));
                this.controller = multiblockController;
                setMultiblockController(multiblockController);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("BlueprintName", blueprintname);
        tag.putBoolean("Formed", formed);
        tag.putString("Repacementmodel", repacementmodel);
        if (getMultiblockController() != null) {
            tag.put("MultiblockController", getMultiblockController().saveToNBT());

            // Save MultiblockStructure separately if it exists within the controller
            if (getMultiblockController().getStructure() != null) {
                tag.put("MultiblockStructure", getMultiblockController().getStructure().saveToNBT());
            }
        }
    }

    public String getRepacementmodel() {
        return repacementmodel;
    }
    @Override
    public void handleUpdateTag(CompoundTag parentNBTTagCompound)
    {
        load(parentNBTTagCompound);
    }
    @Override
    public @NotNull ModelData getModelData() {
        ModelData data = super.getModelData();
        data = data.derive().with(MultiBlockProperties.MODEL_NAME, getRepacementmodel()).build();
        return data;
    }
}
