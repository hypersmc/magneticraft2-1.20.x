package com.magneticraft2.common.block.stage.steam;

import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.steam.SteamEngineInletBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public final class SteamEngineInletBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING =
            net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

    public SteamEngineInletBlock() {
        super(BlockBehaviour.Properties.of().strength(5F,8F)
                .requiresCorrectToolForDrops().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.WEST));
    }

    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING,ctx.getHorizontalDirection().getOpposite());
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {
        b.add(FACING);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return new SteamEngineInletBlockEntity(pos,state);
    }
    @Override public boolean onDestroyedByPlayer(BlockState state,Level level,BlockPos pos,
            Player player,boolean harvest,FluidState fluid) {
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof SteamEngineInletBlockEntity inlet) {
            BlockPos controllerPos=inlet.getLinkedControllerPosition();
            if(controllerPos!=null && level.getBlockEntity(controllerPos) instanceof BaseBlockEntityMagneticraft2 controller)
                controller.onDestroy(level);
        }
        return super.onDestroyedByPlayer(state,level,pos,player,harvest,fluid);
    }
    public static BlockPos externalPipePosition(BlockPos inlet,BlockState state) {
        return inlet.relative(state.getValue(FACING));
    }
}
