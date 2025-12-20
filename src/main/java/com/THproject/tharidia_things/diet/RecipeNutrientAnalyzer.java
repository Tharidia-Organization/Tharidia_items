package com.THproject.tharidia_things.diet;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Analyzes food items by recursively examining their crafting recipes.
 * Priority: crafting analysis > tags/name heuristics.
 * Only applies heuristics to base-level components without recipes.
 */
public class RecipeNutrientAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeNutrientAnalyzer.class);
    
    private static final Cache<ResourceLocation, AnalysisResult> ANALYSIS_CACHE =
            CacheBuilder.newBuilder()
                    .maximumSize(2048)
                    .expireAfterAccess(30, TimeUnit.MINUTES)
                    .build();
    
    private static volatile MinecraftServer currentServer = null;
    
    // Maximum depth to prevent infinite recursion (safety limit)
    private static final int MAX_SAFETY_DEPTH = 50;
    
    // Nutrient profiles for non-food components that contribute to recipes
    private static final Map<String, DietProfile> NON_FOOD_NUTRIENTS = new HashMap<>();
    
    static {
        // Initialize non-food component nutrients (NO WATER for solid ingredients)
        
        // Grains and flours
        NON_FOOD_NUTRIENTS.put("wheat", DietProfile.of(1.0f, 0, 0, 0, 0, 0));
        NON_FOOD_NUTRIENTS.put("flour", DietProfile.of(1.0f, 0, 0, 0, 0, 0));
        NON_FOOD_NUTRIENTS.put("dough", DietProfile.of(1.0f, 0, 0, 0, 0, 0));
        NON_FOOD_NUTRIENTS.put("rice", DietProfile.of(1.0f, 0, 0, 0, 0, 0));
        NON_FOOD_NUTRIENTS.put("corn", DietProfile.of(1.0f, 0, 0, 0, 0, 0));
        NON_FOOD_NUTRIENTS.put("oat", DietProfile.of(1.0f, 0, 0, 0, 0, 0));
        NON_FOOD_NUTRIENTS.put("barley", DietProfile.of(1.0f, 0, 0, 0, 0, 0));
        NON_FOOD_NUTRIENTS.put("rye", DietProfile.of(1.0f, 0, 0, 0, 0, 0));
        
        // Proteins
        NON_FOOD_NUTRIENTS.put("egg", DietProfile.of(0, 1.0f, 0, 0, 0, 0));
        NON_FOOD_NUTRIENTS.put("butter", DietProfile.of(0, 0.5f, 0, 0, 0, 0));
        NON_FOOD_NUTRIENTS.put("cheese", DietProfile.of(0, 0.8f, 0, 0, 0, 0));
        NON_FOOD_NUTRIENTS.put("cream", DietProfile.of(0, 0.3f, 0, 0, 0, 0));
        
        // Sugars
        NON_FOOD_NUTRIENTS.put("sugar", DietProfile.of(0, 0, 0, 0, 1.0f, 0));
        NON_FOOD_NUTRIENTS.put("honey", DietProfile.of(0, 0, 0, 0, 1.0f, 0));
        NON_FOOD_NUTRIENTS.put("syrup", DietProfile.of(0, 0, 0, 0, 1.0f, 0));
        NON_FOOD_NUTRIENTS.put("chocolate", DietProfile.of(0, 0, 0, 0, 0.8f, 0));
        NON_FOOD_NUTRIENTS.put("cocoa", DietProfile.of(0, 0, 0, 0, 0.5f, 0));
        
        // Liquids (these SHOULD have water)
        NON_FOOD_NUTRIENTS.put("milk", DietProfile.of(0, 0.2f, 0, 0, 0, 1.0f));
        NON_FOOD_NUTRIENTS.put("water", DietProfile.of(0, 0, 0, 0, 0, 1.0f));
        NON_FOOD_NUTRIENTS.put("broth", DietProfile.of(0, 0.1f, 0, 0, 0, 1.0f));
        NON_FOOD_NUTRIENTS.put("stock", DietProfile.of(0, 0.1f, 0, 0, 0, 1.0f));
    }
    
    public static void setServer(MinecraftServer server) {
        currentServer = server;
        if (server != null) {
            ANALYSIS_CACHE.invalidateAll();
        }
    }
    
    public static void clearCache() {
        ANALYSIS_CACHE.invalidateAll();
    }
    
    /**
     * Main entry point: analyzes an item and returns its nutrient profile.
     * Uses crafting-first approach with fallback to heuristics.
     */
    public static DietProfile analyze(Item item, DietSystemSettings settings) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        
        // Check cache first
        AnalysisResult cached = ANALYSIS_CACHE.getIfPresent(itemId);
        if (cached != null) {
            return cached.profile;
        }
        
        // Perform analysis
        AnalysisResult result = analyzeRecursive(item, new HashSet<>(), 0, settings);
        
        // Cache result
        ANALYSIS_CACHE.put(itemId, result);
        
        return result.profile;
    }
    
    /**
     * Recursively analyzes an item, prioritizing crafting recipes.
     * Only uses heuristics for base-level items without recipes.
     */
    private static AnalysisResult analyzeRecursive(Item item, Set<Item> visitedItems, int depth, DietSystemSettings settings) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        // Safety check: prevent infinite recursion
        if (depth > MAX_SAFETY_DEPTH) {
            LOGGER.warn("[DIET] Max recursion depth reached for {}", BuiltInRegistries.ITEM.getKey(item));
            return new AnalysisResult(DietProfile.EMPTY, AnalysisMethod.FAILED);
        }
        
        // Prevent circular dependencies
        if (visitedItems.contains(item)) {
            return new AnalysisResult(DietProfile.EMPTY, AnalysisMethod.CIRCULAR);
        }

        visitedItems.add(item);
        
        // Check if it's a food item
        ItemStack stack = new ItemStack(item);
        FoodProperties food = item.getFoodProperties(stack, null);
        boolean isFood = food != null;
        
        // Try to find crafting recipe first (PRIORITY #1)
        MinecraftServer server = currentServer;
        if (server != null) {
            RecipeManager recipeManager = server.getRecipeManager();
            DietProfile recipeProfile = analyzeFromRecipe(item, recipeManager, visitedItems, depth, settings);
            if (recipeProfile != null && !recipeProfile.isEmpty()) {
                LOGGER.debug("[DIET] {} analyzed from recipe (depth {})", itemId, depth);
                return new AnalysisResult(recipeProfile, AnalysisMethod.RECIPE);
            }
        }
        
        // Check if this is a known non-food component with nutrients
        DietProfile nonFoodProfile = getNonFoodNutrients(item);
        if (nonFoodProfile != null && !nonFoodProfile.isEmpty()) {
            LOGGER.debug("[DIET] {} analyzed as non-food component", itemId);
            return new AnalysisResult(nonFoodProfile, AnalysisMethod.NON_FOOD_COMPONENT);
        }
        
        // No recipe found - use heuristics for base components (FALLBACK)
        if (isFood) {
            DietProfile heuristicProfile = analyzeFromHeuristics(item, food, settings);
            LOGGER.debug("[DIET] {} analyzed from heuristics (base component)", itemId);
            return new AnalysisResult(heuristicProfile, AnalysisMethod.HEURISTIC);
        }
        
        // Non-food item without recipe - return empty (no nutrients)
        return new AnalysisResult(DietProfile.EMPTY, AnalysisMethod.NOT_FOOD);
    }
    
    /**
     * Analyzes an item by examining its crafting recipe and recursively analyzing ingredients.
     * Returns null if no suitable recipe is found.
     * Priority: recipe with highest total nutrient values (excluding water to avoid bias).
     */
    private static DietProfile analyzeFromRecipe(Item item, RecipeManager recipeManager, Set<Item> visitedItems, int depth, DietSystemSettings settings) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        // Find all recipes that produce this item
        List<RecipeHolder<?>> matchingRecipes = findRecipesForItem(recipeManager, item);
        
        if (matchingRecipes.isEmpty()) {
            return null;
        }
        
        // Analyze all recipes and select the one with highest nutrient values
        DietProfile bestProfile = null;
        float bestTotalValue = 0.0f;
        
        for (RecipeHolder<?> recipeHolder : matchingRecipes) {
            Recipe<?> recipe = recipeHolder.value();
            
            // Extract ingredients
            List<Ingredient> ingredients = safeGetIngredients(recipe);
            if (ingredients.isEmpty()) {
                continue;
            }
            
            // Recursively analyze each ingredient
            List<ComponentNutrients> componentNutrients = new ArrayList<>();
            
            for (Ingredient ingredient : ingredients) {
                ItemStack[] possibleItems = ingredient.getItems();
                if (possibleItems.length == 0) continue;
                
                // Use first item as representative (most recipes have single-item ingredients)
                ItemStack ingredientStack = possibleItems[0];
                Item ingredientItem = ingredientStack.getItem();
                int count = ingredientStack.getCount();
                
                // Skip if this ingredient is the same as what we're analyzing (circular reference)
                if (visitedItems.contains(ingredientItem)) {
                    continue;
                }
                
                // Recursively analyze this ingredient
                AnalysisResult ingredientResult = analyzeRecursive(ingredientItem, visitedItems, depth + 1, settings);
                
                if (!ingredientResult.profile.isEmpty()) {
                    componentNutrients.add(new ComponentNutrients(ingredientResult.profile, count));
                }
            }
            
            if (componentNutrients.isEmpty()) {
                continue;
            }
            
            // Calculate result count from recipe
            ItemStack result = safeGetRecipeResult(recipe);
            int resultCount = result.isEmpty() ? 1 : result.getCount();
            
            // Special case: For smelting/cooking recipes, don't divide by result count
            // because vanilla recipes often have incorrect count values
            String recipeType = recipe.getClass().getSimpleName().toLowerCase();
            if (recipeType.contains("smelt") || recipeType.contains("cook") || 
                recipeType.contains("campfire") || recipe instanceof SmeltingRecipe) {
                resultCount = 1;
            }
            
            // IMPORTANT: If recipe produces multiple food items, keep values per-item
            // Don't divide further - the combination already accounts for this
            // Example: 3 wheat → 3 bread means each bread gets (3 grain / 3) = 1 grain
            
            // Combine nutrients from all components
            DietProfile profile = combineComponentNutrients(componentNutrients, resultCount);
            
            // Apply rounding: values < 0.1 become 0.0
            profile = roundProfile(profile);
            
            // Calculate total nutrient value for this recipe (EXCLUDING water to avoid bias)
            // Water can come from heuristics and shouldn't influence recipe selection
            float totalValue = 0.0f;
            for (DietCategory category : DietCategory.VALUES) {
                if (category != DietCategory.WATER) {
                    totalValue += profile.get(category);
                }
            }
            
            // Keep the recipe with highest total nutrient value
            if (totalValue > bestTotalValue) {
                bestTotalValue = totalValue;
                bestProfile = profile;
            }
        }
        
        return bestProfile;
    }
    
    /**
     * Combines nutrients from multiple components, accounting for quantities.
     * Formula: sum all component nutrients, then divide by result count.
     * Nutrients are summed exactly without any processing loss.
     */
    private static DietProfile combineComponentNutrients(List<ComponentNutrients> components, int resultCount) {
        float totalGrain = 0, totalProtein = 0, totalVegetable = 0, totalFruit = 0, totalSugar = 0, totalWater = 0;
        
        for (ComponentNutrients component : components) {
            DietProfile profile = component.profile;
            int count = component.count;
            
            totalGrain += profile.get(DietCategory.GRAIN) * count;
            totalProtein += profile.get(DietCategory.PROTEIN) * count;
            totalVegetable += profile.get(DietCategory.VEGETABLE) * count;
            totalFruit += profile.get(DietCategory.FRUIT) * count;
            totalSugar += profile.get(DietCategory.SUGAR) * count;
            totalWater += profile.get(DietCategory.WATER) * count;
        }
        
        // Divide by result count to get per-item nutrients
        float divisor = Math.max(1, resultCount);
        
        return DietProfile.of(
                totalGrain / divisor,
                totalProtein / divisor,
                totalVegetable / divisor,
                totalFruit / divisor,
                totalSugar / divisor,
                totalWater / divisor
        );
    }
    
    /**
     * Gets nutrient profile for known non-food components.
     * Returns null if not a known component.
     */
    private static DietProfile getNonFoodNutrients(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        String path = itemId.getPath().toLowerCase();
        
        // Check exact matches first
        for (Map.Entry<String, DietProfile> entry : NON_FOOD_NUTRIENTS.entrySet()) {
            if (path.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Analyzes an item using heuristics (tags, name patterns).
     * Only used for base-level components without recipes.
     * Public for client-side cache usage.
     */
    public static DietProfile analyzeFromHeuristics(Item item, FoodProperties food, DietSystemSettings settings) {
        float nutrition = food.nutrition();
        float saturation = food.saturation() * settings.saturationScale();
        boolean fast = food.saturation() <= settings.fastFoodSaturationThreshold();
        boolean always = food.canAlwaysEat();
        
        // Determine primary category using existing heuristics
        boolean meatLike = DietHeuristics.isMeatLike(item);
        boolean vegetableHint = DietHeuristics.hasVegetableHints(item);
        boolean fruitHint = DietHeuristics.hasFruitHints(item);
        boolean grainHint = DietHeuristics.hasGrainHints(item);
        boolean drinkHint = DietHeuristics.hasDrinkEffect(item);
        
        float primaryGrain = 0, primaryProtein = 0, primaryVegetable = 0, primaryFruit = 0, primarySugar = 0, primaryWater = 0;
        
        // Priority: Meat > Fruit > Vegetable > Grain > Drink > Sugar
        if (meatLike) {
            primaryProtein = settings.proteinMeatMultiplier() * nutrition;
        } else if (fruitHint) {
            primaryFruit = (nutrition + saturation) * settings.fruitHintMultiplier();
            // Fruits naturally contain sugars
            primarySugar = nutrition * 0.3f;
        } else if (vegetableHint) {
            primaryVegetable = nutrition * settings.vegetableHintMultiplier();
        } else if (grainHint) {
            primaryGrain = (nutrition * settings.grainNutritionMultiplier())
                    + (fast ? settings.fastFoodGrainBonus() : 0.0f);
        } else if (drinkHint) {
            primaryWater = (always ? settings.waterAlwaysEatBonus() : settings.waterDefaultBonus())
                    + settings.drinkWaterBonus();
        } else {
            // Unclassified: distribute across categories
            primaryGrain = nutrition * 0.3f;
            primaryProtein = nutrition * 0.2f;
            primaryVegetable = nutrition * 0.2f;
            primaryFruit = nutrition * 0.1f;
            primarySugar = fast ? settings.fastSugarFlatBonus() : nutrition * 0.1f;
            primaryWater = always ? settings.waterAlwaysEatBonus() : 1.0f;
        }
        
        // Add sugar for fast foods (but NOT for pure proteins like meat)
        if (fast && !grainHint && !meatLike) {
            primarySugar += settings.fastSugarFlatBonus() + saturation * settings.fastSugarSaturationMultiplier();
        }
        
        DietProfile baseProfile = DietProfile.of(
                Math.max(0.0f, primaryGrain),
                Math.max(0.0f, primaryProtein),
                Math.max(0.0f, primaryVegetable),
                Math.max(0.0f, primaryFruit),
                Math.max(0.0f, primarySugar),
                Math.max(0.0f, primaryWater)
        );
        
        // Apply mod heuristics
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        return DietHeuristics.applyModHeuristics(itemId, baseProfile);
    }
    
    /**
     * Finds all recipes that produce the given item.
     */
    private static List<RecipeHolder<?>> findRecipesForItem(RecipeManager recipeManager, Item targetItem) {
        List<RecipeHolder<?>> matchingRecipes = new ArrayList<>();
        
        try {
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                try {
                    Recipe<?> recipe = holder.value();
                    ItemStack result = safeGetRecipeResult(recipe);
                    if (!result.isEmpty() && result.getItem() == targetItem) {
                        matchingRecipes.add(holder);
                    }
                } catch (Exception e) {
                    // Skip problematic recipes
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[DIET] Error finding recipes for {}: {}", targetItem, e.getMessage());
        }
        
        return matchingRecipes;
    }
    
    /**
     * Safely extracts recipe result.
     */
    private static ItemStack safeGetRecipeResult(Recipe<?> recipe) {
        try {
            ItemStack result = recipe.getResultItem(null);
            if (!result.isEmpty()) {
                return result;
            }
            
            if (currentServer != null) {
                result = recipe.getResultItem(currentServer.registryAccess());
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Safely extracts ingredients from recipe.
     */
    private static List<Ingredient> safeGetIngredients(Recipe<?> recipe) {
        try {
            List<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients != null && !ingredients.isEmpty()) {
                return ingredients;
            }
        } catch (Exception e) {
            LOGGER.debug("[DIET] Could not extract ingredients from {}: {}", recipe.getClass().getName(), e.getMessage());
        }
        return List.of();
    }
    
    /**
     * Rounds a profile: values < 0.1 become 0.0 to avoid noise from complex recipes.
     */
    private static DietProfile roundProfile(DietProfile profile) {
        float grain = profile.get(DietCategory.GRAIN);
        float protein = profile.get(DietCategory.PROTEIN);
        float vegetable = profile.get(DietCategory.VEGETABLE);
        float fruit = profile.get(DietCategory.FRUIT);
        float sugar = profile.get(DietCategory.SUGAR);
        float water = profile.get(DietCategory.WATER);
        
        return DietProfile.of(
            grain < 0.1f ? 0.0f : grain,
            protein < 0.1f ? 0.0f : protein,
            vegetable < 0.1f ? 0.0f : vegetable,
            fruit < 0.1f ? 0.0f : fruit,
            sugar < 0.1f ? 0.0f : sugar,
            water < 0.1f ? 0.0f : water
        );
    }
    
    /**
     * Represents nutrients from a component with its quantity.
     */
    private static record ComponentNutrients(DietProfile profile, int count) {}
    
    /**
     * Result of nutrient analysis with method used.
     */
    private static record AnalysisResult(DietProfile profile, AnalysisMethod method) {}
    
    /**
     * Method used to determine nutrients.
     */
    private enum AnalysisMethod {
        RECIPE,              // Analyzed from crafting recipe
        HEURISTIC,           // Analyzed from tags/name patterns
        NON_FOOD_COMPONENT,  // Non-food component with known nutrients
        CIRCULAR,            // Circular dependency detected
        NOT_FOOD,            // Not a food item
        FAILED               // Analysis failed
    }
    
}
