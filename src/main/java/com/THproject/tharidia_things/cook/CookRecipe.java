package com.THproject.tharidia_things.cook;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A food-producing recipe discovered at runtime from the game's RecipeManager.
 * Not loaded from datapack — the recipe itself comes from vanilla/mod datapacks.
 * Only the cook time can be overridden via tharidiathings datapacks.
 */
public record CookRecipe(
        ResourceLocation recipeId,  // ID of the underlying MC recipe
        ItemStack result,           // result item stack (for display + completion check)
        int timeTicks               // cook timer duration (default or overridden)
) {}
