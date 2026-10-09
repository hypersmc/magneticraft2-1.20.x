package com.magneticraft2.common.recipe.stage.copper;

import com.google.gson.JsonObject;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class MechanicalOreWasherRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack output;
    private final ItemStack byproduct;
    private final float byproductChance;
    private final int processTime;
    private final float minSpeed;
    private final float torque;
    private final int waterAmount;

    public MechanicalOreWasherRecipe(
            ResourceLocation id,
            Ingredient input,
            ItemStack output,
            ItemStack byproduct,
            float byproductChance,
            int processTime,
            float minSpeed,
            float torque,
            int waterAmount) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.byproduct = byproduct;
        this.byproductChance = Math.max(0.0F, Math.min(1.0F, byproductChance));
        this.processTime = Math.max(1, processTime);
        this.minSpeed = Math.max(0.0F, minSpeed);
        this.torque = Math.max(0.0F, torque);
        this.waterAmount = Math.max(1, waterAmount);
    }

    @Override
    public boolean matches(Container container, Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return output.copy();
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

    public int getWaterAmount() {
        return waterAmount;
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
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(input);
        return ingredients;
    }

    public static class Type implements RecipeType<MechanicalOreWasherRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "mechanical_ore_washing";
    }

    public static class Serializer implements RecipeSerializer<MechanicalOreWasherRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public MechanicalOreWasherRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = Ingredient.fromJson(json.get("input"));
            ItemStack output = readStack(json.getAsJsonObject("output"));

            ItemStack byproduct = ItemStack.EMPTY;
            float byproductChance = 0.0F;
            if (json.has("byproduct")) {
                byproduct = readStack(json.getAsJsonObject("byproduct"));
                byproductChance = json.has("byproduct_chance")
                        ? json.get("byproduct_chance").getAsFloat()
                        : 1.0F;
            }

            int processTime = json.has("process_time")
                    ? json.get("process_time").getAsInt()
                    : 160;
            float minSpeed = json.has("min_speed")
                    ? json.get("min_speed").getAsFloat()
                    : 20.0F;
            float torque = json.has("torque")
                    ? json.get("torque").getAsFloat()
                    : 3.0F;
            int waterAmount = json.has("water")
                    ? json.get("water").getAsInt()
                    : 250;

            return new MechanicalOreWasherRecipe(
                    id,
                    input,
                    output,
                    byproduct,
                    byproductChance,
                    processTime,
                    minSpeed,
                    torque,
                    waterAmount
            );
        }

        @Override
        public @Nullable MechanicalOreWasherRecipe fromNetwork(
                ResourceLocation id,
                FriendlyByteBuf buffer) {
            return new MechanicalOreWasherRecipe(
                    id,
                    Ingredient.fromNetwork(buffer),
                    buffer.readItem(),
                    buffer.readItem(),
                    buffer.readFloat(),
                    buffer.readInt(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readVarInt()
            );
        }

        @Override
        public void toNetwork(
                FriendlyByteBuf buffer,
                MechanicalOreWasherRecipe recipe) {
            recipe.input.toNetwork(buffer);
            buffer.writeItem(recipe.output);
            buffer.writeItem(recipe.byproduct);
            buffer.writeFloat(recipe.byproductChance);
            buffer.writeInt(recipe.processTime);
            buffer.writeFloat(recipe.minSpeed);
            buffer.writeFloat(recipe.torque);
            buffer.writeVarInt(recipe.waterAmount);
        }

        private static ItemStack readStack(JsonObject json) {
            ResourceLocation itemId =
                    new ResourceLocation(
                            json.get("item").getAsString()
                    );
            var item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                return ItemStack.EMPTY;
            }

            ItemStack stack = item.getDefaultInstance();
            if (json.has("count")) {
                stack.setCount(
                        Math.max(
                                1,
                                json.get("count").getAsInt()
                        )
                );
            }
            return stack;
        }
    }
}
