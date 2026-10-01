package com.magneticraft2.common.recipe.stage.stone;

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

public class primitive_grinder_multiblockrecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack output;
    private final int crushtime;
    public primitive_grinder_multiblockrecipe(ResourceLocation id, Ingredient input, ItemStack output, int crushtime) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.crushtime = crushtime;
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
        return output;
    }
    public int getCrushtime() { return crushtime; }
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
    public static class Type implements RecipeType<primitive_grinder_multiblockrecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "primitive_furnace_multiblock";
    }
    public static class Serializer implements RecipeSerializer<primitive_grinder_multiblockrecipe> {
        public static final Serializer INSTANCE = new Serializer();
        @Override
        public primitive_grinder_multiblockrecipe fromJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
            Ingredient ingredient = Ingredient.fromJson(jsonObject.get("input"));
            ItemStack output1 = ForgeRegistries.ITEMS.getValue(new ResourceLocation(jsonObject.getAsJsonObject("output").get("item").getAsString())).getDefaultInstance();
            int crushtime = jsonObject.get("crushtime").getAsInt();

            return new primitive_grinder_multiblockrecipe(resourceLocation, ingredient, output1, crushtime);
        }

        @Override
        public @Nullable primitive_grinder_multiblockrecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf friendlyByteBuf) {
            Ingredient ingredient = Ingredient.fromNetwork(friendlyByteBuf);
            ItemStack output = friendlyByteBuf.readItem();
            int crushtime = friendlyByteBuf.readInt();
            return new primitive_grinder_multiblockrecipe(resourceLocation, ingredient, output, crushtime);
        }

        @Override
        public void toNetwork(FriendlyByteBuf friendlyByteBuf, primitive_grinder_multiblockrecipe primitiveFurnaceMultiblockrecipe) {
            primitiveFurnaceMultiblockrecipe.input.toNetwork(friendlyByteBuf);
            friendlyByteBuf.writeItem(primitiveFurnaceMultiblockrecipe.output);
            friendlyByteBuf.writeInt(primitiveFurnaceMultiblockrecipe.crushtime);
        }
    }
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(input);
        return ingredients;
    }
}
