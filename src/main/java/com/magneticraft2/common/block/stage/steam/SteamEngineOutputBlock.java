package com.magneticraft2.common.block.stage.steam;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.steam.SteamEngineOutputBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public final class SteamEngineOutputBlock extends GearBlock {
    public SteamEngineOutputBlock(){
        super(BlockBehaviour.Properties.of().strength(5F,8F).requiresCorrectToolForDrops().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.EAST));
    }
    @Override protected BlockEntity createBlockEntity(BlockPos pos,BlockState state){
        return new SteamEngineOutputBlockEntity(pos,state);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){
        Direction face=ctx.getClickedFace();
        if(face.getAxis()==Direction.Axis.Y)face=ctx.getHorizontalDirection().getOpposite();
        return validateGearPlacement(ctx,defaultBlockState().setValue(FACING,face));
    }
    @Override public int getPlacementGearTeeth(BlockState state){return 1;}
    @Override public boolean isShaftLikeForPlacement(BlockState state){return true;}
    @Override public Direction.Axis getPlacementGearAxis(BlockState state){return state.getValue(FACING).getAxis();}
    @Override public boolean onDestroyedByPlayer(BlockState state,Level level,BlockPos pos,
            Player player,boolean harvest,FluidState fluid){
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof SteamEngineOutputBlockEntity output){
            BlockPos controllerPos=output.getLinkedControllerPosition();
            if(controllerPos!=null && level.getBlockEntity(controllerPos) instanceof BaseBlockEntityMagneticraft2 controller)
                controller.onDestroy(level);
        }
        return super.onDestroyedByPlayer(state,level,pos,player,harvest,fluid);
    }
    @Override public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,
                BlockEntityRegistry.STEAM_ENGINE_OUTPUT_BE.get(),
                SteamEngineOutputBlockEntity::serverTick);
    }
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity,A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual,BlockEntityType<E> expected,BlockEntityTicker<? super E> ticker){
        return expected==actual?(BlockEntityTicker<A>)ticker:null;
    }
}
