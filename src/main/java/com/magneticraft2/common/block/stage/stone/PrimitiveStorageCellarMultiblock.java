package com.magneticraft2.common.block.stage.stone;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.stone.PrimitiveStorageCellarMultiblockEntity;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockHitHelper;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.common.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
        super(Properties.of().noOcclusion().isSuffocating((state, level, pos) -> !state.getValue(IS_FORMED)).isViewBlocking((state, level, pos) -> !state.getValue(IS_FORMED)).requiresCorrectToolForDrops());
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
        if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS;
        }

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
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (!(blockEntity instanceof PrimitiveStorageCellarMultiblockEntity cellarEntity)) {
            return;
        }

        Direction formedFacing = getFormedFacing(cellarEntity, pState);
        Vec3 relativeHit = MultiblockHitHelper.relativeToController(pHit, pPos);
        Vec3 localHit = MultiblockHitHelper.toCanonicalWest(relativeHit, formedFacing);
        CellarTarget target = findCellarTarget(localHit);
        PrimitiveStorageCellarLayout.Slot slot = target == null
                ? null
                : PrimitiveStorageCellarLayout.findSlot(target.wall(), target.shelf(), localHit);

        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            String targetName = target == null
                    ? "none"
                    : target.wall().name() + " / " + target.shelf().name()
                    + (slot == null ? " / no slot" : " / slot " + slot.index());

            String message = String.format(
                    "Cellar %s -> %s | local x=%.3f y=%.3f z=%.3f",
                    formedFacing,
                    targetName,
                    localHit.x,
                    localHit.y,
                    localHit.z
            );

            LOGGER.info(message);
            pPlayer.displayClientMessage(Component.literal(message), true);
        }

        if (slot != null) {
            interactWithSlot(cellarEntity, slot.index(), pPlayer, pHand);
        }
    }

    private void interactWithSlot(PrimitiveStorageCellarMultiblockEntity cellarEntity, int slot, Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (heldItem.isEmpty()) {
            ItemStack extracted = cellarEntity.itemHandler.extractItem(slot, 64, false);
            if (!extracted.isEmpty()) {
                if (!player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
                cellarEntity.sync();
            }
            return;
        }

        ItemStack toInsert = heldItem.copy();
        ItemStack remainder = cellarEntity.itemHandler.insertItem(slot, toInsert, false);
        int inserted = heldItem.getCount() - remainder.getCount();

        if (inserted > 0) {
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(inserted);
            }
            cellarEntity.sync();
        }
    }

    private Direction getFormedFacing(PrimitiveStorageCellarMultiblockEntity cellarEntity, BlockState state) {
        String blueprintName = cellarEntity.getMBblueprintname();
        if (blueprintName != null) {
            if (blueprintName.endsWith("_west")) {
                return Direction.WEST;
            }
            if (blueprintName.endsWith("_east")) {
                return Direction.EAST;
            }
            if (blueprintName.endsWith("_north")) {
                return Direction.NORTH;
            }
            if (blueprintName.endsWith("_south")) {
                return Direction.SOUTH;
            }
        }

        return state.hasProperty(FACING) ? state.getValue(FACING) : Direction.WEST;
    }

    @Nullable
    private CellarTarget findCellarTarget(Vec3 localHit) {
        double x = localHit.x;
        double y = localHit.y;
        double z = localHit.z;

        if (y < -1.05D || y > 1.05D) {
            return null;
        }

        PrimitiveStorageCellarLayout.Shelf shelf;
        if (y < -0.33D) {
            shelf = PrimitiveStorageCellarLayout.Shelf.LOWER;
        } else if (y < 0.30D) {
            shelf = PrimitiveStorageCellarLayout.Shelf.MIDDLE;
        } else {
            shelf = PrimitiveStorageCellarLayout.Shelf.UPPER;
        }

        // The WEST-authored replacement model is rendered with WEST.move(-1, 0, 0).
        // After that shift its physical shelf extents, relative to the controller, are:
        // north: x -1.9375..0.9375, z -0.9375..0
        // south: x -1.9375..0.9375, z 1..1.9375
        // west:  x -1.9375..-1,     z 0..1
        // A small tolerance makes clicks on shelf edges behave naturally.
        if (z >= -1.00D && z <= 0.10D && x >= -2.00D && x <= 1.00D) {
            return new CellarTarget(PrimitiveStorageCellarLayout.Wall.NORTH, shelf);
        }
        if (z >= 0.90D && z <= 2.00D && x >= -2.00D && x <= 1.00D) {
            return new CellarTarget(PrimitiveStorageCellarLayout.Wall.SOUTH, shelf);
        }
        if (x >= -2.00D && x <= -0.90D && z >= -0.05D && z <= 1.05D) {
            return new CellarTarget(PrimitiveStorageCellarLayout.Wall.WEST, shelf);
        }

        return null;
    }

    private record CellarTarget(PrimitiveStorageCellarLayout.Wall wall, PrimitiveStorageCellarLayout.Shelf shelf) {
    }
    private VoxelShape controllerLocalSlice(VoxelShape fullShape) {
        return Shapes.join(fullShape, Shapes.block(), BooleanOp.AND).optimize();
    }

    @Override
    public VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof PrimitiveStorageCellarMultiblockEntity furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                String modelname = furnaceEntity.getMBblueprintname();
                switch (modelname) {
                    case "primitive_storagecellar_west":
                        return controllerLocalSlice(WEST.move(-1, 0,0));
                    case "primitive_storagecellar_north":
                        return controllerLocalSlice(VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST));
                    case "primitive_storagecellar_south":
                        return controllerLocalSlice(VoxelShapeUtils.rotateHorizontal(WEST, Direction.WEST));
                    case "primitive_storagecellar_east":
                        return controllerLocalSlice(VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH));
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
                        return controllerLocalSlice(WEST.move(-1, 0,0));
                    case "primitive_storagecellar_north":
                        return controllerLocalSlice(VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST));
                    case "primitive_storagecellar_south":
                        return controllerLocalSlice(VoxelShapeUtils.rotateHorizontal(WEST, Direction.WEST));
                    case "primitive_storagecellar_east":
                        return controllerLocalSlice(VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH));
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
