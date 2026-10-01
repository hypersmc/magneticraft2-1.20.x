package com.magneticraft2.common.block.general;

import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.general.Multiblockfiller_tile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import static com.magneticraft2.common.block.general.BaseBlockMagneticraft2.FACING;
import static com.magneticraft2.common.block.general.BlueprintMultiblock.IS_FORMED;

/**
 * @author JumpWatch on 01-07-2024
 * @Project mgc2-1.20
* @version 1.0.0
 */
public class Multiblockfiller extends BaseEntityBlock {
    private static final Logger LOGGER = LogManager.getLogger("MGC2MultiblockFiller");

    public Multiblockfiller() {
        super(BlockBehaviour.Properties.of().noOcclusion().requiresCorrectToolForDrops());
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!pLevel.isClientSide) {
            // Get the filler block's BlockEntity and read its NBT data
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;
            if (tag != null && tag.contains("controller_x") && tag.contains("controller_y") && tag.contains("controller_z")) {
                // Retrieve the controller position from the NBT data
                BlockPos controllerPos = new BlockPos(tag.getInt("controller_x"), tag.getInt("controller_y"), tag.getInt("controller_z"));
                BlockEntity controllerEntity = pLevel.getBlockEntity(controllerPos);
                Block bl = pLevel.getBlockState(controllerPos).getBlock();
                // Check if the BlockEntity at the controller position is an instance of BaseBlockEntityMagneticraft2
                if (controllerEntity instanceof BaseBlockEntityMagneticraft2 multiblockController) {
                    blockEntity.saveWithoutMetadata();
                    if ((multiblockController).menuProvider != null) {
                        NetworkHooks.openScreen((ServerPlayer) pPlayer, (multiblockController).menuProvider, controllerPos);
                    }
                }
                if (bl instanceof BaseBlockMagneticraft2 multiblockControllerblock) {
                    BlockState controllerState = pLevel.getBlockState(controllerPos);
                    multiblockControllerblock.interactableNoGui(controllerState, pLevel, controllerPos, pPlayer, pHand, pHit);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
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

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        if (pPlacer instanceof Player){
            pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 2);
        }
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter level, BlockPos pos, CollisionContext pContext) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;

        if (tag != null && tag.contains("controller_x") && tag.contains("controller_y") && tag.contains("controller_z")) {
            // Retrieve the controller position from the NBT data
            BlockPos controllerPos = new BlockPos(tag.getInt("controller_x"), tag.getInt("controller_y"), tag.getInt("controller_z"));
            BlockEntity controllerEntity = level.getBlockEntity(controllerPos);
            Block controllerBlock = level.getBlockState(controllerPos).getBlock();
            BlockState controllerState = level.getBlockState(controllerPos);
            // Check if the BlockEntity at the controller position is an instance of BaseBlockEntityMagneticraft2
            if (controllerEntity instanceof BaseBlockEntityMagneticraft2 multiblockController) {
                boolean isformed = controllerBlock.getStateDefinition().any().getValue(IS_FORMED);
                if (isformed) {
                    // getShape() on the controller may itself be clipped to the controller's
                    // local block for server-safe interaction. The visual shape remains the
                    // complete formed multiblock and is therefore the correct source when
                    // deriving this filler block's local slice.
                    VoxelShape controllerShape = controllerBlock.getVisualShape(controllerState, level, controllerPos, pContext);
                    double dx = controllerPos.getX() - pos.getX();
                    double dy = controllerPos.getY() - pos.getY();
                    double dz = controllerPos.getZ() - pos.getZ();
                    VoxelShape localShape = controllerShape.move(dx, dy, dz);
                    return Shapes.join(localShape, Shapes.block(), BooleanOp.AND).optimize();
                }
            }
        }
        return super.getShape(pState, level, pos, pContext);
    }


    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return super.getCollisionShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
        return super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new Multiblockfiller_tile(blockPos, blockState);
    }
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F;
    }
}
