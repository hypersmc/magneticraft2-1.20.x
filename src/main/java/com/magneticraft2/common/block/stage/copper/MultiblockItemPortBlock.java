package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.blockentity.stage.copper.MultiblockItemPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    public enum PortMode {
        INPUT,
        COMBINED_OUTPUT,
        PRIMARY_OUTPUT,
        BYPRODUCT_OUTPUT
    }

    private final PortMode mode;

    public MultiblockItemPortBlock(boolean output) {
        this(
                output
                        ? PortMode.COMBINED_OUTPUT
                        : PortMode.INPUT
        );
    }

    public MultiblockItemPortBlock(PortMode mode) {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .noOcclusion());
        this.mode = mode;
    }

    public PortMode getPortMode() {
        return mode;
    }

    public boolean isOutput() {
        return mode != PortMode.INPUT;
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

                ItemStack stored =
                        port.getInputStackForDisplay();

                if (!stored.isEmpty()) {
                    player.displayClientMessage(
                            Component.literal(
                                    "Input: "
                                            + stored.getCount()
                                            + "x "
                            ).append(
                                    stored.getHoverName()
                            ),
                            true
                    );
                }

                return InteractionResult.CONSUME;
            }
        }

        if (held.isEmpty()) {
            ItemStack extracted =
                    output
                            ? port.extractForPlayer()
                            : port.extractInputForPlayer();

            if (!extracted.isEmpty()) {
                if (!player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
            }

            // The physical port owns empty-hand interaction even when its
            // mapped slot is empty. Do not let an empty Output port fall
            // through to a controller interaction that can touch Input.
            return InteractionResult.CONSUME;
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
