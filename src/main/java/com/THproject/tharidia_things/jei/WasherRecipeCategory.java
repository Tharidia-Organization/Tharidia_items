package com.THproject.tharidia_things.jei;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.recipe.WasherRecipe;
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

public class WasherRecipeCategory implements IRecipeCategory<WasherRecipe> {
    public static final RecipeType<WasherRecipe> TYPE = RecipeType.create(TharidiaThings.MODID, "washing",
            WasherRecipe.class);
    private final IDrawable background;
    private final IDrawableAnimated arrow;
    private final IDrawableStatic slot;
    private final IDrawable icon;

    public WasherRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(67, 26);
        this.arrow = helper.createAnimatedRecipeArrow(100);
        this.slot = helper.getSlotDrawable();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(TharidiaThings.SIEVE_BLOCK.get()));

    }

    @Override
    public RecipeType<WasherRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.tharidiathings.washer_recipe");
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
    public void draw(WasherRecipe recipe, IRecipeSlotsView recipeSlotView, GuiGraphics guiGraphics, double mouseX,
            double mouseY) {
        arrow.draw(guiGraphics, 22, 5);
        slot.draw(guiGraphics, -1, 4);
        slot.draw(guiGraphics, 49, 4);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WasherRecipe recipe, IFocusGroup focuses) {
        // Input Item
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 5)
                .addIngredients(recipe.getInput());

        // Output Item
        builder.addSlot(RecipeIngredientRole.OUTPUT, 50, 5)
                .addItemStacks(recipe.getOutputs().stream().map(WasherRecipe.WeightedOutput::item).toList())
                .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                    recipeSlotView.getDisplayedIngredient(VanillaTypes.ITEM_STACK).ifPresent(stack -> {
                        for (WasherRecipe.WeightedOutput output : recipe.getOutputs()) {
                            if (ItemStack.isSameItemSameComponents(stack, output.item())) {
                                tooltip.add(Component.literal(Math.round(output.chance() * 100) + "% Chance"));
                            }
                        }
                    });
                });
    }
}
