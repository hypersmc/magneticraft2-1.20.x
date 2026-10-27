package com.magneticraft2.common.hologram;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AncientProjectorBlockEntity extends BlockEntity {
    public static final ResourceLocation DEFAULT_RECORDING =
            new ResourceLocation("magneticraft2", "mechanical_transmission");
    public static final int FIRST_RECORDING_TICKS = 900;
    private ResourceLocation recording = DEFAULT_RECORDING;
    private long startTime = -1L;

    public AncientProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ANCIENT_PROJECTOR_BE.get(), pos, state);
    }

    public ResourceLocation recording() { return recording; }
    public long startTime() { return startTime; }

    public void setRecording(ResourceLocation id) {
        recording = id;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AncientProjectorBlockEntity entity) {
        if (entity.startTime < 0L) {
            entity.startTime = level.getGameTime();
            entity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
        if (level.getGameTime() - entity.startTime >= FIRST_RECORDING_TICKS) {
            // Remove the block without loot drops, then return exactly one artifact.
            ItemStack stack = new ItemStack(ItemRegistry.ITEM_ANCIENT_PROJECTOR.get());
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString("Recording", entity.recording.toString());
            level.removeBlock(pos, false);
            level.addFreshEntity(new ItemEntity(level, pos.getX() + .5, pos.getY() + .3,
                    pos.getZ() + .5, stack));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Recording", recording.toString());
        tag.putLong("StartTime", startTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Recording"));
        recording = id == null ? DEFAULT_RECORDING : id;
        startTime = tag.contains("StartTime") ? tag.getLong("StartTime") : -1L;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }
}
