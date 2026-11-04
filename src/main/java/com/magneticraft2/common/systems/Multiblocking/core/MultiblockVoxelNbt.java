package com.magneticraft2.common.systems.Multiblocking.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Small bounded NBT format shared by V2 controller and filler shapes. */
public final class MultiblockVoxelNbt {
    private static final int MAX_BOXES = 512;

    private MultiblockVoxelNbt() {}

    public static void write(CompoundTag tag, String key, @Nullable VoxelShape shape) {
        if (shape == null) {
            tag.remove(key);
            return;
        }
        ListTag boxes = new ListTag();
        for (AABB box : shape.toAabbs()) {
            if (boxes.size() >= MAX_BOXES) {
                throw new IllegalStateException("V2 voxel shape exceeds " + MAX_BOXES + " boxes: " + key);
            }
            CompoundTag b = new CompoundTag();
            b.putDouble("minX", box.minX);
            b.putDouble("minY", box.minY);
            b.putDouble("minZ", box.minZ);
            b.putDouble("maxX", box.maxX);
            b.putDouble("maxY", box.maxY);
            b.putDouble("maxZ", box.maxZ);
            boxes.add(b);
        }
        tag.put(key, boxes);
    }

    @Nullable
    public static VoxelShape read(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) return null;
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        if (list.size() > MAX_BOXES) return null;
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag b = list.getCompound(i);
            double x0=b.getDouble("minX"), y0=b.getDouble("minY"), z0=b.getDouble("minZ");
            double x1=b.getDouble("maxX"), y1=b.getDouble("maxY"), z1=b.getDouble("maxZ");
            if (!valid(x0)||!valid(y0)||!valid(z0)||!valid(x1)||!valid(y1)||!valid(z1)
                    || x1<=x0||y1<=y0||z1<=z0) return null;
            shape = Shapes.joinUnoptimized(shape, Shapes.create(
                    new AABB(x0,y0,z0,x1,y1,z1)), BooleanOp.OR);
        }
        return shape.optimize();
    }

    private static boolean valid(double d) {
        return Double.isFinite(d) && Math.abs(d) <= 1024D;
    }
}
