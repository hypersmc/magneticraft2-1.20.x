package com.magneticraft2.common.blockentity.general;

import com.google.gson.JsonParseException;
import com.magneticraft2.client.model.MultiBlockModel;
import com.magneticraft2.client.model.MultiBlockModelLoader;
import com.magneticraft2.common.block.general.testmultiblockblock;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import com.magneticraft2.common.utils.MultiBlockProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author JumpWatch on 11-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class testmultiblock extends BaseBlockEntityMagneticraft2{
    private String blueprintname = "";
    private boolean formed = false;
    private String repacementmodel = "";
    public testmultiblock(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.testmultiblock.get(), pos, state);
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
            LOGGER.info("Structure found!");
            MultiblockController controller = new MultiblockController(structure);
            if (!controller.getFormed() && !formed){
                controller.identifyAndAddModules(level, worldPosition, structure);
                controller.createStructure(level, worldPosition);
                controller.setFormed(true);
                setMultiblockController(controller);
                formed = true;

                BlockState currentState = level.getBlockState(worldPosition);
                BlockState newState = currentState.setValue(testmultiblockblock.IS_FORMED, true);
                level.setBlock(worldPosition, newState, 3);

                LOGGER.info("Multiblock system got this far!"); // At this point, we have identified the structure,
                // found modules, and should have activated them

                LOGGER.info("Controller made and the multiblock should be formed: {}", controller.getFormed());
                LOGGER.info("Energy module location: {} ", controller.getmodulePos("energy_storage"));
                BlockPos pos = controller.getmodulePos("energy_storage");
                if (pos != null) {
                    testpowermodule testpowermodule = (testpowermodule) level.getBlockEntity(pos);
                    LOGGER.info("Energy module power stored: {}", testpowermodule.getStored());
                }
                requestModelDataUpdate();
                return controller;
            }
            LOGGER.info("Structure already formed!");
            return null;
        } else {
            LOGGER.info("Structure NOT found!");
            return null;
        }
    }


    @Override
    protected MultiblockStructure identifyMultiblockStructure(Level world, BlockPos pos) {
        for (Multiblock multiblock : MultiblockRegistry.getRegisteredMultiblocks().values()) {
            LOGGER.info("Trying Multiblock: {}", multiblock.getName());
            MultiblockStructure structure = multiblock.getStructure();
            if (matchesStructure(world, pos, structure, multiblock)) {
                LOGGER.info("Found multiblock: "+ multiblock.getName());
                LOGGER.info("Replacementmodel: {}", multiblock.getSettings().getReplaceWhenFormed());
                repacementmodel = multiblock.getSettings().getReplaceWhenFormed();
                blueprintname = multiblock.getName();
                return structure;
            }
        }
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
        tag.putString("blueprintname", blueprintname);
        tag.putBoolean("formed", formed);
        tag.putString("repacementmodel", repacementmodel);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        blueprintname = tag.getString("BlueprintName");
        formed = tag.getBoolean("Formed");
        repacementmodel = tag.getString("Repacementmodel");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putString("BlueprintName", blueprintname);
        tag.putBoolean("Formed", formed);
        tag.putString("Repacementmodel", repacementmodel);
//        if (itemcape())
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
        return 0;
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
        return false;
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
        return null;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return null;
    }


    @Override
    public @NotNull ModelData getModelData() {
        ModelData data = super.getModelData();
        data = data.derive().with(MultiBlockProperties.MODEL_NAME, getRepacementmodel()).build();
        return data;
    }
}
