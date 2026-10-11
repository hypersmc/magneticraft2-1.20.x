package com.magneticraft2.common.block.stage.bronze;

import com.magneticraft2.common.registry.registers.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * First Bronze workshop station. A wooden, hand-operated pattern bench.
 *
 * Sneak-right-click with an empty hand to select the pattern, then use
 * a plank to cut one wooden pattern. Each pattern is reusable at the
 * separate Mold Table. The initial bronze tool in the crafting recipe
 * gates this workstation behind the real Copper -> Tin -> Bronze path.
 *
 * This is a single-block workbench, NOT a multiblock. Future Foundry and
 * Bloomery multiblocks remain JSON-defined as required by the MDD.
 */
public class BronzePatternMakerBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty BEARING_SELECTED =
            BooleanProperty.create("bearing_selected");

    public BronzePatternMakerBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.5F)
                .noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(BEARING_SELECTED, false));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BEARING_SELECTED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (!level.isClientSide) {
                boolean selectBearing = !state.getValue(BEARING_SELECTED);
                level.setBlock(pos, state.setValue(BEARING_SELECTED, selectBearing),
                        Block.UPDATE_CLIENTS);
                player.displayClientMessage(Component.translatable(
                        selectBearing
                                ? "message.magneticraft2.pattern_maker.bearing_selected"
                                : "message.magneticraft2.pattern_maker.gear_selected"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (held.is(ItemTags.PLANKS)) {
            if (!level.isClientSide) {
                ItemStack result = new ItemStack(state.getValue(BEARING_SELECTED)
                        ? ItemRegistry.ITEM_WOODEN_BEARING_PATTERN.get()
                        : ItemRegistry.ITEM_WOODEN_GEAR_PATTERN.get());
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                if (!player.getInventory().add(result)) {
                    player.drop(result, false);
                }
                level.playSound(null, pos, SoundEvents.WOOD_PLACE,
                        SoundSource.BLOCKS, 0.7F, 0.8F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (held.isEmpty() && !level.isClientSide) {
            player.displayClientMessage(Component.translatable(
                    state.getValue(BEARING_SELECTED)
                            ? "message.magneticraft2.pattern_maker.bearing_selected"
                            : "message.magneticraft2.pattern_maker.gear_selected"), true);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
