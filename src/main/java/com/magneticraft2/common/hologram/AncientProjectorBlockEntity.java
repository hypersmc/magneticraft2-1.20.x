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
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class AncientProjectorBlockEntity extends BlockEntity {
    public static final ResourceLocation DEFAULT_RECORDING =
            HologramRecordingCatalog.DEFAULT_RECORDING;
    private ResourceLocation recording = DEFAULT_RECORDING;
    /**
     * The exact original artifact metadata (display name, custom lore, etc.).
     * Recording itself is separately validated and always authoritative.
     */
    private CompoundTag artifactItemTag = new CompoundTag();
    /** Latches the datapack definition at placement and is synced to clients. */
    private int durationTicks = 900;
    private int renderRadiusBlocks = HologramRecordingCatalog.DEFAULT_RENDER_RADIUS;
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
    public int renderRadiusBlocks() { return renderRadiusBlocks; }

    /**
     * Forge 1.20.1's frustum culling uses the BlockEntity bounds, not a
     * getRenderBoundingBox method on the BER. The physical artifact is small
     * but its hologram can extend many blocks into the world.
     */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(renderRadiusBlocks);
    }

    /**
     * The item permanently owns its recording. A new placement resolves the
     * server datapack's duration once; a /reload cannot shorten an active film.
     */
    public void setRecording(ResourceLocation id) {
        if (level == null || level.isClientSide || startTime >= 0L) return;
        recording = id == null ? DEFAULT_RECORDING : id;
        HologramRecordingCatalog.RecordingInfo metadata =
                HologramRecordingCatalog.getOrFallback(recording);
        durationTicks = metadata.durationTicks();
        renderRadiusBlocks = metadata.renderRadiusBlocks();
        setChanged();
        sync();
    }

    /**
     * Capture the player's actual collectible artifact before BlockItem
     * consumes it. The same item tags are handed back after playback or
     * early removal, including anvil names and pack-defined lore.
     */
    public void setArtifactItem(ItemStack source) {
        if (source == null || source.isEmpty() || level == null
                || level.isClientSide || startTime >= 0L || itemReturned) return;
        if (!source.is(ItemRegistry.ITEM_ANCIENT_PROJECTOR.get())) return;

        artifactItemTag = source.hasTag()
                ? source.getTag().copy() : new CompoundTag();
        ResourceLocation id = DEFAULT_RECORDING;
        if (artifactItemTag.contains("Recording", 8)) {
            ResourceLocation parsed = ResourceLocation.tryParse(
                    artifactItemTag.getString("Recording"));
            if (parsed != null) id = parsed;
        }
        // Resolve server duration, render bounds, and notify all clients once.
        setRecording(id);
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
        result.setTag(artifactItemTag.copy());
        // Item metadata is preserved, but no custom tag can override the
        // permanent archive selected by this block entity.
        result.getOrCreateTag().putString("Recording", recording.toString());
        return result;
    }

    /**
     * Client-only playback clock. Sound scheduling must never run from the
     * renderer, which can draw several times per tick and is frustum culled.
     * The client class is resolved only when this client-side ticker runs.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  AncientProjectorBlockEntity entity) {
        if (level.isClientSide) {
            com.magneticraft2.client.hologram.HologramSoundPlayback.tick(entity);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AncientProjectorBlockEntity entity) {
        if (entity.itemReturned) return;
        if (entity.startTime < 0L) {
            entity.startTime = level.getGameTime();
            // Plain creative /give items have no Recording NBT. Resolve their
            // default archive before the first playback tick.
            // Resolve both the timeline and culling bounds on the first
            // server tick, including creative projectors without NBT.
            HologramRecordingCatalog.RecordingInfo metadata =
                    HologramRecordingCatalog.getOrFallback(entity.recording);
            entity.durationTicks = metadata.durationTicks();
            entity.renderRadiusBlocks = metadata.renderRadiusBlocks();
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
        tag.put("ArtifactItemTag", artifactItemTag.copy());
        tag.putLong("StartTime", startTime);
        tag.putInt("DurationTicks", durationTicks);
        tag.putInt("RenderRadiusBlocks", renderRadiusBlocks);
        tag.putBoolean("ReturnedArtifact", itemReturned);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Recording"));
        recording = id == null ? DEFAULT_RECORDING : id;
        artifactItemTag = tag.contains("ArtifactItemTag", 10)
                ? tag.getCompound("ArtifactItemTag").copy() : new CompoundTag();
        startTime = tag.contains("StartTime") ? tag.getLong("StartTime") : -1L;
        // Older worlds have only StartTime; their legacy recording was 900
        // ticks. Invalid saved values are never allowed to keep a BE alive
        // indefinitely or remove it immediately.
        int savedDuration = tag.contains("DurationTicks", 3)
                ? tag.getInt("DurationTicks") : 900;
        durationTicks = savedDuration >= 1 && savedDuration <= 12000
                ? savedDuration : 900;
        int savedRadius = tag.contains("RenderRadiusBlocks", 3)
                ? tag.getInt("RenderRadiusBlocks")
                : HologramRecordingCatalog.DEFAULT_RENDER_RADIUS;
        renderRadiusBlocks = savedRadius >= 1 && savedRadius <= 64
                ? savedRadius : HologramRecordingCatalog.DEFAULT_RENDER_RADIUS;
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
