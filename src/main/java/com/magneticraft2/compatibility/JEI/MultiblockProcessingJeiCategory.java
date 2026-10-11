package com.magneticraft2.compatibility.JEI;

import com.magneticraft2.common.magneticraft2;
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

import java.util.List;
import java.util.Locale;

public class MultiblockProcessingJeiCategory
        implements IRecipeCategory<MultiblockJeiRecipe> {

    public static final RecipeType<MultiblockJeiRecipe> TYPE =
            RecipeType.create(
                    magneticraft2.MOD_ID,
                    "multiblock_processing",
                    MultiblockJeiRecipe.class
            );

    private final IDrawable background;
    private final IDrawable icon;

    public MultiblockProcessingJeiCategory(IGuiHelper guiHelper) {
        this.background =
                guiHelper.createBlankDrawable(168, 96);
        this.icon =
                guiHelper.createDrawableItemStack(
                        new ItemStack(
                                net.minecraft.world.item.Items.COPPER_INGOT
                        )
                );
    }

    @Override
    public RecipeType<MultiblockJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(
                "jei.magneticraft2.multiblock_processing"
        );
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            MultiblockJeiRecipe recipe,
            IFocusGroup focuses) {
        int inputY = 22;
        for (List<ItemStack> input :
                recipe.getItemInputs()) {
            builder.addSlot(
                            RecipeIngredientRole.INPUT,
                            8,
                            inputY
                    )
                    .addItemStacks(input);
            inputY += 20;
        }

        FluidStack fluidInput =
                recipe.getFluidInput();
        if (!fluidInput.isEmpty()) {
            builder.addSlot(
                            RecipeIngredientRole.INPUT,
                            34,
                            22
                    )
                    .addFluidStack(
                            fluidInput.getFluid(),
                            fluidInput.getAmount()
                    )
                    .setFluidRenderer(
                            Math.max(
                                    1000,
                                    fluidInput.getAmount()
                            ),
                            true,
                            16,
                            16
                    );
        }

        int outputY = 22;
        for (MultiblockJeiRecipe.Output output :
                recipe.getItemOutputs()) {
            var slot =
                    builder.addSlot(
                                    RecipeIngredientRole.OUTPUT,
                                    142,
                                    outputY
                            )
                            .addItemStack(
                                    output.getStack()
                            );

            if (output.getChance() < 1.0F) {
                slot.addTooltipCallback(
                        (recipeSlotView, tooltip) ->
                                tooltip.add(
                                        Component.translatable(
                                                "jei.magneticraft2.chance",
                                                Math.round(
                                                        output.getChance()
                                                                * 100.0F
                                                )
                                        )
                                )
                );
            }

            outputY += 20;
        }

        FluidStack fluidOutput =
                recipe.getFluidOutput();
        if (!fluidOutput.isEmpty()) {
            builder.addSlot(
                            RecipeIngredientRole.OUTPUT,
                            116,
                            22
                    )
                    .addFluidStack(
                            fluidOutput.getFluid(),
                            fluidOutput.getAmount()
                    )
                    .setFluidRenderer(
                            Math.max(
                                    1000,
                                    fluidOutput.getAmount()
                            ),
                            true,
                            16,
                            16
                    );
        }

        ItemStack machine =
                recipe.getMachineStack();
        if (!machine.isEmpty()) {
            builder.addSlot(
                            RecipeIngredientRole.CATALYST,
                            76,
                            6
                    )
                    .addItemStack(machine);
        }
    }

    @Override
    public void draw(
            MultiblockJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        var font = Minecraft.getInstance().font;

        guiGraphics.drawCenteredString(
                font,
                Component.translatable(
                        recipe.getMachine()
                                .toLanguageKey("block")
                ),
                84,
                7,
                0x404040
        );

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "jei.magneticraft2.time",
                        formatSeconds(
                                recipe.getProcessTime()
                        )
                ),
                56,
                34,
                0x404040,
                false
        );

        if (recipe.getMinSpeed() > 0.0F) {
            guiGraphics.drawString(
                    font,
                    Component.translatable(
                            "jei.magneticraft2.min_speed",
                            formatNumber(
                                    recipe.getMinSpeed()
                            )
                    ),
                    56,
                    48,
                    0x404040,
                    false
            );
        }

        if (recipe.getTorque() > 0.0F) {
            guiGraphics.drawString(
                    font,
                    Component.translatable(
                            "jei.magneticraft2.torque",
                            formatNumber(
                                    recipe.getTorque()
                            )
                    ),
                    56,
                    62,
                    0x404040,
                    false
            );
        }
    }

    @Override
    public ResourceLocation getRegistryName(
            MultiblockJeiRecipe recipe) {
        return recipe.getId();
    }

    private static String formatSeconds(
            int ticks) {
        return String.format(
                Locale.ROOT,
                "%.1f s",
                ticks / 20.0F
        );
    }

    private static String formatNumber(
            float value) {
        if (value == Math.round(value)) {
            return Integer.toString(
                    Math.round(value)
            );
        }

        return String.format(
                Locale.ROOT,
                "%.1f",
                value
        );
    }
}
