package com.magneticraft2.common.blockentity.stage.steam;

import com.magneticraft2.common.block.stage.steam.SteamEngineBlock;
import com.magneticraft2.common.block.stage.steam.SteamEngineInletBlock;
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
import com.magneticraft2.common.systems.PRESSURE.SteamNetworkManager;
import com.magneticraft2.common.systems.steam.SteamEngineMath;
import com.magneticraft2.common.systems.steam.SteamEngineParameters;
import com.magneticraft2.common.utils.MultiBlockProperties;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class SteamEngineBlockEntity extends BaseBlockEntityMagneticraft2 implements MultiblockVoxelHost {
    private static final String PREFIX="steam_engine_";
    private boolean formed,running;
    private String blueprintName="",replacementModel="",voxelProfileName="";
    private List<BlockPos> renderAirAnchors=List.of();
    @Nullable private VoxelShape collision,selection;
    private Direction matchedFacing=Direction.SOUTH;
    @Nullable private BlockPos inletPos,outputPos;
    private long steamCreditMilli,steamDrawnUnits,steamExhaustedMilli;
    private float lastRpm,lastTorque,lastDemand;
    private double lastPressure;

    public SteamEngineBlockEntity(BlockPos pos,BlockState state){
        super(BlockEntityRegistry.STEAM_ENGINE_BE.get(),pos,state);
    }
    public boolean isFormed(){return formed;}
    public boolean isRunning(){return running;}
    public Direction getFacing(){return getBlockState().hasProperty(SteamEngineBlock.FACING)
            ?getBlockState().getValue(SteamEngineBlock.FACING):Direction.SOUTH;}

    public static <E extends BlockEntity> void serverTick(Level level,BlockPos pos,BlockState state,E be){
        if(!level.isClientSide && be instanceof SteamEngineBlockEntity engine)engine.tickServer();
    }

    private void tickServer(){
        if(!formed||level==null||level.isClientSide)return;
        SteamEngineParameters.Rules rules=SteamEngineParameters.rules();
        SteamEngineInletBlockEntity inlet=getInlet();
        SteamEngineOutputBlockEntity output=getOutput();
        if(rules==null||inlet==null||output==null){stopOutput(output);return;}

        BlockPos pipe=SteamEngineInletBlock.externalPipePosition(inlet.getBlockPos(),inlet.getBlockState());
        if(!level.hasChunkAt(pipe)||!level.getBlockState(pipe).is(BlockRegistry.STEAM_PIPE.get())){
            lastPressure=0;stopOutput(output);return;
        }

        lastPressure=SteamNetworkManager.gaugePressureKPa((net.minecraft.server.level.ServerLevel)level,pipe);
        SteamEngineMath.Drive drive=SteamEngineMath.drive(lastPressure,rules.minimumPressureKPa(),
                rules.ratedPressureKPa(),rules.minimumRpm(),rules.ratedRpm(),rules.ratedTorque());
        if(!drive.available()){stopOutput(output);return;}

        lastDemand=GearNetworkManager.getInstance().getSourceTorqueDemand(level,output.getBlockPos());
        boolean overloaded=output.isServerOverloaded();
        int useMilli=SteamEngineMath.milliSteamPerTick(lastDemand,drive.torque(),overloaded,
                rules.idleMilliSteamPerTick(),rules.loadMilliSteamPerTorqueTick());
        if(steamCreditMilli<useMilli){
            long missing=useMilli-steamCreditMilli;
            int request=(int)Math.min(rules.maxDrawUnitsPerTick(),Math.max(1,(missing+999)/1000));
            int drawn=SteamNetworkManager.takeSteam((net.minecraft.server.level.ServerLevel)level,pipe,request);
            if(drawn>0){steamDrawnUnits+=drawn;steamCreditMilli+=drawn*1000L;setChanged();}
        }
        if(steamCreditMilli<useMilli){stopOutput(output);return;}

        steamCreditMilli-=useMilli;
        steamExhaustedMilli+=useMilli;
        lastRpm=drive.rpm();lastTorque=drive.torque();
        output.setEngineDrive(true,lastRpm,lastTorque);
        boolean nowRunning=!output.isServerOverloaded()&&lastRpm>0.01F;
        if(nowRunning!=running){running=nowRunning;sync();}else setChanged();

        if(useMilli>0 && level.getGameTime()%10L==0L){
            BlockPos exhaust=localOffset(0,3,5);
            ((net.minecraft.server.level.ServerLevel)level).sendParticles(ParticleTypes.CLOUD,
                    exhaust.getX()+0.5,exhaust.getY()+0.4,exhaust.getZ()+0.5,
                    2,0.12,0.08,0.12,0.015);
        }
    }

    private void stopOutput(@Nullable SteamEngineOutputBlockEntity output){
        if(output!=null)output.setEngineDrive(false,0,0);
        lastRpm=0;lastTorque=0;lastDemand=0;
        if(running){running=false;sync();}
    }

    private BlockPos localOffset(int x,int y,int z){
        Direction f=getFacing();Direction right=f.getCounterClockWise();
        return worldPosition.relative(right,x).relative(f,z).above(y);
    }
    private @Nullable SteamEngineInletBlockEntity getInlet(){
        if(level==null||inletPos==null||!level.hasChunkAt(inletPos))return null;
        return level.getBlockEntity(inletPos) instanceof SteamEngineInletBlockEntity inlet?inlet:null;
    }
    private @Nullable SteamEngineOutputBlockEntity getOutput(){
        if(level==null||outputPos==null||!level.hasChunkAt(outputPos))return null;
        return level.getBlockEntity(outputPos) instanceof SteamEngineOutputBlockEntity output?output:null;
    }
    @Override public void setMultiblockVoxelShapes(VoxelShape coll,VoxelShape sel){
        collision=coll;selection=sel;sync();
    }
    public @Nullable VoxelShape localCollision(){return collision;}
    public @Nullable VoxelShape localSelection(){return selection==null?collision:selection;}

    public float getDriveRotationDegrees(float partialTicks){
        SteamEngineOutputBlockEntity out=getOutput();
        return out==null?0:out.getVisualRotationDegrees(partialTicks);
    }
    public boolean isOutputOverloadedForAnimation(){
        SteamEngineOutputBlockEntity out=getOutput();
        if(out==null)return false;
        return level!=null&&level.isClientSide
                ?out.isClientOverloaded():out.isServerOverloaded();
    }

    @Override protected MultiblockController createMultiblockController(){
        if(level==null||level.isClientSide||formed)return null;
        MultiblockStructure structure=identifyMultiblockStructure(level,worldPosition);
        if(structure==null||!MultiblockVoxelProfiles.exists(voxelProfileName))return null;
        MultiblockController controller=new MultiblockController(structure);
        if(!controller.identifyAndAddModules(level,worldPosition,structure)
                || !controller.createStructure(level,worldPosition))return null;
        controller.setFormed(true);setMultiblockController(controller);formed=true;
        inletPos=controller.getmodulePos("steam_engine_inlet");
        outputPos=controller.getmodulePos("steam_engine_output");
        configureModules();

        BlockState state=level.getBlockState(worldPosition);
        level.setBlock(worldPosition,state.setValue(SteamEngineBlock.IS_FORMED,true)
                .setValue(SteamEngineBlock.FACING,matchedFacing),Block.UPDATE_ALL);
        MultiblockRenderParts.assign(level,worldPosition,controller,"steam_engine",
                matchedFacing,renderAirAnchors,voxelProfileName);
        requestModelDataUpdate();sync();return controller;
    }

    private void configureModules(){
        if(level==null)return;
        Direction inletFacing=matchedFacing.getClockWise();
        Direction outputFacing=matchedFacing.getCounterClockWise();
        if(inletPos!=null&&level.hasChunkAt(inletPos)){
            BlockState s=level.getBlockState(inletPos);
            if(s.is(BlockRegistry.STEAM_ENGINE_INLET.get()))
                level.setBlock(inletPos,s.setValue(SteamEngineInletBlock.FACING,inletFacing),Block.UPDATE_ALL);
        }
        if(outputPos!=null&&level.hasChunkAt(outputPos)){
            BlockState s=level.getBlockState(outputPos);
            if(s.is(BlockRegistry.STEAM_ENGINE_OUTPUT.get())&&s.hasProperty(DirectionalBlock.FACING))
                level.setBlock(outputPos,s.setValue(DirectionalBlock.FACING,outputFacing),Block.UPDATE_ALL);
            if(level.getBlockEntity(outputPos) instanceof SteamEngineOutputBlockEntity out)out.updateGearNetwork();
        }
    }

    @Override protected MultiblockStructure identifyMultiblockStructure(Level world,BlockPos pos){
        for(Multiblock mb:MultiblockRegistry.getRegisteredMultiblocks().values()){
            if(!mb.getName().startsWith(PREFIX))continue;
            if(!matchesStructure(world,pos,mb.getStructure(),mb))continue;
            blueprintName=mb.getName();replacementModel=mb.getSettings().getReplaceWhenFormed();
            voxelProfileName=mb.getSettings().getVoxelProfile();
            renderAirAnchors=mb.getSettings().getRenderAirAnchors();
            matchedFacing=facingFromName(blueprintName);return mb.getStructure();
        }
        return null;
    }
    private static Direction facingFromName(String n){
        if(n.endsWith("_north"))return Direction.NORTH;if(n.endsWith("_east"))return Direction.EAST;
        if(n.endsWith("_west"))return Direction.WEST;return Direction.SOUTH;
    }

    @Override protected void interactableNoGui(BlockState state,Level world,BlockPos pos,
            Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND)return;
        player.displayClientMessage(Component.literal(String.format(
                "Steam Engine: %.1f kPa | %.1f RPM | torque %.1f | demand %.1f | steam credit %.3f",
                lastPressure,lastRpm,lastTorque,lastDemand,steamCreditMilli/1000.0)),true);
    }

    @Override public void onDestroy(Level world){
        onDestroy(world,null);
    }
    @Override public void onDestroy(Level world,BlockPos playerBreakPos){
        SteamEngineOutputBlockEntity out=getOutput();stopOutput(out);
        steamExhaustedMilli+=steamCreditMilli;steamCreditMilli=0;formed=false;
        super.onDestroy(world,playerBreakPos);
    }

    @Override public void load(CompoundTag tag){
        super.load(tag);
        MultiblockPersistentData data=loadMultiblockData(tag);
        blueprintName=data.blueprintName();formed=data.formed();replacementModel=data.replacementModel();
        matchedFacing=facingFromName(blueprintName);
        voxelProfileName=tag.getString("SteamEngineVoxelProfile");
        collision=MultiblockVoxelNbt.read(tag,"SteamEngineCollision");
        selection=MultiblockVoxelNbt.read(tag,"SteamEngineSelection");
        inletPos=tag.contains("SteamEngineInlet")?BlockPos.of(tag.getLong("SteamEngineInlet")):null;
        outputPos=tag.contains("SteamEngineOutput")?BlockPos.of(tag.getLong("SteamEngineOutput")):null;
        steamCreditMilli=Math.max(0,tag.getLong("SteamEngineCreditMilli"));
        steamDrawnUnits=Math.max(0,tag.getLong("SteamEngineDrawn"));
        steamExhaustedMilli=Math.max(0,tag.getLong("SteamEngineExhaustedMilli"));
        running=tag.getBoolean("SteamEngineRunning");
    }
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);saveMultiblockData(tag,blueprintName,formed,replacementModel);
        if(!voxelProfileName.isBlank())tag.putString("SteamEngineVoxelProfile",voxelProfileName);
        MultiblockVoxelNbt.write(tag,"SteamEngineCollision",collision);
        MultiblockVoxelNbt.write(tag,"SteamEngineSelection",selection);
        if(inletPos!=null)tag.putLong("SteamEngineInlet",inletPos.asLong());
        if(outputPos!=null)tag.putLong("SteamEngineOutput",outputPos.asLong());
        tag.putLong("SteamEngineCreditMilli",steamCreditMilli);
        tag.putLong("SteamEngineDrawn",steamDrawnUnits);
        tag.putLong("SteamEngineExhaustedMilli",steamExhaustedMilli);
        tag.putBoolean("SteamEngineRunning",running);
    }
    @Override public CompoundTag sync(){
        setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);
        return getUpdateTag();
    }
    @Override public CompoundTag getUpdateTag(){CompoundTag tag=new CompoundTag();saveAdditional(tag);return tag;}
    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(Connection net,ClientboundBlockEntityDataPacket packet){
        if(packet.getTag()!=null)load(packet.getTag());
    }

    @Override public int capacityE(){return 0;} @Override public int maxtransferE(){return 0;}
    @Override public int capacityH(){return 0;} @Override public int maxtransferH(){return 0;}
    @Override public int capacityW(){return 0;} @Override public int maxtransferW(){return 0;}
    @Override public int capacityF(){return 0;} @Override public int tanks(){return 0;}
    @Override public int invsize(){return 0;} @Override public int capacityP(){return 0;}
    @Override public int maxtransferP(){return 0;}
    @Override public boolean itemcape(){return false;} @Override public boolean energycape(){return false;}
    @Override public boolean heatcape(){return false;} @Override public boolean wattcape(){return false;}
    @Override public boolean fluidcape(){return false;} @Override public boolean pressurecape(){return false;}
    @Override public boolean HeatCanReceive(){return false;} @Override public boolean HeatCanSend(){return false;}
    @Override public boolean WattCanReceive(){return false;} @Override public boolean WattCanSend(){return false;}
    @Override public boolean EnergyCanReceive(){return false;} @Override public boolean EnergyCanSend(){return false;}
    @Override public boolean PressureCanReceive(){return false;} @Override public boolean PressureCanSend(){return false;}
    @Override public Level getThisWorld(){return level;} @Override public BlockPos getThisPosition(){return worldPosition;}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(7);}
    @Override public @NotNull ModelData getModelData(){
        return super.getModelData().derive().with(MultiBlockProperties.MODEL_NAME,replacementModel).build();
    }
    @Override public Component getDisplayName(){return Component.translatable("block.magneticraft2.steam_engine");}
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return null;}
}
