package com.magneticraft2.common.blockentity.stage.steam;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNode;
import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class SteamEngineOutputBlockEntity extends GearBlockEntity implements IMultiblockModule {
    private BlockPos controllerPos=BlockPos.ZERO;
    private boolean formedModule;

    public SteamEngineOutputBlockEntity(BlockPos pos,BlockState state){
        super(BlockEntityRegistry.STEAM_ENGINE_OUTPUT_BE.get(),pos,state);
    }
    public static <E extends BlockEntity> void serverTick(Level level,BlockPos pos,BlockState state,E be){
        if(!level.isClientSide && be instanceof SteamEngineOutputBlockEntity output)
            output.serverTickGear();
    }
    public @Nullable BlockPos getLinkedControllerPosition(){return formedModule?controllerPos:null;}
    public boolean isServerOverloaded(){return getOrCreateGearNode().isOverloaded();}

    public void setEngineDrive(boolean active,float rpm,float torque){
        GearNode node=getOrCreateGearNode();
        boolean changed=node.isSource()!=active
                || (active && (Math.abs(node.getSpeed()-rpm)>0.01F
                || Math.abs(node.getTorque()-torque)>0.01F));
        if(!changed)return;
        if(active){
            node.setSource(true);node.setSpeed(rpm);node.setTorque(torque);
            node.setDirectionMultiplier(1);node.setMeshPhaseDegrees(0);node.setSourcePos(worldPosition);
            markHasEverRotatedIfMoving(rpm);
        }else node.setSource(false);
        setChanged();updateGearNetwork();
    }

    @Override public int getGearTeeth(){return 1;}
    @Override public float getGearMaxTorque(){return 48F;}
    @Override public boolean isShaftLike(){return true;}
    @Override public Direction.Axis getGearAxis(){
        BlockState s=getBlockState();
        return s.hasProperty(DirectionalBlock.FACING)?s.getValue(DirectionalBlock.FACING).getAxis():Direction.Axis.X;
    }
    @Override public boolean acceptsShaftConnection(Direction side){
        return formedModule && side!=null && getBlockState().hasProperty(DirectionalBlock.FACING)
                && side==getBlockState().getValue(DirectionalBlock.FACING);
    }

    @Override public boolean isValid(Level world,BlockPos pos){return world.getBlockEntity(pos)==this;}
    @Override public void onActivate(Level world,BlockPos pos){
        if(world.isClientSide)return;
        formedModule=true;setChanged();updateGearNetwork();
    }
    @Override public void onDeactivate(Level world,BlockPos pos){
        if(world.isClientSide)return;
        setEngineDrive(false,0,0);formedModule=false;setChanged();
    }
    @Override public String getModuleKey(){return "steam_engine_output";}
    @Override public BlockPos getModuleOffset(){return worldPosition;}
    @Override public IMultiblockModule createModule(Level world,BlockPos pos){return this;}

    @Override public void load(CompoundTag tag){
        super.load(tag);
        controllerPos=new BlockPos(tag.getInt("controller_x"),tag.getInt("controller_y"),tag.getInt("controller_z"));
        formedModule=tag.getBoolean("isformed");
    }
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);
        tag.putInt("controller_x",controllerPos.getX());tag.putInt("controller_y",controllerPos.getY());
        tag.putInt("controller_z",controllerPos.getZ());tag.putBoolean("isformed",formedModule);
    }
    @Override public void handleUpdateTag(CompoundTag tag){load(tag);}
}
