package com.tharidia.tharidia_things.diet;

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
 * Loads diet configuration from datapacks.
 * Files located at data/diet_config.json
 */
public class DietDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DietDataLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "diet_config";

    public DietDataLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading diet configuration...");
        DietRegistry.reset();

        if (data.isEmpty()) {
            LOGGER.warn("No diet_config data found; using defaults.");
            DietRegistry.loadConfig(DietPackConfig.DEFAULT);
            return;
        }

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            try {
                DietPackConfig packConfig = DietPackConfig.CODEC
                        .parse(JsonOps.INSTANCE, entry.getValue())
                        .resultOrPartial(error -> LOGGER.error("Failed to parse diet config {}: {}", entry.getKey(), error))
                        .orElse(null);

                if (packConfig != null) {
                    DietRegistry.loadConfig(packConfig);
                    LOGGER.info("Loaded diet config from {}", entry.getKey());
                    return;
                }
            } catch (Exception ex) {
                LOGGER.error("Error loading diet config {}: {}", entry.getKey(), ex.getMessage());
            }
        }

        LOGGER.warn("All diet configs failed to load; falling back to defaults.");
        DietRegistry.loadConfig(DietPackConfig.DEFAULT);
    }
}
