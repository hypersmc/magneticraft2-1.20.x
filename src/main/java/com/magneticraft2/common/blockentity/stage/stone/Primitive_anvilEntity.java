package com.magneticraft2.common.blockentity.stage.stone;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class Primitive_anvilEntity extends BlockEntity {
    public static final int COPPER_PLATE_HITS = 4;

    private ItemStack storedItem = ItemStack.EMPTY;
    private int hammerHits = 0;

    public Primitive_anvilEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityRegistry.Primitive_anvilEntity.get(), pPos, pBlockState);
    }

    public boolean isEmpty() {
        return storedItem.isEmpty();
    }

    public boolean hasCopperIngot() {
        return storedItem.is(Items.COPPER_INGOT);
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public int getHammerHits() {
        return hammerHits;
    }

    public boolean insertOne(ItemStack stack) {
        if (!storedItem.isEmpty() || stack.isEmpty()) {
            return false;
        }

        storedItem = stack.copy();
        storedItem.setCount(1);
        hammerHits = 0;
        sync();
        return true;
    }

    public boolean hammerCopper() {
        if (!hasCopperIngot()) {
            return false;
        }

        hammerHits++;
        boolean completed = hammerHits >= COPPER_PLATE_HITS;
        if (completed) {
            storedItem = new ItemStack(ItemRegistry.ITEM_COPPER_PLATE.get());
            hammerHits = 0;
        }

        sync();
        return completed;
    }

    public ItemStack takeStoredItem() {
        if (storedItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = storedItem;
        storedItem = ItemStack.EMPTY;
        hammerHits = 0;
        sync();
        return result;
    }

    public void dropStoredItem(Level level) {
        if (level.isClientSide || storedItem.isEmpty()) {
            return;
        }

        Block.popResource(level, worldPosition, storedItem.copy());
        storedItem = ItemStack.EMPTY;
        hammerHits = 0;
        setChanged();
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!storedItem.isEmpty()) {
            tag.put("StoredItem", storedItem.save(new CompoundTag()));
        }
        tag.putInt("HammerHits", hammerHits);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedItem = tag.contains("StoredItem") ? ItemStack.of(tag.getCompound("StoredItem")) : ItemStack.EMPTY;
        hammerHits = tag.getInt("HammerHits");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
