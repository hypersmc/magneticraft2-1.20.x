package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeMechanicalLatheBlock;
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

/** Initial structural controller. No phantom power or machining is provided. */
public final class BronzeMechanicalLatheBlockEntity extends BaseBlockEntityMagneticraft2
        implements MultiblockVoxelHost {
    private String blueprintName="";
    private String replacementModel="";
    private String voxelProfileName="";
    private boolean formed;
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
            Player player,InteractionHand hand,BlockHitResult hit) { }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        MultiblockPersistentData data=loadMultiblockData(tag);
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
