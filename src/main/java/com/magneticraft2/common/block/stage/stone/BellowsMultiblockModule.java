package com.magneticraft2.common.block.stage.stone;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.stone.BellowsMultiblockModuleEntity;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 14-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BellowsMultiblockModule extends BaseEntityBlock {
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public BellowsMultiblockModule() {
        super(BlockBehaviour.Properties.of().noOcclusion().requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));

    }
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING).add(ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BellowsMultiblockModuleEntity(pPos,pState);
    }
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide) {
            // Get the filler block's BlockEntity and read its NBT data
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;

            if (tag != null && tag.contains("controller_x") && tag.contains("controller_y") && tag.contains("controller_z")) {
                // Retrieve the controller position from the NBT data
                BlockPos controllerPos = new BlockPos(tag.getInt("controller_x"), tag.getInt("controller_y"), tag.getInt("controller_z"));
                BlockEntity controllerEntity = pLevel.getBlockEntity(controllerPos);

                // Check if the BlockEntity at the controller position is an instance of BaseBlockEntityMagneticraft2
                if (controllerEntity instanceof BaseBlockEntityMagneticraft2 multiblockController) {
                    if ((multiblockController).menuProvider != null) {
                        NetworkHooks.openScreen((ServerPlayer) pPlayer, (multiblockController).menuProvider, controllerPos);
                    }else{
                        (multiblockController).interactable(pState,pLevel,pPos,pPlayer,pHand,pHit);
                    }
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
}
