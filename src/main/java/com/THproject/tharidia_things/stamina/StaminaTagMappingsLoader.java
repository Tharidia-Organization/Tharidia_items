package com.THproject.tharidia_things.stamina;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Map;

public class StaminaTagMappingsLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "stamina_tag_mappings";

    public StaminaTagMappingsLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        StaminaTagMappings.clear();

        if (data.isEmpty()) {
            return;
        }

        int loadedMappings = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            try {
                JsonObject root = asObject(entry.getValue());
                if (root == null || !root.has("tagMappings")) {
                    continue;
                }

                for (JsonElement mappingEl : root.getAsJsonArray("tagMappings")) {
                    JsonObject mappingObj = asObject(mappingEl);
                    if (mappingObj == null) {
                        continue;
                    }

                    String tagId = getString(mappingObj, "tagId");
                    String modifierTypeRaw = getString(mappingObj, "modifierType");
                    Float value = getFloat(mappingObj, "value");
                    Integer priority = getInt(mappingObj, "priority");
                    Boolean isPercentage = getBoolean(mappingObj, "isPercentage");

                    if (tagId == null || modifierTypeRaw == null || value == null) {
                        continue;
                    }

                    StaminaModifierType modifierType;
                    try {
                        modifierType = StaminaModifierType.valueOf(modifierTypeRaw);
                    } catch (IllegalArgumentException ex) {
                        continue;
                    }

                    boolean computedIsPercentage = isPercentage != null ? isPercentage : modifierType.name().contains("PERCENT");
                    int computedPriority = priority != null ? priority : 0;

                    StaminaTagMappings.put(
                            tagId,
                            new StaminaModifier(modifierType, value, computedIsPercentage, tagId, computedPriority)
                    );
                    loadedMappings++;
                }
            } catch (Exception ex) {
                LOGGER.warn("Failed to load stamina tag mappings from {}: {}", entry.getKey(), ex.getMessage());
            }
        }

        LOGGER.info("Loaded {} stamina tag mappings", loadedMappings);
    }

    private static JsonObject asObject(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            try {
                JsonElement parsed = JsonParser.parseString(element.getAsString());
                return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
            } catch (JsonSyntaxException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String getString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        try {
            return obj.get(key).getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Float getFloat(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        try {
            return obj.get(key).getAsFloat();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer getInt(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Boolean getBoolean(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return null;
        }
    }
}

