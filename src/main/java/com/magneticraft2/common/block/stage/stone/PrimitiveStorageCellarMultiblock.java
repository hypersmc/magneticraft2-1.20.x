package com.magneticraft2.common.block.stage.stone;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.stone.PrimitiveStorageCellarMultiblockEntity;
import com.magneticraft2.common.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class PrimitiveStorageCellarMultiblock extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");
    private static final VoxelShape WEST = Stream.of(Block.box(21, -15, -10, 25, 15, -6), Block.box(21, -15, 22, 25, 15, 26), Block.box(-10, -15, -10, -6, 15, -6), Block.box(-10, -15, 22, -6, 15, 26), Block.box(-15, -16, 16, 31, -15, 31), Block.box(-15, -16, 0, 0, -15, 16), Block.box(-15, -16, -15, 31, -15, 0), Block.box(-15, -6, 0, 0, -5, 16), Block.box(-15, -6, 16, 31, -5, 31), Block.box(-15, -6, -15, 31, -5, 0), Block.box(-15, 4, 0, 0, 5, 16), Block.box(-15, 4, 16, 31, 5, 31), Block.box(-15, 4, -15, 31, 5, 0), Block.box(-16, -16, 31, 32, 16, 32), Block.box(31, -16, 16, 32, 16, 31), Block.box(31, -16, -15, 32, 16, 0), Block.box(-16, -16, -15, -15, 16, 31), Block.box(-15, 15, -15, 31, 16, 31), Block.box(-16, -16, -16, 32, 16, -15)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public PrimitiveStorageCellarMultiblock() {
        super(Properties.of().noOcclusion().requiresCorrectToolForDrops());
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
        if (!pLevel.isClientSide) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof PrimitiveStorageCellarMultiblockEntity multiblockEntity) {
                if (multiblockEntity.isFormed()) {
                    interactableNoGui(pState, pLevel, pPos, pPlayer, pHand, pHit);
                } else {
                    multiblockEntity.onRightClick();
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

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PrimitiveStorageCellarMultiblockEntity(blockPos, blockState);
    }
    @Override
    protected void interactableNoGui(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        // Transform global hit position into block-local NORTH-facing space
        Vec3 local = toLocalHit(pHit, pPos, null);
        double x = local.x, y = local.y, z = local.z;

        LOGGER.info(String.format("Facing: %s, X: %.3f, Y: %.3f, Z: %.3f", null, x, y, z));
    }

    private Vec3 toLocalHit(BlockHitResult hit, BlockPos blockPos, Direction facing) {
        double localX = hit.getLocation().x - blockPos.getX();
        double localY = hit.getLocation().y - blockPos.getY();
        double localZ = hit.getLocation().z - blockPos.getZ();

        // Rotate hit based on block's facing
        return new Vec3(localX, localY, localZ);
    }
    @Override
    public VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof PrimitiveStorageCellarMultiblockEntity furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                String modelname = furnaceEntity.getMBblueprintname();
                switch (modelname) {
                    case "primitive_storagecellar_west":
                        return WEST.move(-1, 0,0);
                    case "primitive_storagecellar_north":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST);
                    case "primitive_storagecellar_south":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.WEST);
                    case "primitive_storagecellar_east":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH);
                }
            }
        }
        return super.getInteractionShape(pState, pLevel, pPos);
    }

    @Override
    public VoxelShape getVisualShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof PrimitiveStorageCellarMultiblockEntity furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                String modelname = furnaceEntity.getMBblueprintname();
                switch (modelname) {
                    case "primitive_storagecellar_west":
                        return WEST.move(-1, 0,0);
                    case "primitive_storagecellar_north":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST);
                    case "primitive_storagecellar_south":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.WEST);
                    case "primitive_storagecellar_east":
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
        if (blockEntity instanceof PrimitiveStorageCellarMultiblockEntity furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                String modelname = furnaceEntity.getMBblueprintname();
                switch (modelname) {
                    case "primitive_storagecellar_west":
                        return WEST.move(-1, 0,0);
                    case "primitive_storagecellar_north":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST);
                    case "primitive_storagecellar_south":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.WEST);
                    case "primitive_storagecellar_east":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH);
                }
            }
        }
        return super.getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof PrimitiveStorageCellarMultiblockEntity furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                String modelname = furnaceEntity.getMBblueprintname();
                switch (modelname) {
                    case "primitive_storagecellar_west":
                        return WEST.move(-1, 0,0);
                    case "primitive_storagecellar_north":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST);
                    case "primitive_storagecellar_south":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.WEST);
                    case "primitive_storagecellar_east":
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH);
                }
            }
        }
        return super.getCollisionShape(pState, pLevel, pPos, pContext);
    }
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        if(pState.getValue(IS_FORMED).booleanValue()){
            return RenderShape.INVISIBLE;
        }
        return RenderShape.MODEL;
    }
}
