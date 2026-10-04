package com.magneticraft2.common.registry.registers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * Magneticraft process fluids.
 *
 * Dirty Water intentionally reuses Minecraft's water sprites with a brown tint.
 * It is chemically/registry-distinct from clean water and can therefore be
 * routed, stored and recycled independently later.
 */
public final class FluidRegistry {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(
                    ForgeRegistries.Keys.FLUID_TYPES,
                    MOD_ID
            );

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(
                    ForgeRegistries.FLUIDS,
                    MOD_ID
            );

    public static final RegistryObject<FluidType> DIRTY_WATER_TYPE =
            FLUID_TYPES.register(
                    "dirty_water",
                    () -> new FluidType(
                            FluidType.Properties.create()
                                    .descriptionId(
                                            "fluid.magneticraft2.dirty_water"
                                    )
                                    .density(1050)
                                    .viscosity(1100)
                    ) {
                        @Override
                        public void initializeClient(
                                Consumer<IClientFluidTypeExtensions> consumer) {
                            consumer.accept(
                                    new IClientFluidTypeExtensions() {
                                        private static final ResourceLocation STILL =
                                                new ResourceLocation(
                                                        "minecraft",
                                                        "block/water_still"
                                                );
                                        private static final ResourceLocation FLOWING =
                                                new ResourceLocation(
                                                        "minecraft",
                                                        "block/water_flow"
                                                );

                                        @Override
                                        public ResourceLocation getStillTexture() {
                                            return STILL;
                                        }

                                        @Override
                                        public ResourceLocation getFlowingTexture() {
                                            return FLOWING;
                                        }

                                        @Override
                                        public int getTintColor() {
                                            return 0xFF6F5A36;
                                        }
                                    }
                            );
                        }
                    }
            );

    public static final RegistryObject<FlowingFluid> DIRTY_WATER =
            FLUIDS.register(
                    "dirty_water",
                    () -> new ForgeFlowingFluid.Source(
                            dirtyWaterProperties()
                    )
            );

    public static final RegistryObject<FlowingFluid> FLOWING_DIRTY_WATER =
            FLUIDS.register(
                    "flowing_dirty_water",
                    () -> new ForgeFlowingFluid.Flowing(
                            dirtyWaterProperties()
                    )
            );

    public static final RegistryObject<LiquidBlock> DIRTY_WATER_BLOCK =
            BlockRegistry.BLOCKS.register(
                    "dirty_water",
                    () -> new LiquidBlock(
                            DIRTY_WATER,
                            BlockBehaviour.Properties.copy(
                                    Blocks.WATER
                            ).noLootTable()
                    )
            );

    public static final RegistryObject<Item> DIRTY_WATER_BUCKET =
            ItemRegistry.ITEMS.register(
                    "dirty_water_bucket",
                    () -> new BucketItem(
                            DIRTY_WATER,
                            new Item.Properties()
                                    .craftRemainder(
                                            Items.BUCKET
                                    )
                                    .stacksTo(1)
                    )
            );

    /**
     * Fluid source/flowing suppliers are registered before Forge invokes them.
     * Keep the Properties object behind a lazy holder so class initialization
     * never reads DIRTY_WATER / FLOWING_DIRTY_WATER before those RegistryObject
     * fields themselves have been assigned.
     */
    private static ForgeFlowingFluid.Properties dirtyWaterProperties() {
        return DirtyWaterPropertiesHolder.PROPERTIES;
    }

    private static final class DirtyWaterPropertiesHolder {
        private static final ForgeFlowingFluid.Properties PROPERTIES =
                new ForgeFlowingFluid.Properties(
                        DIRTY_WATER_TYPE,
                        DIRTY_WATER,
                        FLOWING_DIRTY_WATER
                )
                        .block(DIRTY_WATER_BLOCK)
                        .bucket(DIRTY_WATER_BUCKET)
                        .slopeFindDistance(4)
                        .levelDecreasePerBlock(1);

        private DirtyWaterPropertiesHolder() {
        }
    }

    private FluidRegistry() {
    }

    public static void init(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
