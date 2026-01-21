package com.THproject.tharidia_things.houseboundry.config;

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
 * Loads animal production configuration from datapacks.
 * Files located at data/[namespace]/houseboundry/animals/[entity].json
 */
public class AnimalConfigLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimalConfigLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "houseboundry/animals";

    public AnimalConfigLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("Loading animal production configurations...");
        AnimalConfigRegistry.clear();

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                AnimalProductionConfig config = AnimalProductionConfig.CODEC
                    .parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("Failed to parse animal config {}: {}", fileId, error))
                    .orElse(null);

                if (config != null) {
                    AnimalConfigRegistry.register(config);
                    successCount++;
                    LOGGER.debug("Loaded animal config for {} from {}", config.entityType(), fileId);
                } else {
                    failCount++;
                }
            } catch (Exception ex) {
                LOGGER.error("Error loading animal config {}: {}", fileId, ex.getMessage(), ex);
                failCount++;
            }
        }

        LOGGER.info("Loaded {} animal production configs ({} failed)", successCount, failCount);

        if (successCount == 0) {
            LOGGER.warn("No animal production configs loaded! Animals will have no secondary production.");
        }
    }
}
