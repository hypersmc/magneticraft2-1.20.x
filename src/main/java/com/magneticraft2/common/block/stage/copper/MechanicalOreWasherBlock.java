package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalOreWasherBlockEntity;
import com.magneticraft2.common.recipe.stage.copper.MechanicalOreWasherRecipe;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MechanicalOreWasherBlock extends GearBlock {
    private static final VoxelShape X_SHAPE = Shapes.or(
            Block.box(0.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 13.0D, 15.0D)
    );
    private static final VoxelShape Y_SHAPE = Shapes.or(
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D),
            Block.box(1.0D, 1.0D, 1.0D, 15.0D, 15.0D, 15.0D)
    );
    private static final VoxelShape Z_SHAPE = Shapes.or(
            Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 16.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 13.0D, 15.0D)
    );

    public MechanicalOreWasherBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(4.0F)
                .noOcclusion());

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.EAST)
        );
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MechanicalOreWasherBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return validateGearPlacement(
                context,
                defaultBlockState()
                        .setValue(FACING, context.getClickedFace())
        );
    }

    @Override
    public int getPlacementGearTeeth(BlockState state) {
        return 1;
    }

    @Override
    public boolean isShaftLikeForPlacement(BlockState state) {
        return true;
    }

    @Override
    public Direction.Axis getPlacementGearAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos)
                instanceof MechanicalOreWasherBlockEntity washer)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (held.isEmpty()) {
            ItemStack extracted = washer.extractForPlayer();
            if (!extracted.isEmpty()) {
                if (!player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        ItemStack single = held.copy();
        single.setCount(1);

        boolean validRecipe = level.getRecipeManager()
                .getRecipeFor(
                        MechanicalOreWasherRecipe.Type.INSTANCE,
                        new SimpleContainer(single),
                        level
                )
                .isPresent();

        if (!validRecipe) {
            return InteractionResult.PASS;
        }

        if (washer.insertInput(single)) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> X_SHAPE;
            case Y -> Y_SHAPE;
            case Z -> Z_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos)
                    instanceof MechanicalOreWasherBlockEntity washer) {
                washer.dropContents();
            }
            GearNetworkManager.getInstance()
                    .removeMechanicalLoad(level, pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        BlockEntityRegistry.MECHANICAL_ORE_WASHER_BE.get(),
                        MechanicalOreWasherBlockEntity::serverTick
                );
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity>
    BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType
                ? (BlockEntityTicker<A>) ticker
                : null;
    }
}
