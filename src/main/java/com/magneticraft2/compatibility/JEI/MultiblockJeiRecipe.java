package com.magneticraft2.compatibility.JEI;

import com.magneticraft2.common.recipe.multiblock.MultiblockProcessingRecipe;
import com.magneticraft2.common.recipe.multiblock.MechanicalAssemblyRecipe;
import com.magneticraft2.common.recipe.multiblock.BloomeryRecipe;
import com.magneticraft2.common.recipe.stage.copper.MechanicalOreWasherRecipe;
import com.magneticraft2.common.recipe.stage.copper.MechanicalSifterRecipe;
import com.magneticraft2.common.recipe.stage.stone.primitive_furnace_multiblockrecipe;
import com.magneticraft2.common.recipe.stage.stone.primitive_grinder_multiblockrecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JEI-facing view of a multiblock processing recipe.
 *
 * Keeping JEI on this small compatibility model lets the actual processing
 * system stay independent of JEI and lets old machine-specific recipe types
 * coexist with the generic JSON multiblock recipe type during migration.
 */
public final class MultiblockJeiRecipe {
    public static final ResourceLocation PRIMITIVE_FURNACE =
            new ResourceLocation("magneticraft2", "primitivefurnace_multiblock_nogui");
    public static final ResourceLocation PRIMITIVE_GRINDER =
            new ResourceLocation("magneticraft2", "primitive_grinder_bmultiblock");
    public static final ResourceLocation MECHANICAL_ORE_WASHER =
            new ResourceLocation("magneticraft2", "mechanical_ore_washer");
    public static final ResourceLocation MECHANICAL_SIFTER =
            new ResourceLocation("magneticraft2", "mechanical_sifter");
    public static final ResourceLocation BRONZE_STAMP_MILL =
            new ResourceLocation("magneticraft2", "bronze_stamp_mill");
    public static final ResourceLocation BRONZE_MECHANICAL_LATHE =
            new ResourceLocation("magneticraft2", "bronze_mechanical_lathe");
    public static final ResourceLocation BRONZE_FORGE_HAMMER =
            new ResourceLocation("magneticraft2", "bronze_forge_hammer");
    public static final ResourceLocation BRONZE_ROLLING_MILL =
            new ResourceLocation("magneticraft2", "bronze_rolling_mill");
    public static final ResourceLocation IRON_STOCK_SHEAR =
            new ResourceLocation("magneticraft2", "iron_stock_shear");
    public static final ResourceLocation BRONZE_MECHANICAL_PRESS =
            new ResourceLocation("magneticraft2", "bronze_mechanical_press");
    public static final ResourceLocation BRONZE_PRECISION_BORING_MACHINE =
            new ResourceLocation("magneticraft2", "bronze_precision_boring_machine");
    public static final ResourceLocation BRONZE_MECHANICAL_SAWMILL =
            new ResourceLocation("magneticraft2", "bronze_mechanical_sawmill");
    public static final ResourceLocation BRONZE_MECHANICAL_ASSEMBLY_STATION =
            new ResourceLocation("magneticraft2", "bronze_mechanical_assembly_station");
    public static final ResourceLocation BRONZE_FOUNDRY_SAND_MIXER =
            new ResourceLocation("magneticraft2", "bronze_foundry_sand_mixer");
    public static final ResourceLocation ROTARY_ORE_CLASSIFIER =
            new ResourceLocation("magneticraft2", "rotary_ore_classifier");
    public static final ResourceLocation WATER_SETTLING_TANK =
            new ResourceLocation("magneticraft2", "water_settling_tank");
    public static final ResourceLocation CHARCOAL_RETORT =
            new ResourceLocation("magneticraft2", "charcoal_retort");
    public static final ResourceLocation REFRACTORY_KILN =
            new ResourceLocation("magneticraft2", "refractory_kiln");
    public static final ResourceLocation ORE_ROASTING_KILN =
            new ResourceLocation("magneticraft2", "ore_roasting_kiln");
    public static final ResourceLocation BLOOMERY_FURNACE =
            new ResourceLocation("magneticraft2", "bloomery_furnace");

    private final ResourceLocation id;
    private final ResourceLocation machine;
    private final List<List<ItemStack>> itemInputs;
    private final List<Output> itemOutputs;
    private final FluidStack fluidInput;
    private final FluidStack fluidOutput;
    private final int processTime;
    private final float minSpeed;
    private final float torque;

    private MultiblockJeiRecipe(
            ResourceLocation id,
            ResourceLocation machine,
            List<List<ItemStack>> itemInputs,
            List<Output> itemOutputs,
            FluidStack fluidInput,
            FluidStack fluidOutput,
            int processTime,
            float minSpeed,
            float torque) {
        this.id = id;
        this.machine = machine;
        this.itemInputs = copyInputs(itemInputs);
        this.itemOutputs = copyOutputs(itemOutputs);
        this.fluidInput = fluidInput == null ? FluidStack.EMPTY : fluidInput.copy();
        this.fluidOutput = fluidOutput == null ? FluidStack.EMPTY : fluidOutput.copy();
        this.processTime = Math.max(1, processTime);
        this.minSpeed = Math.max(0.0F, minSpeed);
        this.torque = Math.max(0.0F, torque);
    }

    public static MultiblockJeiRecipe from(BloomeryRecipe recipe) {
        var ingredients = recipe.getIngredients();
        return new MultiblockJeiRecipe(recipe.getId(), BLOOMERY_FURNACE,
                List.of(stacksFor(ingredients.get(0),recipe.oreCount()),
                        stacksFor(ingredients.get(1),recipe.fluxCount()),
                        List.of(new ItemStack(Items.CHARCOAL))),
                List.of(new Output(recipe.bloom(),1F),
                        new Output(recipe.slag(),1F)),
                FluidStack.EMPTY,FluidStack.EMPTY,recipe.processTime(),0F,0F);
    }

    public static MultiblockJeiRecipe from(MechanicalAssemblyRecipe recipe) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (int i = 0; i < MechanicalAssemblyRecipe.INPUT_SLOTS; i++) {
            var part = recipe.part(i);
            inputs.add(stacksFor(part.ingredient(), part.count()));
        }
        return new MultiblockJeiRecipe(recipe.getId(), BRONZE_MECHANICAL_ASSEMBLY_STATION,
                inputs, List.of(new Output(recipe.output(), 1.0F)),
                FluidStack.EMPTY, FluidStack.EMPTY,
                recipe.processTime(), recipe.minSpeed(), recipe.torque());
    }

    public static MultiblockJeiRecipe from(MultiblockProcessingRecipe recipe) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        if (!recipe.getIngredients().isEmpty()) {
            inputs.add(stacksFor(
                    recipe.getIngredients().get(0),
                    recipe.getInputCount()
            ));
        }

        List<Output> outputs = new ArrayList<>();
        outputs.add(new Output(recipe.getOutput(), 1.0F));
        if (!recipe.getByproduct().isEmpty()) {
            outputs.add(new Output(
                    recipe.getByproduct(),
                    recipe.getByproductChance()
            ));
        }

        return new MultiblockJeiRecipe(
                recipe.getId(),
                recipe.getMachine(),
                inputs,
                outputs,
                recipe.getFluidInput(),
                recipe.getFluidOutput(),
                recipe.getProcessTime(),
                recipe.getMinSpeed(),
                recipe.getTorque()
        );
    }

    public static MultiblockJeiRecipe from(
            primitive_furnace_multiblockrecipe recipe,
            RegistryAccess registryAccess) {
        List<List<ItemStack>> inputs = List.of(
                stacksFor(recipe.getInput1(), 1),
                stacksFor(recipe.getInput2(), 1)
        );

        List<Output> outputs = new ArrayList<>();
        outputs.add(new Output(
                recipe.getResultItem(registryAccess),
                1.0F
        ));
        if (!recipe.getSecondOutput().isEmpty()) {
            outputs.add(new Output(
                    recipe.getSecondOutput(),
                    1.0F
            ));
        }

        return new MultiblockJeiRecipe(
                recipe.getId(),
                PRIMITIVE_FURNACE,
                inputs,
                outputs,
                FluidStack.EMPTY,
                FluidStack.EMPTY,
                recipe.getCookTime(),
                0.0F,
                0.0F
        );
    }

    public static MultiblockJeiRecipe from(
            primitive_grinder_multiblockrecipe recipe,
            RegistryAccess registryAccess) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        if (!recipe.getIngredients().isEmpty()) {
            inputs.add(stacksFor(
                    recipe.getIngredients().get(0),
                    1
            ));
        }

        return new MultiblockJeiRecipe(
                recipe.getId(),
                PRIMITIVE_GRINDER,
                inputs,
                List.of(new Output(
                        recipe.getResultItem(registryAccess),
                        1.0F
                )),
                FluidStack.EMPTY,
                FluidStack.EMPTY,
                recipe.getCrushtime(),
                recipe.getMinSpeed(),
                recipe.getTorque()
        );
    }

    public static MultiblockJeiRecipe from(
            MechanicalOreWasherRecipe recipe) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        if (!recipe.getIngredients().isEmpty()) {
            inputs.add(stacksFor(
                    recipe.getIngredients().get(0),
                    1
            ));
        }

        List<Output> outputs = new ArrayList<>();
        outputs.add(new Output(recipe.getOutput(), 1.0F));
        if (!recipe.getByproduct().isEmpty()) {
            outputs.add(new Output(
                    recipe.getByproduct(),
                    recipe.getByproductChance()
            ));
        }

        return new MultiblockJeiRecipe(
                recipe.getId(),
                MECHANICAL_ORE_WASHER,
                inputs,
                outputs,
                new FluidStack(
                        Fluids.WATER,
                        recipe.getWaterAmount()
                ),
                FluidStack.EMPTY,
                recipe.getProcessTime(),
                recipe.getMinSpeed(),
                recipe.getTorque()
        );
    }

    public static MultiblockJeiRecipe from(
            MechanicalSifterRecipe recipe) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        if (!recipe.getIngredients().isEmpty()) {
            inputs.add(stacksFor(
                    recipe.getIngredients().get(0),
                    1
            ));
        }

        List<Output> outputs = new ArrayList<>();
        outputs.add(new Output(recipe.getOutput(), 1.0F));
        if (!recipe.getByproduct().isEmpty()) {
            outputs.add(new Output(
                    recipe.getByproduct(),
                    recipe.getByproductChance()
            ));
        }

        return new MultiblockJeiRecipe(
                recipe.getId(),
                MECHANICAL_SIFTER,
                inputs,
                outputs,
                FluidStack.EMPTY,
                FluidStack.EMPTY,
                recipe.getProcessTime(),
                recipe.getMinSpeed(),
                recipe.getTorque()
        );
    }

    public ResourceLocation getId() {
        return id;
    }

    public ResourceLocation getMachine() {
        return machine;
    }

    public ItemStack getMachineStack() {
        Item item = ForgeRegistries.ITEMS.getValue(machine);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    public List<List<ItemStack>> getItemInputs() {
        return itemInputs;
    }

    public List<Output> getItemOutputs() {
        return itemOutputs;
    }

    public FluidStack getFluidInput() {
        return fluidInput.copy();
    }

    public FluidStack getFluidOutput() {
        return fluidOutput.copy();
    }

    public int getProcessTime() {
        return processTime;
    }

    public float getMinSpeed() {
        return minSpeed;
    }

    public float getTorque() {
        return torque;
    }

    private static List<ItemStack> stacksFor(
            Ingredient ingredient,
            int count) {
        int safeCount = Math.max(1, count);
        List<ItemStack> stacks = new ArrayList<>();

        Arrays.stream(ingredient.getItems())
                .forEach(stack -> {
                    ItemStack copy = stack.copy();
                    copy.setCount(safeCount);
                    stacks.add(copy);
                });

        return Collections.unmodifiableList(stacks);
    }

    private static List<List<ItemStack>> copyInputs(
            List<List<ItemStack>> inputs) {
        List<List<ItemStack>> copied = new ArrayList<>();
        for (List<ItemStack> input : inputs) {
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : input) {
                stacks.add(stack.copy());
            }
            copied.add(Collections.unmodifiableList(stacks));
        }
        return Collections.unmodifiableList(copied);
    }

    private static List<Output> copyOutputs(
            List<Output> outputs) {
        List<Output> copied = new ArrayList<>();
        for (Output output : outputs) {
            if (!output.stack.isEmpty()) {
                copied.add(new Output(
                        output.stack,
                        output.chance
                ));
            }
        }
        return Collections.unmodifiableList(copied);
    }

    public static final class Output {
        private final ItemStack stack;
        private final float chance;

        private Output(
                ItemStack stack,
                float chance) {
            this.stack = stack.copy();
            this.chance = Math.max(
                    0.0F,
                    Math.min(1.0F, chance)
            );
        }

        public ItemStack getStack() {
            return stack.copy();
        }

        public float getChance() {
            return chance;
        }
    }
}
