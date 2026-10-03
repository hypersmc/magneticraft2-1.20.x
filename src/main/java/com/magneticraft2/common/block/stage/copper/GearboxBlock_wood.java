package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.GearboxBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Early Copper Age 1:1 wooden right-angle gearbox.
 *
 * Two perpendicular faces are mechanical shaft ports. INPUT is the Gear V2 reference
 * axis; OUTPUT is converted through a visible pair of wooden bevel gears. Mechanical
 * power may flow either way through the block.
 */
public class GearboxBlock_wood extends Block implements EntityBlock {
    public static final DirectionProperty INPUT = DirectionProperty.create("input");
    public static final DirectionProperty OUTPUT = DirectionProperty.create("output");

    public GearboxBlock_wood() {
        super(BlockBehaviour.Properties.of()
                .strength(2.5F)
                .noOcclusion());

        registerDefaultState(stateDefinition.any()
                .setValue(INPUT, Direction.WEST)
                .setValue(OUTPUT, Direction.UP));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction input = context.getClickedFace().getOpposite();
        Direction output = findPerpendicularLookDirection(
                context,
                input.getAxis()
        );

        return defaultBlockState()
                .setValue(INPUT, input)
                .setValue(OUTPUT, output);
    }

    private Direction findPerpendicularLookDirection(BlockPlaceContext context,
                                                      Direction.Axis inputAxis) {
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis() != inputAxis) {
                return direction;
            }
        }

        return inputAxis == Direction.Axis.Y
                ? Direction.NORTH
                : Direction.UP;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INPUT, OUTPUT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GearboxBlockEntity_wood(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                type,
                BlockEntityRegistry.GEARBOX_BE_WOOD.get(),
                GearboxBlockEntity_wood::serverTick
        );
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual,
            BlockEntityType<E> expected,
            BlockEntityTicker<? super E> ticker) {
        return actual == expected
                ? (BlockEntityTicker<A>) ticker
                : null;
    }

    @Override
    public void onPlace(BlockState state,
                        Level level,
                        BlockPos pos,
                        BlockState oldState,
                        boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof GearboxBlockEntity_wood gearbox) {
            gearbox.updateGearNetwork();
        }
    }

    @Override
    public void onRemove(BlockState state,
                         Level level,
                         BlockPos pos,
                         BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            GearNetworkManager.getInstance().removeGear(pos, level);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(BlockState state,
                                 Level level,
                                 BlockPos pos,
                                 Player player,
                                 InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            Direction oldInput = state.getValue(INPUT);
            Direction oldOutput = state.getValue(OUTPUT);

            if (!level.isClientSide) {
                level.setBlock(
                        pos,
                        state.setValue(INPUT, oldOutput)
                                .setValue(OUTPUT, oldInput),
                        Block.UPDATE_ALL
                );

                if (level.getBlockEntity(pos) instanceof GearboxBlockEntity_wood gearbox) {
                    gearbox.updateGearNetwork();
                }

                player.displayClientMessage(
                        Component.translatable(
                                "message.magneticraft2.gearbox_ports_swapped"
                        ),
                        true
                );
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        Direction input = state.getValue(INPUT);
        Direction output = state.getValue(OUTPUT);
        Direction nextOutput = nextPerpendicularDirection(
                input.getAxis(),
                output
        );

        if (!level.isClientSide) {
            level.setBlock(
                    pos,
                    state.setValue(OUTPUT, nextOutput),
                    Block.UPDATE_ALL
            );

            if (level.getBlockEntity(pos) instanceof GearboxBlockEntity_wood gearbox) {
                gearbox.updateGearNetwork();
            }

            player.displayClientMessage(
                    Component.translatable(
                            "message.magneticraft2.gearbox_output",
                            nextOutput.getName()
                    ),
                    true
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private Direction nextPerpendicularDirection(Direction.Axis inputAxis,
                                                 Direction current) {
        Direction[] directions = Direction.values();
        int start = current.ordinal();

        for (int offset = 1; offset <= directions.length; offset++) {
            Direction candidate =
                    directions[(start + offset) % directions.length];

            if (candidate.getAxis() != inputAxis) {
                return candidate;
            }
        }

        return current;
    }
}
