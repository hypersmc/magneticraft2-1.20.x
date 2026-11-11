package com.magneticraft2.common.block.stage.steam;

import com.magneticraft2.common.block.stage.steel.SteelBoilerVesselBlock;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.steam.BoilerAssemblyRecovery;
import com.magneticraft2.common.systems.steam.BoilerFireboxInstallation;
import com.magneticraft2.common.systems.steam.BoilerWarmup;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Structural fittings with manual water-container interaction at the inlet.
 * No fitting BlockEntity/ticker, automatic fluid capability, pressure or Steam flow.
 * Placement only succeeds at the actual external site on a formed boiler.
 * Boiler teardown does not consume or drop these independently owned parts.
 */
public class BoilerServiceFittingBlock extends HorizontalDirectionalBlock {
    public enum Role {
        WATER(2, 2, -2),
        STEAM(0, 4, -2),
        FIREBOX(0, 1, 1),
        SAFETY(0, 4, -1),
        CHIMNEY(0, 2, 1);
        final int dx, dy, dz;
        Role(int x,int y,int z){dx=x;dy=y;dz=z;}
    }
    private static final Direction[] FACES = {
        Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST
    };
    private static final int[][][] BOXES = {
        {{0,3,3,3,13,13},{3,6,6,9,10,10},{9,5,5,14,11,11},{14,4,4,16,12,12},{11,1,6,13,5,10},{9,0,4,15,1,12},{11,11,7,13,14,9},{7,14,6,16,16,10}},
        {{2,0,2,14,2,14},{4,2,4,12,8,12},{6,8,6,10,13,10},{10,9,6,14,13,10},{14,8,4,16,14,12},{6,13,6,10,14,10},{3,14,4,13,16,12}},
        {{3,3,0,13,13,3},{2,2,3,14,14,4},{1,1,4,3,15,15},{13,1,4,15,15,15},{3,1,4,13,3,15},{3,13,4,13,15,15},{3,3,4,13,13,5},{3,3,14,5,13,16},{11,3,14,13,13,16},{5,11,14,11,13,16},{5,3,14,11,5,16},{5,5,14,11,11,15},{6,5,15,7,11,16},{9,5,15,10,11,16},{4,1,15,12,3,16},{5,15,5,11,16,11}},
        {{2,0,2,14,2,14},{3,2,8,6,9,11},{1,8,11,9,15,12},{3,10,12,7,13,13},{1,8,12,3,15,14},{7,8,12,9,15,14},{3,8,12,7,10,14},{3,13,12,7,15,14},{4,11,13,6,12,14},{10,2,3,14,9,7},{11,9,4,13,13,6},{10,13,3,14,15,7}},
        {{3,0,3,5,2,13},{11,0,3,13,2,13},{5,0,3,11,2,5},{5,0,11,11,2,13},{4,2,4,5,14,12},{11,2,4,12,14,12},{5,2,4,11,14,5},{5,2,11,11,14,12},{3,14,3,5,16,13},{11,14,3,13,16,13},{5,14,3,11,16,5},{5,14,11,11,16,13}}
    };
    private static final VoxelShape[][] SHAPES = buildShapes();
    private final Role role;

    public Role role(){return role;}

    public BoilerServiceFittingBlock(Role role){
        super(BlockBehaviour.Properties.of().strength(5F,8F).sound(SoundType.METAL)
                .requiresCorrectToolForDrops().noOcclusion());
        this.role=role;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    private static VoxelShape[][] buildShapes(){
        VoxelShape[][] result=new VoxelShape[Role.values().length][4];
        for(Role role:Role.values())for(int i=0;i<4;i++){
            VoxelShape shape=Shapes.empty();
            for(int[] b:BOXES[role.ordinal()]){
                int x=b[0],y=b[1],z=b[2],X=b[3],Y=b[4],Z=b[5];
                for(int n=0;n<i;n++){
                    int nx=16-Z,nX=16-z,nz=x,nZ=X;
                    x=nx;X=nX;z=nz;Z=nZ;
                }
                shape=Shapes.joinUnoptimized(shape,Block.box(x,y,z,X,Y,Z),BooleanOp.OR);
            }
            result[role.ordinal()][i]=shape.optimize();
        }
        return result;
    }

    // Transform SOUTH offsets according to the actual directional JSON layouts;
    // the inverse mapping in the legacy model helper is not appropriate here.
    private static BlockPos offset(Role role,Direction facing){
        int x=role.dx,z=role.dz;
        return switch(facing){
            case SOUTH -> new BlockPos(x,role.dy,z);
            case NORTH -> new BlockPos(-x,role.dy,-z);
            case EAST -> new BlockPos(z,role.dy,-x);
            case WEST -> new BlockPos(-z,role.dy,x);
            default -> throw new IllegalArgumentException("Horizontal boiler facing required");
        };
    }

    public static BlockPos attachmentPosition(BlockPos controller,Role role,Direction facing){
        return controller.offset(offset(role,facing));
    }

    public static Direction steamPipeDirection(Direction facing) {
        return switch (facing) {
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Horizontal boiler facing required");
        };
    }

    /** Exact external pipe cell aligned to the baked Steam takeoff flange. */
    public static BlockPos steamPipePosition(BlockPos outlet, Direction facing) {
        return outlet.relative(steamPipeDirection(facing));
    }

    /** Water and Steam fittings use the same outward horizontal flange axis. */
    public static Direction waterPipeDirection(Direction facing) {
        return steamPipeDirection(facing);
    }

    /** Exact Copper Water Pipe cell aligned to the visible feedwater flange. */
    public static BlockPos waterPipePosition(BlockPos inlet, Direction facing) {
        return inlet.relative(waterPipeDirection(facing));
    }



    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        if (role == Role.CHIMNEY) {
            BlockState stack = BoilerFireboxInstallation.stackPlacement(context, this);
            if (stack != null) return stack;
        }
        Level level=context.getLevel();
        BlockPos position=context.getClickedPos();
        for(Direction facing:FACES){
            BlockPos controller=position.subtract(offset(role,facing));
            // The controller is the only source of facing/formed truth, even
            // when its shell has been replaced by multiblock fillers.
            if(!level.hasChunkAt(controller))continue;
            BlockState state=level.getBlockState(controller);
            if(!state.is(BlockRegistry.STEEL_BOILER_VESSEL.get())
                    || !state.getValue(SteelBoilerVesselBlock.IS_FORMED)
                    || state.getValue(SteelBoilerVesselBlock.FACING)!=facing)continue;
            if (role == Role.CHIMNEY && level.getBlockEntity(controller)
                    instanceof com.magneticraft2.common.blockentity.stage.steel.SteelBoilerVesselBlockEntity vessel
                    && vessel.boilerAssemblyTracked()) continue; // never recreate the old post on a V2 root
            BlockPos inward=switch(role){
                case WATER -> position.relative(switch(facing){
                    case SOUTH -> Direction.WEST;
                    case NORTH -> Direction.EAST;
                    case EAST -> Direction.SOUTH;
                    case WEST -> Direction.NORTH;
                    default -> throw new IllegalArgumentException("Horizontal facing required");
                });
                case STEAM, SAFETY, CHIMNEY -> position.below();
                case FIREBOX -> position.relative(facing.getOpposite());
            };
            if (!level.hasChunkAt(inward)) continue;
            BlockState support = level.getBlockState(inward);
            if (role == Role.CHIMNEY) {
                // A real flue must be borne by its separate firebox housing.
                if (!(support.getBlock() instanceof BoilerServiceFittingBlock fitting)
                        || fitting.role() != Role.FIREBOX
                        || support.getValue(FACING) != facing) continue;
            } else if (!support.is(BlockRegistry.multiblockfiller.get())) continue;
            return defaultBlockState().setValue(FACING,facing);
        }
        return null; // No unsupported/free-floating service connections.
    }


    /**
     * The flue is an independent supported block: removing its firebox must
     * not leave an orphan chimney suspended in midair.
     */
    @Override public void onRemove(BlockState oldState, Level level, BlockPos pos,
            BlockState newState, boolean moving) {
        if (oldState.getBlock() != newState.getBlock()
                && BoilerAssemblyRecovery.attachmentRemoved(level, pos)) {
            super.onRemove(oldState, level, pos, newState, moving);
            return;
        }
        if (role == Role.FIREBOX && oldState.getBlock() != newState.getBlock()
                && !level.isClientSide && !moving) {
            BlockPos above = pos.above();
            if (level.hasChunkAt(above)) {
                BlockState upper = level.getBlockState(above);
                if (upper.getBlock() instanceof BoilerServiceFittingBlock fitting
                        && fitting.role() == Role.CHIMNEY
                        && upper.getValue(FACING) == oldState.getValue(FACING))
                    level.destroyBlock(above, true);
            }
        }
        super.onRemove(oldState, level, pos, newState, moving);
    }

    @Override public void setPlacedBy(Level world, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        BoilerFireboxInstallation.placed(world, pos, state, role);
    }
    @Override public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos,
            Player player, boolean harvest, FluidState fluid) {
        if (role == Role.CHIMNEY && (BoilerFireboxInstallation.stackOwner(world, pos) != null
                || (world instanceof ServerLevel server && BoilerAssemblyRecovery.attachment(server, pos) != null))) {
            BoilerAssemblyRecovery.playerBreakStack(world, pos, player);
            return false;
        }
        return super.onDestroyedByPlayer(state, world, pos, player, harvest, fluid);
    }

    @Override public InteractionResult use(BlockState state, Level world, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (role == Role.WATER && (held.isEmpty() || FluidUtil.getFluidHandler(held).isPresent()))
            return BoilerWarmup.useInlet(world, pos, state, player, hand);
        return super.use(state, world, pos, player, hand, hit);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(FACING);
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
        return net.minecraft.world.level.material.PushReaction.BLOCK; // bolted structural hardware
    }

    @Override public BlockState rotate(BlockState state,Rotation rotation){
        return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,
            CollisionContext context){
        Direction facing=state.getValue(FACING);
        for(int i=0;i<FACES.length;i++)if(FACES[i]==facing)return SHAPES[role.ordinal()][i];
        return SHAPES[role.ordinal()][0];
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){
        return getShape(state,level,pos,context);
    }
}
