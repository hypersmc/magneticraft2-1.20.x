package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeMechanicalLatheBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalInputModuleBlockEntity;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** JSON-formed Bronze Lathe with Gear V2 load-gated machining and conserved workpieces. */
public final class BronzeMechanicalLatheBlockEntity extends BaseBlockEntityMagneticraft2
        implements MultiblockVoxelHost {
    private String blueprintName="";
    private String replacementModel="";
    private String voxelProfileName="";
    private boolean formed;
    private ItemStack workpiece=ItemStack.EMPTY;
    private ItemStack cutter=ItemStack.EMPTY;
    private ItemStack output=ItemStack.EMPTY;
    private int machiningTicks;
    private boolean running;
    private static final int CYCLE_TICKS=160;
    private static final float MIN_RPM=12F, TORQUE=2.5F;

    public ItemStack getWorkpiece(){return workpiece;}
    public ItemStack getCutter(){return cutter;}
    public ItemStack getOutput(){return output;}
    public boolean isRunning(){return running;}
    public float getProgress(){return machiningTicks/(float)CYCLE_TICKS;}
    /** Staged feed: 0..15% approach, 15..85% cutting traverse, 85..100% retract. */
    public float getToolFeed(){
        if(!running && machiningTicks==0)return 0F;
        float p=getProgress();
        if(p<.15F)return p/.15F;
        if(p<.85F)return 1F;
        return Math.max(0F,1F-(p-.85F)/.15F);
    }
    public float getCutTraverse(){
        float p=getProgress();
        return Math.max(0F,Math.min(1F,(p-.15F)/.7F));
    }
    public boolean isActivelyCutting(){
        return running && getProgress()>=.15F && getProgress()<.85F;
    }
    public float getSpindleRotation(float partialTick){
        if(!running)return 0F;
        MechanicalInputModuleBlockEntity input=getMechanicalInput();
        return input==null?0F:input.getVisualRotationDegrees(partialTick);
    }
    @Nullable private MechanicalInputModuleBlockEntity getMechanicalInput(){
        if(level==null||!formed)return null;
        BlockPos pos=worldPosition.relative(matchedFacing.getOpposite(),2);
        BlockEntity be=level.getBlockEntity(pos);
        return be instanceof MechanicalInputModuleBlockEntity input?input:null;
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,BronzeMechanicalLatheBlockEntity lathe){
        if(!level.isClientSide)lathe.tickMachining();
    }
    private void stopLoad(){
        if(level!=null)GearNetworkManager.getInstance().removeMechanicalLoad(level,worldPosition);
    }
    private void tickMachining(){
        if(level==null)return;
        boolean hasWork=formed && !workpiece.isEmpty() && output.isEmpty()
                && !cutter.isEmpty() && cutter.getDamageValue()<cutter.getMaxDamage();
        MechanicalInputModuleBlockEntity input=hasWork?getMechanicalInput():null;
        if(!hasWork||input==null){
            stopLoad();
            setRunning(false);
            return;
        }
        GearNode node=input.getOrCreateGearNode();
        boolean canAttempt=node.getEffectiveSpeed()>=MIN_RPM && node.getTorque()+.01F>=TORQUE;
        GearNetworkManager manager=GearNetworkManager.getInstance();
        manager.setMechanicalLoad(level,worldPosition,input.getBlockPos(),TORQUE,canAttempt);
        if(!canAttempt||!manager.getMechanicalLoadState(level,worldPosition).supplied()){
            setRunning(false);
            return;
        }
        setRunning(true);
        machiningTicks++;
        setChanged();
        // A restrained shower of chips near the real cutter/workpiece contact.
        // No particles while stopped, approaching or retracting.
        if(isActivelyCutting() && machiningTicks%8==0 && level instanceof net.minecraft.server.level.ServerLevel server) {
            Direction across=matchedFacing.getClockWise();
            Direction rear=matchedFacing.getOpposite();
            double px=worldPosition.getX()+.5D + across.getStepX()*.45D + rear.getStepX()*.62D;
            double pz=worldPosition.getZ()+.5D + across.getStepZ()*.45D + rear.getStepZ()*.62D;
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    px, worldPosition.getY()+1.5D, pz, 2, .06D,.04D,.06D,.02D);
        }
        if(machiningTicks<CYCLE_TICKS){
            if(level.getGameTime()%20==0)sync();
            return;
        }
        ItemStack finished=new ItemStack(workpiece.is(ItemRegistry.ITEM_ROUGH_BRONZE_GEAR.get())
                ?ItemRegistry.ITEM_PRECISION_BRONZE_GEAR.get()
                :ItemRegistry.ITEM_PRECISION_BRONZE_BEARING.get());
        if(workpiece.hasTag())finished.setTag(workpiece.getTag().copy());
        output=finished;
        workpiece=ItemStack.EMPTY;
        int damage=cutter.getDamageValue()+1;
        if(damage>=cutter.getMaxDamage())cutter=ItemStack.EMPTY;
        else cutter.setDamageValue(damage);
        machiningTicks=0;
        stopLoad();
        setRunning(false);
        sync();
    }
    private void setRunning(boolean value){
        if(running==value)return;
        running=value;
        sync();
    }
    private static void deliver(Player player,ItemStack stack){
        if(!stack.isEmpty()&&!player.getInventory().add(stack))player.drop(stack,false);
    }
    private void interact(Player player,ItemStack held){
        if(!formed)return;
        if(held.isEmpty()){
            if(!output.isEmpty()){
                ItemStack collected=output;output=ItemStack.EMPTY;
                deliver(player,collected);sync();return;
            }
            if(player.isShiftKeyDown()){
                if(!workpiece.isEmpty()){
                    ItemStack recovered=workpiece;workpiece=ItemStack.EMPTY;
                    machiningTicks=0;stopLoad();setRunning(false);
                    deliver(player,recovered);sync();return;
                }
                if(!cutter.isEmpty()){
                    ItemStack recovered=cutter;cutter=ItemStack.EMPTY;
                    stopLoad();setRunning(false);
                    deliver(player,recovered);sync();return;
                }
            }
            player.displayClientMessage(Component.literal(
                    "Lathe: "+(running?"cutting":(!workpiece.isEmpty()?"stalled / waiting for Gear V2 drive":"idle"))+
                    " | "+machiningTicks+"/"+CYCLE_TICKS+" | tool "+(cutter.isEmpty()?"missing":(cutter.getMaxDamage()-cutter.getDamageValue())+" uses")),true);
            return;
        }
        if(held.is(ItemRegistry.ITEM_LATHE_CUTTER.get())){
            if(!cutter.isEmpty()){player.displayClientMessage(Component.literal("Remove the installed cutter first."),true);return;}
            cutter=held.copy();cutter.setCount(1);
        }else if(held.is(ItemRegistry.ITEM_ROUGH_BRONZE_GEAR.get())
                ||held.is(ItemRegistry.ITEM_ROUGH_BRONZE_BEARING.get())){
            if(!workpiece.isEmpty()||!output.isEmpty()){
                player.displayClientMessage(Component.literal("Remove the previous workpiece/output first."),true);return;
            }
            workpiece=held.copy();workpiece.setCount(1);machiningTicks=0;
        }else return;
        if(!player.getAbilities().instabuild)held.shrink(1);
        sync();
    }
    @Override public void onDestroy(Level world){
        stopLoad();
        if(!world.isClientSide){
            ItemStack a=workpiece,b=cutter,c=output;
            workpiece=ItemStack.EMPTY;cutter=ItemStack.EMPTY;output=ItemStack.EMPTY;
            if(!a.isEmpty())Block.popResource(world,worldPosition,a);
            if(!b.isEmpty())Block.popResource(world,worldPosition,b);
            if(!c.isEmpty())Block.popResource(world,worldPosition,c);
        }
        super.onDestroy(world);
    }
    private Direction matchedFacing=Direction.SOUTH;
    @Nullable private VoxelShape collision,selection;

    public BronzeMechanicalLatheBlockEntity(BlockPos pos,BlockState state) {
        super(BlockEntityRegistry.BRONZE_MECHANICAL_LATHE_BE.get(),pos,state);
        menuProvider=null;
    }
    public boolean isFormed() { return formed; }
    @Nullable public VoxelShape getLocalVoxelCollision() { return collision; }
    @Nullable public VoxelShape getLocalVoxelSelection() { return selection!=null?selection:collision; }
    @Override public void setMultiblockVoxelShapes(VoxelShape collision,VoxelShape selection) {
        this.collision=collision;
        this.selection=selection;
        sync();
    }
    private static Direction facingFromName(String name) {
        if(name.endsWith("_north"))return Direction.NORTH;
        if(name.endsWith("_east"))return Direction.EAST;
        if(name.endsWith("_west"))return Direction.WEST;
        return Direction.SOUTH;
    }
    @Override protected @Nullable MultiblockStructure identifyMultiblockStructure(Level world,BlockPos pos) {
        for(Multiblock candidate:MultiblockRegistry.getRegisteredMultiblocks().values()) {
            if(!candidate.getName().startsWith("bronze_mechanical_lathe_")) continue;
            MultiblockStructure structure=candidate.getStructure();
            if(!matchesStructure(world,pos,structure,candidate)) continue;
            blueprintName=candidate.getName();
            replacementModel=candidate.getSettings().getReplaceWhenFormed();
            voxelProfileName=candidate.getSettings().getVoxelProfile();
            matchedFacing=facingFromName(blueprintName);
            return structure;
        }
        return null;
    }
    @Override protected @Nullable MultiblockController createMultiblockController() {
        if(level==null||level.isClientSide||formed)return null;
        MultiblockStructure structure=identifyMultiblockStructure(level,worldPosition);
        if(structure==null||!MultiblockVoxelProfiles.exists(voxelProfileName))return null;
        MultiblockController controller=new MultiblockController(structure);
        controller.identifyAndAddModules(level,worldPosition,structure);
        if(!controller.createStructure(level,worldPosition))return null;
        controller.setFormed(true);
        setMultiblockController(controller);
        formed=true;
        BlockState state=level.getBlockState(worldPosition);
        level.setBlock(worldPosition,state.setValue(BronzeMechanicalLatheBlock.IS_FORMED,true)
                .setValue(BronzeMechanicalLatheBlock.FACING,matchedFacing),Block.UPDATE_ALL);
        MultiblockRenderParts.assign(level,worldPosition,controller,
                "bronze_mechanical_lathe",matchedFacing,java.util.List.of(),voxelProfileName);
        requestModelDataUpdate();
        sync();
        return controller;
    }
    @Override protected void interactableNoGui(BlockState state,Level world,BlockPos pos,
            Player player,InteractionHand hand,BlockHitResult hit) {
        interact(player,player.getItemInHand(hand));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        MultiblockPersistentData data=loadMultiblockData(tag);
        workpiece=ItemStack.of(tag.getCompound("LatheWorkpiece"));
        cutter=ItemStack.of(tag.getCompound("LatheCutter"));
        output=ItemStack.of(tag.getCompound("LatheOutput"));
        machiningTicks=Math.max(0,Math.min(CYCLE_TICKS,tag.getInt("LatheProgress")));
        running=tag.getBoolean("LatheRunning");
        blueprintName=data.blueprintName();
        replacementModel=data.replacementModel();
        formed=data.formed();
        matchedFacing=facingFromName(blueprintName);
        voxelProfileName=tag.getString("v2_voxel_profile");
        collision=MultiblockVoxelNbt.read(tag,"v2_voxel_collision");
        selection=MultiblockVoxelNbt.read(tag,"v2_voxel_selection");
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveMultiblockData(tag,blueprintName,formed,replacementModel);
        if(!workpiece.isEmpty())tag.put("LatheWorkpiece",workpiece.save(new CompoundTag()));
        if(!cutter.isEmpty())tag.put("LatheCutter",cutter.save(new CompoundTag()));
        if(!output.isEmpty())tag.put("LatheOutput",output.save(new CompoundTag()));
        tag.putInt("LatheProgress",machiningTicks);
        tag.putBoolean("LatheRunning",running);
        if(!voxelProfileName.isBlank())tag.putString("v2_voxel_profile",voxelProfileName);
        MultiblockVoxelNbt.write(tag,"v2_voxel_collision",collision);
        MultiblockVoxelNbt.write(tag,"v2_voxel_selection",selection);
    }
    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag=new CompoundTag();
        saveAdditional(tag);
        return tag;
    }
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet) {
        if(packet.getTag()!=null)load(packet.getTag());
    }
    @Override public CompoundTag sync() {
        setChanged();
        if(level!=null&&!level.isClientSide)
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);
        return getUpdateTag();
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
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(5D);}
    @Override public @NotNull ModelData getModelData(){
        return super.getModelData().derive()
                .with(MultiBlockProperties.MODEL_NAME,replacementModel).build();
    }
    @Override public Component getDisplayName(){
        return Component.translatable("block.magneticraft2.bronze_mechanical_lathe");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){
        return null;
    }
}
