package com.magneticraft2.common.recipe.multiblock;

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

/**
 * Separate recipe format: concentrate AND flux are real inputs, while bloom
 * and guaranteed slag are independent outputs. Old multiblock recipes remain
 * wire/datapack compatible.
 */
public final class BloomeryRecipe implements Recipe<Container> {
    public static final int ORE_SLOT = 0, BLOOM_SLOT = 1, SLAG_SLOT = 2, FLUX_SLOT = 3;
    private final ResourceLocation id;
    private final Ingredient ore, flux;
    private final int oreCount, fluxCount, time;
    private final ItemStack bloom, slag;

    public BloomeryRecipe(ResourceLocation id, Ingredient ore, int oreCount,
            Ingredient flux, int fluxCount, ItemStack bloom, ItemStack slag, int time) {
        if (ore.isEmpty() || flux.isEmpty() || oreCount < 1 || oreCount > 64
                || fluxCount < 1 || fluxCount > 64 || bloom.isEmpty()
                || slag.isEmpty() || time < 1 || time > 72000)
            throw new IllegalArgumentException("Invalid Bloomery recipe " + id);
        this.id=id; this.ore=ore; this.oreCount=oreCount;
        this.flux=flux; this.fluxCount=fluxCount;
        this.bloom=bloom.copy(); this.slag=slag.copy(); this.time=time;
    }
    public int processTime() { return time; }
    public int oreCount() { return oreCount; }
    public int fluxCount() { return fluxCount; }
    public ItemStack bloom() { return bloom.copy(); }
    public ItemStack slag() { return slag.copy(); }
    public boolean accepts(int slot, ItemStack stack) {
        return !stack.isEmpty() && ((slot == ORE_SLOT && ore.test(stack))
                || (slot == FLUX_SLOT && flux.test(stack)));
    }
    public boolean matchesSlots(IItemHandler inv) {
        return inv != null && inv.getSlots() >= 4
                && ore.test(inv.getStackInSlot(ORE_SLOT))
                && inv.getStackInSlot(ORE_SLOT).getCount() >= oreCount
                && flux.test(inv.getStackInSlot(FLUX_SLOT))
                && inv.getStackInSlot(FLUX_SLOT).getCount() >= fluxCount;
    }
    @Override public boolean matches(Container inventory, Level level) {
        return inventory.getContainerSize() >= 4
                && ore.test(inventory.getItem(ORE_SLOT))
                && inventory.getItem(ORE_SLOT).getCount() >= oreCount
                && flux.test(inventory.getItem(FLUX_SLOT))
                && inventory.getItem(FLUX_SLOT).getCount() >= fluxCount;
    }
    @Override public ItemStack assemble(Container inventory, RegistryAccess registries) { return bloom(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return w*h >= 4; }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return bloom(); }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list=NonNullList.create(); list.add(ore);list.add(flux);return list;
    }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }

    public static final class Type implements RecipeType<BloomeryRecipe> {
        public static final Type INSTANCE=new Type();
        private Type() {}
    }
    public static final class Serializer implements RecipeSerializer<BloomeryRecipe> {
        public static final Serializer INSTANCE=new Serializer();
        private Serializer() {}
        private static ItemStack readItem(ResourceLocation id, JsonObject obj, String field) {
            if (obj==null) throw new JsonSyntaxException(id+" missing "+field);
            ResourceLocation itemId=new ResourceLocation(GsonHelper.getAsString(obj, "item"));
            var item=ForgeRegistries.ITEMS.getValue(itemId);
            if (item==null || item==Items.AIR)
                throw new JsonSyntaxException(id+" unknown "+field+" item "+itemId);
            int count=GsonHelper.getAsInt(obj,"count",1);
            if (count<1 || count>64) throw new JsonSyntaxException(id+" invalid "+field+" count");
            return new ItemStack(item,count);
        }
        private static Ingredient readIngredient(ResourceLocation id, JsonObject obj, String field) {
            if (obj==null) throw new JsonSyntaxException(id+" missing "+field);
            Ingredient ing=Ingredient.fromJson(obj);
            if (ing.isEmpty()) throw new JsonSyntaxException(id+" empty "+field);
            return ing;
        }
        @Override public BloomeryRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ore=readIngredient(id,json.getAsJsonObject("input"),"input");
            Ingredient flux=readIngredient(id,json.getAsJsonObject("flux"),"flux");
            int oreCount=GsonHelper.getAsInt(json,"input_count",1);
            int fluxCount=GsonHelper.getAsInt(json,"flux_count",1);
            return new BloomeryRecipe(id,ore,oreCount,flux,fluxCount,
                    readItem(id,json.getAsJsonObject("output"),"output"),
                    readItem(id,json.getAsJsonObject("slag"),"slag"),
                    GsonHelper.getAsInt(json,"process_time",800));
        }
        @Override public @Nullable BloomeryRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new BloomeryRecipe(id,Ingredient.fromNetwork(buf),buf.readVarInt(),
                    Ingredient.fromNetwork(buf),buf.readVarInt(),buf.readItem(),buf.readItem(),
                    buf.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf buf,BloomeryRecipe recipe) {
            recipe.ore.toNetwork(buf);buf.writeVarInt(recipe.oreCount);
            recipe.flux.toNetwork(buf);buf.writeVarInt(recipe.fluxCount);
            buf.writeItem(recipe.bloom);buf.writeItem(recipe.slag);buf.writeVarInt(recipe.time);
        }
    }
}
