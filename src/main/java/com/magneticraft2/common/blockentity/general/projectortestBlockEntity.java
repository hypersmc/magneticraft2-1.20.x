package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.client.gui.container.projector.Projector_container;
import com.magneticraft2.common.block.general.projectortest;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.Blueprint.json.Blueprint;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintRegistry;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
* @version 1.0.0
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

    public void setBlueprint(String val) {
        blueprint = cleanBlueprintName(val);
    }

    public boolean setBlueprintFromPlayer(ServerPlayer player, String val) {
        if (level == null || level.isClientSide() || player == null) {
            return false;
        }

        if (player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) > 64.0D) {
            player.sendSystemMessage(Component.literal("You are too far away from the Blueprint Projector."));
            return false;
        }

        String cleanedBlueprintName = cleanBlueprintName(val);
        if (cleanedBlueprintName == null) {
            player.sendSystemMessage(Component.literal("Blueprint selection was empty."));
            return false;
        }

        Blueprint selectedBlueprint = BlueprintRegistry.getRegisteredBlueprint(MOD_ID, cleanedBlueprintName);
        if (selectedBlueprint == null) {
            invalidblueprint = true;
            setChanged();
            sync();
            player.sendSystemMessage(Component.literal("Blueprint does not exist on the server: " + cleanedBlueprintName));
            return false;
        }

        if (!BlueprintRegistry.isBlueprintOwnedByPlayer(selectedBlueprint.getOwner(), player.getName().getString())) {
            invalidblueprint = true;
            setChanged();
            sync();
            player.sendSystemMessage(Component.literal("You do not own blueprint: " + cleanedBlueprintName));
            return false;
        }

        blueprint = cleanedBlueprintName;
        invalidblueprint = false;
        setChanged();
        sync();
        return true;
    }

    public String getBlueprint(){
        return blueprint;
    }

    private String cleanBlueprintName(String val) {
        if (val == null) {
            return null;
        }

        String cleaned = val.trim();
        if (cleaned.isEmpty()) {
            return null;
        }

        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64);
        }

        return cleaned;
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
        tag.putBoolean("InvalidBlueprint", invalidblueprint);
        tag.putBoolean("RenderingOutline", renderingoutline);
        tag.putBoolean("ShouldRenderBlueprint", shouldrenderblueprint);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag == null) return;
        if (tag.contains("Blueprint")){
            blueprint = tag.getString("Blueprint");
        }
        invalidblueprint = tag.getBoolean("InvalidBlueprint");
        renderingoutline = tag.getBoolean("RenderingOutline");
        shouldrenderblueprint = tag.getBoolean("ShouldRenderBlueprint");
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
    protected void interactableNoGui(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        return;
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
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return getUpdateTag();
    }
}
