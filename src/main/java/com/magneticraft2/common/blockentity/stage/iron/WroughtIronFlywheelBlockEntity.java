package com.magneticraft2.common.blockentity.stage.iron;

import com.magneticraft2.common.block.stage.iron.WroughtIronFlywheelBlock;
import com.magneticraft2.common.blockentity.stage.copper.FlywheelBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockRenderParts;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelHost;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelNbt;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelProfiles;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A single axle/node within a reserved, 3x3 vertical wheel sweep.
 * The existing Gear V2 manager owns coasting/load/sync and power selection.
 * Its JSON layout and V2 shapes reserve all six AIR clearance cells.
 */
public final class WroughtIronFlywheelBlockEntity extends FlywheelBlockEntity_wood
        implements MultiblockVoxelHost {
    private static final String PREFIX="iron_flywheel_";
    private static final String PROFILE="magneticraft2:iron_flywheel";
    private boolean formed;
    private String blueprint="";
    @Nullable private MultiblockController assembly;
    @Nullable private CompoundTag pendingAssembly;
    @Nullable private VoxelShape collision,selection;
    private final AABB renderBounds;
    public WroughtIronFlywheelBlockEntity(BlockPos pos,BlockState state){
        super(BlockEntityRegistry.IRON_FLYWHEEL_BE.get(),pos,state);
        renderBounds=new AABB(pos).inflate(2D);
    }
    public boolean isFormed(){return formed;}
    @Override public float getGearMaxTorque(){return 24F;}
    @Override public float getInertiaTorque(){return 12F;}
    @Override protected float getCoastDecayScale(){return 0.40F;}
    @Override public boolean hasStoredInertia(){return formed&&super.hasStoredInertia();}
    @Override public void captureExternalDrive(float speed,int direction){
        if(formed)super.captureExternalDrive(speed,direction);
    }
    @Override public boolean acceptsShaftConnection(Direction face){
        return formed&&super.acceptsShaftConnection(face);
    }
    @Override public boolean supportsExternalGearMesh(){return false;}
    @Nullable public VoxelShape getLocalVoxelCollision(){return collision;}
    @Nullable public VoxelShape getLocalVoxelSelection(){return selection;}
    @Override public void setMultiblockVoxelShapes(VoxelShape coll,VoxelShape select){
        collision=coll;selection=select;broadcast();
    }
    private static MultiblockStructure copy(MultiblockStructure src){
        Map<String,List<List<String>>> layout=new HashMap<>();
        for(var e:src.getLayout().entrySet()){
            List<List<String>> rows=new ArrayList<>();
            for(List<String> row:e.getValue())rows.add(new ArrayList<>(row));
            layout.put(e.getKey(),rows);
        }
        return new MultiblockStructure(src.getDimensions().clone(),layout,
                new HashMap<>(src.getBlocks()));
    }
    @Nullable private Multiblock blueprint(){
        Direction facing=getBlockState().getValue(WroughtIronFlywheelBlock.FACING);
        return MultiblockRegistry.getRegisteredMultiblock("magneticraft2",
                PREFIX+facing.getName());
    }
    private void restoreAssembly(){
        if(!formed||assembly!=null||pendingAssembly==null||level==null)return;
        Multiblock found=blueprint();
        if(found==null)return; // Datapacks may still be loading.
        MultiblockController restored=new MultiblockController(copy(found.getStructure()));
        restored.loadFromNBT(pendingAssembly);
        restored.setMasterPos(worldPosition);
        assembly=restored;
        pendingAssembly=null;
    }
    public boolean formWheel(){
        if(level==null||level.isClientSide||formed||!MultiblockVoxelProfiles.exists(PROFILE))
            return false;
        Multiblock mb=blueprint();
        if(mb==null)return false;
        MultiblockStructure pattern=copy(mb.getStructure());
        MultiblockController ctrl=new MultiblockController(pattern);
        if(!ctrl.createStructure(level,worldPosition))return false;
        Direction facing=getBlockState().getValue(WroughtIronFlywheelBlock.FACING);
        try{
            MultiblockRenderParts.assign(level,worldPosition,ctrl,"iron_flywheel",
                    facing,mb.getSettings().getRenderAirAnchors(),PROFILE);
        }catch(RuntimeException invalid){
            // A failed model/voxel assignment must not dismantle the user's
            // valid construction or spawn duplicate item drops. Restore the
            // exact original blocks, including AIR, before reporting failure.
            // These are the same snapshots used by normal teardown.
            for(var old:ctrl.getBlockMap().entrySet()){
                level.setBlock(old.getKey(),old.getValue(),Block.UPDATE_ALL);
            }
            collision=null;
            selection=null;
            org.apache.logging.log4j.LogManager
                    .getLogger("Magneticraft2IronFlywheel")
                    .error("Could not form iron flywheel at {}",worldPosition,invalid);
            broadcast();
            return false;
        }
        assembly=ctrl;
        formed=true;
        blueprint=mb.getName();
        level.setBlock(worldPosition,getBlockState().setValue(
                WroughtIronFlywheelBlock.IS_FORMED,true),Block.UPDATE_ALL);
        updateGearNetwork();
        broadcast();
        return true;
    }
    public void dismantleWheel(){
        if(level==null||level.isClientSide||!formed)return;
        restoreAssembly();
        if(assembly==null)return; // Do not lose the saved original layout.
        formed=false;
        getOrCreateGearNode().setSource(false);
        GearNetworkManager.getInstance().removeGear(worldPosition,level);
        assembly.destroyStructure(level);
        assembly=null;pendingAssembly=null;
    }
    private void broadcast(){
        setChanged();
        if(level!=null&&!level.isClientSide)
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),
                    Block.UPDATE_ALL);
    }
    public static <E extends BlockEntity> void serverTick(Level world,BlockPos pos,
            BlockState state,E tile){
        if(world.isClientSide||!(tile instanceof WroughtIronFlywheelBlockEntity be))return;
        if(be.formed)be.restoreAssembly();
        be.serverTickGear();
        be.markHasEverRotatedIfMoving(be.getServerSpeed());
    }
    @Override public AABB getRenderBoundingBox(){return renderBounds;}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);
        tag.putBoolean("IronFlywheelFormed",formed);
        tag.putString("IronFlywheelBlueprint",blueprint);
        if(assembly!=null)tag.put("IronFlywheelStructure",assembly.saveToNBT());
        else if(pendingAssembly!=null)tag.put("IronFlywheelStructure",pendingAssembly);
        MultiblockVoxelNbt.write(tag,"iron_flywheel_collision",collision);
        MultiblockVoxelNbt.write(tag,"iron_flywheel_selection",selection);
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);
        formed=tag.getBoolean("IronFlywheelFormed");
        blueprint=tag.getString("IronFlywheelBlueprint");
        pendingAssembly=tag.contains("IronFlywheelStructure")
                ?tag.getCompound("IronFlywheelStructure").copy():null;
        assembly=null;
        collision=MultiblockVoxelNbt.read(tag,"iron_flywheel_collision");
        selection=MultiblockVoxelNbt.read(tag,"iron_flywheel_selection");
    }
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(Connection net,ClientboundBlockEntityDataPacket p){
        if(p.getTag()!=null)load(p.getTag());
    }
}
