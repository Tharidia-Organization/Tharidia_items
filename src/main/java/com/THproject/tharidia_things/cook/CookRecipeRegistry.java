package com.THproject.tharidia_things.cook;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.RecipeInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Discovers all food-producing recipes from the game's RecipeManager.
 * The list is rebuilt lazily after each datapack reload (dirty flag).
 * Cook times can be overridden per output item via tharidiathings datapacks.
 */
public final class CookRecipeRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CookRecipeRegistry.class);
    public static final int DEFAULT_COOK_TIME = 400;

    // Time overrides: output item id → ticks. Set by CookRecipeLoader.
    private static Map<ResourceLocation, Integer> timeOverrides = new HashMap<>();

    // Cache rebuilt after each reload
    private static volatile boolean dirty = true;
    private static List<CookRecipe> cache = new ArrayList<>();
    // Maps recipeId → holder for ingredient lookup during startCooking
    private static Map<ResourceLocation, RecipeHolder<?>> holderCache = new HashMap<>();

    private CookRecipeRegistry() {}

    /** Called by CookRecipeLoader after each reload. Marks cache as stale. */
    public static void setTimeOverrides(Map<ResourceLocation, Integer> overrides) {
        timeOverrides = overrides;
        dirty = true;
    }

    /** Returns all discovered food recipes, rebuilding cache if stale. */
    public static List<CookRecipe> discover(RecipeManager rm, RegistryAccess registryAccess) {
        if (dirty) {
            rebuildCache(rm, registryAccess);
            dirty = false;
        }
        return cache;
    }

    /** Returns the underlying RecipeHolder for a given recipe ID (for ingredient access). */
    public static Optional<RecipeHolder<?>> getHolder(ResourceLocation recipeId) {
        return Optional.ofNullable(holderCache.get(recipeId));
    }

    /** Returns the cook time for the given output item ID (default if no override). */
    public static int getTimeForItem(ResourceLocation itemId) {
        return timeOverrides.getOrDefault(itemId, DEFAULT_COOK_TIME);
    }

    /** Forces a cache rebuild on next discover() call (e.g. after server reload). */
    public static void markDirty() {
        dirty = true;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private static void rebuildCache(RecipeManager rm, RegistryAccess registryAccess) {
        List<CookRecipe> discovered = new ArrayList<>();
        Map<ResourceLocation, RecipeHolder<?>> holders = new LinkedHashMap<>();

        addRecipesOfType(rm, RecipeType.CRAFTING,         registryAccess, discovered, holders);
        addRecipesOfType(rm, RecipeType.SMELTING,         registryAccess, discovered, holders);
        addRecipesOfType(rm, RecipeType.SMOKING,          registryAccess, discovered, holders);
        addRecipesOfType(rm, RecipeType.CAMPFIRE_COOKING, registryAccess, discovered, holders);

        cache = discovered;
        holderCache = holders;
        LOGGER.info("[CookRecipeRegistry] Discovered {} food recipe(s)", discovered.size());
    }

    private static <I extends RecipeInput, T extends Recipe<I>> void addRecipesOfType(
            RecipeManager rm,
            RecipeType<T> type,
            RegistryAccess registryAccess,
            List<CookRecipe> out,
            Map<ResourceLocation, RecipeHolder<?>> holders) {

        for (RecipeHolder<T> holder : rm.getAllRecipesFor(type)) {
            try {
                ItemStack result = holder.value().getResultItem(registryAccess);
                if (result.isEmpty()) continue;
                // Only food items
                if (!result.has(DataComponents.FOOD)) continue;

                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());
                int time = timeOverrides.getOrDefault(itemId, DEFAULT_COOK_TIME);

                out.add(new CookRecipe(holder.id(), result.copy(), time));
                holders.put(holder.id(), holder);
            } catch (Exception ex) {
                LOGGER.debug("[CookRecipeRegistry] Skipped recipe {}: {}", holder.id(), ex.getMessage());
            }
        }
    }
}
