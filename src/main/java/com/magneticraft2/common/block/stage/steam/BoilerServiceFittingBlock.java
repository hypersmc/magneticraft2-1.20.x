package com.magneticraft2.common.block.stage.steam;

import com.magneticraft2.common.block.stage.steel.SteelBoilerVesselBlock;
import com.magneticraft2.common.registry.registers.BlockRegistry;
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
 * Steam 0-A: purely structural, externally installed service fittings.
 * No BlockEntity, capabilities, pressure, thermal conversion or server ticker.
 * Placement only succeeds at the actual external site on a formed boiler.
 * Boiler teardown does not consume or drop these independently owned parts.
 */
public final class BoilerServiceFittingBlock extends HorizontalDirectionalBlock {
    public enum Role {
        WATER(2, 2, -2),
        STEAM(0, 4, -2),
        FIREBOX(0, 0, -2),
        SAFETY(0, 4, -1);
        final int dx, dy, dz;
        Role(int x,int y,int z){dx=x;dy=y;dz=z;}
    }
    private static final Direction[] FACES = {
        Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST
    };
    private static final int[][][] BOXES = {
        {{0,3,3,2,13,13},{2,6,6,10,10,10},{10,4,4,12,12,12},{12,4,4,16,6,12},{12,10,4,16,12,12},{12,6,4,16,10,6},{12,6,10,16,10,12}},
        {{2,0,2,14,2,14},{6,2,6,10,11,10},{3,11,3,13,13,13},{3,13,3,5,16,13},{11,13,3,13,16,13},{5,13,3,11,16,5},{5,13,11,11,16,13}},
        {{1,0,1,15,3,15},{3,3,3,13,12,13},{1,12,1,15,16,15},{4,4,0,12,11,3},{5,5,0,11,10,1}},
        {{2,0,2,14,2,14},{6,2,6,10,9,10},{3,9,3,13,12,13},{3,12,3,5,14,5},{11,12,3,13,14,5},{3,12,11,5,14,13},{11,12,11,13,14,13}}
    };
    private static final VoxelShape[][] SHAPES = buildShapes();
    private final Role role;

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

    // Transform canonical SOUTH offsets; this is the same orientation contract as
    // SteelBoilerModelParts. Positions are relative to the original controller.
    private static BlockPos offset(Role role,Direction facing){
        int x=role.dx,z=role.dz;
        return switch(facing){
            case SOUTH -> new BlockPos(x,role.dy,z);
            case NORTH -> new BlockPos(-x,role.dy,-z);
            case EAST -> new BlockPos(-z,role.dy,x);
            case WEST -> new BlockPos(z,role.dy,-x);
            default -> throw new IllegalArgumentException("Horizontal boiler facing required");
        };
    }

    public static BlockPos attachmentPosition(BlockPos controller,Role role,Direction facing){
        return controller.offset(offset(role,facing));
    }

    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
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
            BlockPos inward=switch(role){
                case WATER -> position.relative(facing.getClockWise().getOpposite());
                case STEAM, SAFETY -> position.below();
                case FIREBOX -> position.above();
            };
            if(!level.hasChunkAt(inward)
                    || !level.getBlockState(inward).is(BlockRegistry.multiblockfiller.get()))
                continue;
            return defaultBlockState().setValue(FACING,facing);
        }
        return null; // No unsupported/free-floating service connections.
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(FACING);
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
