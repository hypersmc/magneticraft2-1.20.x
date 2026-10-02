package com.magneticraft2.common.block.stage.stone;

import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.stone.BellowsMultiblockModuleEntity;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.common.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 14-11-2024
 * @Project mgc2-1.20
* @version 1.0.0
 */
public class BellowsMultiblockModule extends BaseEntityBlock {
    public static final DirectionProperty FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    // The authored NORTH model points its nozzle toward +X. Keep the collision
    // in that same canonical orientation and rotate it exactly like the model.
    private static final VoxelShape IDLE_SHAPE = Shapes.or(
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 7.0D, 12.0D),
            Block.box(12.0D, 0.0D, 7.5D, 16.0D, 1.0D, 8.5D)
    ).optimize();

    private static final VoxelShape ACTIVE_SHAPE = Shapes.or(
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 4.6D, 12.0D),
            Block.box(12.0D, 0.0D, 7.5D, 16.0D, 1.0D, 8.5D)
    ).optimize();
    public BellowsMultiblockModule() {
        super(BlockBehaviour.Properties.of().noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));

    }
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return (BlockState) state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING).add(ACTIVE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = state.getValue(ACTIVE) ? ACTIVE_SHAPE : IDLE_SHAPE;
        return VoxelShapeUtils.rotateHorizontal(shape, state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_ALL);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BellowsMultiblockModuleEntity(pPos,pState);
    }
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BellowsMultiblockModuleEntity bellows) || !bellows.isFormedModule()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        boolean pumped = bellows.pump();
        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            String message = pumped
                    ? "Bellows: " + bellows.getStored() + "/" + bellows.getMaxStored() + " air"
                    : "Bellows: " + bellows.getStored() + "/" + bellows.getMaxStored() + " air (full or mid-stroke)";
            player.displayClientMessage(Component.literal(message), true);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide) {
            // Get the filler block's BlockEntity and read its NBT data
            BlockEntity blockEntity = level.getBlockEntity(pos);
            CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;

            if (tag != null && tag.contains("controller_x") && tag.contains("controller_y") && tag.contains("controller_z")) {
                // Retrieve the controller position from the NBT data
                BlockPos controllerPos = new BlockPos(tag.getInt("controller_x"), tag.getInt("controller_y"), tag.getInt("controller_z"));
                BlockEntity controllerEntity = level.getBlockEntity(controllerPos);

                // Check if the BlockEntity at the controller position is an instance of BaseBlockEntityMagneticraft2
                if (controllerEntity instanceof BaseBlockEntityMagneticraft2 multiblockController) {
                    multiblockController.onDestroy(level); // Call onDestroy on the controller
                }
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    static {
        FACING = BlockStateProperties.FACING;
    }
}
