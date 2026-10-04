package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.copper.MultiblockItemPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable item port for JSON multiblocks.
 *
 * Input and output are distinct registered blocks so a multiblock layout can
 * describe the material flow explicitly. The block itself survives formation
 * and stays visible, which makes it obvious where belts/hoppers/players should
 * interact with the formed machine.
 */
public class MultiblockItemPortBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            DirectionalBlock.FACING;

    private final boolean output;

    public MultiblockItemPortBlock(boolean output) {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .noOcclusion());
        this.output = output;

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    public boolean isOutput() {
        return output;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state) {
        return new MultiblockItemPortBlockEntity(
                pos,
                state
        );
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos)
                instanceof MultiblockItemPortBlockEntity port)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held =
                player.getItemInHand(hand);

        if (!output && !held.isEmpty()) {
            ItemStack one = held.copy();
            one.setCount(1);

            if (port.insertFromPlayer(one)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                return InteractionResult.CONSUME;
            }
        }

        if (output && held.isEmpty()) {
            ItemStack extracted =
                    port.extractForPlayer();

            if (!extracted.isEmpty()) {
                if (!player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            boolean willHarvest,
            FluidState fluid) {
        if (!level.isClientSide
                && level.getBlockEntity(pos)
                instanceof MultiblockItemPortBlockEntity port) {
            BlockPos controllerPos =
                    port.getControllerPos();

            if (controllerPos != null
                    && level.getBlockEntity(controllerPos)
                    instanceof BaseBlockEntityMagneticraft2 controller) {
                controller.onDestroy(level);
            }
        }

        return super.onDestroyedByPlayer(
                state,
                level,
                pos,
                player,
                willHarvest,
                fluid
        );
    }
}
