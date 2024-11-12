package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.client.gui.container.blueprintmaker.blueprintmaker_container;
import com.magneticraft2.common.block.general.BlueprintMultiblock;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.systems.Blueprint.core.BlueprintBuilder;
import com.magneticraft2.common.systems.Blueprint.core.BlueprintSaver;
import com.magneticraft2.common.systems.Blueprint.json.Blueprint;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 12-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BlueprintMultiblockEntity extends BaseBlockEntityMagneticraft2 {
    private String blueprintname = "";
    private boolean formed = false;
    private String repacementmodel = "";

    private BlockPos pos1;
    private BlockPos pos2;
    private long pos1long;
    private long pos2long;
    private long initialGameTime = 0;
    private boolean shouldsave = false;
    private String blueprintname1;
    public void setShouldsave(boolean save){
        shouldsave = save;
    }
    public boolean getShouldsave(){
        return shouldsave;
    }

    public void setBlueprintname(String name){
        blueprintname1 = name;
    }
    public String getBlueprintname(){
        return blueprintname1;
    }
    public void setInitialGameTime(long gameTime) {
        initialGameTime = gameTime;
    }

    public long getInitialGameTime() {
        return initialGameTime;
    }
    private boolean justPlaced = true;

    public boolean isJustPlaced() {
        return justPlaced;
    }
    public void setJustPlaced(boolean value) {
        justPlaced = value;
    }

    public BlueprintMultiblockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.blueprintmultiblockentity.get(), pos, state);
        menuProvider = this;
    }
    public boolean isFormed() {
        return formed;
    }
    public String getRepacementmodel() {
        return repacementmodel;
    }
    private void setpos1(long value){
        pos1 = BlockPos.of(value);
    }
    private void setpos2(long value){
        pos2 = BlockPos.of(value);
    }
    private void setpos1long(long val) {
        pos1long = val;
    }
    private void setpos2long(long val) {
        pos2long = val;
    }
    private void resetpos1(){
        pos1 = new BlockPos(-65,-65,-65);
    }
    private void resetpos2(){
        pos2 = new BlockPos(-65,-65,-65);
    }

    public BlockPos getPos1BlockPos(){
        return pos1;
    }
    public BlockPos getPos2BlockPos(){
        return pos2;
    }
    public long getPos1long(){
        return pos1long;
    }
    public long getPos2long(){
        return pos2long;
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
                BlockState newState = currentState.setValue(BlueprintMultiblock.IS_FORMED, true);

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
        pos1 =  BlockPos.of(tag.getLong("pos1"));
        pos2 = BlockPos.of(tag.getLong("pos2"));
        pos1long = tag.getLong("pos1long");
        pos2long = tag.getLong("pos2long");
        blueprintname = tag.getString("blueprintname");
        shouldsave = tag.getBoolean("shouldsave");
        initialGameTime = tag.getLong("time");
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
        pos1 =  BlockPos.of(tag.getLong("pos1"));
        pos2 = BlockPos.of(tag.getLong("pos2"));
        pos1long = tag.getLong("pos1long");
        pos2long = tag.getLong("pos2long");
        blueprintname = tag.getString("blueprintname");
        shouldsave = tag.getBoolean("shouldsave");
        initialGameTime = tag.getLong("time");
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
        if (pos1long != 0){
            tag.putLong("pos1long", pos1long);
        }
        if (pos2long != 0){
            tag.putLong("pos2long", pos2long);
        }
        if (pos1 != null) {
            tag.putLong("pos1", pos1.asLong());
        }
        if (pos2 != null){
            tag.putLong("pos2", pos2.asLong());
        }
        if (blueprintname != null){
            tag.putString("blueprintname", blueprintname);
        }
        tag.putBoolean("shouldsave", shouldsave);
        tag.putLong("time", initialGameTime);
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
        return 1;
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
        return Component.translatable("screen.magneticraft2.blueprintmaker");
    }
    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        if (!level.isClientSide()){
            BlueprintMultiblockEntity entity = (BlueprintMultiblockEntity) e.getLevel().getBlockEntity(pos);
            LazyOptional<IItemHandler> optionalHandler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
            IItemHandler itemHandler = optionalHandler.orElse(null);
            ItemStack itemStack = itemHandler.getStackInSlot(0);
            entity.sync();

            if (itemStack.getItem() == ItemRegistry.item_blueprintmarker.get()) {
                if (itemStack.getTag() == null){
                    return;
                }
                long pos1xyz = itemStack.getTag().getLong("pos1");
                long pos2xyz = itemStack.getTag().getLong("pos2");
                if (itemStack.getTag().contains("pos1")) {
                    if (entity.pos1long == 0) {
                        entity.setpos1long(pos1xyz);
                    }
                    if (!entity.pos1.equals(BlockPos.of(pos1xyz))) {
                        entity.setpos1(pos1xyz);
                    }
                }
                if (itemStack.getTag().contains("pos2")) {
                    if (entity.pos2long == 0) {
                        entity.setpos2long(pos2xyz);
                    }
                    if (!entity.pos2.equals(BlockPos.of(pos2xyz))) {
                        entity.setpos2(pos2xyz);
                    }
                }
            }else if(itemStack.getItem() != ItemRegistry.item_blueprintmarker.get()){
                entity.resetpos1();
                entity.resetpos2();
                entity.setpos1long(0);
                entity.setpos2long(0);
            }

        }
    }
    public void saveBlueprintClient(String owner){
        File savedir = new File("blueprints");
        Blueprint newBlueprint = BlueprintBuilder.buildBlueprint(blueprintname, owner, level, pos1, pos2);
        LOGGER.info(newBlueprint.getName());
        LOGGER.info(newBlueprint.getOwner());
        LOGGER.info(newBlueprint.getStructure().getBlocks());
        LOGGER.info(Arrays.toString(newBlueprint.getStructure().getDimensions()));
        LOGGER.info(newBlueprint.getStructure().getLayout());
        BlueprintRegistry.registerBlueprint(MOD_ID, newBlueprint, owner);
        BlueprintSaver.saveBlueprintClient(newBlueprint, savedir, owner);
        setBlueprintname(null);
        setShouldsave(false);
    }
    public void saveBlueprintServer(String owner){
        File savedir = new File("blueprints");
        Blueprint newBlueprint = BlueprintBuilder.buildBlueprint(blueprintname, owner, level, pos1, pos2);
        LOGGER.info(newBlueprint.getName());
        LOGGER.info(newBlueprint.getStructure().getBlocks());
        LOGGER.info(Arrays.toString(newBlueprint.getStructure().getDimensions()));
        LOGGER.info(newBlueprint.getStructure().getLayout());
        BlueprintRegistry.registerBlueprint(MOD_ID, newBlueprint, owner);
        BlueprintSaver.saveBlueprintServer(newBlueprint, savedir);

        setBlueprintname(null);
        setShouldsave(false);
    }
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new blueprintmaker_container(pContainerId, level,getBlockPos(),pPlayerInventory,pPlayer);
    }


    @Override
    public @NotNull ModelData getModelData() {
        ModelData data = super.getModelData();
        data = data.derive().with(MultiBlockProperties.MODEL_NAME, getRepacementmodel()).build();
        return data;
    }
}
