package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.client.gui.container.projector.Projector_container;
import com.magneticraft2.common.block.general.projectortest;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class projectortestBlockEntity extends BaseBlockEntityMagneticraft2 {
    private String blueprint;
    private boolean invalidblueprint = false;
    private boolean renderingoutline = false;
    private boolean shouldrenderblueprint = false;
    public projectortestBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.projectortestBlockEntity.get(), pos, state);
        menuProvider = this;
    }
    public Direction getProjectionDirection() {
        return this.getBlockState().getValue(projectortest.FACING);
    }

    @Override
    public AABB getRenderBoundingBox() {
    return new AABB(worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),worldPosition.getX()+10,worldPosition.getY()+10,worldPosition.getZ()+10);
    }
    public void setBlueprint(String val) {
        blueprint = val;
    }
    public String getBlueprint(){
        return blueprint;
    }
    public void setInvalidBlueprint(boolean val){
        invalidblueprint = val;
    }
    public boolean getInvalidBlueprint(){
        return invalidblueprint;
    }
    public void setRenderingoutline(boolean val) {
        renderingoutline = val;
    }
    public boolean getRenderingoutline(){
        return renderingoutline;
    }
    public void setShouldrenderblueprint(boolean val){
        shouldrenderblueprint = val;
    }
    public boolean getShouldrenderblueprint(){
        return shouldrenderblueprint;
    }
    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.magneticraft2.projector");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory playerinv, Player player) {
        return new Projector_container(i,level,getBlockPos(),playerinv,player);
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
        if (blueprint != null) {
            tag.putString("Blueprint", blueprint);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag == null) return;
        if (tag.getString("Blueprint") != null){
            blueprint = tag.getString("Blueprint");
        }
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
    public CompoundTag sync() {
        return null;
    }
}
