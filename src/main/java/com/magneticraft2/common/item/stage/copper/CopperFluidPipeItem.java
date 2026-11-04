package com.magneticraft2.common.item.stage.copper;

import com.magneticraft2.common.block.stage.copper.DiagonalWaterPipeBlock;
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

/** Existing pipe item, with an optional sneak-use two-endpoint 45-degree mode. */
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
        BlockPos clicked = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()
                || !level.getBlockState(clicked).is(BlockRegistry.WATER_PIPE.get())) {
            return super.useOn(context);
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();
        String dimension = level.dimension().location().toString();
        if (!tag.contains(FIRST) || !dimension.equals(tag.getString(DIMENSION))) {
            tag.putLong(FIRST, clicked.asLong());
            tag.putString(DIMENSION, dimension);
            notice(player, "message.magneticraft2.diagonal_pipe_start",
                    clicked.getX(), clicked.getY(), clicked.getZ());
            return InteractionResult.SUCCESS;
        }

        BlockPos first = BlockPos.of(tag.getLong(FIRST));
        if (first.equals(clicked)) {
            clear(tag);
            notice(player, "message.magneticraft2.diagonal_pipe_clear");
            return InteractionResult.SUCCESS;
        }
        if (!level.hasChunkAt(first)
                || !level.getBlockState(first).is(BlockRegistry.WATER_PIPE.get())) {
            clear(tag);
            return fail(player, "message.magneticraft2.diagonal_pipe_missing");
        }

        int dx = clicked.getX() - first.getX();
        int dy = clicked.getY() - first.getY();
        int dz = clicked.getZ() - first.getZ();
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
        if (!player.getAbilities().instabuild && stack.getCount() < span) {
            notice(player, "message.magneticraft2.diagonal_pipe_not_enough", span);
            return InteractionResult.FAIL;
        }
        Plan plan = findPlan(level, first, clicked, player, stack, span);
        if (plan == null) {
            return fail(player, "message.magneticraft2.diagonal_pipe_blocked");
        }

        // All placement cells and diagonal clearance cells were checked first.
        List<BlockPos> placed = new ArrayList<>();
        for (int i = 0; i < span; i++) {
            BlockPos pos = plan.cells().get(i);
            if (!level.getBlockState(pos).isAir()
                    || !level.setBlock(pos, stateFor(plan, i), Block.UPDATE_ALL)) {
                for (BlockPos installed : placed) {
                    level.setBlock(installed, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                return fail(player, "message.magneticraft2.diagonal_pipe_blocked");
            }
            placed.add(pos);
        }
        clear(tag);
        if (!player.getAbilities().instabuild) {
            stack.shrink(span);
        }
        notice(player, "message.magneticraft2.diagonal_pipe_placed", span);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.magneticraft2.diagonal_pipe"));
    }

    private static BlockState stateFor(Plan plan, int index) {
        int last = plan.cells().size() - 1;
        DiagonalWaterPipeBlock.Section section = index == 0
                ? (plan.forward() ? DiagonalWaterPipeBlock.Section.POSITIVE
                                  : DiagonalWaterPipeBlock.Section.NEGATIVE)
                : index == last
                ? (plan.forward() ? DiagonalWaterPipeBlock.Section.NEGATIVE
                                  : DiagonalWaterPipeBlock.Section.POSITIVE)
                : DiagonalWaterPipeBlock.Section.BOTH;
        DiagonalWaterPipeBlock.Socket socket = index == 0
                ? DiagonalWaterPipeBlock.Socket.of(plan.firstSocket())
                : index == last
                ? DiagonalWaterPipeBlock.Socket.of(plan.lastSocket())
                : DiagonalWaterPipeBlock.Socket.NONE;
        return BlockRegistry.DIAGONAL_WATER_PIPE.get().defaultBlockState()
                .setValue(DiagonalWaterPipeBlock.AXIS, plan.axis())
                .setValue(DiagonalWaterPipeBlock.SECTION, section)
                .setValue(DiagonalWaterPipeBlock.SOCKET, socket);
    }

    @Nullable
    private static Plan findPlan(Level level, BlockPos start, BlockPos end,
                                 Player player, ItemStack item, int span) {
        BlockPos delta = end.subtract(start);
        BlockPos travel = new BlockPos(Integer.signum(delta.getX()),
                Integer.signum(delta.getY()), Integer.signum(delta.getZ()));
        DiagonalWaterPipeBlock.Axis axis =
                DiagonalWaterPipeBlock.Axis.fromTravelStep(travel);
        if (axis == null) {
            return null;
        }

        Direction[] options = new Direction[2];
        int count = 0;
        for (Direction direction : Direction.values()) {
            if ((direction.getStepX() != 0 && direction.getStepX() == travel.getX())
                    || (direction.getStepY() != 0 && direction.getStepY() == travel.getY())
                    || (direction.getStepZ() != 0 && direction.getStepZ() == travel.getZ())) {
                options[count++] = direction;
            }
        }
        if (count != 2) {
            return null;
        }

        for (int route = 0; route < 2; route++) {
            Direction first = options[route];
            Direction last = options[1 - route];
            BlockPos firstCell = start.relative(first);
            List<BlockPos> cells = new ArrayList<>();
            boolean clear = true;

            for (int i = 0; i < span; i++) {
                BlockPos cell = firstCell.offset(travel.getX() * i,
                        travel.getY() * i, travel.getZ() * i);
                if (!level.hasChunkAt(cell)
                        || !level.isInWorldBounds(cell)
                        || !level.getWorldBorder().isWithinBounds(cell)
                        || !level.mayInteract(player, cell)
                        || !player.mayUseItemAt(cell, first, item)
                        || !level.getBlockState(cell).isAir()) {
                    clear = false;
                    break;
                }
                cells.add(cell.immutable());
                if (i > 0 && !DiagonalWaterPipeBlock.isCornerClear(
                        level, cells.get(i - 1), cell)) {
                    clear = false;
                    break;
                }
            }

            if (clear && cells.size() == span
                    && cells.get(span - 1).relative(last).equals(end)) {
                return new Plan(List.copyOf(cells), axis, travel.equals(axis.step()),
                        first.getOpposite(), last);
            }
        }
        return null;
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

    private record Plan(List<BlockPos> cells, DiagonalWaterPipeBlock.Axis axis,
                        boolean forward, Direction firstSocket, Direction lastSocket) {
    }
}
