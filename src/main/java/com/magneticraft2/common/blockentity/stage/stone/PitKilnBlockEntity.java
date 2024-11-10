package com.magneticraft2.common.blockentity.stage.stone;

import com.magneticraft2.common.block.stage.stone.PitKilnBlock;
import com.magneticraft2.common.registry.FinalRegistry;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
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
 * v1.0.0
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
        level.sendBlockUpdated( worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL );
        CompoundTag tag = super.getUpdateTag();
        loadClientData(tag);
        return null;
    }
    private void loadClientData(CompoundTag tag) {
        tag.putBoolean("IsBurning", isBurning);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("TotalTime", totalTime);

        // Save the clay items to NBT
        tag.put("inv", itemHandler.serializeNBT());
    }



    public void activate(BlockState state, Level world, BlockPos pos) {
        // Dear maintainer:
        // Once you are done trying to 'optimize' this,
        // and have realized what a terrible mistake that was,
        // please increment the following counter as a warning to the next person:
        // total_hours_wasted_here = 18
        this.level = world;
        // Check if there are enough items in the kiln's inventory to start the firing process
        PitKilnBlockEntity blockEntity = (PitKilnBlockEntity) world.getBlockEntity(pos);
        LazyOptional<IItemHandler> optionalHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
        IItemHandler itemHandler = optionalHandler.orElse(null);
        if (itemHandler.getStackInSlot(0).getItem() == Items.OAK_LOG && itemHandler.getStackInSlot(0).getCount() == 8 && itemHandler.getStackInSlot(1).getItem() == Items.WHEAT&& itemHandler.getStackInSlot(1).getCount() == 4) {
            // Remove the logs and hay from the kiln's inventory
            itemHandler.extractItem(0, 8, false);
            itemHandler.extractItem(1, 4, false);

            // Start the firing process
            BlockState currentState = level.getBlockState(pos);
            BlockState newState = currentState.setValue(PitKilnBlock.LOG_COUNT, getLogCount()).setValue(PitKilnBlock.WHEAT_COUNT, getWheatCount()).setValue(PitKilnBlock.ACTIVATED, true);
            level.setBlock(pos, newState, 3);
            isBurning = true;
            burnTime = Magneticraft2ConfigCommon.GENERAL.PitKilnTime.get();
            world.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
            setChanged();
        }
    }
    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        PitKilnBlockEntity entity = (PitKilnBlockEntity) e.getLevel().getBlockEntity(pos);
        if (!level.isClientSide()) {
            entity.sync();
            if (entity.isBurning) {
                // Decrease the burn time and increase the total time
                if (entity.burnTime > 0) {
                    if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()){
                        LOGGER.info(entity.burnTime);
                    }
                    entity.burnTime--;
                }
                entity.totalTime++;
                if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()){
                    LOGGER.info(entity.totalTime);
                }

                // Update the fire and smoke based on the burn time
                if (entity.burnTime >= 0 && entity.totalTime <= Magneticraft2ConfigCommon.GENERAL.PitKilnTime.get() + 2) {
                    BlockPos upPos = pos.above();
                    BlockState upState = level.getBlockState(upPos);

                    if (upState.getBlock() == Blocks.FIRE) {
                        level.setBlockAndUpdate(upPos, Blocks.FIRE.defaultBlockState());
                    }
                    if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                        LOGGER.info("finished");
                    }
                    SoundEvent soundEvent = SoundEvents.FIRE_EXTINGUISH;
                    level.playSound(null, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);

                    // Wait for the sound to finish playing
                    level.getBlockTicks().willTickThisTick(pos, level.getBlockState(pos).getBlock());

                    // Continue with the code execution after the sound has finished playing
                    return;
                }

                // Check if the firing process is complete
                if (entity.totalTime >= Magneticraft2ConfigCommon.GENERAL.PitKilnTime.get() + 2) {
                    if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                        LOGGER.info("finished2");
                    }

                    BlockPos upPos = pos.above();
                    BlockState upState = level.getBlockState(upPos);
                    if (upState.getBlock() == Blocks.FIRE) {
                        level.setBlockAndUpdate(upPos, Blocks.AIR.defaultBlockState());
                    }
                    entity.burnTime = 0;
                    entity.totalTime = 0;
                    entity.isBurning = false;
                    BlockState currentState = level.getBlockState(pos);
                    BlockState newState = currentState.setValue(PitKilnBlock.LOG_COUNT, entity.getLogCount()).setValue(PitKilnBlock.WHEAT_COUNT, entity.getWheatCount()).setValue(PitKilnBlock.ACTIVATED, false);
                    level.setBlock(pos, newState, 3);
                    //Convert clay to ceramic
                    for (int i = 2; i <= 5; i++) {
                        if (!entity.itemHandler.getStackInSlot(i).isEmpty()) {
                            ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), entity.convertClayToCeramic(entity.itemHandler.getStackInSlot(i)).getItem().getDefaultInstance());
                            level.addFreshEntity(itemEntity);
                            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                                LOGGER.info("Item that should have been dropped: " + entity.convertClayToCeramic(entity.itemHandler.getStackInSlot(i)));
                            }
                            entity.itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                            if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
                                LOGGER.info("ran for slot: " + i);
                            }
                        }
                    }
                    if (entity.itemHandler.getStackInSlot(2).isEmpty() && entity.itemHandler.getStackInSlot(3).isEmpty() && entity.itemHandler.getStackInSlot(4).isEmpty() && entity.itemHandler.getStackInSlot(5).isEmpty()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            if (level.getBlockState(pos).getBlock() == BlockRegistry.PitKilnblock.get()) {
                BlockState currentState = level.getBlockState(pos);
                BlockState newState = currentState.setValue(PitKilnBlock.LOG_COUNT, entity.getLogCount()).setValue(PitKilnBlock.WHEAT_COUNT, entity.getWheatCount());
                level.setBlock(pos, newState, 3);
            }
        }
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
                setChanged();
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
        if (itemStack.getItem().equals(ItemRegistry.item_clay_pot.get().asItem())) {
            return ItemRegistry.item_ceramic_pot.get().getDefaultInstance();
        }
        return Items.AIR.getDefaultInstance();
    }
}
