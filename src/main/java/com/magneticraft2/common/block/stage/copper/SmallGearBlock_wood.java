package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.blockentity.stage.copper.SmallGearBlockEntity_wood;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class SmallGearBlock_wood extends GearBlock {
    public static final BooleanProperty VERTICAL_FACING_up = BooleanProperty.create("vertical_facing_up");
    public static final BooleanProperty VERTICAL_FACING_down = BooleanProperty.create("vertical_facing_down");

    public SmallGearBlock_wood() {
        super(BlockBehaviour.Properties.of().strength(3.5F).noOcclusion());
    }


    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }
}
