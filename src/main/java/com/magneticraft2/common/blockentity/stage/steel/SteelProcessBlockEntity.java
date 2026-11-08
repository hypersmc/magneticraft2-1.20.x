package com.magneticraft2.common.blockentity.stage.steel;

import com.magneticraft2.common.block.stage.steel.SteelProcessBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.copper.MultiblockItemPortBlockEntity;
import com.magneticraft2.common.recipe.multiblock.SteelProcessRecipe;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressure;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressurizedGas;
import com.magneticraft2.common.systems.PRESSURE.IPressurizedGasHandler;
import com.magneticraft2.common.systems.PRESSURE.LegacyAirPort;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockRenderParts;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelHost;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelNbt;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockVoxelProfiles;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockRegistry;
import com.magneticraft2.common.systems.Multiblocking.json.MultiblockStructure;
import com.magneticraft2.common.systems.material.HotBloomWorkpiece;
import com.magneticraft2.common.utils.MultiBlockProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * First Steel vertical slice: three physically separate JSON multiblocks
 * and a single server-authoritative inventory, HEAT, AIR and fuel contract.
 * Actual Steel is only made by the refining machine after Coke and Pig Iron.
 * Visuals use small reusable baked parts; MGC2 Studio owns later polish.
 */
public final class SteelProcessBlockEntity extends BaseBlockEntityMagneticraft2
        implements MultiblockVoxelHost {
    // The real three-input port uses slots 0,3,4. Output 1 and slag 2
    // remain the pre-existing physical primary/byproduct module convention.
    private static final int HEAT_CAPACITY = 40000, AIR_CAPACITY = 12;
    private static final int HEAT_GAIN = 100, HEAT_LOSS = 4;
    private static final int FUEL_QUEUE_LIMIT = 16, AIR_INTERVAL = 5;
    private final ItemStack[] activeInputs = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private final ItemStack[] lastInputs = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private ItemStack lastOutput = ItemStack.EMPTY, lastSlag = ItemStack.EMPTY;
    private ItemStack fuel = ItemStack.EMPTY;
    private int burnRemaining, burnTotal, progress, airStep;
    @Nullable private ResourceLocation activeRecipeId;
    private boolean formed;
    private String blueprintName = "", replacementModel = "", voxelProfileName = "";
    private Direction matchedFacing = Direction.SOUTH;
    private List<BlockPos> renderAirAnchors = List.of();
    @Nullable private VoxelShape localVoxelCollision, localVoxelSelection;
    private final LazyOptional<IPressurizedGasHandler> gasInput =
            LazyOptional.of(() -> new LegacyAirPort(pressureHandler));

    public SteelProcessBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.STEEL_PROCESS_BE.get(), pos, state);
        menuProvider = null;
    }
    private ResourceLocation machineId() {
        return ((SteelProcessBlock)getBlockState().getBlock()).machine();
    }
    private boolean airRequired() { return !machineId().getPath().equals("steel_coke_oven"); }
    private String machineName() { return machineId().getPath(); }
    private int ignitionThreshold() { return switch (machineName()) {
        case "steel_coke_oven" -> 7000;
        case "steel_blast_furnace" -> 18000;
        default -> 22000;
    }; }
    private int processHeatCost() { return switch (machineName()) {
        case "steel_coke_oven" -> 10;
        case "steel_blast_furnace" -> 16;
        default -> 20;
    }; }

    @Override public void setMultiblockVoxelShapes(VoxelShape collision, VoxelShape selection) {
        localVoxelCollision=collision; localVoxelSelection=selection; sync();
    }
    @Nullable public VoxelShape getLocalVoxelCollision() { return localVoxelCollision; }
    @Nullable public VoxelShape getLocalVoxelSelection() {
        return localVoxelSelection == null ? localVoxelCollision : localVoxelSelection;
    }
    public boolean isFormed() { return formed; }

    /**
     * Only one physical formed side-wall tuyere may receive AIR. This
     * delegates to the same typed AIR handler used by the Bronze Bloomery.
     * It never creates pressure or accepts STEAM.
     */
    public <T> @NotNull LazyOptional<T> getSideTuyereCapability(
            @NotNull Capability<T> cap, @Nullable Direction side, BlockPos portPos) {
        if (!formed || !airRequired() || level == null || side == null) return LazyOptional.empty();
        Direction outward = matchedFacing.getClockWise();
        BlockPos inlet = worldPosition.relative(outward, 2)
                .relative(matchedFacing.getOpposite(), 2).above();
        if (side != outward || !inlet.equals(portPos)) return LazyOptional.empty();
        if (cap == CapabilityPressure.PRESSURE) return pressure.cast();
        if (cap == CapabilityPressurizedGas.GAS) return gasInput.cast();
        return LazyOptional.empty();
    }

    @Override protected @Nullable MultiblockController createMultiblockController() {
        if (level == null || level.isClientSide || formed) return null;
        MultiblockStructure structure=identifyMultiblockStructure(level,worldPosition);
        if (structure == null) return null;
        if (voxelProfileName.isBlank() || !MultiblockVoxelProfiles.exists(voxelProfileName))
            return null; // Never use invisible/no-collision filler fallback.
        MultiblockController controller=new MultiblockController(structure);
        controller.identifyAndAddModules(level,worldPosition,structure);
        if (!controller.createStructure(level,worldPosition)) return null;
        for(BlockPos pos:controller.getBlockMap().keySet())
            if(level.getBlockEntity(pos) instanceof MultiblockItemPortBlockEntity port)
                port.linkFormedController(worldPosition);
        controller.setFormed(true);
        setMultiblockController(controller);
        formed=true;
        BlockState old=level.getBlockState(worldPosition);
        if(old.hasProperty(SteelProcessBlock.IS_FORMED))
            level.setBlock(worldPosition,old.setValue(SteelProcessBlock.IS_FORMED,true)
                    .setValue(SteelProcessBlock.FACING,matchedFacing),Block.UPDATE_ALL);
        MultiblockRenderParts.assign(level,worldPosition,controller,machineName(),
                matchedFacing,renderAirAnchors,voxelProfileName);
        requestModelDataUpdate();sync();return controller;
    }

    @Override protected @Nullable MultiblockStructure identifyMultiblockStructure(Level world,BlockPos pos) {
        String prefix=machineName()+"_";
        for(Multiblock multiblock:MultiblockRegistry.getRegisteredMultiblocks().values()){
            if(!multiblock.getName().startsWith(prefix))continue;
            MultiblockStructure candidate=multiblock.getStructure();
            if(!matchesStructure(world,pos,candidate,multiblock))continue;
            blueprintName=multiblock.getName();
            replacementModel=multiblock.getSettings().getReplaceWhenFormed();
            matchedFacing=facingFromName(blueprintName);
            renderAirAnchors=multiblock.getSettings().getRenderAirAnchors();
            voxelProfileName=multiblock.getSettings().getVoxelProfile();
            return candidate;
        }
        return null;
    }
    private static Direction facingFromName(String s) {
        if(s.endsWith("_north"))return Direction.NORTH;
        if(s.endsWith("_east"))return Direction.EAST;
        if(s.endsWith("_west"))return Direction.WEST;
        return Direction.SOUTH;
    }

    public boolean acceptsIngredientSlot(int slot,ItemStack stack) {
        if(level==null || stack.isEmpty())return false;
        for(SteelProcessRecipe recipe:level.getRecipeManager()
                .getAllRecipesFor(SteelProcessRecipe.Type.INSTANCE))
            if(recipe.machine().equals(machineId()) && recipe.accepts(slot,stack))
                return true;
        return false;
    }
    @Nullable private SteelProcessRecipe recipe() {
        if(level==null)return null;
        for(SteelProcessRecipe recipe:level.getRecipeManager()
                .getAllRecipesFor(SteelProcessRecipe.Type.INSTANCE))
            if(recipe.machine().equals(machineId()) && recipe.matchesSlots(itemHandler))
                return recipe;
        return null;
    }
    private int fuelTime(ItemStack stack) {
        if(stack.isEmpty() || !stack.getCraftingRemainingItem().isEmpty())return 0;
        if(stack.is(ItemRegistry.ITEM_METALLURGICAL_COKE.get()))return 3200;
        // Blast reduction and the refining vessel require the hotter
        // metallurgical fuel, not arbitrary low-grade wooden combustibles.
        if(airRequired())return 0;
        return Math.max(0,ForgeHooks.getBurnTime(stack,RecipeType.SMELTING));
    }
    private static ItemStack identity(ItemStack stack) {
        if(stack.isEmpty())return ItemStack.EMPTY;
        ItemStack copy=stack.copy();copy.setCount(1);return copy;
    }
    private boolean inputIdentityChanged(SteelProcessRecipe recipe) {
        for(int i=0;i<3;i++) {
            ItemStack now=i<recipe.inputCount()
                    ? itemHandler.getStackInSlot(SteelProcessRecipe.INPUT_SLOTS[i])
                    : ItemStack.EMPTY;
            if(!ItemStack.isSameItemSameTags(now,activeInputs[i]))return true;
        }
        return false;
    }
    private void rememberInputs(SteelProcessRecipe recipe) {
        for(int i=0;i<3;i++)activeInputs[i]=i<recipe.inputCount()
                ?identity(itemHandler.getStackInSlot(SteelProcessRecipe.INPUT_SLOTS[i]))
                :ItemStack.EMPTY;
    }
    private void resetProgress() {
        progress=0;airStep=0;activeRecipeId=null;
        for(int i=0;i<3;i++)activeInputs[i]=ItemStack.EMPTY;
    }
    private boolean outputFits(ItemStack product,ItemStack slag) {
        return itemHandler.insertItem(1,product,true).isEmpty()
                && (slag.isEmpty()||itemHandler.insertItem(2,slag,true).isEmpty());
    }

    public static <E extends BlockEntity> void serverTick(Level world,BlockPos pos,
            BlockState state,E tile) {
        if(world.isClientSide || !(tile instanceof SteelProcessBlockEntity furnace))return;
        furnace.tickProcess();
        if(furnace.level==null)return;
        boolean different=!ItemStack.isSameItemSameTags(
                furnace.lastOutput,furnace.itemHandler.getStackInSlot(1))
                || !ItemStack.isSameItemSameTags(
                        furnace.lastSlag,furnace.itemHandler.getStackInSlot(2));
        for(int i=0;i<3;i++)different|=!ItemStack.isSameItemSameTags(
                furnace.lastInputs[i],furnace.itemHandler.getStackInSlot(SteelProcessRecipe.INPUT_SLOTS[i]));
        if(different)furnace.sync();
    }
    private void tickProcess(){
        if(level==null||!formed)return;
        boolean changed=false,notify=false;
        int heat=Math.max(0,Math.min(HEAT_CAPACITY,heatHandler.getHeatStored()));
        if(burnRemaining>0){
            burnRemaining--;heat=Math.min(HEAT_CAPACITY,heat+HEAT_GAIN);changed=true;
        }
        int cooled=Math.max(0,heat-HEAT_LOSS);
        changed|=cooled!=heat;heat=cooled;
        SteelProcessRecipe recipe=recipe();
        if(recipe==null){
            if(progress!=0||activeRecipeId!=null){resetProgress();changed=true;notify=true;}
        }else{
            if(!recipe.getId().equals(activeRecipeId)||inputIdentityChanged(recipe)){
                resetProgress();activeRecipeId=recipe.getId();rememberInputs(recipe);
                changed=true;notify=true;
            }
            ItemStack product=recipe.output(),slag=recipe.slag();
            // Transfer only the existing source material's approved quality;
            // never carry a temporary hot-workpiece timer into molten Steel.
            if(!machineName().equals("steel_coke_oven"))
                product=HotBloomWorkpiece.consolidate(itemHandler.getStackInSlot(0),product);
            if(outputFits(product,slag)){
                if(burnRemaining==0 && !fuel.isEmpty() && heat<ignitionThreshold()+processHeatCost()){
                    int time=fuelTime(fuel);
                    if(time>0){
                        fuel.shrink(1);if(fuel.isEmpty())fuel=ItemStack.EMPTY;
                        burnRemaining=time;burnTotal=time;changed=true;notify=true;
                    }
                }
                if(burnRemaining>0 && heat>=ignitionThreshold()+processHeatCost()){
                    boolean airAvailable=!recipe.requiresAir() || airStep!=0
                            || pressureHandler.getPressureStored()>0;
                    if(airAvailable){
                        if(recipe.requiresAir() && airStep==0
                                && pressureHandler.extractPressure(1,false)!=1)
                            throw new IllegalStateException("Steel furnace AIR reservation lost");
                        if(recipe.requiresAir())airStep=(airStep+1)%AIR_INTERVAL;
                        heat-=processHeatCost();
                        progress++;changed=true;
                        if(progress>=recipe.processTime()){
                            if(!outputFits(product,slag))
                                throw new IllegalStateException("Steel output reservation lost");
                            for(int i=0;i<recipe.inputCount();i++){
                                int slot=SteelProcessRecipe.INPUT_SLOTS[i];
                                int count=recipe.input(i).count();
                                ItemStack removed=itemHandler.extractItem(slot,count,false);
                                if(removed.getCount()!=count)
                                    throw new IllegalStateException("Steel input reservation lost");
                            }
                            if(!itemHandler.insertItem(1,product,false).isEmpty()
                                    || (!slag.isEmpty()&&!itemHandler.insertItem(2,slag,false).isEmpty()))
                                throw new IllegalStateException("Steel output commit failed");
                            resetProgress();notify=true;
                        }
                    }
                }
            }
        }
        if(heatHandler.getHeatStored()!=heat){heatHandler.setHeat(heat);changed=true;}
        if(changed){setChanged();if(notify||level.getGameTime()%20L==0L)sync();}
    }

    @Override protected void interactableNoGui(BlockState state, Level world, BlockPos pos,
            net.minecraft.world.entity.player.Player player, InteractionHand hand, BlockHitResult hit) {
        if(world.isClientSide||!formed)return;
        ItemStack held=player.getItemInHand(hand);
        if(held.isEmpty()&&player.isShiftKeyDown()&&!fuel.isEmpty()){
            ItemStack item=fuel.copy();fuel=ItemStack.EMPTY;
            if(!player.getInventory().add(item))player.drop(item,false);
            sync();return;
        }
        if(!held.isEmpty()){
            boolean valid=fuelTime(held)>0 && fuel.getCount()<FUEL_QUEUE_LIMIT
                    && (fuel.isEmpty()||ItemStack.isSameItemSameTags(fuel,held));
            if(valid){
                if(fuel.isEmpty()){fuel=held.copy();fuel.setCount(1);}else fuel.grow(1);
                if(!player.getAbilities().instabuild)held.shrink(1);
                sync();
            }
            player.displayClientMessage(Component.literal(valid
                    ? "Steel furnace fuel queued" : "Invalid fuel or full queue"),true);
            return;
        }
        SteelProcessRecipe r=recipe();
        player.displayClientMessage(Component.literal(
                "Steel furnace: "+machineName()+", fuel "+fuel.getCount()
                +", burning "+burnRemaining+", HEAT "+heatHandler.getHeatStored()+"/"+HEAT_CAPACITY
                +", AIR "+pressureHandler.getPressureStored()+"/"+AIR_CAPACITY
                +", progress "+progress+"/"+(r==null?0:r.processTime())),true);
    }
    @Override public void onDestroy(Level world){
        if(!world.isClientSide&&!fuel.isEmpty()){
            Block.popResource(world,worldPosition,fuel.copy());fuel=ItemStack.EMPTY;
        }
        super.onDestroy(world);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();gasInput.invalidate();}
    @Override public void load(CompoundTag tag){
        super.load(tag);
        MultiblockPersistentData data=loadMultiblockData(tag);
        blueprintName=data.blueprintName();formed=data.formed();
        replacementModel=data.replacementModel();matchedFacing=facingFromName(blueprintName);
        voxelProfileName=tag.getString("SteelVoxelProfile");
        localVoxelCollision=MultiblockVoxelNbt.read(tag,"SteelVoxelCollision");
        localVoxelSelection=MultiblockVoxelNbt.read(tag,"SteelVoxelSelection");
        fuel=ItemStack.of(tag.getCompound("SteelFuel"));
        burnTotal=Math.max(0,tag.getInt("SteelBurnTotal"));
        burnRemaining=Math.max(0,Math.min(burnTotal,tag.getInt("SteelBurnRemaining")));
        progress=Math.max(0,tag.getInt("SteelProgress"));
        airStep=Math.max(0,Math.min(AIR_INTERVAL-1,tag.getInt("SteelAirStep")));
        activeRecipeId=tag.contains("SteelRecipe")
                ?ResourceLocation.tryParse(tag.getString("SteelRecipe")):null;
        for(int i=0;i<3;i++)activeInputs[i]=ItemStack.of(tag.getCompound("SteelActiveInput"+i));
        for(int i=0;i<3;i++)lastInputs[i]=identity(itemHandler.getStackInSlot(SteelProcessRecipe.INPUT_SLOTS[i]));
        lastOutput=itemHandler.getStackInSlot(1).copy();lastSlag=itemHandler.getStackInSlot(2).copy();
    }
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);
        saveMultiblockData(tag,blueprintName,formed,replacementModel);
        if(!voxelProfileName.isBlank())tag.putString("SteelVoxelProfile",voxelProfileName);
        MultiblockVoxelNbt.write(tag,"SteelVoxelCollision",localVoxelCollision);
        MultiblockVoxelNbt.write(tag,"SteelVoxelSelection",localVoxelSelection);
        tag.putInt("SteelBurnTotal",burnTotal);tag.putInt("SteelBurnRemaining",burnRemaining);
        tag.putInt("SteelProgress",progress);tag.putInt("SteelAirStep",airStep);
        if(!fuel.isEmpty())tag.put("SteelFuel",fuel.save(new CompoundTag()));
        if(activeRecipeId!=null)tag.putString("SteelRecipe",activeRecipeId.toString());
        for(int i=0;i<3;i++)if(!activeInputs[i].isEmpty())
            tag.put("SteelActiveInput"+i,activeInputs[i].save(new CompoundTag()));
    }
    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag(){CompoundTag t=new CompoundTag();saveAdditional(t);return t;}
    @Override public void onDataPacket(Connection c,ClientboundBlockEntityDataPacket p){
        if(p.getTag()!=null)load(p.getTag());
    }
    @Override public CompoundTag sync(){
        setChanged();
        for(int i=0;i<3;i++)
            lastInputs[i]=identity(itemHandler.getStackInSlot(SteelProcessRecipe.INPUT_SLOTS[i]));
        lastOutput=itemHandler.getStackInSlot(1).copy();
        lastSlag=itemHandler.getStackInSlot(2).copy();
        if(level!=null&&!level.isClientSide)
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);
        return getUpdateTag();
    }
    @Override public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,@Nullable Direction side) {
        // The real item modules and side-wall tuyere are the ONLY I/O paths.
        if(cap==ForgeCapabilities.ITEM_HANDLER||cap==CapabilityPressure.PRESSURE
                ||cap==CapabilityPressurizedGas.GAS)return LazyOptional.empty();
        return super.getCapability(cap,side);
    }
    @Override public int capacityE(){return 0;}
    @Override public int maxtransferE(){return 0;}
    @Override public int capacityH(){return HEAT_CAPACITY;}
    @Override public int maxtransferH(){return 0;}
    @Override public int capacityW(){return 0;}
    @Override public int maxtransferW(){return 0;}
    @Override public int capacityF(){return 0;}
    @Override public int tanks(){return 0;}
    @Override public int invsize(){return 5;}
    @Override public int capacityP(){return AIR_CAPACITY;}
    @Override public int maxtransferP(){return 1;}
    @Override public boolean itemcape(){return true;}
    @Override public boolean energycape(){return false;}
    @Override public boolean heatcape(){return true;}
    @Override public boolean wattcape(){return false;}
    @Override public boolean fluidcape(){return false;}
    @Override public boolean pressurecape(){return true;}
    @Override public boolean HeatCanReceive(){return false;}
    @Override public boolean HeatCanSend(){return false;}
    @Override public boolean WattCanReceive(){return false;}
    @Override public boolean WattCanSend(){return false;}
    @Override public boolean EnergyCanReceive(){return false;}
    @Override public boolean EnergyCanSend(){return false;}
    @Override public boolean PressureCanReceive(){return airRequired();}
    @Override public boolean PressureCanSend(){return false;}
    @Override public Level getThisWorld(){return level;}
    @Override public BlockPos getThisPosition(){return worldPosition;}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(6.0D);}
    @Override public @NotNull ModelData getModelData(){
        return super.getModelData().derive()
                .with(MultiBlockProperties.MODEL_NAME,replacementModel).build();
    }
    @Override public Component getDisplayName(){return Component.translatable(machineId().toLanguageKey("block"));}
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inventory,
            net.minecraft.world.entity.player.Player player){return null;}
}
