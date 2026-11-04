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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Deliberately separate multi-input recipe type: the old
 * multiblock_processing serializer and all its save/network callers remain
 * byte-for-byte compatible. Four physically stored item slots, one result.
 */
public final class MechanicalAssemblyRecipe implements Recipe<Container> {
    public static final int INPUT_SLOTS = 4;
    public record Part(Ingredient ingredient, int count) {}
    private final ResourceLocation id;
    private final List<Part> parts;
    private final ItemStack result;
    private final int processTime;
    private final float minSpeed, torque;

    public MechanicalAssemblyRecipe(ResourceLocation id, List<Part> parts,
            ItemStack result, int processTime, float minSpeed, float torque) {
        if (parts == null || parts.size() != INPUT_SLOTS || result.isEmpty()
                || processTime < 1 || processTime > 72000
                || !Float.isFinite(minSpeed) || minSpeed <= 0
                || !Float.isFinite(torque) || torque <= 0)
            throw new IllegalArgumentException("Invalid multi-input assembly " + id);
        for (Part part : parts)
            if (part.ingredient().isEmpty() || part.count() <= 0 || part.count() > 64)
                throw new IllegalArgumentException("Invalid ingredient in " + id);
        this.id = id;
        this.parts = List.copyOf(parts);
        this.result = result.copy();
        this.processTime = processTime;
        this.minSpeed = minSpeed;
        this.torque = torque;
    }

    public Part part(int slot) { return parts.get(slot); }
    public int processTime() { return processTime; }
    public float minSpeed() { return minSpeed; }
    public float torque() { return torque; }
    public ItemStack output() { return result.copy(); }

    public boolean accepts(int slot, ItemStack item) {
        return slot >= 0 && slot < INPUT_SLOTS && !item.isEmpty()
                && parts.get(slot).ingredient().test(item);
    }
    public boolean matchesSlots(net.minecraftforge.items.IItemHandler inventory) {
        if (inventory == null || inventory.getSlots() < INPUT_SLOTS + 1) return false;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getCount() < parts.get(i).count()
                    || !parts.get(i).ingredient().test(stack)) return false;
        }
        return true;
    }
    @Override public boolean matches(Container c, Level world) {
        if (c.getContainerSize() < INPUT_SLOTS) return false;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack item = c.getItem(i);
            if (item.getCount() < parts.get(i).count()
                    || !parts.get(i).ingredient().test(item)) return false;
        }
        return true;
    }
    @Override public ItemStack assemble(Container c, RegistryAccess registry) { return output(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return w * h >= INPUT_SLOTS; }
    @Override public ItemStack getResultItem(RegistryAccess registry) { return output(); }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (Part part : parts) list.add(part.ingredient());
        return list;
    }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }

    public static final class Type implements RecipeType<MechanicalAssemblyRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }
    public static final class Serializer implements RecipeSerializer<MechanicalAssemblyRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private Serializer() {}

        @Override public MechanicalAssemblyRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonArray raw = GsonHelper.getAsJsonArray(json, "ingredients");
            if (raw.size() != INPUT_SLOTS)
                throw new JsonSyntaxException(id + ": exactly four assembly ingredients required");
            List<Part> parts = new ArrayList<>(INPUT_SLOTS);
            for (int i = 0; i < INPUT_SLOTS; i++) {
                JsonObject entry = raw.get(i).getAsJsonObject();
                if (!entry.has("ingredient"))
                    throw new JsonSyntaxException(id + ": missing ingredient in slot " + i);
                Ingredient item = Ingredient.fromJson(entry.get("ingredient"));
                int count = GsonHelper.getAsInt(entry, "count", 1);
                if (count <= 0 || count > 64 || item.isEmpty())
                    throw new JsonSyntaxException(id + ": invalid ingredient/count in slot " + i);
                parts.add(new Part(item, count));
            }
            JsonObject output = GsonHelper.getAsJsonObject(json, "result");
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(output, "item"));
            var item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null || item == net.minecraft.world.item.Items.AIR)
                throw new JsonSyntaxException(id + ": unknown result " + itemId);
            int count = GsonHelper.getAsInt(output, "count", 1);
            if (count <= 0 || count > 64)
                throw new JsonSyntaxException(id + ": invalid result count");
            JsonObject power = GsonHelper.getAsJsonObject(json, "mechanical");
            return new MechanicalAssemblyRecipe(id, parts, new ItemStack(item, count),
                    GsonHelper.getAsInt(json, "process_time", 200),
                    GsonHelper.getAsFloat(power, "min_speed"),
                    GsonHelper.getAsFloat(power, "torque"));
        }

        @Override public @Nullable MechanicalAssemblyRecipe fromNetwork(
                ResourceLocation id, FriendlyByteBuf buf) {
            List<Part> parts = new ArrayList<>(INPUT_SLOTS);
            for (int i = 0; i < INPUT_SLOTS; i++)
                parts.add(new Part(Ingredient.fromNetwork(buf), buf.readVarInt()));
            return new MechanicalAssemblyRecipe(id, parts, buf.readItem(),
                    buf.readVarInt(), buf.readFloat(), buf.readFloat());
        }
        @Override public void toNetwork(FriendlyByteBuf buf, MechanicalAssemblyRecipe recipe) {
            for (Part part : recipe.parts) {
                part.ingredient().toNetwork(buf);
                buf.writeVarInt(part.count());
            }
            buf.writeItem(recipe.result);
            buf.writeVarInt(recipe.processTime);
            buf.writeFloat(recipe.minSpeed);
            buf.writeFloat(recipe.torque);
        }
    }
}
