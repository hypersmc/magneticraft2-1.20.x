package com.magneticraft2.common.block.stage.stone;

import com.magneticraft2.common.block.general.BaseBlockMagneticraft2;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;

import com.magneticraft2.common.blockentity.stage.stone.PrimitiveFurnaceMultiblockEntity_nogui;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockHitHelper;
import com.magneticraft2.common.utils.Magneticraft2ConfigCommon;
import com.magneticraft2.common.utils.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author JumpWatch on 13-11-2024
 * @Project mgc2-1.20
* @version 1.0.0
 */
public class PrimitiveFurnaceMultiblock_nogui extends BaseBlockMagneticraft2 {
    public static final BooleanProperty IS_FORMED = BooleanProperty.create("is_formed");

    private static final List<FurnaceZoneBox> INTERACTION_ZONE_BOXES = List.of(
            // WEST-authored canonical layout. For the furnace opening, X is the
            // useful depth axis: lower X is farther back inside the furnace and
            // higher X is closer to the player/opening. Z splits left/right.
            //
            // Keep every pad on the reachable surface around y=0.063.
            //
            // Back: fuel.
            new FurnaceZoneBox(FurnaceZone.FUEL_INPUT, new AABB(0.14D, 0.00D, 0.34D, 0.38D, 0.14D, 0.66D)),

            // Middle: smeltable input.
            new FurnaceZoneBox(FurnaceZone.SMELTABLE_INPUT, new AABB(0.42D, 0.00D, 0.34D, 0.68D, 0.14D, 0.66D)),

            // Front/outward: the two outputs, split across the opening.
            new FurnaceZoneBox(FurnaceZone.PRIMARY_OUTPUT, new AABB(0.72D, 0.00D, 0.50D, 0.98D, 0.14D, 0.78D)),
            new FurnaceZoneBox(FurnaceZone.SECONDARY_OUTPUT, new AABB(0.72D, 0.00D, 0.18D, 0.98D, 0.14D, 0.46D))
    );
    private static final VoxelShape WEST_NEW = Stream.of(Block.box(4.68629, -16, 0, 11.31371, -15, 16), Block.box(0, -16, 4.68629, 16, -15, 11.31371), Stream.of(Block.box(4.68629, -15, 0, 11.31371, -14, 1), Block.box(4.68629, -15, 13, 11.31371, -8, 16), Block.box(0, -15, 4.68629, 3, -8, 11.31371), Block.box(13, -15, 4.68629, 16, -8, 11.31371), Stream.of(Block.box(4.68629, -8, 0, 11.31371, 13, 3), Block.box(4.68629, -8, 13, 11.31371, 13, 16), Block.box(0, -8, 4.68629, 3, 13, 11.31371), Block.box(13, -8, 4.68629, 16, 13, 11.31371), Stream.of(Block.box(5.51472, 13, 2, 10.48528, 32, 5), Block.box(5.51472, 13, 11, 10.48528, 32, 14), Block.box(2, 13, 5.51472, 5, 32, 10.48528), Block.box(11, 13, 5.51472, 14, 32, 10.48528)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get()).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    // WEST is the canonical physical furnace shape. It is the same voxel that
    // was already working for the west-facing GUI-less furnace; every other
    // orientation is derived from it using the same canonical transform as
    // physical item/click placement.
    private static final VoxelShape FORMED_WEST =
            VoxelShapeUtils.rotateHorizontal(WEST_NEW, Direction.EAST).move(0.0D, 1.0D, 0.0D);
    private static final VoxelShape FORMED_EAST = rotateFromCanonicalWest(FORMED_WEST, Direction.EAST);
    private static final VoxelShape FORMED_NORTH = rotateFromCanonicalWest(FORMED_WEST, Direction.NORTH);
    private static final VoxelShape FORMED_SOUTH = rotateFromCanonicalWest(FORMED_WEST, Direction.SOUTH);

    public PrimitiveFurnaceMultiblock_nogui() {
        super(Properties.of().noOcclusion().isSuffocating((state, level, pos) -> !state.getValue(IS_FORMED)).isViewBlocking((state, level, pos) -> !state.getValue(IS_FORMED)).requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(IS_FORMED, Boolean.FALSE).setValue(FACING, Direction.NORTH));
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState pState) {
        return true;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape formedShape = getFormedShape(state, level, pos);
        return formedShape != null ? formedShape : super.getInteractionShape(state, level, pos);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape formedShape = getFormedShape(state, level, pos);
        return formedShape != null ? formedShape : super.getVisualShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape formedShape = getFormedShape(state, level, pos);
        return formedShape != null ? formedShape : super.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape formedShape = getFormedShape(state, level, pos);
        return formedShape != null ? formedShape : super.getCollisionShape(state, level, pos, context);
    }

    @Nullable
    private VoxelShape getFormedShape(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity)
                || !furnaceEntity.isFormed()) {
            return null;
        }

        return switch (getFormedFacing(furnaceEntity, state)) {
            case WEST -> FORMED_WEST;
            case EAST -> FORMED_EAST;
            case NORTH -> FORMED_NORTH;
            case SOUTH -> FORMED_SOUTH;
            default -> FORMED_WEST;
        };
    }

    private static VoxelShape rotateFromCanonicalWest(VoxelShape canonicalShape, Direction formedFacing) {
        if (formedFacing == Direction.WEST) {
            return canonicalShape;
        }

        VoxelShape rotated = Shapes.empty();
        for (AABB box : canonicalShape.toAabbs()) {
            double minX = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;

            double[] xs = {box.minX, box.maxX};
            double[] zs = {box.minZ, box.maxZ};

            for (double x : xs) {
                for (double z : zs) {
                    Vec3 transformed = MultiblockHitHelper.fromCanonicalWest(
                            new Vec3(x, 0.0D, z),
                            formedFacing
                    );
                    minX = Math.min(minX, transformed.x);
                    minZ = Math.min(minZ, transformed.z);
                    maxX = Math.max(maxX, transformed.x);
                    maxZ = Math.max(maxZ, transformed.z);
                }
            }

            rotated = Shapes.or(
                    rotated,
                    Shapes.create(new AABB(minX, box.minY, minZ, maxX, box.maxY, maxZ))
            );
        }

        return rotated.optimize();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return (BlockState) this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection()).setValue(IS_FORMED, Boolean.FALSE);
    }
    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return (BlockState) state.setValue(FACING, direction.rotate(state.getValue(FACING))).setValue(IS_FORMED, Boolean.FALSE);
    }
    @Override
    public void animateTick(BlockState pState, Level level, BlockPos pos, RandomSource pRandom) {
        super.animateTick(pState, level, pos, pRandom);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity) {
            if (furnaceEntity.isFormed()) {
                if (furnaceEntity.isCooking()) {
                    if (level.getGameTime() % 1 == 0) { // Checks if it’s every 10 ticks
                        double x = pos.getX() + 0.5;
                        double y = pos.getY() + 3.0; // Slightly above the block
                        double z = pos.getZ() + 0.5;

                        level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y, z, 0.0, 0.03, 0.0);
                    }
                    if (level.getGameTime() % 1 == 0) { // Checks if it’s every 5 ticks
                        RandomSource random = level.random;

                        // Number of flames to spawn
                        int flameCount = 5;

                        for (int i = 0; i < flameCount; i++) {
                            // Randomize position within the block bounds
                            double x = pos.getX() + 0.3 + random.nextDouble() * 0.4; // Between 0.3 and 0.4 on X
                            double y = pos.getY() + 0.1 + random.nextDouble() * 0.3; // Between 0.1 and 0.3 on Y
                            double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4; // Between 0.3 and 0.4 on Z

                            // Add flame particle with slight upward velocity
                            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.01, 0.0);
                        }
                    }
                }
            }


        }

    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(IS_FORMED).add(FACING);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof PrimitiveFurnaceMultiblockEntity_nogui multiblockEntity) {
                if (multiblockEntity.isFormed()) {
                    interactableNoGui(pState, pLevel, pPos, pPlayer, pHand, pHit);
                } else {
                    multiblockEntity.onRightClick();
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide){
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BaseBlockEntityMagneticraft2 testmultiblock){
                testmultiblock.onDestroy(level); // it's not a test anymore but just old relic since it was called testmultiblock from making the multiblock system.
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide() ? null : createTickerHelper(pBlockEntityType, BlockEntityRegistry.primitivefurnacemultiblockentity_nogui.get(), PrimitiveFurnaceMultiblockEntity_nogui::serverTick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new PrimitiveFurnaceMultiblockEntity_nogui(pPos,pState);
    }
    @Override
    protected void interactableNoGui(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (!(blockEntity instanceof PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity)) {
            return;
        }

        Direction formedFacing = getFormedFacing(furnaceEntity, pState);
        Vec3 relativeHit = MultiblockHitHelper.relativeToController(pHit, pPos);
        Vec3 localHit = MultiblockHitHelper.toCanonicalWest(relativeHit, formedFacing);
        Direction localFace = toCanonicalWest(pHit.getDirection(), formedFacing);
        FurnaceZone zone = findFurnaceZone(localHit);

        if (Magneticraft2ConfigCommon.GENERAL.DevMode.get()) {
            String message = String.format(
                    "Furnace %s -> %s | local x=%.3f y=%.3f z=%.3f | face=%s",
                    formedFacing,
                    zone.displayName,
                    localHit.x,
                    localHit.y,
                    localHit.z,
                    localFace
            );
            LOGGER.info(message);
            pPlayer.displayClientMessage(Component.literal(message), true);
        }

        IItemHandler itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        if (itemHandler == null) {
            return;
        }

        ItemStack heldItem = pPlayer.getItemInHand(pHand);

        switch (zone) {
            case SMELTABLE_INPUT -> insertOne(itemHandler, 0, heldItem, pPlayer, furnaceEntity);
            case FUEL_INPUT -> {
                if (heldItem.is(Items.COAL)) {
                    insertOne(itemHandler, 1, heldItem, pPlayer, furnaceEntity);
                }
            }
            case PRIMARY_OUTPUT -> {
                if (heldItem.isEmpty()) {
                    extractOutput(itemHandler, 2, pPlayer, furnaceEntity);
                }
            }
            case SECONDARY_OUTPUT -> {
                if (heldItem.isEmpty()) {
                    extractOutput(itemHandler, 3, pPlayer, furnaceEntity);
                }
            }
            case NONE -> {
            }
        }
    }

    private FurnaceZone findFurnaceZone(Vec3 localHit) {
        // The renderer consumes these exact same boxes in DevMode, so what is
        // drawn in-world is always the area that the interaction code tests.
        for (FurnaceZoneBox zoneBox : INTERACTION_ZONE_BOXES) {
            if (zoneBox.bounds.contains(localHit)) {
                return zoneBox.zone;
            }
        }
        return FurnaceZone.NONE;
    }

    public static List<FurnaceZoneBox> getInteractionZoneBoxes() {
        return INTERACTION_ZONE_BOXES;
    }

    private void insertOne(IItemHandler itemHandler,
                           int slot,
                           ItemStack heldItem,
                           Player player,
                           PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity) {
        if (heldItem.isEmpty()) {
            return;
        }

        ItemStack oneItem = heldItem.copy();
        oneItem.setCount(1);
        ItemStack remainder = itemHandler.insertItem(slot, oneItem, false);
        int inserted = 1 - remainder.getCount();

        if (inserted <= 0) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            heldItem.shrink(inserted);
        }
        furnaceEntity.sync();
    }

    private void extractOutput(IItemHandler itemHandler,
                               int slot,
                               Player player,
                               PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity) {
        ItemStack extracted = itemHandler.extractItem(slot, 64, false);
        if (extracted.isEmpty()) {
            return;
        }

        if (!player.getInventory().add(extracted)) {
            player.drop(extracted, false);
        }
        furnaceEntity.sync();
    }

    public static Direction getFormedFacing(PrimitiveFurnaceMultiblockEntity_nogui furnaceEntity, BlockState state) {
        String blueprintName = furnaceEntity.getMBblueprintname();
        if (blueprintName != null && !blueprintName.isEmpty()) {
            String normalized = blueprintName.endsWith("_nogui")
                    ? blueprintName.substring(0, blueprintName.length() - "_nogui".length())
                    : blueprintName;

            if (normalized.endsWith("_west")) {
                return Direction.WEST;
            }
            if (normalized.endsWith("_east")) {
                return Direction.EAST;
            }
            if (normalized.endsWith("_north")) {
                return Direction.NORTH;
            }
            if (normalized.endsWith("_south")) {
                return Direction.SOUTH;
            }
        }

        return state.hasProperty(FACING) ? state.getValue(FACING) : Direction.WEST;
    }

    private Direction toCanonicalWest(Direction worldFace, Direction formedFacing) {
        if (worldFace.getAxis().isVertical()) {
            return worldFace;
        }

        return switch (formedFacing) {
            case WEST -> worldFace;
            case EAST -> switch (worldFace) {
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                default -> worldFace;
            };
            case NORTH -> switch (worldFace) {
                case NORTH -> Direction.WEST;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                case WEST -> Direction.SOUTH;
                default -> worldFace;
            };
            case SOUTH -> switch (worldFace) {
                case NORTH -> Direction.EAST;
                case SOUTH -> Direction.WEST;
                case EAST -> Direction.SOUTH;
                case WEST -> Direction.NORTH;
                default -> worldFace;
            };
            default -> worldFace;
        };
    }

    public record FurnaceZoneBox(FurnaceZone zone, AABB bounds) {
    }

    public enum FurnaceZone {
        SMELTABLE_INPUT("smeltable input", "INPUT"),
        FUEL_INPUT("fuel input", "FUEL"),
        PRIMARY_OUTPUT("primary output", "OUT 1"),
        SECONDARY_OUTPUT("secondary output", "OUT 2"),
        NONE("none", "NONE");

        private final String displayName;
        private final String debugLabel;

        FurnaceZone(String displayName, String debugLabel) {
            this.displayName = displayName;
            this.debugLabel = debugLabel;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDebugLabel() {
            return debugLabel;
        }
    }

}
