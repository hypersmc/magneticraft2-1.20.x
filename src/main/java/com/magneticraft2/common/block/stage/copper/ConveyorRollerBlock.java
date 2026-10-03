package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.stage.copper.ConveyorRollerBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.GEAR.ItemBeltConnectionManager;
import com.magneticraft2.common.systems.GEAR.ItemBeltGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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

/**
 * Mechanical endpoint for a continuous wide item belt.
 *
 * The FACING axis is the roller/shaft axis. Item-belt travel is perpendicular to that
 * axis, so a normal Wooden Shaft can drive the roller directly from either side.
 */
public class ConveyorRollerBlock extends GearBlock {
    // Two long wooden skids carry a pair of cross-ties. Each cross-tie feeds a
    // short pedestal into the bearing housing at the end of the roller shaft.
    // This keeps the frame open for horizontal/45-degree belts while making the
    // drum visibly and physically connected to the base instead of floating.
    private static final VoxelShape BASE_X = Shapes.or(
            Block.box(0.0D, 0.0D, 2.0D, 16.0D, 3.0D, 5.0D),
            Block.box(0.0D, 0.0D, 11.0D, 16.0D, 3.0D, 14.0D),

            Block.box(1.0D, 0.0D, 3.0D, 3.0D, 3.0D, 13.0D),
            Block.box(13.0D, 0.0D, 3.0D, 15.0D, 3.0D, 13.0D),

            Block.box(1.0D, 3.0D, 6.0D, 3.0D, 8.0D, 10.0D),
            Block.box(13.0D, 3.0D, 6.0D, 15.0D, 8.0D, 10.0D),

            Block.box(0.0D, 6.0D, 6.0D, 2.5D, 10.0D, 10.0D),
            Block.box(13.5D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D),

            // Two intersecting prisms approximate the animated octagonal drum.
            Block.box(2.0D, 4.0D, 3.0D, 14.0D, 12.0D, 13.0D),
            Block.box(2.0D, 3.0D, 4.0D, 14.0D, 13.0D, 12.0D)
    );

    private static final VoxelShape BASE_Z = Shapes.or(
            Block.box(2.0D, 0.0D, 0.0D, 5.0D, 3.0D, 16.0D),
            Block.box(11.0D, 0.0D, 0.0D, 14.0D, 3.0D, 16.0D),

            Block.box(3.0D, 0.0D, 1.0D, 13.0D, 3.0D, 3.0D),
            Block.box(3.0D, 0.0D, 13.0D, 13.0D, 3.0D, 15.0D),

            Block.box(6.0D, 3.0D, 1.0D, 10.0D, 8.0D, 3.0D),
            Block.box(6.0D, 3.0D, 13.0D, 10.0D, 8.0D, 15.0D),

            Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 2.5D),
            Block.box(6.0D, 6.0D, 13.5D, 10.0D, 10.0D, 16.0D),

            // Rotated equivalent of the animated octagonal drum.
            Block.box(3.0D, 4.0D, 2.0D, 13.0D, 12.0D, 14.0D),
            Block.box(4.0D, 3.0D, 2.0D, 12.0D, 13.0D, 14.0D)
    );

    public ConveyorRollerBlock() {
        super(BlockBehaviour.Properties.of().strength(2.5F).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.EAST));
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorRollerBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction axisDirection = context.getClickedFace();

        // Floor/ceiling placement still creates a horizontal roller. Point its axle
        // across the player so the eventual belt runs away from/toward the player.
        if (axisDirection.getAxis() == Direction.Axis.Y) {
            axisDirection = context.getHorizontalDirection().getClockWise();
        }

        BlockState placementState = defaultBlockState().setValue(FACING, axisDirection);
        return validateGearPlacement(context, placementState);
    }

    @Override
    public int getPlacementGearTeeth(BlockState state) {
        return 8;
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state,
                               BlockGetter level,
                               BlockPos pos,
                               CollisionContext context) {
        return getRollerShape(state, level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state,
                                        BlockGetter level,
                                        BlockPos pos,
                                        CollisionContext context) {
        return getRollerShape(state, level, pos);
    }

    private VoxelShape getRollerShape(BlockState state,
                                      BlockGetter level,
                                      BlockPos pos) {
        Direction.Axis axis = state.getValue(FACING).getAxis();
        VoxelShape base = axis == Direction.Axis.Z ? BASE_Z : BASE_X;

        if (!(level.getBlockEntity(pos) instanceof ConveyorRollerBlockEntity roller)) {
            return base;
        }

        BlockPos partnerPos = roller.getItemBeltPartner();
        if (partnerPos == null) {
            return base;
        }

        ItemBeltGeometry.Layout layout = ItemBeltGeometry.create(
                pos,
                partnerPos,
                axis,
                ItemBeltConnectionManager.MAX_ITEM_BELT_SPAN
        );
        if (layout == null) {
            return base;
        }

        ItemBeltBlock beltBlock = BlockRegistry.ITEM_BELT_BLOCK.get();
        VoxelShape beltShape = beltBlock.getPhysicalShape(
                beltBlock.stateFor(layout)
        );

        // The roller's collision follows the actual connected belt geometry: horizontal
        // gets upper/lower runs and 45-degree gets the two sloped runs. Straight vertical
        // Item Belts are rejected by ItemBeltGeometry. The open frame/roller remains
        // collidable as well.
        return Shapes.or(base, beltShape);
    }

    @Override
    public InteractionResult use(BlockState state,
                                 Level level,
                                 BlockPos pos,
                                 Player player,
                                 InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ConveyorRollerBlockEntity roller
                && roller.getItemBeltPartner() != null
                && ItemBeltConnectionManager.pickupLookedAtItem(
                level,
                pos,
                roller.getItemBeltPartner(),
                player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void onRemove(BlockState state,
                         Level level,
                         BlockPos pos,
                         BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ConveyorRollerBlockEntity roller) {
                ItemBeltConnectionManager.ensureRegistered(roller);
            }
            ItemBeltConnectionManager.breakAtRoller(level, pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                BlockEntityRegistry.CONVEYOR_ROLLER_BE.get(),
                ConveyorRollerBlockEntity::tick
        );
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}
