package com.THproject.tharidia_things.jei;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.recipe.WasherRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class WasherRecipeCategory implements IRecipeCategory<WasherRecipe> {
    public static final RecipeType<WasherRecipe> TYPE = RecipeType.create(TharidiaThings.MODID, "washer",
            WasherRecipe.class);
    private final IDrawable background;
    protected final IDrawableAnimated arrow;
    private final IDrawable icon;

    public WasherRecipeCategory(IGuiHelper helper) {
        // Using JEI's built-in texture for a simple furnace-like layout
        // coordinates 0, 168, 82, 26 corresponds to a 2 slot layout with arrow in
        // standard vanilla gui texture often used by JEI
        this.background = helper.createDrawable(
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/demo_background.png"), 0, 168, 82, 26);

        this.arrow = helper.createAnimatedRecipeArrow(100);

        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(TharidiaThings.WASHER_BLOCK.get()));

    }

    @Override
    public RecipeType<WasherRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.tharidiathings.washer");
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
        arrow.draw(guiGraphics, 24, 5);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WasherRecipe recipe, IFocusGroup focuses) {
        // Input Item
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 5)
                .addIngredients(recipe.getInput());

        // Output Item
        builder.addSlot(RecipeIngredientRole.OUTPUT, 50, 5)
                .addItemStack(recipe.getResultItem(null));
    }
}
