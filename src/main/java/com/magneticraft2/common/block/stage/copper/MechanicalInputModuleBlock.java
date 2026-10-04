package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalInputModuleBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable Gear V2 input module for JSON multiblocks.
 *
 * It behaves as a normal 16T wooden shaft while the structure is unformed.
 * Once a multiblock forms the physical block stays in place (because it is an
 * IMultiblockModule) but becomes visually hidden so the formed machine renderer
 * can own the bearing/shaft geometry without z-fighting.
 */
public class MechanicalInputModuleBlock extends GearBlock {
    public static final BooleanProperty FORMED =
            BooleanProperty.create("formed");

    private static final VoxelShape X_SHAPE =
            Shapes.box(0.0D, 0.25D, 0.25D, 1.0D, 0.75D, 0.75D);
    private static final VoxelShape Y_SHAPE =
            Shapes.box(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 0.75D);
    private static final VoxelShape Z_SHAPE =
            Shapes.box(0.25D, 0.25D, 0.0D, 0.75D, 0.75D, 1.0D);

    public MechanicalInputModuleBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .noOcclusion());

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.EAST)
                        .setValue(FORMED, false)
        );
    }

    @Override
    protected BlockEntity createBlockEntity(
            BlockPos pos,
            BlockState state) {
        return new MechanicalInputModuleBlockEntity(
                pos,
                state
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {
        return validateGearPlacement(
                context,
                defaultBlockState()
                        .setValue(
                                FACING,
                                context.getClickedFace()
                        )
                        .setValue(FORMED, false)
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
        return state.getValue(FORMED)
                ? RenderShape.INVISIBLE
                : RenderShape.MODEL;
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
        builder.add(FACING, FORMED);
    }

    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            boolean willHarvest,
            FluidState fluid) {
        if (!level.isClientSide) {
            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (blockEntity
                    instanceof MechanicalInputModuleBlockEntity module) {
                CompoundTag tag =
                        module.saveWithoutMetadata();

                if (tag.contains("controller_x")
                        && tag.contains("controller_y")
                        && tag.contains("controller_z")) {
                    BlockPos controllerPos =
                            new BlockPos(
                                    tag.getInt("controller_x"),
                                    tag.getInt("controller_y"),
                                    tag.getInt("controller_z")
                            );

                    BlockEntity controllerEntity =
                            level.getBlockEntity(controllerPos);

                    if (controllerEntity
                            instanceof BaseBlockEntityMagneticraft2 controller) {
                        controller.onDestroy(level);
                    }
                }
            }
        }

        return super.onDestroyedByPlayer(
                state,
                level,
                pos,
                player,
                willHarvest,
                fluid
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity>
    BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        BlockEntityRegistry
                                .MECHANICAL_INPUT_MODULE_BE
                                .get(),
                        MechanicalInputModuleBlockEntity
                                ::serverTick
                );
    }

    @Nullable
    protected static <E extends BlockEntity,
                      A extends BlockEntity>
    BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType
                ? (BlockEntityTicker<A>) ticker
                : null;
    }
}
