package com.magneticraft2.common.systems.Multiblocking.core;

import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 10-06-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockModule implements IMultiblockModule {
    private final MultiblockStructure structure;
    private final Map<BlockPos, BlockState> blockMap;
    private final List<BlockPos> modulePositions;
    private final String moduleKey;
    private final BlockPos moduleOffset;

    public MultiblockModule(MultiblockStructure structure, String moduleKey, BlockPos moduleOffset) {
        this.moduleKey = moduleKey;
        this.moduleOffset = moduleOffset;
        this.structure = structure;
        this.blockMap = new HashMap<>();
        this.modulePositions = new ArrayList<>();
    }

    public boolean isValid(Level world, BlockPos pos) {
        // Check if the module is valid at the given position
        Map<BlockPos, BlockState> tempMap = new HashMap<>();
        List<BlockPos> tempPositions = new ArrayList<>();

        for (String layer : structure.getLayout().keySet()) {
            int layerIndex = Integer.parseInt(layer) - 1;
            List<List<String>> rowList = structure.getLayout().get(layer);

            for (int rowIndex = 0; rowIndex < rowList.size(); rowIndex++) {
                String row = rowList.get(rowIndex).toString();

                for (int columnIndex = 0; columnIndex < row.length(); columnIndex++) {
                    String blockId = row.substring(columnIndex, columnIndex + 1);

                    if (blockId.equals(" ")) {
                        continue;
                    }

                    if (!structure.getModules().contains(blockId)) {
                        continue;
                    }

                    BlockPos offsetPos = new BlockPos(columnIndex, layerIndex, rowIndex);
                    BlockPos checkPos = pos.offset(offsetPos);
                    BlockState checkState = world.getBlockState(checkPos);

                    if (checkState.isAir()) {
                        return false;
                    }

                    if (!structure.getBlocks().get(blockId).equals(checkState.getBlock())) {
                        return false;
                    }

                    tempMap.put(offsetPos, checkState);
                    tempPositions.add(offsetPos);
                }
            }
        }

        blockMap.clear();
        blockMap.putAll(tempMap);
        modulePositions.clear();
        modulePositions.addAll(tempPositions);

        return true;
    }

    public void onActivate(Level world, BlockPos pos) {
        // Activate the module at the given position
        // TODO: Implement module activation logic
    }

    public void onDeactivate(Level world, BlockPos pos) {
        // Deactivate the module at the given position
        // TODO: Implement module deactivation logic
    }

    @Override
    public String getModuleKey() {
        return moduleKey;
    }

    @Override
    public BlockPos getModuleOffset() {
        return moduleOffset;
    }

    @Override
    public IMultiblockModule createModule(Level world, BlockPos pos) {
        for (Map.Entry<BlockPos, BlockState> entry : blockMap.entrySet()) {
            BlockPos offsetPos = entry.getKey();
            BlockPos placePos = pos.offset(offsetPos);
            world.setBlock(placePos, entry.getValue(), 2);
        }
        return null;
    }


    public Map<BlockPos, BlockState> getBlockMap() {
        return blockMap;
    }

    public List<BlockPos> getModulePositions() {
        return modulePositions;
    }

    public MultiblockStructure getStructure() {
        return structure;
    }
}