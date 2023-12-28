package com.magneticraft2.common.systems.Multiblocking.core;

import com.magneticraft2.common.block.general.testmultiblockblock;
import com.magneticraft2.common.block.general.testpowermoduleblock;
import com.magneticraft2.common.blockentity.general.testpowermodule;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author JumpWatch on 11-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiblockController {
    public static final Logger LOGGER = LogManager.getLogger("MGCMultiblockController");

    private final MultiblockStructure structure;
    private final Map<BlockPos, BlockState> blockMap;
    private BlockPos masterPos;
    private final Map<String, MultiblockModule> modules;

    public MultiblockController(MultiblockStructure structure) {
        this.structure = structure;
        this.blockMap = new HashMap<>();
        this.masterPos = null;
        this.modules = new HashMap<>();
    }

    public boolean isValidStructure(Level world, BlockPos pos) {
        masterPos = pos;

        // Initialize modules
        modules.clear();
        for (IMultiblockModule module : structure.getModules()) {
            String moduleKey = module.getModuleKey();
            BlockPos modulePos = module.getModuleOffset();
            if (module.isValid(world, modulePos)) {
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                    LOGGER.info("module pos: " + modulePos);
                    LOGGER.info("structure: " + structure);
                    LOGGER.info("module key: " + moduleKey);
                }
                modules.put(moduleKey, new MultiblockModule(structure, moduleKey, modulePos));
            } else {
                return false;
            }
        }

        return true;
    }
    public boolean createStructure(Level world, BlockPos pos) {
        if (!isValidStructure(world, pos)) {
            return false;
        }

        // Create the structure at the given position
        for (Map.Entry<BlockPos, BlockState> entry : blockMap.entrySet()) {
            BlockPos offsetPos = entry.getKey();
            BlockPos placePos = pos.offset(offsetPos);
            world.setBlock(placePos, entry.getValue(), 2);
        }

        // Enable modules
        for (IMultiblockModule module : modules.values()) {
            BlockPos modulePos = module.getModuleOffset();
            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                LOGGER.info("activating: " + module.getModuleKey());
                LOGGER.info("Module pos" + modulePos);
                LOGGER.info("Modules: " + modules.values());
                LOGGER.info("Module instance: " + module); // Log the module instance
                LOGGER.info("Entering the loop to activate modules"); // Log the entry to the loop
                LOGGER.info("Module class: " + module.getClass().getName());
            }
            module.onActivate(world, modulePos);
        }

        LOGGER.info("Multiblock system got this far!"); // At this point we have identified the structure, found modules and activated them
        return true;
    }
    public boolean identifyAndAddModules(Level world, BlockPos pos, @NotNull MultiblockStructure structure) {
        int[] dimensions = structure.getDimensions();
        Block controllerBlock = world.getBlockState(pos).getBlock();
        BlockPos controllerOffset = findControllerOffset(structure, controllerBlock);
        BlockPos adjustedPos = pos.offset(-controllerOffset.getX(), -controllerOffset.getY(), -controllerOffset.getZ());

        for (int y = 0; y < dimensions[1]; y++) {
            for (int z = 0; z < dimensions[2]; z++) {
                for (int x = 0; x < dimensions[0]; x++) {
                    BlockPos currentPos = adjustedPos.offset(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(currentPos);

                    if (blockEntity instanceof IMultiblockModule) {
                        LOGGER.info("Module at: " + currentPos);
                        IMultiblockModule module = (IMultiblockModule) blockEntity;
                        String moduleKey = module.getModuleKey();
                        // Assuming MultiblockModule is the correct type to add to the modules map
                        structure.addModule(moduleKey, new MultiblockModule(structure, moduleKey, currentPos));
                    }
                }
            }
        }

        return true; // Return true if modules are identified and added successfully, false otherwise
    }

    public BlockPos findControllerOffset(MultiblockStructure structure, Block controllerBlock) {
        Map<String, List<List<String>>> layout = structure.getLayout();
        int[] dimensions = structure.getDimensions();
        Map<String, Block> blocks = structure.getBlocks();

        for (int y = 0; y < dimensions[1]; y++) {
            List<List<String>> layer = layout.get("layer" + (y + 1));
            for (int z = 0; z < dimensions[2]; z++) {
                for (int x = 0; x < dimensions[0]; x++) {
                    String blockKey = layer.get(z).get(x);
                    if (blocks.get(blockKey) == controllerBlock) {
                        // Calculate the offset based on the relative position of the controller block in the structure
                        return new BlockPos(x - dimensions[0] / 2 +1, y - dimensions[1] / 2+1, z - dimensions[2] / 2+1);
                    }
                }
            }
        }
        return BlockPos.ZERO; // Default offset (0, 0, 0) if the controller block is not found in the structure
    }

    public boolean destroyStructure(Level world) {
        if (masterPos == null) {
            return false;
        }

        // Disable modules
        for (IMultiblockModule module : modules.values()) {
            module.onDeactivate(world, masterPos);
        }

        // Destroy the structure
        for (Map.Entry<BlockPos, BlockState> entry : blockMap.entrySet()) {
            BlockPos offsetPos = entry.getKey();
            BlockPos destroyPos = masterPos.offset(offsetPos);
            world.setBlock(destroyPos, Blocks.AIR.defaultBlockState(), 2);
        }

        blockMap.clear();
        masterPos = null;

        return true;
    }

    public MultiblockStructure getStructure() {
        return structure;
    }

    public Map<BlockPos, BlockState> getBlockMap() {
        return blockMap;
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }
}
