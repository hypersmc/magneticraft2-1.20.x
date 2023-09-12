package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 11-09-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class testmultiblock extends BaseBlockEntityMagneticraft2{
    public testmultiblock(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.testmultiblock.get(), pos, state);
    }

    @Override
    protected MultiblockController createMultiblockController() {
        MultiblockStructure structure = identifyMultiblockStructure(level, worldPosition);
        if (structure != null) {
            LOGGER.info("structure found!");
            return new MultiblockController(structure);
        } else {
            LOGGER.info("structure NOT found!");
            return null;
        }
    }


    @Override
    protected MultiblockStructure identifyMultiblockStructure(Level world, BlockPos pos) {
        for (Multiblock multiblock : MultiblockRegistry.getRegisteredMultiblocks().values()) {
            MultiblockStructure structure = multiblock.getStructure();
            if (matchesStructure(world, pos, structure)) {
                LOGGER.info("Found multiblock: "+ multiblock.getName());
                return structure;
            }
        }
        return null;
    }


    private boolean matchesStructure(Level world, BlockPos pos, MultiblockStructure structure) {
        Map<String, List<List<String>>> layout = structure.getLayout();
        Map<String, Block> blocks = structure.getBlocks();
        int[] dimensions = structure.getDimensions();

        for (int y = 0; y < dimensions[1]; y++) {
            List<List<String>> layer = layout.get("layer" + (y + 1));
            for (int z = 0; z < dimensions[2]; z++) {
                for (int x = 0; x < dimensions[0]; x++) {
                    String blockKey = layer.get(z).get(x);
                    if (!blockKey.equals(" ")) {
                        Block expectedBlock = blocks.get(blockKey);
                        LOGGER.info("uhhhh: " + pos.offset(x, y, z));
                        BlockState stateInWorld = world.getBlockState(pos.offset(x, y, z));
                        LOGGER.info("Coordinates X: " + x + " Y: " + y + " Z: " + z );
                        LOGGER.info("expectedBlock: " + expectedBlock);
                        LOGGER.info("block in world: " + stateInWorld);
                        if (!stateInWorld.is(expectedBlock)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public int capacityE() {
        return 0;
    }

    @Override
    public int maxtransferE() {
        return 0;
    }

    @Override
    public int capacityH() {
        return 0;
    }

    @Override
    public int maxtransferH() {
        return 0;
    }

    @Override
    public int capacityW() {
        return 0;
    }

    @Override
    public int maxtransferW() {
        return 0;
    }

    @Override
    public int capacityF() {
        return 0;
    }

    @Override
    public int tanks() {
        return 0;
    }

    @Override
    public int invsize() {
        return 0;
    }

    @Override
    public int capacityP() {
        return 0;
    }

    @Override
    public int maxtransferP() {
        return 0;
    }

    @Override
    public boolean itemcape() {
        return false;
    }

    @Override
    public boolean energycape() {
        return false;
    }

    @Override
    public boolean heatcape() {
        return false;
    }

    @Override
    public boolean wattcape() {
        return false;
    }

    @Override
    public boolean fluidcape() {
        return false;
    }

    @Override
    public boolean pressurecape() {
        return false;
    }

    @Override
    public boolean HeatCanReceive() {
        return false;
    }

    @Override
    public boolean HeatCanSend() {
        return false;
    }

    @Override
    public boolean WattCanReceive() {
        return false;
    }

    @Override
    public boolean WattCanSend() {
        return false;
    }

    @Override
    public boolean EnergyCanReceive() {
        return false;
    }

    @Override
    public boolean EnergyCanSend() {
        return false;
    }

    @Override
    public boolean PressureCanReceive() {
        return false;
    }

    @Override
    public boolean PressureCanSend() {
        return false;
    }

    @Override
    public Level getThisWorld() {
        return level;
    }

    @Override
    public BlockPos getThisPosition() {
        return worldPosition;
    }

    @Override
    public CompoundTag sync() {
        return null;
    }

    @Override
    public Component getDisplayName() {
        return null;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return null;
    }
}
