package com.THproject.tharidia_things.diet;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Central registry for diet profiles and configuration, backed by datapacks.
 */
public final class DietRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(DietRegistry.class);

    private static final List<TagKey<Item>> MEAT_TAGS = List.of(
            ItemTags.MEAT,
            ItemTags.FISHES,
            tag("c", "meat"),
            tag("c", "cooked_meat"),
            tag("c", "raw_meat"),
            tag("c", "foods/meat"),
            tag("forge", "raw_meat"),
            tag("forge", "cooked_meat"),
            tag("forge", "meat"),
            tag("minecraft", "meat")
    );
    private static final List<TagKey<Item>> VEGETABLE_TAGS = List.of(
            tag("c", "vegetables"),
            tag("c", "foods/vegetable"),
            tag("forge", "vegetables"),
            tag("forge", "vegetable"),
            tag("minecraft", "vegetables")
    );
    private static final List<TagKey<Item>> FRUIT_TAGS = List.of(
            tag("c", "fruits"),
            tag("c", "foods/fruit"),
            tag("forge", "fruits"),
            tag("forge", "fruit"),
            tag("minecraft", "fruits")
    );
    private static final List<TagKey<Item>> GRAIN_TAGS = List.of(
            tag("c", "grains"),
            tag("c", "bread"),
            tag("c", "foods/grain"),
            tag("c", "crops"),
            tag("forge", "bread"),
            tag("forge", "grain"),
            tag("forge", "grains"),
            tag("forge", "crops/wheat"),
            tag("forge", "crops/rice"),
            tag("minecraft", "grains")
    );
    private static final List<TagKey<Item>> DRINK_TAGS = List.of(
            tag("c", "drinks"),
            tag("c", "foods/drink"),
            tag("c", "soups"),
            tag("c", "stews"),
            tag("c", "beverages"),
            tag("forge", "drinks"),
            tag("forge", "soups"),
            tag("forge", "beverages"),
            tag("minecraft", "drinks")
    );

    private static final String[] MEAT_KEYWORDS = {
            "meat", "beef", "pork", "chicken", "mutton", "steak",
            "bacon", "sausage", "ham", "turkey", "duck", "fish", "salmon", "cod",
            "lamb", "veal", "venison", "rabbit", "goat", "quail", "pheasant",
            "anchovy", "tuna", "trout", "halibut", "sardine", "mackerel",
            "shrimp", "crab", "lobster", "scallop", "clam", "oyster", "mussel",
            "jerky", "pepperoni", "salami", "prosciutto", "chorizo", "carnitas",
            "ribs", "chop", "cutlet", "fillet", "drumstick", "wing", "thigh"
    };
    private static final String[] VEGETABLE_KEYWORDS = {
            "carrot", "potato", "tomato", "lettuce", "cabbage", "onion",
            "garlic", "pepper", "mushroom", "salad", "veg", "bean",
            "broccoli", "cauliflower", "celery", "cucumber", "zucchini", "squash",
            "pumpkin", "eggplant", "asparagus", "spinach", "kale", "chard",
            "radish", "turnip", "beet", "pea", "lentil", "chickpea",
            "okra", "artichoke", "leek", "shallot", "ginger", "horseradish",
            "brussels", "sprout", "endive", "arugula", "bok", "choy", "daikon"
    };
    private static final String[] FRUIT_KEYWORDS = {
            "berry", "berries", "apple", "melon", "orange", "banana", "grape",
            "peach", "pear", "fruit", "cherry", "citrus", "coco",
            "strawberry", "blueberry", "raspberry", "blackberry", "cranberry",
            "sweet_berries", "glow_berries", "chorus_fruit",
            "watermelon", "cantaloupe", "honeydew", "kiwi", "pineapple", "mango",
            "papaya", "apricot", "plum", "nectarine", "fig", "date", "pomegranate",
            "lemon", "lime", "grapefruit", "tangerine", "clementine", "coconut",
            "avocado", "olive", "persimmon", "lychee", "dragonfruit", "passion"
    };
    private static final String[] GRAIN_KEYWORDS = {
            "bread", "wheat", "grain", "flour", "pasta", "rice",
            "noodle", "dough", "cereal", "oat", "bun", "cake", "cookie",
            "barley", "rye", "corn", "millet", "quinoa", "amaranth", "spelt",
            "bagel", "muffin", "croissant", "biscuit", "roll", "tortilla", "pita",
            "macaroni", "spaghetti", "linguine", "fettuccine", "penne", "rotini",
            "pancake", "waffle", "crepe", "biscotti", "pretzel", "cracker", "crisp",
            "bran", "germ", "grits", "polenta", "couscous", "bulgur", "farro"
    };
    private static final String[] DRINK_KEYWORDS = {
            "soup", "stew", "tea", "drink", "juice", "smoothie",
            "broth", "coffee", "milkshake", "latte", "cappuccino", "espresso",
            "cocoa", "chocolate", "hot", "cider", "punch", "lemonade",
            "soda", "pop", "cola", "ginger", "ale", "beer", "wine",
            "water", "mineral", "sparkling", "tonic", "syrup", "nectar",
            "gazpacho", "bisque", "chowder", "gumbo", "curry", "ramen", "pho"
    };

    private static volatile DietPackConfig config = DietPackConfig.DEFAULT;

    // Fast cache per item stack (identity). Auto-clears when stack GC'ed.
    private static final Cache<ResourceLocation, DietProfile> PROFILE_CACHE =
            CacheBuilder.newBuilder()
                    .maximumSize(256)
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build();
    
    // Recipe analysis cache - aggressive caching to avoid performance impact
    private static final Cache<ResourceLocation, DietProfile> RECIPE_ANALYSIS_CACHE =
            CacheBuilder.newBuilder()
                    .maximumSize(1024)
                    .expireAfterAccess(30, TimeUnit.MINUTES)
                    .build();
    
    // Mod ID heuristics map for better food categorization
    private static final Map<String, ModHeuristics> MOD_HEURISTICS = new HashMap<>();
    
    // Server reference for recipe access
    private static volatile MinecraftServer currentServer = null;
    
    // Max recursion depth for recipe analysis to prevent performance issues
    private static final int MAX_RECIPE_DEPTH = 2;
    
    static {
        // Initialize mod ID heuristics based on popular food mods
        MOD_HEURISTICS.put("farmersdelight", new ModHeuristics(1.2f, 1.3f, 1.4f, 1.3f, 1.1f, 1.0f));
        MOD_HEURISTICS.put("croptopia", new ModHeuristics(1.1f, 1.1f, 1.5f, 1.4f, 1.2f, 1.0f));
        MOD_HEURISTICS.put("pamhc2foodcore", new ModHeuristics(1.1f, 1.1f, 1.3f, 1.3f, 1.2f, 1.0f));
        MOD_HEURISTICS.put("cuisine", new ModHeuristics(1.3f, 1.2f, 1.3f, 1.2f, 1.1f, 1.0f));
        MOD_HEURISTICS.put("brew", new ModHeuristics(1.0f, 1.0f, 1.0f, 1.0f, 1.2f, 1.3f));
        MOD_HEURISTICS.put("quark", new ModHeuristics(1.1f, 1.0f, 1.0f, 1.0f, 1.1f, 1.0f));
        MOD_HEURISTICS.put("botania", new ModHeuristics(1.0f, 1.0f, 1.2f, 1.3f, 1.1f, 1.1f));
    }

    private DietRegistry() {}

    public static void setServer(MinecraftServer server) {
        currentServer = server;
        if (server != null) {
            RECIPE_ANALYSIS_CACHE.invalidateAll();
        }
    }

    public static void loadConfig(DietPackConfig newConfig) {
        config = newConfig == null ? DietPackConfig.DEFAULT : newConfig;
        PROFILE_CACHE.invalidateAll();
        RECIPE_ANALYSIS_CACHE.invalidateAll();
        LOGGER.info("[DIET] Diet config loaded with {} explicit item entries", config.items().size());
        LOGGER.info("[DIET] Current settings - decay_interval: {}s, saturation_scale: {}", 
            config.settings().decayIntervalSeconds(), config.settings().saturationScale());
        LOGGER.info("[DIET] Decay rates - grain: {}, protein: {}, vegetable: {}, fruit: {}, sugar: {}, water: {}",
            config.decayRates().get(DietCategory.GRAIN), config.decayRates().get(DietCategory.PROTEIN), 
            config.decayRates().get(DietCategory.VEGETABLE), config.decayRates().get(DietCategory.FRUIT),
            config.decayRates().get(DietCategory.SUGAR), config.decayRates().get(DietCategory.WATER));
    }

    public static void reset() {
        config = DietPackConfig.DEFAULT;
        PROFILE_CACHE.invalidateAll();
    }

    public static DietProfile getProfile(ItemStack stack) {
        if (stack.isEmpty()) {
            return DietProfile.EMPTY;
        }
        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);

        DietProfile cached = PROFILE_CACHE.getIfPresent(id);
        if (cached != null) {
            return cached;
        }

        DietProfile profile = config.items().get(id);
        if (profile == null) {
            profile = deriveProfile(item);
        }
        PROFILE_CACHE.put(id, profile);
        return profile;
    }

    public static DietProfile getDecayRates() {
        return config.decayRates();
    }

    public static DietProfile getMaxValues() {
        return config.maxValues();
    }

    public static DietSystemSettings getSettings() {
        return config.settings();
    }

    private static DietProfile deriveProfile(Item item) {
        ItemStack sampleStack = item.getDefaultInstance();
        if (sampleStack.isEmpty()) {
            sampleStack = new ItemStack(item);
        }
        FoodProperties food = item.getFoodProperties(sampleStack, null);
        if (food == null) {
            return DietProfile.EMPTY;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        DietSystemSettings settings = config.settings();
        float nutrition = food.nutrition();
        float saturation = food.saturation() * settings.saturationScale();
        boolean meatLike = isMeatLike(item);
        boolean vegetableHint = hasVegetableHints(item);
        boolean fruitHint = hasFruitHints(item);
        boolean grainHint = hasGrainHints(item);
        boolean drinkHint = hasDrinkEffect(item);
        boolean fast = food.saturation() <= settings.fastFoodSaturationThreshold(); // Fast food heuristic: low saturation
        boolean always = food.canAlwaysEat();

        // Variables are now calculated in primary category section below

        // Determine primary category and allocate nutrition accordingly
        // Priority: Meat > Fruit > Vegetable > Grain > Drink > Sugar
        // Fruit and Vegetable MUST be checked before Grain to avoid conflicts
        float primaryGrain = 0, primaryProtein = 0, primaryVegetable = 0, primaryFruit = 0, primarySugar = 0, primaryWater = 0;
        
        if (meatLike) {
            primaryProtein = settings.proteinMeatMultiplier() * nutrition;
        } else if (fruitHint) {
            primaryFruit = (nutrition + saturation) * settings.fruitHintMultiplier();
        } else if (vegetableHint) {
            // Use nutrition instead of saturation for vegetables to give proper value
            primaryVegetable = nutrition * settings.vegetableHintMultiplier();
        } else if (grainHint) {
            primaryGrain = (nutrition * settings.grainNutritionMultiplier())
                    + (fast ? settings.fastFoodGrainBonus() : 0.0f);
        } else if (drinkHint) {
            primaryWater = (always ? settings.waterAlwaysEatBonus() : settings.waterDefaultBonus())
                    + settings.drinkWaterBonus();
        } else {
            // Default: small amounts across all categories for unclassified foods
            primaryGrain = nutrition * 0.3f;
            primaryProtein = nutrition * 0.2f;
            primaryVegetable = nutrition * 0.2f;
            primaryFruit = nutrition * 0.1f;
            primarySugar = fast ? settings.fastSugarFlatBonus() : nutrition * 0.1f;
            primaryWater = always ? settings.waterAlwaysEatBonus() : 1.0f;
        }
        
        // Add sugar for fast foods regardless of category
        if (fast && !grainHint) {
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
        
        // Try to enhance with recipe analysis
        DietProfile enhancedProfile = analyzeRecipeIngredients(itemId, baseProfile);
        
        // Apply mod ID heuristics for better categorization
        return applyModHeuristics(itemId, enhancedProfile);
    }

    private static boolean hasDrinkEffect(Item item) {
        if (matchesAnyTag(item, DRINK_TAGS)) {
            return true;
        }
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        return containsKeyword(path, DRINK_KEYWORDS);
    }

    private static boolean hasVegetableHints(Item item) {
        // Tags have priority - more reliable for modded items
        if (matchesAnyTag(item, VEGETABLE_TAGS)) {
            return true;
        }
        // Check both path and full ID for better modded compatibility
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        String fullId = id.toString();
        return containsKeyword(path, VEGETABLE_KEYWORDS) || containsKeyword(fullId, VEGETABLE_KEYWORDS);
    }

    private static boolean hasFruitHints(Item item) {
        // Tags have priority - more reliable for modded items
        if (matchesAnyTag(item, FRUIT_TAGS)) {
            return true;
        }
        // Check both path and full ID for better modded compatibility
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        String fullId = id.toString();
        return containsKeyword(path, FRUIT_KEYWORDS) || containsKeyword(fullId, FRUIT_KEYWORDS);
    }

    private static boolean hasGrainHints(Item item) {
        // Tags have priority - more reliable for modded items
        if (matchesAnyTag(item, GRAIN_TAGS)) {
            return true;
        }
        // Check both path and full ID for better modded compatibility
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        String fullId = id.toString();
        return containsKeyword(path, GRAIN_KEYWORDS) || containsKeyword(fullId, GRAIN_KEYWORDS);
    }

    private static boolean isMeatLike(Item item) {
        // Tags have priority - more reliable for modded items
        if (matchesAnyTag(item, MEAT_TAGS)) {
            return true;
        }
        // Check both path and full ID for better modded compatibility
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        String fullId = id.toString();
        return containsKeyword(path, MEAT_KEYWORDS) || containsKeyword(fullId, MEAT_KEYWORDS);
    }

    private static boolean matchesAnyTag(Item item, List<TagKey<Item>> tags) {
        var holder = item.builtInRegistryHolder();
        for (TagKey<Item> tag : tags) {
            if (holder.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsKeyword(String value, String[] keywords) {
        String lower = value.toLowerCase();
        for (String keyword : keywords) {
            // Use word boundary matching to avoid false positives
            // e.g., "corn" in "acorn" or "unicorn" should not match
            if (containsAsWord(lower, keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a keyword exists as a whole word or part of compound words.
     * Prevents false positives like "corn" matching in "acorn" or "unicorn".
     */
    private static boolean containsAsWord(String text, String keyword) {
        int index = text.indexOf(keyword);
        if (index == -1) {
            return false;
        }
        
        // Check all occurrences
        while (index >= 0) {
            boolean validStart = index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1));
            boolean validEnd = (index + keyword.length() >= text.length()) || 
                              !Character.isLetterOrDigit(text.charAt(index + keyword.length()));
            
            // Allow underscore as word separator (common in mod IDs)
            if (index > 0 && text.charAt(index - 1) == '_') {
                validStart = true;
            }
            if (index + keyword.length() < text.length() && text.charAt(index + keyword.length()) == '_') {
                validEnd = true;
            }
            
            if (validStart && validEnd) {
                return true;
            }
            
            // Check next occurrence
            index = text.indexOf(keyword, index + 1);
        }
        
        return false;
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
    
    private static DietProfile applyModHeuristics(ResourceLocation itemId, DietProfile profile) {
        String modId = itemId.getNamespace();
        
        // Skip vanilla - no heuristics needed
        if ("minecraft".equals(modId)) {
            return profile;
        }
        
        // Check for exact mod ID match
        ModHeuristics heuristics = MOD_HEURISTICS.get(modId);
        if (heuristics != null) {
            LOGGER.debug("[DIET] Applying {} mod heuristics to {}", modId, itemId);
            return applyHeuristics(profile, heuristics);
        }
        
        // Check for partial matches in mod ID (for mod variants)
        for (Map.Entry<String, ModHeuristics> entry : MOD_HEURISTICS.entrySet()) {
            if (modId.contains(entry.getKey())) {
                LOGGER.debug("[DIET] Applying {} mod heuristics (partial match) to {}", entry.getKey(), itemId);
                return applyHeuristics(profile, entry.getValue());
            }
        }
        
        // Special cases based on patterns - common mod naming conventions
        if (modId.contains("farm") || modId.contains("harvest") || modId.contains("grow") || modId.contains("agricraft")) {
            LOGGER.debug("[DIET] Applying farm heuristics to {}", itemId);
            ModHeuristics farmHeuristics = new ModHeuristics(1.2f, 1.1f, 1.3f, 1.2f, 1.1f, 1.0f);
            return applyHeuristics(profile, farmHeuristics);
        }
        
        if (modId.contains("food") || modId.contains("cook") || modId.contains("kitchen") || 
            modId.contains("culinary") || modId.contains("cuisine")) {
            LOGGER.debug("[DIET] Applying food heuristics to {}", itemId);
            ModHeuristics foodHeuristics = new ModHeuristics(1.1f, 1.1f, 1.2f, 1.2f, 1.1f, 1.0f);
            return applyHeuristics(profile, foodHeuristics);
        }
        
        return profile;
    }
    
    private static DietProfile applyHeuristics(DietProfile profile, ModHeuristics heuristics) {
        return DietProfile.of(
                profile.get(DietCategory.GRAIN) * heuristics.grainMultiplier,
                profile.get(DietCategory.PROTEIN) * heuristics.proteinMultiplier,
                profile.get(DietCategory.VEGETABLE) * heuristics.vegetableMultiplier,
                profile.get(DietCategory.FRUIT) * heuristics.fruitMultiplier,
                profile.get(DietCategory.SUGAR) * heuristics.sugarMultiplier,
                profile.get(DietCategory.WATER) * heuristics.waterMultiplier
        );
    }
    
    private static record ModHeuristics(
        float grainMultiplier,
        float proteinMultiplier,
        float vegetableMultiplier,
        float fruitMultiplier,
        float sugarMultiplier,
        float waterMultiplier
    ) {}
    
    /**
     * Analyzes recipes to infer nutrient composition from ingredients.
     * Uses aggressive caching to avoid performance impact.
     */
    private static DietProfile analyzeRecipeIngredients(ResourceLocation itemId, DietProfile baseProfile) {
        // Check cache first
        DietProfile cached = RECIPE_ANALYSIS_CACHE.getIfPresent(itemId);
        if (cached != null) {
            return cached;
        }
        
        // If no server or recipe manager available, return base profile
        MinecraftServer server = currentServer;
        if (server == null) {
            RECIPE_ANALYSIS_CACHE.put(itemId, baseProfile);
            return baseProfile;
        }
        
        RecipeManager recipeManager = server.getRecipeManager();
        if (recipeManager == null) {
            RECIPE_ANALYSIS_CACHE.put(itemId, baseProfile);
            return baseProfile;
        }
        
        // Find recipes that produce this item
        Item targetItem = BuiltInRegistries.ITEM.get(itemId);
        if (targetItem == null) {
            RECIPE_ANALYSIS_CACHE.put(itemId, baseProfile);
            return baseProfile;
        }
        
        DietProfile recipeProfile = analyzeRecipesForItem(recipeManager, targetItem, new HashSet<>(), 0);
        
        // If we found a recipe profile, blend it with base profile (70% recipe, 30% base)
        DietProfile finalProfile;
        if (recipeProfile != null && !recipeProfile.isEmpty()) {
            finalProfile = blendProfiles(recipeProfile, baseProfile, 0.7f);
        } else {
            finalProfile = baseProfile;
        }
        
        RECIPE_ANALYSIS_CACHE.put(itemId, finalProfile);
        return finalProfile;
    }
    
    /**
     * Recursively analyzes recipes for an item, tracking visited items to prevent infinite loops.
     * Works with vanilla and modded recipe types (Farmer's Delight, Create, etc.).
     */
    private static DietProfile analyzeRecipesForItem(RecipeManager recipeManager, Item targetItem, Set<Item> visited, int depth) {
        // Prevent infinite recursion and excessive depth
        if (depth >= MAX_RECIPE_DEPTH || visited.contains(targetItem)) {
            return null;
        }
        visited.add(targetItem);
        
        // Find all recipes that produce this item - works with any recipe type
        List<RecipeHolder<?>> matchingRecipes = new ArrayList<>();
        try {
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                try {
                    Recipe<?> recipe = holder.value();
                    // Safe result extraction - handles modded recipes
                    ItemStack result = safeGetRecipeResult(recipe);
                    if (!result.isEmpty() && result.getItem() == targetItem) {
                        matchingRecipes.add(holder);
                    }
                } catch (Exception e) {
                    // Skip problematic recipes from mods
                    continue;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[DIET] Error iterating recipes for {}: {}", targetItem, e.getMessage());
            return null;
        }
        
        if (matchingRecipes.isEmpty()) {
            return null;
        }
        
        // Prioritize crafting recipes, then use first available
        RecipeHolder<?> recipeHolder = selectBestRecipe(matchingRecipes);
        Recipe<?> recipe = recipeHolder.value();
        
        // Extract ingredients - safe for modded recipes
        List<Ingredient> ingredients = safeGetIngredients(recipe);
        if (ingredients.isEmpty()) {
            return null;
        }
        
        // Sum up nutrient profiles from all ingredients
        float totalGrain = 0, totalProtein = 0, totalVegetable = 0, totalFruit = 0, totalSugar = 0, totalWater = 0;
        int ingredientCount = 0;
        
        for (Ingredient ingredient : ingredients) {
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length == 0) continue;
            
            // Use first item in ingredient as representative
            ItemStack stack = stacks[0];
            Item ingredientItem = stack.getItem();
            
            // Get profile for this ingredient (may recurse)
            DietProfile ingredientProfile = getIngredientProfile(recipeManager, ingredientItem, visited, depth + 1);
            if (ingredientProfile != null && !ingredientProfile.isEmpty()) {
                totalGrain += ingredientProfile.get(DietCategory.GRAIN);
                totalProtein += ingredientProfile.get(DietCategory.PROTEIN);
                totalVegetable += ingredientProfile.get(DietCategory.VEGETABLE);
                totalFruit += ingredientProfile.get(DietCategory.FRUIT);
                totalSugar += ingredientProfile.get(DietCategory.SUGAR);
                totalWater += ingredientProfile.get(DietCategory.WATER);
                ingredientCount++;
            }
        }
        
        if (ingredientCount == 0) {
            return null;
        }
        
        // Average the nutrients (recipes combine ingredients)
        float scale = 0.8f; // Slight reduction to account for processing
        return DietProfile.of(
                totalGrain * scale,
                totalProtein * scale,
                totalVegetable * scale,
                totalFruit * scale,
                totalSugar * scale,
                totalWater * scale
        );
    }
    
    /**
     * Gets the diet profile for an ingredient, using cache or deriving it.
     */
    private static DietProfile getIngredientProfile(RecipeManager recipeManager, Item item, Set<Item> visited, int depth) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        
        // Check if we have an explicit config entry
        DietProfile explicit = config.items().get(itemId);
        if (explicit != null) {
            return explicit;
        }
        
        // Check cache
        DietProfile cached = PROFILE_CACHE.getIfPresent(itemId);
        if (cached != null) {
            return cached;
        }
        
        // If it's a food item, derive its profile normally
        ItemStack stack = new ItemStack(item);
        FoodProperties food = item.getFoodProperties(stack, null);
        if (food != null) {
            // Don't recurse for food items - use direct derivation
            return deriveProfileDirect(item);
        }
        
        // For non-food ingredients, try to analyze their recipe
        return analyzeRecipesForItem(recipeManager, item, visited, depth);
    }
    
    /**
     * Direct profile derivation without recipe analysis (to avoid infinite recursion).
     */
    private static DietProfile deriveProfileDirect(Item item) {
        ItemStack sampleStack = item.getDefaultInstance();
        if (sampleStack.isEmpty()) {
            sampleStack = new ItemStack(item);
        }
        FoodProperties food = item.getFoodProperties(sampleStack, null);
        if (food == null) {
            return DietProfile.EMPTY;
        }
        
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        DietSystemSettings settings = config.settings();
        float nutrition = food.nutrition();
        float saturation = food.saturation() * settings.saturationScale();
        boolean meatLike = isMeatLike(item);
        boolean vegetableHint = hasVegetableHints(item);
        boolean fruitHint = hasFruitHints(item);
        boolean grainHint = hasGrainHints(item);
        boolean drinkHint = hasDrinkEffect(item);
        boolean fast = food.saturation() <= settings.fastFoodSaturationThreshold();
        boolean always = food.canAlwaysEat();
        
        // Priority: Meat > Fruit > Vegetable > Grain > Drink > Sugar
        float primaryGrain = 0, primaryProtein = 0, primaryVegetable = 0, primaryFruit = 0, primarySugar = 0, primaryWater = 0;
        
        if (meatLike) {
            primaryProtein = settings.proteinMeatMultiplier() * nutrition;
        } else if (fruitHint) {
            primaryFruit = (nutrition + saturation) * settings.fruitHintMultiplier();
        } else if (vegetableHint) {
            primaryVegetable = nutrition * settings.vegetableHintMultiplier();
        } else if (grainHint) {
            primaryGrain = (nutrition * settings.grainNutritionMultiplier())
                    + (fast ? settings.fastFoodGrainBonus() : 0.0f);
        } else if (drinkHint) {
            primaryWater = (always ? settings.waterAlwaysEatBonus() : settings.waterDefaultBonus())
                    + settings.drinkWaterBonus();
        } else {
            primaryGrain = nutrition * 0.3f;
            primaryProtein = nutrition * 0.2f;
            primaryVegetable = nutrition * 0.2f;
            primaryFruit = nutrition * 0.1f;
            primarySugar = fast ? settings.fastSugarFlatBonus() : nutrition * 0.1f;
            primaryWater = always ? settings.waterAlwaysEatBonus() : 1.0f;
        }
        
        if (fast && !grainHint) {
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
        
        return applyModHeuristics(itemId, baseProfile);
    }
    
    /**
     * Blends two profiles with a given weight (0.0 = all profile2, 1.0 = all profile1).
     */
    private static DietProfile blendProfiles(DietProfile profile1, DietProfile profile2, float weight) {
        float invWeight = 1.0f - weight;
        return DietProfile.of(
                profile1.get(DietCategory.GRAIN) * weight + profile2.get(DietCategory.GRAIN) * invWeight,
                profile1.get(DietCategory.PROTEIN) * weight + profile2.get(DietCategory.PROTEIN) * invWeight,
                profile1.get(DietCategory.VEGETABLE) * weight + profile2.get(DietCategory.VEGETABLE) * invWeight,
                profile1.get(DietCategory.FRUIT) * weight + profile2.get(DietCategory.FRUIT) * invWeight,
                profile1.get(DietCategory.SUGAR) * weight + profile2.get(DietCategory.SUGAR) * invWeight,
                profile1.get(DietCategory.WATER) * weight + profile2.get(DietCategory.WATER) * invWeight
        );
    }
    
    /**
     * Safely extracts recipe result, handling modded recipe types.
     */
    private static ItemStack safeGetRecipeResult(Recipe<?> recipe) {
        try {
            // Try standard method first
            ItemStack result = recipe.getResultItem(null);
            if (!result.isEmpty()) {
                return result;
            }
            
            // Some modded recipes may need a registry access
            if (currentServer != null) {
                result = recipe.getResultItem(currentServer.registryAccess());
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (Exception e) {
            // Modded recipe may have different result access pattern
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Safely extracts ingredients from recipe, handling modded recipe types.
     */
    private static List<Ingredient> safeGetIngredients(Recipe<?> recipe) {
        try {
            List<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients != null && !ingredients.isEmpty()) {
                return ingredients;
            }
        } catch (Exception e) {
            LOGGER.debug("[DIET] Could not extract ingredients from recipe {}: {}", recipe.getClass().getName(), e.getMessage());
        }
        return List.of();
    }
    
    /**
     * Selects the best recipe from a list, prioritizing crafting recipes over others.
     * This ensures we get the most representative recipe for nutrient analysis.
     */
    private static RecipeHolder<?> selectBestRecipe(List<RecipeHolder<?>> recipes) {
        if (recipes.isEmpty()) {
            return null;
        }
        
        // Priority order: CraftingRecipe > SmeltingRecipe > Others
        RecipeHolder<?> craftingRecipe = null;
        RecipeHolder<?> smeltingRecipe = null;
        RecipeHolder<?> firstRecipe = recipes.get(0);
        
        for (RecipeHolder<?> holder : recipes) {
            Recipe<?> recipe = holder.value();
            String recipeType = recipe.getClass().getSimpleName().toLowerCase();
            
            // Prioritize crafting recipes (including modded crafting variants)
            if (recipeType.contains("craft") || recipe instanceof CraftingRecipe) {
                craftingRecipe = holder;
                break; // Crafting is highest priority
            }
            
            // Second priority: smelting/cooking recipes
            if (smeltingRecipe == null && (recipeType.contains("smelt") || 
                recipeType.contains("cook") || recipeType.contains("campfire") ||
                recipe instanceof SmeltingRecipe)) {
                smeltingRecipe = holder;
            }
        }
        
        // Return in priority order
        if (craftingRecipe != null) {
            return craftingRecipe;
        }
        if (smeltingRecipe != null) {
            return smeltingRecipe;
        }
        return firstRecipe;
    }
}
