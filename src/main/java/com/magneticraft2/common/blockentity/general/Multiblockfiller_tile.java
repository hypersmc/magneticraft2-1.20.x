package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 01-07-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Multiblockfiller_tile extends BlockEntity {
    private int Master_X;
    private int Master_Y;
    private int Master_Z;
    public Multiblockfiller_tile(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityRegistry.multiblockfillerBlockEntity.get(), pPos, pBlockState);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    public CompoundTag sync() {
        level.sendBlockUpdated( worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL );
        CompoundTag tag = super.getUpdateTag();
        loadClientData(tag);
        return null;
    }
    private void loadClientData(CompoundTag tag) {
        this.Master_X = tag.getInt("controller_x");
        this.Master_Y = tag.getInt("controller_y");
        this.Master_Z = tag.getInt("controller_z");
    }
    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.Master_X = pTag.getInt("controller_x");
        this.Master_Y = pTag.getInt("controller_y");
        this.Master_Z = pTag.getInt("controller_z");
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.putInt("controller_x", this.Master_X);
        pTag.putInt("controller_y", this.Master_Y);
        pTag.putInt("controller_z", this.Master_Z);
    }
}
