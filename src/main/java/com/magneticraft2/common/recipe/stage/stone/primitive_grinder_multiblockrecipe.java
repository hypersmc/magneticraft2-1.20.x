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
    private final float minSpeed;
    private final float torque;

    public primitive_grinder_multiblockrecipe(
            ResourceLocation id,
            Ingredient input,
            ItemStack output,
            int crushtime,
            float minSpeed,
            float torque) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.crushtime = Math.max(1, crushtime);
        this.minSpeed = Math.max(0.0F, minSpeed);
        this.torque = Math.max(0.0F, torque);
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
    public float getMinSpeed() { return minSpeed; }
    public float getTorque() { return torque; }
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
        public static final String ID = "primitive_grinder_bmultiblock";
    }
    public static class Serializer implements RecipeSerializer<primitive_grinder_multiblockrecipe> {
        public static final Serializer INSTANCE = new Serializer();
        @Override
        public primitive_grinder_multiblockrecipe fromJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
            Ingredient ingredient = Ingredient.fromJson(jsonObject.get("input"));
            JsonObject outputJson = jsonObject.getAsJsonObject("output");
            ItemStack output1 = ForgeRegistries.ITEMS.getValue(new ResourceLocation(outputJson.get("item").getAsString())).getDefaultInstance();
            if (outputJson.has("count")) {
                output1.setCount(outputJson.get("count").getAsInt());
            }
            int crushtime = jsonObject.get("crushtime").getAsInt();
            float minSpeed = jsonObject.has("min_speed")
                    ? jsonObject.get("min_speed").getAsFloat()
                    : 20.0F;
            float torque = jsonObject.has("torque")
                    ? jsonObject.get("torque").getAsFloat()
                    : 4.0F;

            return new primitive_grinder_multiblockrecipe(
                    resourceLocation,
                    ingredient,
                    output1,
                    crushtime,
                    minSpeed,
                    torque
            );
        }

        @Override
        public @Nullable primitive_grinder_multiblockrecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf friendlyByteBuf) {
            Ingredient ingredient = Ingredient.fromNetwork(friendlyByteBuf);
            ItemStack output = friendlyByteBuf.readItem();
            int crushtime = friendlyByteBuf.readInt();
            float minSpeed = friendlyByteBuf.readFloat();
            float torque = friendlyByteBuf.readFloat();
            return new primitive_grinder_multiblockrecipe(
                    resourceLocation,
                    ingredient,
                    output,
                    crushtime,
                    minSpeed,
                    torque
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf friendlyByteBuf, primitive_grinder_multiblockrecipe primitiveFurnaceMultiblockrecipe) {
            primitiveFurnaceMultiblockrecipe.input.toNetwork(friendlyByteBuf);
            friendlyByteBuf.writeItem(primitiveFurnaceMultiblockrecipe.output);
            friendlyByteBuf.writeInt(primitiveFurnaceMultiblockrecipe.crushtime);
            friendlyByteBuf.writeFloat(primitiveFurnaceMultiblockrecipe.minSpeed);
            friendlyByteBuf.writeFloat(primitiveFurnaceMultiblockrecipe.torque);
        }
    }
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(input);
        return ingredients;
    }
}
