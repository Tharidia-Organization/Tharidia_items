package com.THproject.tharidia_things.spice;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.Set;

/**
 * Dynamic shapeless recipe: any edible food + 1 or more spice items = spiced food.
 * Similar in pattern to vanilla's firework rocket recipe.
 */
public class SpicedFoodRecipe extends CustomRecipe {

    public SpicedFoodRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack foodStack = ItemStack.EMPTY;
        Set<SpiceType> spicesInGrid = EnumSet.noneOf(SpiceType.class);

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            // Check if it's a spice item
            SpiceType spiceType = getSpiceType(stack);
            if (spiceType != null) {
                // Duplicate spice type in grid -> no match
                if (!spicesInGrid.add(spiceType)) {
                    return false;
                }
                continue;
            }

            // Check if it's food
            if (stack.getItem().getFoodProperties(stack, null) != null) {
                // More than one food item -> no match
                if (!foodStack.isEmpty()) {
                    return false;
                }
                foodStack = stack;
                continue;
            }

            // Neither food nor spice -> no match
            return false;
        }

        // Must have exactly 1 food and at least 1 spice
        if (foodStack.isEmpty() || spicesInGrid.isEmpty()) {
            return false;
        }

        // Check that the food doesn't already have all the spices being added
        SpiceData existing = foodStack.get(SpiceDataComponents.SPICE_DATA.get());
        if (existing != null) {
            for (SpiceType spice : spicesInGrid) {
                if (!existing.hasSpice(spice)) {
                    return true; // At least one new spice
                }
            }
            return false; // All spices already present
        }

        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack foodStack = ItemStack.EMPTY;
        Set<SpiceType> spicesInGrid = EnumSet.noneOf(SpiceType.class);

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            SpiceType spiceType = getSpiceType(stack);
            if (spiceType != null) {
                spicesInGrid.add(spiceType);
                continue;
            }

            if (stack.getItem().getFoodProperties(stack, null) != null) {
                foodStack = stack;
            }
        }

        if (foodStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = foodStack.copy();
        result.setCount(1);

        // Get existing spice data or start empty
        SpiceData existing = result.get(SpiceDataComponents.SPICE_DATA.get());
        SpiceData newData = existing != null ? existing : SpiceData.EMPTY;

        // Merge in new spices
        newData = newData.merge(new SpiceData(spicesInGrid));
        result.set(SpiceDataComponents.SPICE_DATA.get(), newData);

        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TharidiaThings.SPICED_FOOD_RECIPE_SERIALIZER.get();
    }

    /**
     * Returns the SpiceType if the item is a registered spice item, null otherwise.
     */
    private static SpiceType getSpiceType(ItemStack stack) {
        if (stack.is(TharidiaThings.COCA.get())) {
            return SpiceType.COCA;
        }
        if (stack.is(TharidiaThings.SPIRU.get())) {
            return SpiceType.SPIRU;
        }
        return null;
    }
}
