package com.magneticraft2.common.block.stage.steel;
import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.stage.steel.SteelBoilerVesselBlockEntity;
import com.magneticraft2.common.item.stage.steam.BoilerServiceFittingItem;
import com.magneticraft2.common.systems.steam.BoilerAssemblyRecovery;
import com.magneticraft2.common.systems.steam.BoilerAssemblyHost;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.ItemStack;
import java.util.List;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Physical vessel with manual water/fuel warm-up; no Steam or pressure generation. */
public final class SteelBoilerVesselBlock extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED=BooleanProperty.create("is_formed");
    public SteelBoilerVesselBlock(){
        super(BlockBehaviour.Properties.of().strength(5F,8F).requiresCorrectToolForDrops().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(IS_FORMED,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){
        super.createBlockStateDefinition(b);b.add(FACING,IS_FORMED);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx){
        return defaultBlockState().setValue(FACING,ctx.getHorizontalDirection());
    }
    @Override public BlockState rotate(BlockState s,LevelAccessor level,BlockPos pos,Rotation r){
        if (level.getBlockEntity(pos) instanceof SteelBoilerVesselBlockEntity vessel
                && vessel.boilerAssemblyTracked() && vessel.isFormed()) return s;
        return s.setValue(FACING,r.rotate(s.getValue(FACING))).setValue(IS_FORMED,false);
    }
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,
            Player player,InteractionHand hand,BlockHitResult hit){
        if (player.getItemInHand(hand).getItem() instanceof BoilerServiceFittingItem)
            return InteractionResult.PASS;
        if(hand!=InteractionHand.MAIN_HAND ||
                !(world.getBlockEntity(pos) instanceof SteelBoilerVesselBlockEntity vessel))
            return InteractionResult.PASS;
        if(world.isClientSide)return InteractionResult.SUCCESS;
        if(!vessel.isFormed())vessel.onRightClick();
        if (vessel.isFormed()) vessel.warmup().status(player);
        else player.displayClientMessage(Component.translatable(
                "message.magneticraft2.steel_boiler_vessel.incomplete"), true);
        return InteractionResult.CONSUME;
    }
    @Override protected void interactableNoGui(BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        // Interaction is handled by use(): formation and inert-vessel status only.
    }

    @Override public boolean onDestroyedByPlayer(BlockState s,Level world,BlockPos pos,
            Player player,boolean harvest,FluidState fluid){
        if (BoilerAssemblyRecovery.handles(world, pos)) {
            BoilerAssemblyRecovery.playerBreak(world, pos, player);
            return false; // manual atomic teardown; never also run vanilla loot
        }
        if(!world.isClientSide && world.getBlockEntity(pos) instanceof SteelBoilerVesselBlockEntity vessel)
            vessel.onDestroy(world);
        return super.onDestroyedByPlayer(s,world,pos,player,harvest,fluid);
    }
    @Override public void onRemove(BlockState oldState, Level world, BlockPos pos,
            BlockState newState, boolean moving) {
        if (oldState.getBlock() != newState.getBlock())
            BoilerAssemblyRecovery.unexpectedRemoval(world, pos);
        super.onRemove(oldState, world, pos, newState, moving);
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof BoilerAssemblyHost host && host.boilerAssemblyTracked())
            return List.of(); // persisted recovery owns the original controller item
        return super.getDrops(state, params);
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter world,BlockPos pos,CollisionContext context){
        if(s.getValue(IS_FORMED) && world.getBlockEntity(pos) instanceof SteelBoilerVesselBlockEntity v
                && v.localSelection()!=null)return v.localSelection();
        return super.getShape(s,world,pos,context);
    }
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter world,BlockPos pos,CollisionContext context){
        if(s.getValue(IS_FORMED) && world.getBlockEntity(pos) instanceof SteelBoilerVesselBlockEntity v
                && v.localCollision()!=null)return v.localCollision();
        return super.getCollisionShape(s,world,pos,context);
    }
    @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide || type != BlockEntityRegistry.STEEL_BOILER_VESSEL_BE.get()) return null;
        return (level, pos, current, entity) -> {
            if (entity instanceof SteelBoilerVesselBlockEntity vessel) vessel.warmup().tick();
        };
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new SteelBoilerVesselBlockEntity(pos,state);
    }
}
