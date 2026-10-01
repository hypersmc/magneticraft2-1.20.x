package com.magneticraft2.common.block.general;

import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.general.BlueprintMultiblockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * @author JumpWatch on 12-11-2024
 * @Project mgc2-1.20
* @version 1.0.0
 */
public class BlueprintMultiblock extends BaseBlockMagneticraft2{
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");
    private static final VoxelShape WEST = Stream.of(Block.box(-16, 12, -16, 16, 16, 32), Block.box(13, 0, -16, 16, 12, -13), Block.box(-16, 0, -16, -13, 12, -13), Block.box(13, 0, 29, 16, 12, 32), Block.box(-16, 0, 29, -13, 12, 32)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public BlueprintMultiblock() {
        super(BlockBehaviour.Properties.of().noOcclusion().isSuffocating((state, level, pos) -> !state.getValue(IS_FORMED)).isViewBlocking((state, level, pos) -> !state.getValue(IS_FORMED)).requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(IS_FORMED, Boolean.FALSE).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(IS_FORMED).add(FACING);
    }
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return (BlockState) this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection()).setValue(IS_FORMED, Boolean.FALSE);
    }
    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return (BlockState) state.setValue(FACING, direction.rotate(state.getValue(FACING))).setValue(IS_FORMED, Boolean.FALSE);
    }
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide){
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            BlueprintMultiblockEntity multiblockEntity = (BlueprintMultiblockEntity) blockEntity;
            if (blockEntity instanceof BaseBlockEntityMagneticraft2 blueprintmaker){
                if (multiblockEntity.isFormed()){
                    NetworkHooks.openScreen((ServerPlayer) pPlayer, (blueprintmaker).menuProvider, blockEntity.getBlockPos());
                }else {
                    multiblockEntity.onRightClick();
                    multiblockEntity.setJustPlaced(false);
                    multiblockEntity.setInitialGameTime(pLevel.getGameTime());
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide){
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BaseBlockEntityMagneticraft2 testmultiblock){
                testmultiblock.onDestroy(level);
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : createTickerHelper(pBlockEntityType, BlockEntityRegistry.blueprintmultiblockentity.get(), BlueprintMultiblockEntity::serverTick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BlueprintMultiblockEntity(pPos,pState);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
         BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof BlueprintMultiblockEntity furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                String modelname = furnaceEntity.getMBblueprintname();
                switch (modelname) {
                    case "blueprintmaker_west":
                        return WEST;
                    case "blueprintmaker_north":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST);
                    case "blueprintmaker_south":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.WEST);
                    case "blueprintmaker_east":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH);
                }
            }
        }
        return super.getInteractionShape(pState, pLevel, pPos);
    }

    @Override
    public VoxelShape getVisualShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof BlueprintMultiblockEntity furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                String modelname = furnaceEntity.getMBblueprintname();
                switch (modelname) {
                    case "blueprintmaker_west":
                        return WEST;
                    case "blueprintmaker_north":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST);
                    case "blueprintmaker_south":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.WEST);
                    case "blueprintmaker_east":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH);
                }
            }
        }
        return super.getVisualShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
        return super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof BlueprintMultiblockEntity furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                String modelname = furnaceEntity.getMBblueprintname();
                switch (modelname) {
                    case "blueprintmaker_west":
                        return WEST;
                    case "blueprintmaker_north":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST);
                    case "blueprintmaker_south":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.WEST);
                    case "blueprintmaker_east":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH);
                }
            }
        }
        return super.getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof BlueprintMultiblockEntity furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                String modelname = furnaceEntity.getMBblueprintname();
                switch (modelname) {
                    case "blueprintmaker_west":
                        return WEST;
                    case "blueprintmaker_north":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST);
                    case "blueprintmaker_south":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.WEST);
                    case "blueprintmaker_east":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH);
                }
            }
        }
        return super.getCollisionShape(pState, pLevel, pPos, pContext);
    }

    @Override
    protected void interactableNoGui(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {

    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        if(pState.getValue(IS_FORMED).booleanValue()){
            return RenderShape.INVISIBLE;
        }
        return RenderShape.MODEL;
    }
}
