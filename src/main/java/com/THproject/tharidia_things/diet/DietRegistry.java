package com.THproject.tharidia_things.diet;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Central registry for diet profiles and configuration, backed by datapacks.
 */
public final class DietRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(DietRegistry.class);


    private static volatile DietPackConfig config = DietPackConfig.DEFAULT;

    // Fast cache per item stack (identity). Auto-clears when stack GC'ed.
    private static final Cache<ResourceLocation, DietProfile> PROFILE_CACHE =
            CacheBuilder.newBuilder()
                    .maximumSize(512)
                    .expireAfterAccess(10, TimeUnit.MINUTES)
                    .build();
    
    // Persistent cache for pre-calculated profiles
    private static volatile DietProfileCache persistentCache = null;
    
    // Server reference for recipe access
    private static volatile MinecraftServer currentServer = null;

    private DietRegistry() {}

    public static void setServer(MinecraftServer server) {
        currentServer = server;
        RecipeNutrientAnalyzer.setServer(server);
        if (server != null) {
            PROFILE_CACHE.invalidateAll();
            RecipeNutrientAnalyzer.clearCache();
            
            // Initialize persistent cache
            initializePersistentCache(server);
        } else {
            // Server shutting down, save cache
            if (persistentCache != null) {
                persistentCache.save();
                persistentCache = null;
            }
        }
    }
    
    private static void initializePersistentCache(MinecraftServer server) {
        try {
            java.nio.file.Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            persistentCache = new DietProfileCache(worldDir);
            persistentCache.load();
            
            // Start background calculation if needed
            if (persistentCache.needsRecalculation()) {
                LOGGER.info("[DIET] Starting background calculation of diet profiles...");
                persistentCache.calculateAsync(server, config.settings());
            } else {
                LOGGER.info("[DIET] Using cached diet profiles");
            }
        } catch (Exception e) {
            LOGGER.error("[DIET] Failed to initialize persistent cache", e);
            persistentCache = null;
        }
    }

    public static void loadConfig(DietPackConfig newConfig) {
        config = newConfig == null ? DietPackConfig.DEFAULT : newConfig;
        PROFILE_CACHE.invalidateAll();
        RecipeNutrientAnalyzer.clearCache();
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
        if (persistentCache != null) {
            persistentCache.clear();
        }
    }

    public static DietProfile getProfile(ItemStack stack) {
        if (stack.isEmpty()) {
            return DietProfile.EMPTY;
        }
        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);

        // Check memory cache first (fastest)
        DietProfile cached = PROFILE_CACHE.getIfPresent(id);
        if (cached != null) {
            return cached;
        }

        // Check explicit config entries (highest priority)
        DietProfile profile = config.items().get(id);
        if (profile != null) {
            PROFILE_CACHE.put(id, profile);
            return profile;
        }
        
        // Check persistent cache (pre-calculated)
        if (persistentCache != null) {
            profile = persistentCache.getProfile(id);
            if (profile != null) {
                PROFILE_CACHE.put(id, profile);
                return profile;
            }
        }
        
        // Fallback: calculate on-demand (only if not in persistent cache)
        profile = deriveProfile(item);
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

        DietSystemSettings settings = config.settings();
        
        // Use new crafting-first analysis system
        // This will recursively analyze recipes and only use heuristics for base components
        return RecipeNutrientAnalyzer.analyze(item, settings);
    }

}
