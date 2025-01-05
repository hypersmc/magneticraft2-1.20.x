package com.magneticraft2.common.recipe.stage.stone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import static com.magneticraft2.common.magneticraft2.MOD_ID;

/**
 * @author JumpWatch on 14-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class primitive_furnace_multiblockrecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient input1;
    private final Ingredient input2; // Second input
    private final ItemStack output1;
    private final ItemStack output2; // Second output
    private final int cookTime;

    public primitive_furnace_multiblockrecipe(ResourceLocation id, Ingredient input1, Ingredient input2, ItemStack output1, ItemStack output2, int cookTime) {
        this.id = id;
        this.input1 = input1;
        this.input2 = input2;
        this.output1 = output1;
        this.output2 = output2;
        this.cookTime = cookTime;
    }

    @Override
    public boolean matches(Container container, Level world) {
        // Check if the container has at least 2 slots and matches both inputs
        return input1.test(container.getItem(0)) && input2.test(container.getItem(1));
    }

    @Override
    public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
        return output1.copy(); // Returns the primary output
    }


    public ItemStack getSecondOutput() {
        return output2.copy(); // Returns the secondary output
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2; // Require at least 2 slots for crafting
    }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return output1;
    }

    public ItemStack getSecondaryResultItem() {
        return output2; // Secondary result item
    }

    public Ingredient getInput1() {
        return input1;
    }

    public Ingredient getInput2() {
        return input2;
    }

    public int getCookTime() {
        return cookTime;
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

    public static class Type implements RecipeType<primitive_furnace_multiblockrecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "primitive_furnace_multiblock";
    }

    public static class Serializer implements RecipeSerializer<primitive_furnace_multiblockrecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public primitive_furnace_multiblockrecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input1 = Ingredient.fromJson(json.get("input1"));
            Ingredient input2 = Ingredient.fromJson(json.get("input2"));
            ItemStack output1 = ForgeRegistries.ITEMS.getValue(new ResourceLocation(json.getAsJsonObject("output1").get("item").getAsString())).getDefaultInstance();
            ItemStack output2 = ForgeRegistries.ITEMS.getValue(new ResourceLocation(json.getAsJsonObject("output2").get("item").getAsString())).getDefaultInstance();
            int cookTime = json.get("cookTime").getAsInt();

            return new primitive_furnace_multiblockrecipe(recipeId, input1, input2, output1, output2, cookTime);
        }

        @Override
        public primitive_furnace_multiblockrecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient input1 = Ingredient.fromNetwork(buffer);
            Ingredient input2 = Ingredient.fromNetwork(buffer);
            ItemStack output1 = buffer.readItem();
            ItemStack output2 = buffer.readItem();
            int cookTime = buffer.readVarInt();

            return new primitive_furnace_multiblockrecipe(recipeId, input1, input2, output1, output2, cookTime);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, primitive_furnace_multiblockrecipe recipe) {
            recipe.input1.toNetwork(buffer);
            recipe.input2.toNetwork(buffer);
            buffer.writeItem(recipe.output1);
            buffer.writeItem(recipe.output2);
            buffer.writeVarInt(recipe.cookTime);
        }
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(input1);
        ingredients.add(input2);
        return ingredients;
    }
}