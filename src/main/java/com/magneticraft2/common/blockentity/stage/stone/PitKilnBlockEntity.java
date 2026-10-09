package com.magneticraft2.common.blockentity.stage.stone;

import com.magneticraft2.common.block.stage.stone.PitKilnBlock;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * @author JumpWatch on 01-07-2023
 * @Project mgc2-1.20
* @version 1.0.0
 */
public class PitKilnBlockEntity extends BlockEntity {

    private boolean isBurning = false;
    private int burnTime = 0;
    private static final Logger LOGGER = LogManager.getLogger("Pitkiln");
    private int totalTime = 0;
    public final ItemStackHandler itemHandler = createInv(); //Item
    public final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler); //Creating LazyOptional for Item

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction dir) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return LazyOptional.empty();

    }
    public PitKilnBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.PitKilnblockEntity.get(), pos, state);

    }


    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create( this );
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag( pkt.getTag() );
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        saveAdditional(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(CompoundTag parentNBTTagCompound)
    {
        load(parentNBTTagCompound);
    }
    public CompoundTag sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(),
                    getBlockState(), Block.UPDATE_CLIENTS);
        }
        return getUpdateTag();
    }

    /**
     * Dear maintainer:
     * Once you are done trying to 'optimize' this,
     * and have realized what a terrible mistake that was,
     * please increment the following counter as a warning to the next person:
     * total_hours_wasted_here = 18
     **/
    public boolean activate(BlockState state, Level world, BlockPos pos) {
        if (world.isClientSide || isBurning) {
            return false;
        }

        ItemStack logs = itemHandler.getStackInSlot(0);
        ItemStack wheat = itemHandler.getStackInSlot(1);
        if (!logs.is(ItemTags.LOGS) || logs.getCount() < 8
                || !wheat.is(Items.WHEAT) || wheat.getCount() < 4) {
            return false;
        }

        itemHandler.extractItem(0, 8, false);
        itemHandler.extractItem(1, 4, false);

        isBurning = true;
        totalTime = 0;
        burnTime = Math.max(1, Magneticraft2ConfigCommon.GENERAL.PitKilnTime.get());

        // Keep the loaded logs/straw visually present while the batch fires.
        // The physical vanilla FIRE block is deliberately not placed; it can
        // go out naturally, spread, or grief the blocks surrounding the pit.
        BlockState burningState = state
                .setValue(PitKilnBlock.LOG_COUNT, 8)
                .setValue(PitKilnBlock.WHEAT_COUNT, 4)
                .setValue(PitKilnBlock.ACTIVATED, true);
        world.setBlock(pos, burningState, Block.UPDATE_ALL);
        world.playSound(null, pos, SoundEvents.FIRE_AMBIENT,
                SoundSource.BLOCKS, 0.8F, 1.0F);
        sync();
        return true;
    }

    /**
     * Handles the server tick logic for the {@link PitKilnBlockEntity}. This method is called periodically to update
     * the state of the Pit Kiln, manage the burning process, update block states, and manage item transformations
     * when the Pit Kiln has completed its firing process.
     *
     * @param level The current level where the block entity is located.
     * @param pos The position of the block entity in the level.
     * @param estate The current block state of the block entity.
     * @param e The block entity being ticked, expected to be an instance of {@link PitKilnBlockEntity}.
     */
    public static <E extends BlockEntity> void serverTick(
            Level level, BlockPos pos, BlockState state, E blockEntity) {
        if (level.isClientSide || !(blockEntity instanceof PitKilnBlockEntity kiln)
                || !state.is(BlockRegistry.PitKilnblock.get())) {
            return;
        }

        if (!kiln.isBurning) {
            // Inventory-related model changes only need a block state update
            // when the actual wheat/log counts have changed.
            int logs = Math.min(8, kiln.getLogCount());
            int wheat = Math.min(4, kiln.getWheatCount());
            if (state.getValue(PitKilnBlock.LOG_COUNT) != logs
                    || state.getValue(PitKilnBlock.WHEAT_COUNT) != wheat
                    || state.getValue(PitKilnBlock.ACTIVATED)) {
                level.setBlock(pos, state
                        .setValue(PitKilnBlock.LOG_COUNT, logs)
                        .setValue(PitKilnBlock.WHEAT_COUNT, wheat)
                        .setValue(PitKilnBlock.ACTIVATED, false),
                        Block.UPDATE_ALL);
            }
            return;
        }

        kiln.totalTime++;
        kiln.burnTime--;
        kiln.setChanged();

        if (kiln.burnTime > 0) {
            // Occasional crackle, not an extinguish sound EVERY server tick.
            if (kiln.totalTime % 100 == 0) {
                level.playSound(null, pos, SoundEvents.FIRE_AMBIENT,
                        SoundSource.BLOCKS, 0.55F,
                        0.9F + level.random.nextFloat() * 0.2F);
            }
            return;
        }

        kiln.isBurning = false;
        kiln.burnTime = 0;
        kiln.totalTime = 0;

        // Finish exactly once and preserve each input's stack count.
        for (int slot = 2; slot <= 5; slot++) {
            ItemStack contents = kiln.itemHandler.getStackInSlot(slot);
            if (contents.isEmpty()) {
                continue;
            }

            ItemStack result = kiln.convertClayToCeramic(contents);
            if (result.isEmpty()) {
                result = contents.copy(); // Do not destroy unexpected contents.
            } else {
                result.setCount(contents.getCount());
            }
            Block.popResource(level, pos.above(), result);
            kiln.itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }

        // Clean up an old-version fire if one is still present above the kiln.
        if (level.getBlockState(pos.above()).is(Blocks.FIRE)) {
            level.removeBlock(pos.above(), false);
        }

        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS, 0.9F, 1.0F);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isBurning = tag.getBoolean("IsBurning");
        burnTime = tag.getInt("BurnTime");
        totalTime = tag.getInt("TotalTime");
        itemHandler.deserializeNBT(tag.getCompound("inv"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putBoolean("IsBurning", isBurning);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("TotalTime", totalTime);

        // Save the clay items to NBT
        tag.put("inv", itemHandler.serializeNBT());
    }




    private ItemStackHandler createInv() {
        return new ItemStackHandler(6) {

            @Override
            protected void onContentsChanged(int slot) {
                sync(); // Inventory changes must reach the client without per-tick spam.
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return super.isItemValid(slot, stack);
            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                return super.insertItem(slot, stack, simulate);
            }

        };

    }
    public int getLogCount() {
        return itemHandler.getStackInSlot(0).getCount();
    }

    public int getWheatCount() {
        return itemHandler.getStackInSlot(1).getCount();
    }

    /**
     * Since I didn't bother doing a recipe handler
     */
    private ItemStack convertClayToCeramic(ItemStack itemStack){
        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            LOGGER.info(itemStack.getItem());
        }
        if (itemStack.is(ItemRegistry.item_clay_pot.get())) {
            return ItemRegistry.item_ceramic_pot.get().getDefaultInstance();
        }
        if (itemStack.is(Items.CLAY_BALL)) {
            return Items.BRICK.getDefaultInstance();
        }
        return Items.AIR.getDefaultInstance();
    }
}
