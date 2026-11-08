package com.magneticraft2.common.recipe.multiblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The three first-Steel furnaces share a validated, JSON-driven process
 * contract with up to three REAL independent inputs and two reserved outputs.
 * No implicit conversion of coal into coke/pig iron/steel is possible.
 */
public final class SteelProcessRecipe implements Recipe<Container> {
    public static final int[] INPUT_SLOTS = {0, 3, 4};
    public static final int PRODUCT_SLOT = 1, SLAG_SLOT = 2;

    public record IngredientCount(Ingredient ingredient, int count) {
        public IngredientCount {
            if (ingredient == null || ingredient.isEmpty() || count < 1 || count > 64)
                throw new IllegalArgumentException("Invalid steel input ingredient");
        }
    }

    private final ResourceLocation id, machine;
    private final List<IngredientCount> inputs;
    private final ItemStack output, slag;
    private final int processTime;
    private final boolean requiresAir;

    public SteelProcessRecipe(ResourceLocation id, ResourceLocation machine,
            List<IngredientCount> inputs, ItemStack output, ItemStack slag,
            int processTime, boolean requiresAir) {
        if (id == null || machine == null || inputs == null
                || inputs.isEmpty() || inputs.size() > INPUT_SLOTS.length
                || output == null || output.isEmpty() || output.getCount() > 64
                || slag == null || (!slag.isEmpty() && slag.getCount() > 64)
                || processTime < 1 || processTime > 72000)
            throw new IllegalArgumentException("Invalid Steel process recipe " + id);
        String name = machine.toString();
        // A datapack cannot delete AIR or slag from a reduction/refining
        // process, or secretly move a recipe to a different furnace.
        if (!(name.equals("magneticraft2:steel_coke_oven")
                || name.equals("magneticraft2:steel_blast_furnace")
                || name.equals("magneticraft2:steel_refining_furnace")))
            throw new IllegalArgumentException("Unknown Steel machine " + machine);
        boolean cokeOven = name.equals("magneticraft2:steel_coke_oven");
        boolean blast = name.equals("magneticraft2:steel_blast_furnace");
        if (requiresAir == cokeOven || (cokeOven && (!slag.isEmpty() || inputs.size() != 1))
                || (blast && (slag.isEmpty() || inputs.size() != 3))
                || (!cokeOven && !blast && (slag.isEmpty() || inputs.size() != 1)))
            throw new IllegalArgumentException("Wrong required ports/byproducts/AIR for " + id);
        this.id = id;
        this.machine = machine;
        this.inputs = List.copyOf(inputs);
        this.output = output.copy();
        this.slag = slag.copy();
        this.processTime = processTime;
        this.requiresAir = requiresAir;
    }

    public ResourceLocation machine() { return machine; }
    public int processTime() { return processTime; }
    public int inputCount() { return inputs.size(); }
    public IngredientCount input(int index) { return inputs.get(index); }
    public ItemStack output() { return output.copy(); }
    public ItemStack slag() { return slag.copy(); }
    public boolean requiresAir() { return requiresAir; }

    public boolean accepts(int actualSlot, ItemStack item) {
        if (item.isEmpty()) return false;
        for (int i = 0; i < inputs.size(); i++)
            if (INPUT_SLOTS[i] == actualSlot && inputs.get(i).ingredient().test(item))
                return true;
        return false;
    }

    public boolean matchesSlots(IItemHandler inventory) {
        if (inventory == null || inventory.getSlots() <= INPUT_SLOTS[INPUT_SLOTS.length - 1])
            return false;
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack stack = inventory.getStackInSlot(INPUT_SLOTS[i]);
            IngredientCount source = inputs.get(i);
            if (!source.ingredient().test(stack) || stack.getCount() < source.count())
                return false;
        }
        return true;
    }

    @Override public boolean matches(Container inventory, Level level) {
        if (inventory.getContainerSize() < 5) return false;
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack stack = inventory.getItem(INPUT_SLOTS[i]);
            IngredientCount source = inputs.get(i);
            if (!source.ingredient().test(stack) || stack.getCount() < source.count())
                return false;
        }
        return true;
    }

    @Override public ItemStack assemble(Container container, RegistryAccess access) {
        return output();
    }
    @Override public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 5;
    }
    @Override public ItemStack getResultItem(RegistryAccess access) { return output(); }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (IngredientCount input : inputs) list.add(input.ingredient());
        return list;
    }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }

    public static final class Type implements RecipeType<SteelProcessRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }

    public static final class Serializer implements RecipeSerializer<SteelProcessRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private Serializer() {}

        private static ItemStack readItem(ResourceLocation id, @Nullable JsonObject object, String key) {
            if (object == null) return ItemStack.EMPTY;
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(object, "item"));
            var item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null || item == Items.AIR)
                throw new JsonSyntaxException(id + " unknown " + key + " " + itemId);
            int count = GsonHelper.getAsInt(object, "count", 1);
            if (count < 1 || count > 64) throw new JsonSyntaxException(id + " invalid " + key + " count");
            return new ItemStack(item, count);
        }

        @Override public SteelProcessRecipe fromJson(ResourceLocation id, JsonObject json) {
            ResourceLocation machine = new ResourceLocation(GsonHelper.getAsString(json, "machine"));
            JsonArray raw = GsonHelper.getAsJsonArray(json, "inputs");
            if (raw.size() < 1 || raw.size() > INPUT_SLOTS.length)
                throw new JsonSyntaxException(id + " expects 1-3 material inputs");
            List<IngredientCount> inputs = new ArrayList<>(raw.size());
            for (var value : raw) {
                JsonObject entry = value.getAsJsonObject();
                Ingredient ingredient = Ingredient.fromJson(entry);
                inputs.add(new IngredientCount(ingredient, GsonHelper.getAsInt(entry, "count", 1)));
            }
            return new SteelProcessRecipe(id, machine, inputs,
                    readItem(id, GsonHelper.getAsJsonObject(json, "output"), "output"),
                    json.has("slag") ? readItem(id, GsonHelper.getAsJsonObject(json, "slag"), "slag")
                                     : ItemStack.EMPTY,
                    GsonHelper.getAsInt(json, "process_time", 600),
                    GsonHelper.getAsBoolean(json, "requires_air", false));
        }

        @Override public @Nullable SteelProcessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ResourceLocation machine = buf.readResourceLocation();
            int count = buf.readVarInt();
            if (count < 1 || count > INPUT_SLOTS.length)
                throw new IllegalArgumentException("Invalid steel process input length " + count);
            List<IngredientCount> inputs = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
                inputs.add(new IngredientCount(Ingredient.fromNetwork(buf), buf.readVarInt()));
            return new SteelProcessRecipe(id, machine, inputs, buf.readItem(), buf.readItem(),
                    buf.readVarInt(), buf.readBoolean());
        }

        @Override public void toNetwork(FriendlyByteBuf buf, SteelProcessRecipe recipe) {
            buf.writeResourceLocation(recipe.machine);
            buf.writeVarInt(recipe.inputs.size());
            for (IngredientCount input : recipe.inputs) {
                input.ingredient().toNetwork(buf);
                buf.writeVarInt(input.count());
            }
            buf.writeItem(recipe.output);
            buf.writeItem(recipe.slag);
            buf.writeVarInt(recipe.processTime);
            buf.writeBoolean(recipe.requiresAir);
        }
    }
}
