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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AncientProjectorBlockEntity extends BlockEntity {
    public static final ResourceLocation DEFAULT_RECORDING =
            HologramRecordingCatalog.DEFAULT_RECORDING;
    private ResourceLocation recording = DEFAULT_RECORDING;
    /** Latches the datapack definition at placement and is synced to clients. */
    private int durationTicks = 900;
    private long startTime = -1L;
    // Removal can be triggered by a break, replacement or normal completion.
    // Only one of those paths is allowed to return the artifact.
    private boolean itemReturned = false;

    public AncientProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ANCIENT_PROJECTOR_BE.get(), pos, state);
    }

    public ResourceLocation recording() { return recording; }
    public long startTime() { return startTime; }
    public int durationTicks() { return durationTicks; }

    /**
     * The item permanently owns its recording. A new placement resolves the
     * server datapack's duration once; a /reload cannot shorten an active film.
     */
    public void setRecording(ResourceLocation id) {
        if (level == null || level.isClientSide || startTime >= 0L) return;
        recording = id == null ? DEFAULT_RECORDING : id;
        durationTicks = HologramRecordingCatalog.getOrFallback(recording).durationTicks();
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    /**
     * Server-only, one-shot transfer of the contained recording back to an
     * item. Called by AncientProjectorBlock.onRemove before the BE is discarded.
     */
    public ItemStack takeArtifact() {
        if (itemReturned || level == null || level.isClientSide) {
            return ItemStack.EMPTY;
        }
        itemReturned = true;
        ItemStack result = new ItemStack(ItemRegistry.ITEM_ANCIENT_PROJECTOR.get());
        result.getOrCreateTag().putString("Recording", recording.toString());
        return result;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AncientProjectorBlockEntity entity) {
        if (entity.itemReturned) return;
        if (entity.startTime < 0L) {
            entity.startTime = level.getGameTime();
            // Plain creative /give items have no Recording NBT. Resolve their
            // default archive before the first playback tick.
            entity.durationTicks = HologramRecordingCatalog
                    .getOrFallback(entity.recording).durationTicks();
            entity.setChanged();
            entity.sync();
        }
        if (level.getGameTime() - entity.startTime >= entity.durationTicks) {
            // onRemove handles the return for BOTH completion and early break.
            // Never also spawn a duplicate from the ticker.
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Recording", recording.toString());
        tag.putLong("StartTime", startTime);
        tag.putInt("DurationTicks", durationTicks);
        tag.putBoolean("ReturnedArtifact", itemReturned);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Recording"));
        recording = id == null ? DEFAULT_RECORDING : id;
        startTime = tag.contains("StartTime") ? tag.getLong("StartTime") : -1L;
        // Older worlds have only StartTime; their legacy recording was 900
        // ticks. Invalid saved values are never allowed to keep a BE alive
        // indefinitely or remove it immediately.
        int savedDuration = tag.contains("DurationTicks", 3)
                ? tag.getInt("DurationTicks") : 900;
        durationTicks = savedDuration >= 1 && savedDuration <= 12000
                ? savedDuration : 900;
        itemReturned = tag.getBoolean("ReturnedArtifact");
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
