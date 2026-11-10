package com.magneticraft2.common.block.stage.steam;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.steam.SteamEngineBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class SteamEngineBlock extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED=BooleanProperty.create("is_formed");
    private static final VoxelShape SOUTH=VoxelShapeUtils.combine(
            Block.box(-32,0,-96,48,10,16),
            Block.box(-20,10,-92,36,45,-48),
            Block.box(-18,10,-48,34,28,-18),
            Block.box(28,5,-55,48,52,-10)
    );
    private static final VoxelShape NORTH=VoxelShapeUtils.rotate(SOUTH,Rotation.CLOCKWISE_180);
    private static final VoxelShape EAST=VoxelShapeUtils.rotate(SOUTH,Rotation.COUNTERCLOCKWISE_90);
    private static final VoxelShape WEST=VoxelShapeUtils.rotate(SOUTH,Rotation.CLOCKWISE_90);

    public SteamEngineBlock(){
        super(BlockBehaviour.Properties.of().strength(5F,8F).requiresCorrectToolForDrops().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(IS_FORMED,false));
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx){
        return defaultBlockState().setValue(FACING,ctx.getHorizontalDirection()).setValue(IS_FORMED,false);
    }
    @Override public BlockState rotate(BlockState state,LevelAccessor level,BlockPos pos,Rotation r){
        return state.setValue(FACING,r.rotate(state.getValue(FACING))).setValue(IS_FORMED,false);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){
        super.createBlockStateDefinition(b);b.add(FACING,IS_FORMED);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,
            InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof SteamEngineBlockEntity engine)||hand!=InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!engine.isFormed())engine.onRightClick();else engine.interactable(state,level,pos,player,hand,hit);
        return InteractionResult.CONSUME;
    }
    @Override protected void interactableNoGui(BlockState state,Level level,BlockPos pos,
            Player player,InteractionHand hand,BlockHitResult hit){}
    @Override public boolean onDestroyedByPlayer(BlockState state,Level level,BlockPos pos,
            Player player,boolean harvest,FluidState fluid){
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof BaseBlockEntityMagneticraft2 be)be.onDestroy(level);
        return super.onDestroyedByPlayer(state,level,pos,player,harvest,fluid);
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext ctx){
        if(!s.getValue(IS_FORMED))return super.getShape(s,level,pos,ctx);
        return switch(s.getValue(FACING)){case NORTH->NORTH;case EAST->EAST;case WEST->WEST;default->SOUTH;};
    }
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext ctx){
        return getShape(s,level,pos,ctx);
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new SteamEngineBlockEntity(pos,state);
    }
    @Override public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,BlockEntityRegistry.STEAM_ENGINE_BE.get(),
                SteamEngineBlockEntity::serverTick);
    }
}
