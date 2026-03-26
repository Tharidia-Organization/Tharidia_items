package com.THproject.tharidia_things.cook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads cook time overrides from datapacks.
 * Location: data/<namespace>/cook_time_overrides/<name>.json
 *
 * File format:
 * {
 *   "item": "minecraft:bread",
 *   "time_ticks": 500
 * }
 *
 * This does NOT define recipes — all food recipes are auto-discovered from the game.
 * This only overrides the cooking timer duration for a specific output item.
 */
public class CookRecipeLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CookRecipeLoader.class);
    private static final Gson GSON = new GsonBuilder().create();

    public CookRecipeLoader() {
        super(GSON, "cook_time_overrides");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Integer> overrides = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            try {
                JsonObject obj = entry.getValue().getAsJsonObject();
                ResourceLocation itemId = ResourceLocation.parse(obj.get("item").getAsString());
                int timeTicks = obj.get("time_ticks").getAsInt();
                overrides.put(itemId, timeTicks);
                LOGGER.debug("[CookTime] Override: {} = {} ticks", itemId, timeTicks);
            } catch (Exception ex) {
                LOGGER.error("[CookTime] Failed to parse {}: {}", entry.getKey(), ex.getMessage());
            }
        }

        CookRecipeRegistry.setTimeOverrides(overrides);
        LOGGER.info("[CookTime] Loaded {} cook time override(s)", overrides.size());
    }
}
