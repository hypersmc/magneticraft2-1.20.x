package com.magneticraft2.compatibility.JEI;

import com.magneticraft2.common.magneticraft2;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

/**
 * Wood Tar fuel information is separate from ordinary multiblock item
 * recipes: a Kiln burns fuel into stored HEAT, not once per produced item.
 * Input fluid makes pressing JEI 'Uses' on Wood Tar show this machine.
 */
public final class KilnLiquidFuelJeiCategory
        implements IRecipeCategory<KilnLiquidFuelJeiCategory.FuelRecipe> {
    public static final ResourceLocation WOOD_TAR_ID =
            new ResourceLocation(magneticraft2.MOD_ID, "refractory_kiln_wood_tar_fuel");

    public static final RecipeType<FuelRecipe> TYPE = RecipeType.create(
            magneticraft2.MOD_ID, "kiln_liquid_fuel", FuelRecipe.class);

    public record FuelRecipe(ResourceLocation id, FluidStack fluid, int burnTicks) {}

    private final IDrawable background;
    private final IDrawable icon;

    public KilnLiquidFuelJeiCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(168, 74);
        icon = guiHelper.createDrawableItemStack(
                new ItemStack(BlockRegistry.REFRACTORY_KILN.get()));
    }

    @Override public RecipeType<FuelRecipe> getRecipeType() { return TYPE; }

    @Override public Component getTitle() {
        return Component.translatable("jei.magneticraft2.kiln_liquid_fuel");
    }

    @Override public IDrawable getBackground() { return background; }

    @Override public IDrawable getIcon() { return icon; }

    @Override public void setRecipe(
            IRecipeLayoutBuilder builder, FuelRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 22)
                .addFluidStack(recipe.fluid().getFluid(), recipe.fluid().getAmount())
                .setFluidRenderer(Math.max(1000, recipe.fluid().getAmount()),
                        true, 16, 16);

        builder.addSlot(RecipeIngredientRole.CATALYST, 140, 22)
                .addItemStack(new ItemStack(BlockRegistry.REFRACTORY_KILN.get()))
                .addTooltipCallback((slot, tooltip) -> tooltip.add(
                        Component.translatable("jei.magneticraft2.kiln_tar_layout_tooltip")));
    }

    @Override public void draw(
            FuelRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
            double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font,
                Component.translatable("jei.magneticraft2.kiln_fuel_amount",
                        recipe.fluid().getAmount()), 35, 22, 0x404040, false);
        graphics.drawString(font,
                Component.translatable("jei.magneticraft2.kiln_fuel_ticks",
                        recipe.burnTicks()), 35, 35, 0x404040, false);
        graphics.drawString(font,
                Component.translatable("jei.magneticraft2.kiln_tar_layout_short"),
                8, 56, 0x404040, false);
    }

    @Override public ResourceLocation getRegistryName(FuelRecipe recipe) {
        return recipe.id();
    }
}
