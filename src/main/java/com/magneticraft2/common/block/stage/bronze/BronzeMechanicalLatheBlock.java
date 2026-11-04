package com.magneticraft2.common.block.stage.bronze;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeMechanicalLatheBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Formable, JSON-defined 5x2x3 Bronze Lathe structure (machining to follow). */
public final class BronzeMechanicalLatheBlock extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");

    public BronzeMechanicalLatheBlock() {
        super(BlockBehaviour.Properties.of().strength(3.5F)
                .requiresCorrectToolForDrops().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH)
                .setValue(IS_FORMED,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(FACING,IS_FORMED);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING,ctx.getHorizontalDirection());
    }
    @Override public BlockState rotate(BlockState state,LevelAccessor world,BlockPos pos,Rotation rotation) {
        return state.setValue(FACING,rotation.rotate(state.getValue(FACING)))
                .setValue(IS_FORMED,false);
    }
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,
            Player player,InteractionHand hand,BlockHitResult hit) {
        if(hand!=InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if(!(world.getBlockEntity(pos) instanceof BronzeMechanicalLatheBlockEntity lathe))
            return InteractionResult.PASS;
        if(world.isClientSide) return InteractionResult.SUCCESS;
        if(!lathe.isFormed()) lathe.onRightClick();
        if(lathe.isFormed())lathe.interactable(state,world,pos,player,hand,hit);
        else player.displayClientMessage(Component.literal("Incomplete Mechanical Lathe: see Bronze Age construction layout."),true);
        return InteractionResult.CONSUME;
    }
    @Override public boolean onDestroyedByPlayer(BlockState state,Level world,BlockPos pos,
            Player player,boolean harvest,FluidState fluid) {
        if(!world.isClientSide && world.getBlockEntity(pos) instanceof BronzeMechanicalLatheBlockEntity lathe)
            lathe.onDestroy(world);
        return super.onDestroyedByPlayer(state,world,pos,player,harvest,fluid);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) {
        if(state.getValue(IS_FORMED) && world.getBlockEntity(pos) instanceof BronzeMechanicalLatheBlockEntity lathe
                && lathe.getLocalVoxelSelection()!=null) return lathe.getLocalVoxelSelection();
        return super.getShape(state,world,pos,context);
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) {
        if(state.getValue(IS_FORMED) && world.getBlockEntity(pos) instanceof BronzeMechanicalLatheBlockEntity lathe
                && lathe.getLocalVoxelCollision()!=null) return lathe.getLocalVoxelCollision();
        return super.getCollisionShape(state,world,pos,context);
    }
    @Override
    protected void interactableNoGui(BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof BronzeMechanicalLatheBlockEntity lathe) {
            lathe.interactable(state, world, pos, player, hand, hit);
        }
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return new BronzeMechanicalLatheBlockEntity(pos,state);
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,
                BlockEntityRegistry.BRONZE_MECHANICAL_LATHE_BE.get(),
                BronzeMechanicalLatheBlockEntity::serverTick);
    }
}
