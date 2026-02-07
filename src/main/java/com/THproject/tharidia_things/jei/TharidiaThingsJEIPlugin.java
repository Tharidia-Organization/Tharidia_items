package com.THproject.tharidia_things.jei;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.recipe.WasherRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

@JeiPlugin
public class TharidiaThingsJEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new WasherRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        assert Minecraft.getInstance().level != null;
        List<WasherRecipe> recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(TharidiaThings.WASHER_RECIPE_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();

        registration.addRecipes(WasherRecipeCategory.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(TharidiaThings.SIEVE_BLOCK.get()), WasherRecipeCategory.TYPE);
    }
}
