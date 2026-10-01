package com.magneticraft2.common.block.stage.stone;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;

import com.magneticraft2.common.blockentity.stage.stone.PrimitiveFurnaceMultiblockEntity_nogui;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * @author JumpWatch on 13-11-2024
 * @Project mgc2-1.20
* @version 1.0.0
 */
public class PrimitiveFurnaceMultiblock_nogui extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");
    private static final VoxelShape WEST = Stream.of(Block.box(4.68629, 0, 0, 11.31371, 1, 16), Block.box(0, 0, 4.68629, 16, 1, 11.31371), Block.box(0, 0, 4.68629, 16, 1, 11.31371), Block.box(4.68629, 0, 0, 11.31371, 1, 16), Stream.of(Block.box(4.68629, 1, 0, 11.31371, 8, 1), Block.box(15, 1, 4.68629, 16, 2, 11.31371), Block.box(4.68629, 1, 13, 11.31371, 8, 16), Block.box(0, 1, 4.68629, 3, 8, 11.31371), Block.box(0, 1, 4.68629, 3, 8, 11.31371), Block.box(4.68629, 1, 0, 11.31371, 8, 3), Block.box(15, 1, 4.68629, 16, 8, 11.31371), Block.box(4.68629, 1, 13, 11.31371, 8, 16), Stream.of(Block.box(4.68629, 8, 0, 11.31371, 16, 3), Block.box(13, 8, 4.68629, 16, 16, 11.31371), Block.box(4.68629, 8, 13, 11.31371, 16, 16), Block.box(0, 8, 4.68629, 3, 16, 11.31371), Block.box(0, 8, 4.68629, 3, 16, 11.31371), Block.box(4.68629, 8, 0, 11.31371, 16, 3), Block.box(13, 8, 4.68629, 16, 16, 11.31371), Block.box(4.68629, 8, 13, 11.31371, 16, 16), Block.box(14, 0, 0, 16, 16, 4.7), Block.box(14, 0, 11.299999999999999, 16, 16, 16), Block.box(11.3, 0, 0, 14, 16, 2), Block.box(1.9999999999999982, 0, 14, 4.699999999999999, 16, 16), Block.box(2, 0, 0, 4.699999999999999, 16, 2), Block.box(11.299999999999999, 0, 14, 14, 16, 16), Block.box(0, 0, 11.299999999999999, 2, 16, 16), Block.box(0, 0, 0, 2, 16, 4.7)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape WEST_NEW = Stream.of(Block.box(4.68629, -16, 0, 11.31371, -15, 16), Block.box(0, -16, 4.68629, 16, -15, 11.31371), Stream.of(Block.box(4.68629, -15, 0, 11.31371, -14, 1), Block.box(4.68629, -15, 13, 11.31371, -8, 16), Block.box(0, -15, 4.68629, 3, -8, 11.31371), Block.box(13, -15, 4.68629, 16, -8, 11.31371), Stream.of(Block.box(4.68629, -8, 0, 11.31371, 13, 3), Block.box(4.68629, -8, 13, 11.31371, 13, 16), Block.box(0, -8, 4.68629, 3, 13, 11.31371), Block.box(13, -8, 4.68629, 16, 13, 11.31371), Stream.of(Block.box(5.51472, 13, 2, 10.48528, 32, 5), Block.box(5.51472, 13, 11, 10.48528, 32, 14), Block.box(2, 13, 5.51472, 5, 32, 10.48528), Block.box(11, 13, 5.51472, 14, 32, 10.48528)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final VoxelShape WEST_NEW2 = Stream.of(Block.box(4.68629, -16, 0, 11.31371, -15, 16), Block.box(0, -16, 4.68629, 16, -15, 11.31371), Block.box(0, -16, 4.68629, 16, -15, 11.31371), Block.box(4.68629, -16, 0, 11.31371, -15, 16), Block.box(4.68629, -15, 0, 11.31371, -8, 1), Block.box(15, -15, 4.68629, 16, -14, 11.31371), Block.box(4.68629, -15, 13, 11.31371, -8, 16), Block.box(0, -15, 4.68629, 3, -8, 11.31371), Block.box(0, -15, 4.68629, 3, -8, 11.31371), Block.box(4.68629, -15, 0, 11.31371, -8, 3), Block.box(15, -15, 4.68629, 16, -8, 11.31371), Block.box(4.68629, -15, 13, 11.31371, -8, 16), Stream.of(Block.box(4.68629, -8, 0, 11.31371, 13, 3), Block.box(13, -8, 4.68629, 16, 13, 11.31371), Block.box(4.68629, -8, 13, 11.31371, 13, 16), Block.box(0, -8, 4.68629, 3, 13, 11.31371), Block.box(0, -8, 4.68629, 3, 13, 11.31371), Block.box(4.68629, -8, 0, 11.31371, 13, 3), Block.box(13, -8, 4.68629, 16, 13, 11.31371), Block.box(4.68629, -8, 13, 11.31371, 13, 16), Stream.of(Block.box(5.51472, 13, 2, 10.48528, 32, 5), Block.box(11, 13, 5.51472, 14, 32, 10.48528), Block.box(5.51472, 13, 11, 10.48528, 32, 14), Block.box(2, 13, 5.51472, 5, 32, 10.48528), Block.box(2, 13, 5.51472, 5, 32, 10.48528), Block.box(5.51472, 13, 2, 10.48528, 32, 5), Block.box(11, 13, 5.51472, 14, 32, 10.48528), Block.box(5.51472, 13, 11, 10.48528, 32, 14)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public PrimitiveFurnaceMultiblock_nogui() {
        super(Properties.of().noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(IS_FORMED, Boolean.FALSE).setValue(FACING, Direction.NORTH));
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState pState) {
        return true;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        Direction direction = pState.getValue(FACING);
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                switch (direction) {
                    case WEST:
                        return VoxelShapeUtils.rotateHorizontal(WEST_NEW, Direction.EAST).move(0,1,0);
                    case NORTH:
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.NORTH).move(0,1,0);
                    case SOUTH:
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH).move(0,1,0);
                    case EAST:
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST).move(0,1,0);
                }
            }
        }
        return super.getInteractionShape(pState, pLevel, pPos);
    }

    @Override
    public VoxelShape getVisualShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction direction = pState.getValue(FACING);
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity) {
            if (furnaceEntity.isFormed()) {

                switch (direction) {
                    case WEST:
                        return VoxelShapeUtils.rotateHorizontal(WEST_NEW, Direction.EAST).move(0,1,0);
                    case NORTH:
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.NORTH).move(0,1,0);
                    case SOUTH:
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH).move(0,1,0);
                    case EAST:
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST).move(0,1,0);
                }
            }
        }
        return super.getVisualShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction direction = pState.getValue(FACING);
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                switch (direction) {
                    case WEST:
                        return VoxelShapeUtils.rotateHorizontal(WEST_NEW, Direction.EAST).move(0,1,0);
                    case NORTH:
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.NORTH).move(0,1,0);
                    case SOUTH:
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.SOUTH).move(0,1,0);
                    case EAST:
                        return VoxelShapeUtils.rotateHorizontal(WEST, Direction.EAST).move(0,1,0);
                }
            }
        }
        return super.getShape(pState, pLevel, pPos, pContext);
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
    public void animateTick(BlockState pState, Level level, BlockPos pos, RandomSource pRandom) {
        super.animateTick(pState, level, pos, pRandom);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                if (furnaceEntity.isCooking()) {
                    if (level.getGameTime() % 1 == 0) { // Checks if it’s every 10 ticks
                        double x = pos.getX() + 0.5;
                        double y = pos.getY() + 3.0; // Slightly above the block
                        double z = pos.getZ() + 0.5;

                        level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y, z, 0.0, 0.03, 0.0);
                    }
                    if (level.getGameTime() % 1 == 0) { // Checks if it’s every 5 ticks
                        RandomSource random = level.random;

                        // Number of flames to spawn
                        int flameCount = 5;

                        for (int i = 0; i < flameCount; i++) {
                            // Randomize position within the block bounds
                            double x = pos.getX() + 0.3 + random.nextDouble() * 0.4; // Between 0.3 and 0.4 on X
                            double y = pos.getY() + 0.1 + random.nextDouble() * 0.3; // Between 0.1 and 0.3 on Y
                            double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4; // Between 0.3 and 0.4 on Z

                            // Add flame particle with slight upward velocity
                            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.01, 0.0);
                        }
                    }
                }
            }


        }

    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(IS_FORMED).add(FACING);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof PrimitiveFurnaceMultiblockEntity_nogui multiblockEntity) {
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
                testmultiblock.onDestroy(level); // it's not a test anymore but just old relic since it was called testmultiblock from making the multiblock system.
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : createTickerHelper(pBlockEntityType, BlockEntityRegistry.primitivefurnacemultiblockentity_nogui.get(), PrimitiveFurnaceMultiblockEntity_nogui::serverTick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new PrimitiveFurnaceMultiblockEntity_nogui(pPos,pState);
    }
    @Override
    protected void interactableNoGui(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        // Transform global hit position into block-local NORTH-facing space
        Vec3 local = toLocalHit(pHit, pPos, pState.getValue(FACING));
        double x = local.x, y = local.y, z = local.z;

        LOGGER.info(String.format("Facing: %s, X: %.3f, Y: %.3f, Z: %.3f", pState.getValue(FACING), x, y, z));

        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (!(blockEntity instanceof PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity)) return;

        IItemHandler itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        if (itemHandler == null) return;

        ItemStack heldItem = pPlayer.getItemInHand(pHand);

        // 🔵 Smeltable input (center zone)
        if (inBox(x, 0.25, 0.75) && inBox(z, 0.25, 0.75) && inBox(y, 0.25, 0.6)) {
            LOGGER.info("BLUE zone - inserting smeltable");
            itemHandler.insertItem(0, heldItem, false);
            heldItem.shrink(1);
            furnaceEntity.sync();
            return;
        }

        // 🔴 Coal input (outer edges, lowered to y 0.0–0.3)
        if (((inBox(x,0.1,0.9) && inBox(z,0.05,0.15)) ||
                (inBox(x,0.1,0.9) && inBox(z,0.85,0.95)) ||
                (inBox(x,0.05,0.15)&& inBox(z,0.2,0.8)) ||
                (inBox(x,0.85,0.95)&& inBox(z,0.2,0.8)))
                && inBox(y, 0.0, 0.3)) {
            LOGGER.info("RED zone - inserting coal");
            if (heldItem.getItem() == Items.COAL) {
                itemHandler.insertItem(1, heldItem, false);
                heldItem.shrink(1);
                furnaceEntity.sync();
            }
            return;
        }
        // 🟩 Green output 1 (bottom left)
        if (inBox(x, 0.1, 0.4) && inBox(z, 0.1, 0.4) && inBox(y, 0.0, 0.4)) {
            LOGGER.info("GREEN zone - output slot 1 clicked");
            // Output logic here
            return;
        }

        // 🟫 Grayish output 2 (bottom right)
        if (inBox(x, 0.6, 0.9) && inBox(z, 0.1, 0.4) && inBox(y, 0.0, 0.4)) {
            LOGGER.info("GRAY zone - output slot 2 clicked");
            // Output logic here
            return;
        }

        LOGGER.info("No zone matched");
    }

    private boolean isWithinBounds(double value, double bound1, double bound2) {
        double min = Math.min(bound1, bound2);
        double max = Math.max(bound1, bound2);
        return value >= min && value <= max;
    }
    private Vec3 toLocalHit(BlockHitResult hit, BlockPos blockPos, Direction facing) {
        double localX = hit.getLocation().x - blockPos.getX();
        double localY = hit.getLocation().y - blockPos.getY();
        double localZ = hit.getLocation().z - blockPos.getZ();

        // Rotate hit based on block's facing
        switch (facing) {
            case NORTH:
                return new Vec3(localX, localY, localZ);
            case SOUTH:
                return new Vec3(1 - localX, localY, 1 - localZ);
            case EAST:
                return new Vec3(1 - localZ, localY, localX);
            case WEST:
                return new Vec3(localZ, localY, 1 - localX);
            default:
                return new Vec3(localX, localY, localZ);
        }
    }
    private boolean inBox(double value, double min, double max) {
        return value >= min && value <= max;
    }
}
