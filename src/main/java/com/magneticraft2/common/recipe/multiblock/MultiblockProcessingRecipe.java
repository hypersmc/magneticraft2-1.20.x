package com.magneticraft2.common.recipe.multiblock;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Generic datapack recipe for JSON multiblock processing machines.
 *
 * A recipe identifies the controller/machine it belongs to, then describes
 * item I/O, optional byproduct, mechanical requirements and optional fluid I/O.
 * The multiblock controller remains responsible for deciding how those
 * resources are physically exposed through its modules.
 */
public class MultiblockProcessingRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final ResourceLocation machine;
    private final Ingredient input;
    private final int inputCount;
    private final ItemStack output;
    private final ItemStack byproduct;
    private final float byproductChance;
    private final int processTime;
    private final float minSpeed;
    private final float torque;
    private final FluidStack fluidInput;
    private final FluidStack fluidOutput;

    public MultiblockProcessingRecipe(
            ResourceLocation id,
            ResourceLocation machine,
            Ingredient input,
            int inputCount,
            ItemStack output,
            ItemStack byproduct,
            float byproductChance,
            int processTime,
            float minSpeed,
            float torque,
            FluidStack fluidInput,
            FluidStack fluidOutput) {
        this.id = id;
        this.machine = machine;
        this.input = input;
        this.inputCount = Math.max(1, inputCount);
        this.output = output.copy();
        this.byproduct = byproduct.copy();
        this.byproductChance =
                Math.max(
                        0.0F,
                        Math.min(1.0F, byproductChance)
                );
        this.processTime = Math.max(1, processTime);
        this.minSpeed = Math.max(0.0F, minSpeed);
        this.torque = Math.max(0.0F, torque);
        this.fluidInput =
                fluidInput == null
                        ? FluidStack.EMPTY
                        : fluidInput.copy();
        this.fluidOutput =
                fluidOutput == null
                        ? FluidStack.EMPTY
                        : fluidOutput.copy();
    }

    @Override
    public boolean matches(
            Container container,
            Level level) {
        ItemStack stack = container.getItem(0);

        return !stack.isEmpty()
                && stack.getCount() >= inputCount
                && input.test(stack);
    }

    public boolean matchesInput(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getCount() >= inputCount
                && input.test(stack);
    }

    public boolean acceptsInputType(ItemStack stack) {
        return !stack.isEmpty()
                && input.test(stack);
    }

    public boolean isForMachine(ResourceLocation machineId) {
        return machine.equals(machineId);
    }

    public ResourceLocation getMachine() {
        return machine;
    }

    public int getInputCount() {
        return inputCount;
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    public ItemStack getByproduct() {
        return byproduct.copy();
    }

    public float getByproductChance() {
        return byproductChance;
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

    public FluidStack getFluidInput() {
        return fluidInput.copy();
    }

    public FluidStack getFluidOutput() {
        return fluidOutput.copy();
    }

    @Override
    public ItemStack assemble(
            Container container,
            RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(
            RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients =
                NonNullList.create();
        ingredients.add(input);
        return ingredients;
    }

    public static final class Type
            implements RecipeType<MultiblockProcessingRecipe> {
        public static final Type INSTANCE =
                new Type();
        public static final String ID =
                "multiblock_processing";

        private Type() {
        }
    }

    public static final class Serializer
            implements RecipeSerializer<MultiblockProcessingRecipe> {
        public static final Serializer INSTANCE =
                new Serializer();

        private Serializer() {
        }

        @Override
        public MultiblockProcessingRecipe fromJson(
                ResourceLocation id,
                JsonObject json) {
            if (!json.has("machine")) {
                throw new JsonSyntaxException(
                        "Multiblock recipe "
                                + id
                                + " is missing 'machine'"
                );
            }

            ResourceLocation machine =
                    new ResourceLocation(
                            json.get("machine")
                                    .getAsString()
                    );

            Ingredient input =
                    Ingredient.fromJson(
                            json.get("input")
                    );

            int inputCount =
                    json.has("input_count")
                            ? Math.max(
                                    1,
                                    json.get("input_count")
                                            .getAsInt()
                            )
                            : 1;

            ItemStack output =
                    readItemStack(
                            id,
                            "output",
                            json.getAsJsonObject(
                                    "output"
                            )
                    );

            ItemStack byproduct =
                    ItemStack.EMPTY;
            float byproductChance = 0.0F;

            if (json.has("byproduct")) {
                byproduct =
                        readItemStack(
                                id,
                                "byproduct",
                                json.getAsJsonObject(
                                        "byproduct"
                                )
                        );
                byproductChance =
                        json.has("byproduct_chance")
                                ? json.get(
                                        "byproduct_chance"
                                ).getAsFloat()
                                : 1.0F;
            }

            int processTime =
                    json.has("process_time")
                            ? json.get("process_time")
                                    .getAsInt()
                            : 160;

            float minSpeed = 0.0F;
            float torque = 0.0F;

            if (json.has("mechanical")) {
                JsonObject mechanical =
                        json.getAsJsonObject(
                                "mechanical"
                        );

                minSpeed =
                        mechanical.has("min_speed")
                                ? mechanical.get(
                                        "min_speed"
                                ).getAsFloat()
                                : 0.0F;
                torque =
                        mechanical.has("torque")
                                ? mechanical.get(
                                        "torque"
                                ).getAsFloat()
                                : 0.0F;
            } else {
                // Transitional support for the two first Copper Age recipe
                // JSONs while packs migrate to the nested mechanical object.
                minSpeed =
                        json.has("min_speed")
                                ? json.get("min_speed")
                                        .getAsFloat()
                                : 0.0F;
                torque =
                        json.has("torque")
                                ? json.get("torque")
                                        .getAsFloat()
                                : 0.0F;
            }

            FluidStack fluidInput =
                    json.has("fluid_input")
                            ? readFluidStack(
                                    id,
                                    "fluid_input",
                                    json.getAsJsonObject(
                                            "fluid_input"
                                    )
                            )
                            : FluidStack.EMPTY;

            FluidStack fluidOutput =
                    json.has("fluid_output")
                            ? readFluidStack(
                                    id,
                                    "fluid_output",
                                    json.getAsJsonObject(
                                            "fluid_output"
                                    )
                            )
                            : FluidStack.EMPTY;

            return new MultiblockProcessingRecipe(
                    id,
                    machine,
                    input,
                    inputCount,
                    output,
                    byproduct,
                    byproductChance,
                    processTime,
                    minSpeed,
                    torque,
                    fluidInput,
                    fluidOutput
            );
        }

        @Override
        public @Nullable MultiblockProcessingRecipe
        fromNetwork(
                ResourceLocation id,
                FriendlyByteBuf buffer) {
            ResourceLocation machine =
                    buffer.readResourceLocation();
            Ingredient input =
                    Ingredient.fromNetwork(buffer);
            int inputCount =
                    buffer.readVarInt();
            ItemStack output =
                    buffer.readItem();
            ItemStack byproduct =
                    buffer.readItem();
            float byproductChance =
                    buffer.readFloat();
            int processTime =
                    buffer.readVarInt();
            float minSpeed =
                    buffer.readFloat();
            float torque =
                    buffer.readFloat();
            FluidStack fluidInput =
                    readFluidStack(buffer);
            FluidStack fluidOutput =
                    readFluidStack(buffer);

            return new MultiblockProcessingRecipe(
                    id,
                    machine,
                    input,
                    inputCount,
                    output,
                    byproduct,
                    byproductChance,
                    processTime,
                    minSpeed,
                    torque,
                    fluidInput,
                    fluidOutput
            );
        }

        @Override
        public void toNetwork(
                FriendlyByteBuf buffer,
                MultiblockProcessingRecipe recipe) {
            buffer.writeResourceLocation(
                    recipe.machine
            );
            recipe.input.toNetwork(buffer);
            buffer.writeVarInt(
                    recipe.inputCount
            );
            buffer.writeItem(
                    recipe.output
            );
            buffer.writeItem(
                    recipe.byproduct
            );
            buffer.writeFloat(
                    recipe.byproductChance
            );
            buffer.writeVarInt(
                    recipe.processTime
            );
            buffer.writeFloat(
                    recipe.minSpeed
            );
            buffer.writeFloat(
                    recipe.torque
            );
            writeFluidStack(
                    buffer,
                    recipe.fluidInput
            );
            writeFluidStack(
                    buffer,
                    recipe.fluidOutput
            );
        }

        private static ItemStack readItemStack(
                ResourceLocation recipeId,
                String field,
                JsonObject json) {
            if (json == null
                    || !json.has("item")) {
                throw new JsonSyntaxException(
                        "Multiblock recipe "
                                + recipeId
                                + " has invalid '"
                                + field
                                + "'"
                );
            }

            ResourceLocation itemId =
                    new ResourceLocation(
                            json.get("item")
                                    .getAsString()
                    );

            var item =
                    ForgeRegistries.ITEMS
                            .getValue(itemId);

            if (item == null) {
                throw new JsonSyntaxException(
                        "Unknown item '"
                                + itemId
                                + "' in multiblock recipe "
                                + recipeId
                );
            }

            ItemStack stack =
                    item.getDefaultInstance();

            if (json.has("count")) {
                stack.setCount(
                        Math.max(
                                1,
                                json.get("count")
                                        .getAsInt()
                        )
                );
            }

            return stack;
        }

        private static FluidStack readFluidStack(
                ResourceLocation recipeId,
                String field,
                JsonObject json) {
            if (json == null
                    || !json.has("fluid")
                    || !json.has("amount")) {
                throw new JsonSyntaxException(
                        "Multiblock recipe "
                                + recipeId
                                + " has invalid '"
                                + field
                                + "'"
                );
            }

            ResourceLocation fluidId =
                    new ResourceLocation(
                            json.get("fluid")
                                    .getAsString()
                    );

            var fluid =
                    ForgeRegistries.FLUIDS
                            .getValue(fluidId);

            if (fluid == null) {
                throw new JsonSyntaxException(
                        "Unknown fluid '"
                                + fluidId
                                + "' in multiblock recipe "
                                + recipeId
                );
            }

            int amount =
                    Math.max(
                            1,
                            json.get("amount")
                                    .getAsInt()
                    );

            return new FluidStack(
                    fluid,
                    amount
            );
        }

        private static void writeFluidStack(
                FriendlyByteBuf buffer,
                FluidStack stack) {
            boolean present =
                    stack != null
                            && !stack.isEmpty();

            buffer.writeBoolean(present);

            if (!present) {
                return;
            }

            ResourceLocation fluidId =
                    ForgeRegistries.FLUIDS
                            .getKey(
                                    stack.getFluid()
                            );

            if (fluidId == null) {
                throw new IllegalStateException(
                        "Cannot sync unregistered fluid "
                                + stack.getFluid()
                );
            }

            buffer.writeResourceLocation(
                    fluidId
            );
            buffer.writeVarInt(
                    stack.getAmount()
            );
        }

        private static FluidStack readFluidStack(
                FriendlyByteBuf buffer) {
            if (!buffer.readBoolean()) {
                return FluidStack.EMPTY;
            }

            ResourceLocation fluidId =
                    buffer.readResourceLocation();
            var fluid =
                    ForgeRegistries.FLUIDS
                            .getValue(fluidId);

            if (fluid == null) {
                return FluidStack.EMPTY;
            }

            return new FluidStack(
                    fluid,
                    Math.max(
                            1,
                            buffer.readVarInt()
                    )
            );
        }
    }
}
