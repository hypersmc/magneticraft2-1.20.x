package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.utils.MultiBlockProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-block multiblock proxy. V2 parts are chunk-baked static models, not BERs.
 * Visual IDs and collision snapshots survive independently of the controller's
 * chunk, so a partially loaded large multiblock does not become a missing mesh
 * or a full-cube phantom collision.
 */
public class Multiblockfiller_tile extends BlockEntity {
    private int Master_X;
    private int Master_Y;
    private int Master_Z;

    private String renderPart = "";
    @Nullable private ModelData modelDataCache;

    // Legacy machines still lazily obtain their shape from the controller.
    // V2 uses a persisted local snapshot to survive controller chunk unload.
    private boolean hasLocalShapeSnapshot;
    @Nullable private VoxelShape cachedLocalShape;

    public Multiblockfiller_tile(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.multiblockfillerBlockEntity.get(), pos, state);
    }

    /**
     * Called once at formation, server side. Only pre-baked part names are sent
     * to the client: geometry never travels in packets and is never rebuilt
     * every frame. Collision is stored per local block for chunk independence.
     */
    public void setV2ModelPart(String name, @NotNull VoxelShape localCollision) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Multiblock V2 part ID cannot be empty");
        }
        renderPart = name;
        cachedLocalShape = localCollision.optimize();
        hasLocalShapeSnapshot = true;
        modelDataCache = null;
        sync();
    }

    @Override
    public @NotNull ModelData getModelData() {
        if (modelDataCache == null) {
            modelDataCache = renderPart.isEmpty()
                    ? ModelData.EMPTY
                    : ModelData.builder()
                        .with(MultiBlockProperties.MODEL_NAME, renderPart)
                        .build();
        }
        return modelDataCache;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) {
            handleUpdateTag(packet.getTag());
        }
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
        setChanged();
        requestModelDataUpdate();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return getUpdateTag();
    }

    public BlockPos getControllerPos() {
        return new BlockPos(Master_X, Master_Y, Master_Z);
    }

    @Nullable
    public VoxelShape getCachedLocalShape() {
        return cachedLocalShape;
    }

    public void setCachedLocalShape(@Nullable VoxelShape shape) {
        cachedLocalShape = shape;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        Master_X = tag.getInt("controller_x");
        Master_Y = tag.getInt("controller_y");
        Master_Z = tag.getInt("controller_z");
        renderPart = tag.getString("render_part");
        modelDataCache = null;

        hasLocalShapeSnapshot = tag.contains("local_collision", Tag.TAG_LIST);
        cachedLocalShape = null;
        if (hasLocalShapeSnapshot) {
            VoxelShape shape = Shapes.empty();
            ListTag list = tag.getList("local_collision", Tag.TAG_COMPOUND);
            for (int index = 0; index < list.size(); index++) {
                CompoundTag b = list.getCompound(index);
                double minX = b.getDouble("minX"), minY = b.getDouble("minY"), minZ = b.getDouble("minZ");
                double maxX = b.getDouble("maxX"), maxY = b.getDouble("maxY"), maxZ = b.getDouble("maxZ");
                if (maxX > minX && maxY > minY && maxZ > minZ) {
                    shape = Shapes.or(shape, Shapes.create(new AABB(minX, minY, minZ, maxX, maxY, maxZ)));
                }
            }
            cachedLocalShape = shape.optimize();
        }
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("controller_x", Master_X);
        tag.putInt("controller_y", Master_Y);
        tag.putInt("controller_z", Master_Z);
        if (!renderPart.isEmpty()) {
            tag.putString("render_part", renderPart);
        }
        if (hasLocalShapeSnapshot && cachedLocalShape != null) {
            ListTag boxes = new ListTag();
            for (AABB aabb : cachedLocalShape.toAabbs()) {
                CompoundTag b = new CompoundTag();
                b.putDouble("minX", aabb.minX);
                b.putDouble("minY", aabb.minY);
                b.putDouble("minZ", aabb.minZ);
                b.putDouble("maxX", aabb.maxX);
                b.putDouble("maxY", aabb.maxY);
                b.putDouble("maxZ", aabb.maxZ);
                boxes.add(b);
            }
            tag.put("local_collision", boxes);
        }
    }
}
