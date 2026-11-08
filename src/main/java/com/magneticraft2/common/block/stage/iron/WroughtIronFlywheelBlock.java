package com.magneticraft2.common.block.stage.iron;
import com.magneticraft2.common.block.stage.copper.FlywheelBlock_wood;
import com.magneticraft2.common.blockentity.stage.iron.WroughtIronFlywheelBlockEntity;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;

/** Gear V2 axle/controller for a real 3x3x1 supported Iron flywheel. */
public final class WroughtIronFlywheelBlock extends FlywheelBlock_wood {
    public static final BooleanProperty IS_FORMED=BooleanProperty.create("is_formed");
    private static final VoxelShape AXLE=Block.box(5,5,5,11,11,11);
    public WroughtIronFlywheelBlock(){
        super();
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH)
                .setValue(IS_FORMED,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){
        super.createBlockStateDefinition(b);
        b.add(IS_FORMED);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx){
        Direction clicked=ctx.getClickedFace();
        Direction facing=clicked.getAxis().isHorizontal()?clicked:ctx.getHorizontalDirection();
        return validateGearPlacement(ctx,defaultBlockState()
                .setValue(FACING,facing).setValue(IS_FORMED,false));
    }
    @Override protected BlockEntity createBlockEntity(BlockPos pos,BlockState state){
        return new WroughtIronFlywheelBlockEntity(pos,state);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,
            BlockPos pos,CollisionContext ctx){
        if(state.getValue(IS_FORMED)
                && world.getBlockEntity(pos) instanceof WroughtIronFlywheelBlockEntity be
                && be.getLocalVoxelSelection()!=null)return be.getLocalVoxelSelection();
        return AXLE;
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter world,
            BlockPos pos,CollisionContext ctx){
        if(state.getValue(IS_FORMED)
                && world.getBlockEntity(pos) instanceof WroughtIronFlywheelBlockEntity be
                && be.getLocalVoxelCollision()!=null)return be.getLocalVoxelCollision();
        return AXLE;
    }
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,
            Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND||!player.getItemInHand(hand).isEmpty())
            return InteractionResult.PASS;
        if(!(world.getBlockEntity(pos) instanceof WroughtIronFlywheelBlockEntity wheel))
            return InteractionResult.PASS;
        if(world.isClientSide)return InteractionResult.SUCCESS;
        if(!wheel.isFormed()){
            boolean formed=wheel.formWheel();
            player.displayClientMessage(Component.translatable(formed
                    ?"message.magneticraft2.iron_flywheel.formed"
                    :"message.magneticraft2.iron_flywheel.incomplete"),true);
        }else player.displayClientMessage(Component.literal(String.format(Locale.ROOT,
                "Iron Flywheel: %.1f stored RPM",wheel.getStoredSpeed())),true);
        return InteractionResult.CONSUME;
    }
    @Override public boolean onDestroyedByPlayer(BlockState state,Level world,BlockPos pos,
            Player player,boolean harvest,FluidState fluid){
        if(!world.isClientSide&&world.getBlockEntity(pos) instanceof WroughtIronFlywheelBlockEntity be)
            be.dismantleWheel();
        return super.onDestroyedByPlayer(state,world,pos,player,harvest,fluid);
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level world,BlockState state,BlockEntityType<T> type){
        return world.isClientSide?null:createTickerHelper(type,
                BlockEntityRegistry.IRON_FLYWHEEL_BE.get(),
                WroughtIronFlywheelBlockEntity::serverTick);
    }
}
