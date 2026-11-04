package com.magneticraft2.common.item.stage.copper;

import com.magneticraft2.common.block.stage.copper.DiagonalWaterPipeBlock;
import com.magneticraft2.common.block.stage.copper.WaterPipeBlock;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Normal Copper pipe placement is unchanged. Sneak-click two existing pipes
 * to build an exactly straight 45-degree span between their centres.
 */
public final class CopperFluidPipeItem extends BlockItem {
    private static final String FIRST = "Mgc2DiagonalPipeStart";
    private static final String DIMENSION = "Mgc2DiagonalPipeDimension";
    private static final int MAX_SPAN = 8;

    public CopperFluidPipeItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos end = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()
                || !level.getBlockState(end).is(BlockRegistry.WATER_PIPE.get())) {
            return super.useOn(context);
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();
        String dimension = level.dimension().location().toString();
        if (!tag.contains(FIRST) || !dimension.equals(tag.getString(DIMENSION))) {
            tag.putLong(FIRST, end.asLong());
            tag.putString(DIMENSION, dimension);
            notice(player, "message.magneticraft2.diagonal_pipe_start",
                    end.getX(), end.getY(), end.getZ());
            return InteractionResult.SUCCESS;
        }

        BlockPos start = BlockPos.of(tag.getLong(FIRST));
        if (start.equals(end)) {
            clear(tag);
            notice(player, "message.magneticraft2.diagonal_pipe_clear");
            return InteractionResult.SUCCESS;
        }
        if (!level.hasChunkAt(start)
                || !level.getBlockState(start).is(BlockRegistry.WATER_PIPE.get())) {
            clear(tag);
            return fail(player, "message.magneticraft2.diagonal_pipe_missing");
        }

        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        int dz = end.getZ() - start.getZ();
        int axes = (dx == 0 ? 0 : 1) + (dy == 0 ? 0 : 1) + (dz == 0 ? 0 : 1);
        int span = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (axes != 2 || (dx != 0 && Math.abs(dx) != span)
                || (dy != 0 && Math.abs(dy) != span)
                || (dz != 0 && Math.abs(dz) != span)) {
            return fail(player, "message.magneticraft2.diagonal_pipe_angle");
        }
        if (span < 2 || span > MAX_SPAN) {
            return fail(player, "message.magneticraft2.diagonal_pipe_length");
        }

        BlockPos travel = new BlockPos(Integer.signum(dx),
                Integer.signum(dy), Integer.signum(dz));
        DiagonalWaterPipeBlock.Axis axis =
                DiagonalWaterPipeBlock.Axis.fromTravelStep(travel);
        WaterPipeBlock.DiagonalPort firstPort =
                WaterPipeBlock.DiagonalPort.forStep(travel);
        WaterPipeBlock.DiagonalPort lastPort = WaterPipeBlock.DiagonalPort.forStep(
                new BlockPos(-travel.getX(), -travel.getY(), -travel.getZ()));
        if (axis == null || firstPort == WaterPipeBlock.DiagonalPort.NONE
                || lastPort == WaterPipeBlock.DiagonalPort.NONE) {
            return fail(player, "message.magneticraft2.diagonal_pipe_angle");
        }

        BlockState startState = level.getBlockState(start);
        BlockState endState = level.getBlockState(end);
        if (startState.getValue(WaterPipeBlock.DIAGONAL)
                != WaterPipeBlock.DiagonalPort.NONE
                || endState.getValue(WaterPipeBlock.DIAGONAL)
                != WaterPipeBlock.DiagonalPort.NONE) {
            return fail(player, "message.magneticraft2.diagonal_pipe_in_use");
        }

        int required = span - 1; // Both ordinary endpoint pipes already exist.
        if (!player.getAbilities().instabuild && stack.getCount() < required) {
            notice(player, "message.magneticraft2.diagonal_pipe_not_enough", required);
            return InteractionResult.FAIL;
        }

        List<BlockPos> middle = new ArrayList<>(required);
        if (!level.mayInteract(player, start) || !level.mayInteract(player, end)) {
            return fail(player, "message.magneticraft2.diagonal_pipe_blocked");
        }
        for (int i = 1; i < span; i++) {
            BlockPos pos = start.offset(travel.getX() * i,
                    travel.getY() * i, travel.getZ() * i);
            if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos)
                    || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), stack)
                    || !level.getBlockState(pos).isAir()) {
                return fail(player, "message.magneticraft2.diagonal_pipe_blocked");
            }
            middle.add(pos.immutable());
        }
        // Check the complete tube: start->first, each middle crossing and
        // final->end. Never route a thick pipe through a solid corner.
        for (int i = 0; i < span; i++) {
            BlockPos a = start.offset(travel.getX() * i,
                    travel.getY() * i, travel.getZ() * i);
            BlockPos b = a.offset(travel.getX(), travel.getY(), travel.getZ());
            if (!DiagonalWaterPipeBlock.isCornerClear(level, a, b)) {
                return fail(player, "message.magneticraft2.diagonal_pipe_blocked");
            }
        }

        BlockState middleState = BlockRegistry.DIAGONAL_WATER_PIPE.get()
                .defaultBlockState()
                .setValue(DiagonalWaterPipeBlock.AXIS, axis)
                .setValue(DiagonalWaterPipeBlock.SECTION,
                        DiagonalWaterPipeBlock.Section.BOTH)
                .setValue(DiagonalWaterPipeBlock.SOCKET,
                        DiagonalWaterPipeBlock.Socket.NONE);
        List<BlockPos> placed = new ArrayList<>(required);
        boolean succeeded = true;
        for (BlockPos pos : middle) {
            if (!level.getBlockState(pos).isAir()
                    || !level.setBlock(pos, middleState, Block.UPDATE_ALL)) {
                succeeded = false;
                break;
            }
            placed.add(pos);
        }
        if (succeeded) {
            succeeded = level.setBlock(start, startState.setValue(
                    WaterPipeBlock.DIAGONAL, firstPort), Block.UPDATE_ALL);
        }
        if (succeeded) {
            succeeded = level.setBlock(end, endState.setValue(
                    WaterPipeBlock.DIAGONAL, lastPort), Block.UPDATE_ALL);
        }
        if (!succeeded) {
            // Restore exact original endpoint blockstates after removing the
            // intermediate pipes. Failed placements consume no materials.
            for (BlockPos pos : placed) {
                if (level.getBlockState(pos).is(BlockRegistry.DIAGONAL_WATER_PIPE.get())) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            level.setBlock(start, startState, Block.UPDATE_ALL);
            level.setBlock(end, endState, Block.UPDATE_ALL);
            return fail(player, "message.magneticraft2.diagonal_pipe_blocked");
        }

        clear(tag);
        if (!player.getAbilities().instabuild) {
            stack.shrink(required);
        }
        notice(player, "message.magneticraft2.diagonal_pipe_placed", required);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.magneticraft2.diagonal_pipe"));
    }

    private static void clear(CompoundTag tag) {
        tag.remove(FIRST);
        tag.remove(DIMENSION);
    }

    private static void notice(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }

    private static InteractionResult fail(Player player, String key) {
        notice(player, key);
        return InteractionResult.FAIL;
    }
}
