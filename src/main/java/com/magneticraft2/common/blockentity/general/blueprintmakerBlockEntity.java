package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.client.gui.container.blueprintmaker.blueprintmaker_container;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.systems.Blueprint.json.Blueprint;
import com.magneticraft2.common.systems.Blueprint.core.BlueprintBuilder;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintRegistry;
import com.magneticraft2.common.systems.Blueprint.core.BlueprintSaver;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 20-08-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class blueprintmakerBlockEntity extends BaseBlockEntityMagneticraft2{
    private BlockPos pos1;
    private BlockPos pos2;
    private long pos1long;
    private long pos2long;
    private long initialGameTime = 0;
    private boolean shouldsave = false;
    private String blueprintname;

    public void setShouldsave(boolean save){
        shouldsave = save;
    }
    public boolean getShouldsave(){
        return shouldsave;
    }

    public void setBlueprintname(String name){
        blueprintname = name;
    }
    public String getBlueprintname(){
        return blueprintname;
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
    public blueprintmakerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.blueprintmakerBlockEntity.get(), pos, state);
        menuProvider = this;
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
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(CompoundTag parentNBTTagCompound)
    {
        load(parentNBTTagCompound);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
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
    public void load(CompoundTag tag) {
        super.load(tag);
        pos1 =  BlockPos.of(tag.getLong("pos1"));
        pos2 = BlockPos.of(tag.getLong("pos2"));
        pos1long = tag.getLong("pos1long");
        pos2long = tag.getLong("pos2long");
        blueprintname = tag.getString("blueprintname");
        shouldsave = tag.getBoolean("shouldsave");
        initialGameTime = tag.getLong("time");

    }

    @Override
    protected MultiblockController createMultiblockController() {
        return null;
    }

    @Override
    protected MultiblockStructure identifyMultiblockStructure(Level world, BlockPos pos) {
        return null;
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
    public CompoundTag sync() {
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
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
    }

        @Override
    public Component getDisplayName() {
        return Component.translatable("screen.magneticraft2.blueprintmaker");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new blueprintmaker_container(pContainerId, level,getBlockPos(),pPlayerInventory,pPlayer);
    }
    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        if (!level.isClientSide()){
            blueprintmakerBlockEntity entity = (blueprintmakerBlockEntity) e.getLevel().getBlockEntity(pos);
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
}
