package com.THproject.tharidia_things.stable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Loads stable configuration from datapacks.
 * File located at data/[namespace]/stable_config/default.json
 * Only the first found config is used (allows datapack override).
 */
public class StableConfigLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(StableConfigLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "stable_config";

    // Static config holder - defaults to DEFAULT if no datapack config found
    private static StableConfig currentConfig = StableConfig.DEFAULT;

    public StableConfigLoader() {
        super(GSON, DIRECTORY);
    }

    /**
     * Gets the current stable configuration.
     * Returns DEFAULT if no datapack config is loaded.
     */
    public static StableConfig getConfig() {
        return currentConfig;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading stable configuration...");

        // Reset to default
        currentConfig = StableConfig.DEFAULT;

        // Look for "default" config first, then any other
        JsonElement configJson = null;
        ResourceLocation configId = null;

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            // Prefer "default" config
            if (fileId.getPath().equals("default") || fileId.getPath().endsWith("/default")) {
                configJson = entry.getValue();
                configId = fileId;
                break;
            }
            // Otherwise use first found
            if (configJson == null) {
                configJson = entry.getValue();
                configId = fileId;
            }
        }

        if (configJson != null) {
            final ResourceLocation finalConfigId = configId;
            try {
                StableConfig config = StableConfig.CODEC
                    .parse(JsonOps.INSTANCE, configJson)
                    .resultOrPartial(error -> LOGGER.error("Failed to parse stable config {}: {}", finalConfigId, error))
                    .orElse(null);

                if (config != null) {
                    currentConfig = config;
                    LOGGER.info("Loaded stable config from {}", finalConfigId);
                    logConfig(config);
                }
            } catch (Exception ex) {
                LOGGER.error("Error loading stable config {}: {}", finalConfigId, ex.getMessage(), ex);
            }
        } else {
            LOGGER.info("No stable config found in datapacks, using defaults");
        }
    }

    private void logConfig(StableConfig config) {
        LOGGER.debug("  maxAnimals: {}", config.maxAnimals());
        LOGGER.debug("  feedRequiredForBreeding: {}", config.feedRequiredForBreeding());
        LOGGER.debug("  growthTimeTicks: {} ({} seconds)", config.growthTimeTicks(), config.growthTimeSeconds());
        LOGGER.debug("  eggProductionTimeTicks: {}", config.eggProductionTimeTicks());
        LOGGER.debug("  maxEggsPerChicken: {}", config.maxEggsPerChicken());
        LOGGER.debug("  milkProductionTimeTicks: {} ({} seconds)", config.milkProductionTimeTicks(), config.milkProductionTimeSeconds());
        LOGGER.debug("  waterDurationTicks: {} ({} seconds)", config.waterDurationTicks(), config.waterDurationSeconds());
        LOGGER.debug("  maxFoodItems: {}", config.maxFoodItems());
        LOGGER.debug("  foodConsumptionRateTicks: {}", config.foodConsumptionRateTicks());
        LOGGER.debug("  feedUsesRequired: {}", config.feedUsesRequired());
        LOGGER.debug("  maxManure: {}", config.maxManure());
        LOGGER.debug("  adultManureRateTicks: {}", config.adultManureRateTicks());
        LOGGER.debug("  babyManureRateTicks: {}", config.babyManureRateTicks());
        LOGGER.debug("  manureCollectAmount: {}", config.manureCollectAmount());
        LOGGER.debug("  beddingDecayIntervalTicks: {}", config.beddingDecayIntervalTicks());
        LOGGER.debug("  beddingStartFreshness: {}", config.beddingStartFreshness());
    }
}
