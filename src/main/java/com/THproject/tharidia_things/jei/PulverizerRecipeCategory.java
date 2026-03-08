package com.THproject.tharidia_things.jei;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.recipe.PulverizerRecipe;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class PulverizerRecipeCategory implements IRecipeCategory<PulverizerRecipe> {
    public static final RecipeType<PulverizerRecipe> TYPE = RecipeType.create(TharidiaThings.MODID, "pulverizer",
            PulverizerRecipe.class);

    private final IDrawable background;
    private final IDrawableAnimated arrow;
    private final IDrawableStatic slot;
    private final IDrawable icon;

    public PulverizerRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(67, 26);
        this.arrow = helper.createAnimatedRecipeArrow(100);
        this.slot = helper.getSlotDrawable();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(TharidiaThings.PULVERIZER_BLOCK.get()));
    }

    @Override
    public RecipeType<PulverizerRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.tharidiathings.pulverizer_recipe");
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
    public void draw(PulverizerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX,
            double mouseY) {
        arrow.draw(guiGraphics, 22, 5);
        slot.draw(guiGraphics, -1, 4);
        slot.draw(guiGraphics, 49, 4);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PulverizerRecipe recipe, IFocusGroup focuses) {
        // Input item
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 5)
                .addIngredients(recipe.getInput());

        // Output item
        builder.addSlot(RecipeIngredientRole.OUTPUT, 50, 5)
                .addItemStacks(recipe.getOutputs().stream().map(PulverizerRecipe.WeightedOutput::item).toList())
                .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                    recipeSlotView.getDisplayedIngredient(VanillaTypes.ITEM_STACK).ifPresent(stack -> {
                        for (PulverizerRecipe.WeightedOutput output : recipe.getOutputs()) {
                            if (ItemStack.isSameItemSameComponents(stack, output.item())) {
                                tooltip.add(Component.literal(Math.round(output.chance() * 100) + "% Chance"));
                            }
                        }
                    });
                });
    }
}
