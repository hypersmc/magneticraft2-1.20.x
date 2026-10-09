package com.magneticraft2.common.item.stage.stone.tools;

import com.magneticraft2.common.blockentity.stage.stone.PitKilnBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * @author JumpWatch on 14-07-2023
 * @Project mgc2-1.20
 * @version 1.0.0
 */
public class FireStarter extends Item {
    public FireStarter() {
        super(new Properties().stacksTo(1).setNoRepair().defaultDurability(5).durability(5));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 60; // A predictable three-second hold, not a release-time window.
    }

    /**
     * A completed hold ignites once, even if the player keeps the mouse held.
     * Item#releaseUsing only runs when the player INTERRUPTS the action, which
     * made the previous final-tick timing window effectively impossible.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            BlockHitResult hit = getPlayerPOVHitResult(
                    level, player, ClipContext.Fluid.NONE
            );
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hit.getBlockPos();
                BlockPos above = pos.above();
                BlockEntity blockEntity = level.getBlockEntity(pos);
                boolean ignited = false;

                if (blockEntity instanceof PitKilnBlockEntity pitKiln) {
                    ignited = pitKiln.activate(level.getBlockState(pos), level, pos);
                    if (!ignited) {
                        player.displayClientMessage(
                                Component.translatable(
                                        "message.magneticraft2.fire_starter.kiln_not_ready"
                                ), true
                        );
                    }
                } else if (level.isEmptyBlock(above)
                        && Blocks.FIRE.defaultBlockState().canSurvive(level, above)) {
                    level.setBlock(above, Blocks.FIRE.defaultBlockState(), 11);
                    level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                    ignited = true;
                }

                if (ignited && !player.getAbilities().instabuild) {
                    stack.hurtAndBreak(1, player,
                            p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                }
            }
        }
        return stack;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack,
                          int remainingUseDuration) {
        if (level.isClientSide && entity instanceof Player player
                && remainingUseDuration % 4 == 0) {
            BlockHitResult hit = getPlayerPOVHitResult(
                    level, player, ClipContext.Fluid.NONE
            );
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 pos = hit.getLocation();
                makeEffects(level, pos.x, pos.y, pos.z, remainingUseDuration,
                        getUseDuration(stack), level.random);
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        // Begin use on BOTH sides: the client must animate/track the hold too.
        player.startUsingItem(context.getHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                    InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    private void makeEffects(Level world,
                             double x,
                             double y,
                             double z,
                             int timeLeft,
                             int total,
                             RandomSource random) {
        int count = total - timeLeft;
        if (random.nextFloat() + 0.3 < count / (double) total) {
            world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0F, 0.1F, 0.0F);
        }
        if (timeLeft < 10 && random.nextFloat() + 0.3 < count / (double) total) {
            world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0F, 0.1F, 0.0F);
        }
    }
}
