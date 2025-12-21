package com.THproject.tharidia_things.diet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Persistent cache for pre-calculated diet profiles.
 * Automatically updates when mods are added/removed or items change.
 */
public class DietProfileCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(DietProfileCache.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static final String CACHE_VERSION = "1.2";
    private static final String CACHE_FILENAME = "diet_profiles_cache.json";
    
    // Analysis logic version - automatically generated from class signatures
    // This will automatically invalidate the cache when RecipeNutrientAnalyzer or DietHeuristics change
    private static final String ANALYSIS_LOGIC_VERSION = generateAnalysisVersion();
    
    private final Path cacheFile;
    private final Map<ResourceLocation, DietProfile> profiles = new ConcurrentHashMap<>();
    private final Set<ResourceLocation> knownItems = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean isCalculating = new AtomicBoolean(false);
    private String lastModListHash = "";
    
    public DietProfileCache(Path worldDirectory) {
        this.cacheFile = worldDirectory.resolve(CACHE_FILENAME);
    }
    
    /**
     * Loads cache from disk if available and valid
     */
    public void load() {
        if (!Files.exists(cacheFile)) {
            LOGGER.info("[DIET CACHE] No cache file found, will calculate on first use");
            return;
        }
        
        try {
            String json = Files.readString(cacheFile);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            
            String version = root.has("version") ? root.get("version").getAsString() : "";
            String modListHash = root.has("mod_list_hash") ? root.get("mod_list_hash").getAsString() : "";
            String analysisVersion = root.has("analysis_version") ? root.get("analysis_version").getAsString() : "";
            
            if (!CACHE_VERSION.equals(version)) {
                LOGGER.info("[DIET CACHE] Cache version mismatch (expected {}, got {}), will recalculate", CACHE_VERSION, version);
                return;
            }
            
            if (!ANALYSIS_LOGIC_VERSION.equals(analysisVersion)) {
                LOGGER.info("[DIET CACHE] Analysis logic changed (expected {}, got {}), will recalculate", ANALYSIS_LOGIC_VERSION, analysisVersion);
                return;
            }
            
            String currentModListHash = calculateModListHash();
            if (!currentModListHash.equals(modListHash)) {
                LOGGER.info("[DIET CACHE] Mod list changed, will recalculate profiles");
                return;
            }
            
            JsonObject profilesObj = root.getAsJsonObject("profiles");
            int loadedCount = 0;
            
            for (String key : profilesObj.keySet()) {
                try {
                    ResourceLocation itemId = ResourceLocation.parse(key);
                    JsonObject profileData = profilesObj.getAsJsonObject(key);
                    
                    DietProfile profile = deserializeProfile(profileData);
                    profiles.put(itemId, profile);
                    knownItems.add(itemId);
                    loadedCount++;
                } catch (Exception e) {
                    LOGGER.warn("[DIET CACHE] Failed to load profile for {}: {}", key, e.getMessage());
                }
            }
            
            lastModListHash = modListHash;
            LOGGER.info("[DIET CACHE] Loaded {} pre-calculated profiles from cache", loadedCount);
            
        } catch (Exception e) {
            LOGGER.error("[DIET CACHE] Failed to load cache file: {}", e.getMessage());
            profiles.clear();
            knownItems.clear();
        }
    }
    
    /**
     * Saves cache to disk
     */
    public void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", CACHE_VERSION);
            root.addProperty("analysis_version", ANALYSIS_LOGIC_VERSION);
            root.addProperty("mod_list_hash", lastModListHash);
            root.addProperty("calculated_at", System.currentTimeMillis());
            
            JsonObject profilesObj = new JsonObject();
            for (Map.Entry<ResourceLocation, DietProfile> entry : profiles.entrySet()) {
                profilesObj.add(entry.getKey().toString(), serializeProfile(entry.getValue()));
            }
            root.add("profiles", profilesObj);
            
            String json = GSON.toJson(root);
            Files.writeString(cacheFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            LOGGER.info("[DIET CACHE] Saved {} profiles to cache", profiles.size());
            
        } catch (IOException e) {
            LOGGER.error("[DIET CACHE] Failed to save cache: {}", e.getMessage());
        }
    }
    
    /**
     * Starts background calculation of all food items
     */
    public CompletableFuture<Void> calculateAsync(MinecraftServer server, DietSystemSettings settings) {
        if (isCalculating.getAndSet(true)) {
            LOGGER.warn("[DIET CACHE] Calculation already in progress");
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("[DIET CACHE] Starting background calculation of diet profiles...");
                long startTime = System.currentTimeMillis();
                
                List<Item> foodItems = collectFoodItems();
                Set<ResourceLocation> currentItems = new HashSet<>();
                AtomicInteger calculated = new AtomicInteger(0);
                AtomicInteger cached = new AtomicInteger(0);
                
                for (Item item : foodItems) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                    currentItems.add(itemId);
                    
                    // Skip if already calculated and item hasn't changed
                    if (profiles.containsKey(itemId)) {
                        cached.incrementAndGet();
                        continue;
                    }
                    
                    // Calculate profile
                    DietProfile profile = RecipeNutrientAnalyzer.analyze(item, settings);
                    if (!profile.isEmpty()) {
                        profiles.put(itemId, profile);
                        calculated.incrementAndGet();
                    }
                    
                    // Log progress every 50 items
                    if ((calculated.get() + cached.get()) % 50 == 0) {
                        LOGGER.info("[DIET CACHE] Progress: {}/{} items processed", 
                            calculated.get() + cached.get(), foodItems.size());
                    }
                }
                
                // Remove items that no longer exist
                int removed = 0;
                Iterator<ResourceLocation> iterator = knownItems.iterator();
                while (iterator.hasNext()) {
                    ResourceLocation itemId = iterator.next();
                    if (!currentItems.contains(itemId)) {
                        profiles.remove(itemId);
                        iterator.remove();
                        removed++;
                    }
                }
                
                knownItems.addAll(currentItems);
                lastModListHash = calculateModListHash();
                
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.info("[DIET CACHE] Calculation complete in {}ms: {} new, {} cached, {} removed", 
                    duration, calculated.get(), cached.get(), removed);
                
                // Save to disk
                save();
                
            } catch (Exception e) {
                LOGGER.error("[DIET CACHE] Error during background calculation", e);
            } finally {
                isCalculating.set(false);
            }
        });
    }
    
    /**
     * Gets a pre-calculated profile, or null if not available
     */
    public DietProfile getProfile(ResourceLocation itemId) {
        return profiles.get(itemId);
    }
    
    /**
     * Gets all cached profiles for syncing to clients
     */
    public Map<ResourceLocation, DietProfile> getAllProfiles() {
        return new HashMap<>(profiles);
    }
    
    /**
     * Checks if cache needs recalculation (mod list changed or analysis logic updated)
     */
    public boolean needsRecalculation() {
        String currentHash = calculateModListHash();
        return !currentHash.equals(lastModListHash) || profiles.isEmpty();
    }
    
    /**
     * Gets the current analysis logic version for debugging
     */
    public static String getAnalysisVersion() {
        return ANALYSIS_LOGIC_VERSION;
    }
    
    /**
     * Clears all cached profiles
     */
    public void clear() {
        profiles.clear();
        knownItems.clear();
        lastModListHash = "";
    }
    
    /**
     * Collects all food items from the registry
     */
    private List<Item> collectFoodItems() {
        List<Item> foodItems = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (item.getFoodProperties(stack, null) != null) {
                foodItems.add(item);
            }
        }
        return foodItems;
    }
    
    /**
     * Calculates a hash of all loaded mods to detect changes
     */
    private String calculateModListHash() {
        List<String> modIds = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String namespace = id.getNamespace();
            if (!modIds.contains(namespace)) {
                modIds.add(namespace);
            }
        }
        Collections.sort(modIds);
        return String.valueOf(modIds.hashCode());
    }
    
    private JsonObject serializeProfile(DietProfile profile) {
        JsonObject obj = new JsonObject();
        for (DietCategory category : DietCategory.VALUES) {
            float value = profile.get(category);
            if (value > 0.0f) {
                obj.addProperty(category.name().toLowerCase(), value);
            }
        }
        return obj;
    }
    
    private DietProfile deserializeProfile(JsonObject obj) {
        float[] values = new float[DietCategory.COUNT];
        for (DietCategory category : DietCategory.VALUES) {
            String key = category.name().toLowerCase();
            if (obj.has(key)) {
                values[category.ordinal()] = obj.get(key).getAsFloat();
            }
        }
        return new DietProfile(values);
    }
    
    /**
     * Generates a version string based on the analysis classes.
     * This automatically changes when RecipeNutrientAnalyzer or DietHeuristics are modified.
     */
    private static String generateAnalysisVersion() {
        try {
            // Get class signatures - these change when methods are added/modified
            String analyzerHash = String.valueOf(RecipeNutrientAnalyzer.class.getDeclaredMethods().length);
            String heuristicsHash = String.valueOf(DietHeuristics.class.getDeclaredMethods().length);
            String fieldsHash = String.valueOf(RecipeNutrientAnalyzer.class.getDeclaredFields().length);
            
            // Combine into version string
            return "auto-" + analyzerHash + "-" + heuristicsHash + "-" + fieldsHash;
        } catch (Exception e) {
            LOGGER.warn("[DIET CACHE] Failed to generate automatic version, using fallback", e);
            return "fallback-v1";
        }
    }
}
