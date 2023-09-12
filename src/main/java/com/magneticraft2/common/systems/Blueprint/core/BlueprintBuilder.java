package com.magneticraft2.common.systems.Blueprint.core;

import com.magneticraft2.common.systems.Blueprint.json.Blueprint;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.*;

/**
 * @author JumpWatch on 25-08-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BlueprintBuilder {
    private static final String BLOCK_KEY_PREFIX = "BlockKey"; // Use your preferred prefix
    public static Blueprint buildBlueprint(String blueprintName, Level world, BlockPos corner1, BlockPos corner2) {
        // Determine dimensions based on the two corners
        int minX = Math.min(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        int[] dimensions = new int[]{width, height, depth};

        // Build the layout based on the blocks between the corners
        Map<String, List<List<String>>> layout = new LinkedHashMap<>(); // Use a LinkedHashMap to maintain insertion order
        Map<String, Block> blocks = new HashMap<>();

        for (int y = minY; y <= maxY; y++) {
            List<List<String>> layer = new ArrayList<>();
            for (int z = minZ; z <= maxZ; z++) {
                List<String> row = new ArrayList<>();
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    String blockKey = getBlockKey(blocks, block);
                    row.add(blockKey);
                }
                layer.add(row);
            }
            layout.put("layer" + (y - minY + 1), layer);
        }

        // Adjust the layout to match the example JSON layout
        return new Blueprint(
                blueprintName,
                new BlueprintStructure(dimensions, layout, blocks),
                blocks
        );
    }
    private static String getBlockKey(Map<String, Block> blockMap, Block block) {
        for (Map.Entry<String, Block> entry : blockMap.entrySet()) {
            if (entry.getValue() == block) {
                return entry.getKey();
            }
        }

        // Check if a key already exists for the block
        String existingKey = getKeyForBlock(blockMap, block);
        if (existingKey != null) {
            return existingKey;
        }

        String newKey = BLOCK_KEY_PREFIX + (blockMap.size() + 1); // Create a new key for the block
        blockMap.put(newKey, block);
        return newKey;
    }
    private static String getKeyForBlock(Map<String, Block> blockMap, Block block) {
        for (Map.Entry<String, Block> entry : blockMap.entrySet()) {
            if (entry.getValue().equals(block)) {
                return entry.getKey();
            }
        }
        return null;
    }

}
