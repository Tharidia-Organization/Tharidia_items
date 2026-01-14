package com.THproject.tharidia_things.weight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Loads weight data from datapacks
 * JSON files should be placed in: data/[namespace]/weight_config/[filename].json
 */
public class WeightDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeightDataLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "weight_config";
    
    public WeightDataLoader() {
        super(GSON, DIRECTORY);
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading weight configuration from datapacks...");
        
        WeightRegistry.clear();
        
        if (data.isEmpty()) {
            LOGGER.warn("No weight configuration found! Using defaults.");
            loadDefaults();
            return;
        }

        List<Map.Entry<ResourceLocation, JsonElement>> orderedEntries = orderEntries(data);

        Map<String, Double> mergedWeights = new HashMap<>();
        WeightData.WeightThresholds mergedThresholds = null;
        WeightData.WeightDebuffs mergedDebuffs = null;
        boolean loadedAny = false;

        for (Map.Entry<ResourceLocation, JsonElement> entry : orderedEntries) {
            ResourceLocation location = entry.getKey();
            JsonElement json = entry.getValue();

            WeightData parsed = tryParse(location, json);
            if (parsed == null) {
                continue;
            }

            loadedAny = true;
            mergedWeights.putAll(parsed.getItemWeights());
            mergedThresholds = parsed.getThresholds();
            mergedDebuffs = parsed.getDebuffs();
        }

        if (!loadedAny) {
            LOGGER.warn("No valid weight configuration loaded. Using defaults.");
            loadDefaults();
            return;
        }

        WeightData merged = new WeightData(
                Map.copyOf(mergedWeights),
                mergedThresholds != null ? mergedThresholds : new WeightData.WeightThresholds(100, 200, 300, 400),
                mergedDebuffs != null ? mergedDebuffs : new WeightData.WeightDebuffs(0.95, 0.85, 0.7, 0.5, true, true)
        );
        WeightRegistry.setWeightData(merged);
        LOGGER.info("Successfully loaded merged weight configuration from {} files", orderedEntries.size());
    }

    private static List<Map.Entry<ResourceLocation, JsonElement>> orderEntries(Map<ResourceLocation, JsonElement> data) {
        Comparator<Map.Entry<ResourceLocation, JsonElement>> comparator = Comparator
                .comparing((Map.Entry<ResourceLocation, JsonElement> e) -> !e.getKey().getPath().equals("default"))
                .thenComparing(e -> e.getKey().getNamespace())
                .thenComparing(e -> e.getKey().getPath());

        return data.entrySet().stream().sorted(comparator).collect(Collectors.toList());
    }

    private static WeightData tryParse(ResourceLocation location, JsonElement json) {
        try {
            LOGGER.info("Loading weight config from: {}", location);

            Optional<WeightData> decoded = WeightData.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("Failed to parse weight data {}: {}", location, error));

            if (decoded.isPresent()) {
                return decoded.get();
            }
        } catch (Exception e) {
            LOGGER.error("Error decoding weight data from {}: {}", location, e.getMessage());
        }

        try {
            if (!json.isJsonObject()) {
                return null;
            }

            JsonObject obj = json.getAsJsonObject();

            Map<String, Double> itemWeights = new HashMap<>();
            if (obj.has("item_weights") && obj.get("item_weights").isJsonObject()) {
                for (Map.Entry<String, JsonElement> weightEntry : obj.getAsJsonObject("item_weights").entrySet()) {
                    if (weightEntry.getValue().isJsonPrimitive() && weightEntry.getValue().getAsJsonPrimitive().isNumber()) {
                        itemWeights.put(weightEntry.getKey(), weightEntry.getValue().getAsDouble());
                    }
                }
            }

            WeightData.WeightThresholds thresholds = null;
            if (obj.has("thresholds") && obj.get("thresholds").isJsonObject()) {
                JsonObject t = obj.getAsJsonObject("thresholds");
                if (t.has("light") && t.has("medium") && t.has("heavy") && t.has("overencumbered")) {
                    thresholds = new WeightData.WeightThresholds(
                            t.get("light").getAsDouble(),
                            t.get("medium").getAsDouble(),
                            t.get("heavy").getAsDouble(),
                            t.get("overencumbered").getAsDouble()
                    );
                }
            }

            WeightData.WeightDebuffs debuffs = null;
            if (obj.has("debuffs") && obj.get("debuffs").isJsonObject()) {
                JsonObject d = obj.getAsJsonObject("debuffs");
                if (d.has("light_speed_multiplier")
                        && d.has("medium_speed_multiplier")
                        && d.has("heavy_speed_multiplier")
                        && d.has("overencumbered_speed_multiplier")
                        && d.has("heavy_disable_swim_up")
                        && d.has("overencumbered_disable_swim_up")) {
                    debuffs = new WeightData.WeightDebuffs(
                            d.get("light_speed_multiplier").getAsDouble(),
                            d.get("medium_speed_multiplier").getAsDouble(),
                            d.get("heavy_speed_multiplier").getAsDouble(),
                            d.get("overencumbered_speed_multiplier").getAsDouble(),
                            d.get("heavy_disable_swim_up").getAsBoolean(),
                            d.get("overencumbered_disable_swim_up").getAsBoolean()
                    );
                }
            }

            if (itemWeights.isEmpty() && thresholds == null && debuffs == null) {
                return null;
            }

            return new WeightData(
                    itemWeights,
                    thresholds != null ? thresholds : new WeightData.WeightThresholds(100, 200, 300, 400),
                    debuffs != null ? debuffs : new WeightData.WeightDebuffs(0.95, 0.85, 0.7, 0.5, true, true)
            );
        } catch (Exception e) {
            LOGGER.error("Error parsing weight data from {}: {}", location, e.getMessage());
            return null;
        }
    }
    
    /**
     * Loads default weight configuration
     */
    private void loadDefaults() {
        try {
            // Create default configuration
            Map<String, Double> defaultWeights = Map.of(
                "minecraft:stone", 2.0,
                "minecraft:iron_ingot", 3.0,
                "minecraft:gold_ingot", 4.0,
                "minecraft:diamond", 5.0,
                "minecraft:netherite_ingot", 6.0,
                "minecraft:anvil", 50.0,
                "minecraft:feather", 0.1
            );
            
            WeightData.WeightThresholds thresholds = new WeightData.WeightThresholds(
                100.0,  // light
                200.0,  // medium
                300.0,  // heavy
                400.0   // overencumbered
            );
            
            WeightData.WeightDebuffs debuffs = new WeightData.WeightDebuffs(
                0.95,   // light speed multiplier
                0.85,   // medium speed multiplier
                0.70,   // heavy speed multiplier
                0.50,   // overencumbered speed multiplier
                true,   // heavy disable swim up
                true    // overencumbered disable swim up
            );
            
            WeightData defaultData = new WeightData(defaultWeights, thresholds, debuffs);
            WeightRegistry.setWeightData(defaultData);
            
            LOGGER.info("Loaded default weight configuration");
        } catch (Exception e) {
            LOGGER.error("Failed to load default weight configuration", e);
        }
    }
}
