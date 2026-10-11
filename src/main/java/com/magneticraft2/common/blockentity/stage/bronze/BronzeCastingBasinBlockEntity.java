package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.item.stage.bronze.BronzePouringLadleItem;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * One mold, one quality-tagged Bronze unit. There are no item/fluid capabilities
 * so neither an arm nor a vanilla pipe can bypass the heat/pour contract.
 *
 * State transitions: EMPTY -> MOLD -> COOLING -> RESULT -> EMPTY.
 * Breaking while cooling refunds the original ingot-equivalent (not a rough
 * casting), and the already-used green sand mold is intentionally consumed.
 */
public final class BronzeCastingBasinBlockEntity extends BlockEntity {
    private static final int GEAR=1, BEARING=2;
    private ItemStack mold=ItemStack.EMPTY;
    private ItemStack sourceBronze=ItemStack.EMPTY;
    private ItemStack result=ItemStack.EMPTY;
    private int castType;
    private int coolingTicks;

    public BronzeCastingBasinBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BRONZE_CASTING_BASIN_BE.get(),pos,state);
    }

    public ItemStack displayMold() { return mold; }
    public ItemStack displayOutput() { return result; }
    public boolean isCooling() { return !sourceBronze.isEmpty(); }
    public int getCoolingTicks() { return coolingTicks; }
    public int getCoolingDuration() {
        return Magneticraft2ConfigCommon.GENERAL.FoundryCastingCoolTicks.get();
    }
    public int getCastType() { return castType; }

    private static int kind(ItemStack stack) {
        if (stack.is(ItemRegistry.ITEM_GEAR_SAND_MOLD.get())) return GEAR;
        if (stack.is(ItemRegistry.ITEM_BEARING_SAND_MOLD.get())) return BEARING;
        return 0;
    }

    public void use(Player player, ItemStack held) {
        if (level == null || level.isClientSide) return;

        if (held.isEmpty()) {
            if (!result.isEmpty()) {
                ItemStack collected=result;
                result=ItemStack.EMPTY;
                castType=0;
                give(player,collected);
                sync();
            } else if (player.isShiftKeyDown() && !mold.isEmpty() && !isCooling()) {
                ItemStack recovered=mold;
                mold=ItemStack.EMPTY;
                give(player,recovered);
                sync();
            } else {
                player.displayClientMessage(Component.translatable(
                        isCooling() ? "message.magneticraft2.basin.cooling"
                                : (mold.isEmpty() ? "message.magneticraft2.basin.need_mold"
                                                  : "message.magneticraft2.basin.ready"),
                        coolingTicks),true);
            }
            return;
        }

        int newKind=kind(held);
        if (newKind!=0) {
            if (!mold.isEmpty() || isCooling() || !result.isEmpty()) {
                message(player,"busy");
                return;
            }
            mold=held.copy();
            mold.setCount(1);
            if (!player.getAbilities().instabuild) held.shrink(1);
            sync();
            message(player,"mold_added");
            return;
        }

        if (held.is(ItemRegistry.ITEM_BRONZE_POURING_LADLE.get())) {
            // Check every receiver precondition BEFORE altering the ladle.
            if (mold.isEmpty() || isCooling() || !result.isEmpty()) {
                message(player,"need_mold");
                return;
            }
            if (!BronzePouringLadleItem.isHot(held,level)) {
                message(player,"need_hot_ladle");
                return;
            }
            int shape=kind(mold);
            if (shape==0) { message(player,"need_mold"); return; }

            // Exactly ONE hot ingot-equivalent is removed from the player's
            // ladle and installed into the saved basin state, on the server.
            ItemStack transferred=BronzePouringLadleItem.takeHot(held,level);
            if (transferred.isEmpty()) { message(player,"need_hot_ladle"); return; }
            player.getInventory().setChanged();

            sourceBronze=transferred;
            mold=ItemStack.EMPTY;
            castType=shape;
            coolingTicks=Math.max(1,getCoolingDuration());
            sync();
            level.playSound(null,worldPosition,SoundEvents.LAVA_EXTINGUISH,
                    SoundSource.BLOCKS,.7F,1.2F);
            message(player,"poured");
            return;
        }
        message(player,"invalid");
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state,
                                  BronzeCastingBasinBlockEntity basin) {
        if (world.isClientSide || basin.sourceBronze.isEmpty()) return;
        if (basin.coolingTicks>0) basin.coolingTicks--;
        if (basin.coolingTicks==0) {
            ItemStack created=new ItemStack(basin.castType==GEAR
                    ? ItemRegistry.ITEM_ROUGH_BRONZE_GEAR.get()
                    : ItemRegistry.ITEM_ROUGH_BRONZE_BEARING.get());
            // Exact original input NBT survives the foundry, ladle and cast.
            if (basin.sourceBronze.hasTag()) {
                created.setTag(basin.sourceBronze.getTag().copy());
            }
            basin.result=created;
            basin.sourceBronze=ItemStack.EMPTY;
            basin.sync();
            world.playSound(null,pos,SoundEvents.ANVIL_LAND,
                    SoundSource.BLOCKS,.35F,1.5F);
        } else {
            basin.setChanged();
            if (world.getGameTime()%20L==0) basin.sync();
        }
    }

    private static void give(Player player,ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack))
            player.drop(stack,false);
    }

    private static void message(Player player,String key) {
        player.displayClientMessage(Component.translatable(
                "message.magneticraft2.basin."+key),true);
    }

    public void dropContents() {
        if (level==null || level.isClientSide) return;
        ItemStack a=mold,b=result,c=sourceBronze;
        mold=ItemStack.EMPTY;
        result=ItemStack.EMPTY;
        sourceBronze=ItemStack.EMPTY;
        castType=0;
        coolingTicks=0;
        // The consumed mold is not refunded when poured; the bronze itself
        // is always returned if the incomplete basin is dismantled.
        if (!a.isEmpty()) Block.popResource(level,worldPosition,a);
        if (!b.isEmpty()) Block.popResource(level,worldPosition,b);
        if (!c.isEmpty()) Block.popResource(level,worldPosition,c);
        setChanged();
    }

    private void sync() {
        setChanged();
        if (level!=null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mold=ItemStack.of(tag.getCompound("Mold"));
        sourceBronze=ItemStack.of(tag.getCompound("OriginalBronze"));
        result=ItemStack.of(tag.getCompound("RoughCasting"));
        castType=Math.max(0,Math.min(2,tag.getInt("CastingShape")));
        coolingTicks=Math.max(0,tag.getInt("CoolingTicks"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!mold.isEmpty())tag.put("Mold",mold.save(new CompoundTag()));
        if (!sourceBronze.isEmpty())tag.put("OriginalBronze",sourceBronze.save(new CompoundTag()));
        if (!result.isEmpty())tag.put("RoughCasting",result.save(new CompoundTag()));
        tag.putInt("CastingShape",castType);
        tag.putInt("CoolingTicks",coolingTicks);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag=new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag()!=null) load(packet.getTag());
    }
}
