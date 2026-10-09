package com.magneticraft2.common.block.stage.stone;

import com.magneticraft2.common.blockentity.stage.stone.Primitive_anvilEntity;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class Primitive_anvilBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING;
    private static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");
    private static final BooleanProperty HALFBROKEN = BooleanProperty.create("halfbroken");
    private static final VoxelShape HALF_BROKEN = Stream.of(Block.box(4, 0, 2, 12, 4, 14), Block.box(4, 4, 3, 12, 5, 13), Block.box(4, 5, 4, 12, 9, 12), Block.box(4, 9, 3, 12, 10, 13), Stream.of(Block.box(7, 10, 0, 12, 12, 15), Block.box(10, 14, 0, 12, 16, 15), Block.box(9, 12, 11, 12, 14, 16), Block.box(6, 14, 0, 10, 16, 6), Block.box(6, 14, 6, 10, 15, 10), Block.box(4, 14, 0, 6, 16, 10), Block.box(4, 12, 0, 12, 14, 11), Block.box(4, 10, 0, 7, 12, 13), Block.box(6, 10, 13, 7, 11, 14), Block.box(6, 12, 11, 9, 13, 12), Block.box(7.25, 15, 6, 10.25, 16, 7)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape NEW = Stream.of(Block.box(4, 10, 0, 12, 16, 16), Block.box(4, 0, 2, 12, 4, 14), Block.box(4, 4, 3, 12, 5, 13), Block.box(4, 5, 4, 12, 9, 12), Block.box(4, 9, 3, 12, 10, 13)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public Primitive_anvilBlock() {
        super(BlockBehaviour.Properties.of().strength(3.5F).noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVATED, false).setValue(HALFBROKEN, false));

    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction direction = pState.getValue(FACING);
        boolean halfbroken = pState.getValue(HALFBROKEN);
        if (halfbroken) {
            switch (direction) {
                case NORTH:
                    return VoxelShapeUtils.rotateHorizontal(HALF_BROKEN, Direction.SOUTH);
                case SOUTH:
                    return VoxelShapeUtils.rotateHorizontal(HALF_BROKEN, Direction.NORTH);
                case EAST:
                    return VoxelShapeUtils.rotateHorizontal(HALF_BROKEN, Direction.WEST);
                case WEST:
                    return VoxelShapeUtils.rotateHorizontal(HALF_BROKEN, Direction.EAST);
            }
        }else {
            switch (direction) {
                case NORTH:
                    return VoxelShapeUtils.rotateHorizontal(NEW, Direction.SOUTH);
                case SOUTH:
                    return VoxelShapeUtils.rotateHorizontal(NEW, Direction.NORTH);
                case EAST:
                    return VoxelShapeUtils.rotateHorizontal(NEW, Direction.WEST);
                case WEST:
                    return VoxelShapeUtils.rotateHorizontal(NEW, Direction.EAST);
            }
        }
        return super.getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return (BlockState) this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getClockWise()).setValue(ACTIVATED, false).setValue(HALFBROKEN, false);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return (BlockState) state.setValue(FACING, direction.rotate(state.getValue(FACING))).setValue(ACTIVATED, false).setValue(HALFBROKEN, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(new Property[]{FACING, ACTIVATED, HALFBROKEN});
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Primitive_anvilEntity anvil)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (anvil.isEmpty() && held.is(Items.COPPER_INGOT)) {
            if (anvil.insertOne(held)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.setBlock(pos, state.setValue(ACTIVATED, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.45F, 0.8F);
                return InteractionResult.CONSUME;
            }
        }

        if (held.is(ItemRegistry.item_stone_hammer.get()) && anvil.hasCopperIngot()) {
            boolean completed = anvil.hammerCopper();
            if (!player.getAbilities().instabuild) {
                held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            }
            level.playSound(
                    null,
                    pos,
                    completed ? SoundEvents.ANVIL_USE : SoundEvents.ANVIL_HIT,
                    SoundSource.BLOCKS,
                    completed ? 0.8F : 0.55F,
                    completed ? 1.15F : 1.35F
            );
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty() && !anvil.isEmpty()) {
            ItemStack result = anvil.takeStoredItem();
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
            level.setBlock(pos, state.setValue(ACTIVATED, false), Block.UPDATE_ALL);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Primitive_anvilEntity anvil) {
                anvil.dropStoredItem(level);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new Primitive_anvilEntity(blockPos, blockState);
    }


    static {
        FACING = BlockStateProperties.FACING;
    }
}
