package com.magneticraft2.common.blockentity.stage.steam;

import com.magneticraft2.common.block.stage.steam.SteamEngineInletBlock;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class SteamEngineInletBlockEntity extends BlockEntity implements IMultiblockModule {
    private BlockPos controllerPos=BlockPos.ZERO;
    private boolean formedModule;

    public SteamEngineInletBlockEntity(BlockPos pos,BlockState state) {
        super(BlockEntityRegistry.STEAM_ENGINE_INLET_BE.get(),pos,state);
    }
    public @Nullable BlockPos getLinkedControllerPosition() {
        return formedModule?controllerPos:null;
    }
    @Override public boolean isValid(Level world,BlockPos pos){return world.getBlockEntity(pos)==this;}
    @Override public void onActivate(Level world,BlockPos pos){
        if(world.isClientSide)return;
        formedModule=true;
        BlockState state=world.getBlockState(worldPosition);
        if(state.hasProperty(SteamEngineInletBlock.FORMED))
            world.setBlock(worldPosition,state.setValue(SteamEngineInletBlock.FORMED,true),Block.UPDATE_ALL);
        setChanged();sync();
    }
    @Override public void onDeactivate(Level world,BlockPos pos){
        if(world.isClientSide)return;
        formedModule=false;
        BlockState state=world.getBlockState(worldPosition);
        if(state.hasProperty(SteamEngineInletBlock.FORMED))
            world.setBlock(worldPosition,state.setValue(SteamEngineInletBlock.FORMED,false),Block.UPDATE_ALL);
        setChanged();sync();
    }
    @Override public String getModuleKey(){return "steam_engine_inlet";}
    @Override public BlockPos getModuleOffset(){return worldPosition;}
    @Override public IMultiblockModule createModule(Level world,BlockPos pos){return this;}
    @Override public void load(CompoundTag tag){
        super.load(tag);
        controllerPos=new BlockPos(tag.getInt("controller_x"),tag.getInt("controller_y"),tag.getInt("controller_z"));
        formedModule=tag.getBoolean("isformed");
    }
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);
        tag.putInt("controller_x",controllerPos.getX());
        tag.putInt("controller_y",controllerPos.getY());
        tag.putInt("controller_z",controllerPos.getZ());
        tag.putBoolean("isformed",formedModule);
    }
    @Override public void handleUpdateTag(CompoundTag tag){load(tag);}
    @Override public CompoundTag getUpdateTag(){CompoundTag t=new CompoundTag();saveAdditional(t);return t;}
    private void sync(){
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);
    }
}
