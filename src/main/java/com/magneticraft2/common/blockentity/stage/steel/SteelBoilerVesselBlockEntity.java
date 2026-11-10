package com.magneticraft2.common.blockentity.stage.steel;
import com.magneticraft2.common.block.stage.steel.SteelBoilerVesselBlock;
import com.magneticraft2.common.block.stage.steam.BoilerServiceFittingBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockRenderParts;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelHost;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelNbt;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelProfiles;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import com.magneticraft2.common.utils.MultiBlockProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** No BE ticking, reservoir, material duplication, POWER, AIR or STEAM capability. */
public final class SteelBoilerVesselBlockEntity extends BaseBlockEntityMagneticraft2 implements MultiblockVoxelHost {
    private static final String PREFIX="steel_boiler_vessel_";
    private boolean formed;
    private String blueprintName="",replacementModel="",voxelProfileName="";
    private Direction matchedFacing=Direction.SOUTH;
    private List<BlockPos> renderAirAnchors=List.of();
    @Nullable private VoxelShape collision,selection;
    public SteelBoilerVesselBlockEntity(BlockPos pos,BlockState state){
        super(BlockEntityRegistry.STEEL_BOILER_VESSEL_BE.get(),pos,state);
        menuProvider=null;
    }
    public boolean isFormed(){return formed;}
    @Override protected void interactableNoGui(BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        // Steel IV-C vessel has no GUI or processing. The controller block handles
        // formation and status feedback; no inventory or capability is exposed.
    }
    @Override public void setMultiblockVoxelShapes(VoxelShape coll,VoxelShape sel){
        collision=coll;selection=sel;sync();
    }
    @Nullable public VoxelShape localCollision(){return collision;}
    @Nullable public VoxelShape localSelection(){return selection==null?collision:selection;}
    private static Direction facingFromName(String name){
        if(name.endsWith("_north"))return Direction.NORTH;
        if(name.endsWith("_east"))return Direction.EAST;
        if(name.endsWith("_west"))return Direction.WEST;
        return Direction.SOUTH;
    }
    @Override protected @Nullable MultiblockStructure identifyMultiblockStructure(Level world,BlockPos pos){
        for(Multiblock bp:MultiblockRegistry.getRegisteredMultiblocks().values()){
            if(!bp.getName().startsWith(PREFIX))continue;
            MultiblockStructure s=bp.getStructure();
            if(!matchesStructure(world,pos,s,bp))continue;
            blueprintName=bp.getName();replacementModel=bp.getSettings().getReplaceWhenFormed();
            voxelProfileName=bp.getSettings().getVoxelProfile();
            renderAirAnchors=bp.getSettings().getRenderAirAnchors();
            matchedFacing=facingFromName(blueprintName);
            return s;
        }
        return null;
    }
    @Override protected @Nullable MultiblockController createMultiblockController(){
        if(level==null||level.isClientSide||formed)return null;
        MultiblockStructure s=identifyMultiblockStructure(level,worldPosition);
        if(s==null||!MultiblockVoxelProfiles.exists(voxelProfileName))return null;
        MultiblockController c=new MultiblockController(s);
        c.identifyAndAddModules(level,worldPosition,s);
        if(!c.createStructure(level,worldPosition))return null;
        c.setFormed(true);setMultiblockController(c);formed=true;
        BlockState state=level.getBlockState(worldPosition);
        if(state.hasProperty(SteelBoilerVesselBlock.IS_FORMED))
            level.setBlock(worldPosition,state.setValue(SteelBoilerVesselBlock.IS_FORMED,true)
                 .setValue(SteelBoilerVesselBlock.FACING,matchedFacing),Block.UPDATE_ALL);
        MultiblockRenderParts.assign(level,worldPosition,c,"steel_boiler_vessel",
                matchedFacing,renderAirAnchors,voxelProfileName);
        requestModelDataUpdate();sync();return c;
    }

    /**
     * Steam 0-A fittings are independently owned blocks, not the original
     * multiblock filler positions. Drop them before the vessel is dismantled.
     */
    @Override public void onDestroy(Level world) {
        if (!world.isClientSide && formed) {
            formed = false; // prevents duplicate attachment drops on reentry
            BlockState controllerState = world.getBlockState(worldPosition);
            if (controllerState.hasProperty(SteelBoilerVesselBlock.FACING)) {
                Direction facing = controllerState.getValue(SteelBoilerVesselBlock.FACING);
                BoilerServiceFittingBlock.Role[] roles = BoilerServiceFittingBlock.Role.values();
                for (int i = roles.length - 1; i >= 0; i--) {
                    BoilerServiceFittingBlock.Role role = roles[i];
                    BlockPos target = BoilerServiceFittingBlock.attachmentPosition(worldPosition, role, facing);
                    if (!world.hasChunkAt(target)) continue;
                    BlockState part = world.getBlockState(target);
                    if (part.getBlock() instanceof BoilerServiceFittingBlock fitting
                            && fitting.role() == role
                            && part.getValue(BoilerServiceFittingBlock.FACING) == facing)
                        world.destroyBlock(target, true);
                }
            }
        }
        super.onDestroy(world);
    }

    @Override public void load(CompoundTag tag){
        super.load(tag);
        MultiblockPersistentData saved=loadMultiblockData(tag);
        blueprintName=saved.blueprintName();formed=saved.formed();
        replacementModel=saved.replacementModel();matchedFacing=facingFromName(blueprintName);
        voxelProfileName=tag.getString("BoilerVesselVoxelProfile");
        collision=MultiblockVoxelNbt.read(tag,"BoilerVesselCollision");
        selection=MultiblockVoxelNbt.read(tag,"BoilerVesselSelection");
    }
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);
        saveMultiblockData(tag,blueprintName,formed,replacementModel);
        if(!voxelProfileName.isBlank())tag.putString("BoilerVesselVoxelProfile",voxelProfileName);
        MultiblockVoxelNbt.write(tag,"BoilerVesselCollision",collision);
        MultiblockVoxelNbt.write(tag,"BoilerVesselSelection",selection);
    }
    @Override public CompoundTag sync(){
        setChanged();
        if(level!=null&&!level.isClientSide)
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);
        return getUpdateTag();
    }
    @Override public CompoundTag getUpdateTag(){CompoundTag tag=new CompoundTag();saveAdditional(tag);return tag;}
    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(Connection conn,ClientboundBlockEntityDataPacket packet){
        if(packet.getTag()!=null)load(packet.getTag());
    }
    @Override public int capacityE(){return 0;}
    @Override public int maxtransferE(){return 0;}
    @Override public int capacityH(){return 0;}
    @Override public int maxtransferH(){return 0;}
    @Override public int capacityW(){return 0;}
    @Override public int maxtransferW(){return 0;}
    @Override public int capacityF(){return 0;}
    @Override public int tanks(){return 0;}
    @Override public int invsize(){return 0;}
    @Override public int capacityP(){return 0;}
    @Override public int maxtransferP(){return 0;}
    @Override public boolean itemcape(){return false;}
    @Override public boolean energycape(){return false;}
    @Override public boolean heatcape(){return false;}
    @Override public boolean wattcape(){return false;}
    @Override public boolean fluidcape(){return false;}
    @Override public boolean pressurecape(){return false;}
    @Override public boolean HeatCanReceive(){return false;}
    @Override public boolean HeatCanSend(){return false;}
    @Override public boolean WattCanReceive(){return false;}
    @Override public boolean WattCanSend(){return false;}
    @Override public boolean EnergyCanReceive(){return false;}
    @Override public boolean EnergyCanSend(){return false;}
    @Override public boolean PressureCanReceive(){return false;}
    @Override public boolean PressureCanSend(){return false;}
    @Override public Level getThisWorld(){return level;}
    @Override public BlockPos getThisPosition(){return worldPosition;}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(6D);}
    @Override public @NotNull ModelData getModelData(){
        return super.getModelData().derive().with(MultiBlockProperties.MODEL_NAME,replacementModel).build();
    }
    @Override public Component getDisplayName(){
        return Component.translatable("block.magneticraft2.steel_boiler_vessel");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inv,
            net.minecraft.world.entity.player.Player player){return null;}
}
