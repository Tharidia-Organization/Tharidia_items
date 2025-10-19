package com.tharidia.tharidia_things.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Configuration for fatigue system.
 * Loaded from datapacks at data/tharidiathings/fatigue_config/config.json
 */
public class FatigueConfig extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Fatigue system settings
    private static int maxFatigueTicks = 40 * 60 * 20; // 40 minutes in ticks
    private static int bedRestTime = 60 * 20; // 1 minute in ticks
    private static int proximityRecoveryInterval = 10 * 20; // 10 seconds in ticks
    private static int proximityRecoveryAmount = 60 * 20; // 1 minute of fatigue per recovery
    private static double bedProximityRange = 20.0; // 20 blocks
    
    // Performance settings
    private static int movementCheckInterval = 5; // Check movement every 5 ticks
    private static int bedCheckInterval = 20; // Check bed proximity every 20 ticks (1 second)
    private static int playerBatchSize = 5; // Process players in batches (1/5th per tick)
    
    // Warning thresholds (in minutes)
    private static int warningThreshold5Min = 5; // 5 minutes
    private static int warningThreshold1Min = 1; // 1 minute
    
    // Exhaustion effects
    private static int exhaustionSlownessLevel = 3; // Slowness level (3 = -60% speed)
    private static int exhaustionEffectDuration = 40; // Effect duration in ticks
    private static boolean applyNauseaEffect = true; // Whether to apply nausea/confusion
    
    // Day/Night cycle settings (for custom day lengths)
    private static int dayCycleLength = 24000; // Full day/night cycle in ticks (vanilla = 24000)
    private static int dayEndTime = 12541; // When day ends and night starts (vanilla = 12541)
    
    public FatigueConfig() {
        super(GSON, "fatigue_config");
        initializeDefaults();
    }
    
    /**
     * Initialize default fatigue settings
     */
    private void initializeDefaults() {
        maxFatigueTicks = 40 * 60 * 20;
        bedRestTime = 60 * 20;
        proximityRecoveryInterval = 10 * 20;
        proximityRecoveryAmount = 60 * 20;
        bedProximityRange = 20.0;
        movementCheckInterval = 5;
        bedCheckInterval = 20;
        playerBatchSize = 5;
        warningThreshold5Min = 5;
        warningThreshold1Min = 1;
        exhaustionSlownessLevel = 3;
        exhaustionEffectDuration = 40;
        applyNauseaEffect = true;
        dayCycleLength = 24000;
        dayEndTime = 12541;
        
        LOGGER.info("Initialized default fatigue configuration");
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> configs, ResourceManager resourceManager, ProfilerFiller profiler) {
        // Reset to defaults before loading
        initializeDefaults();
        
        int loaded = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : configs.entrySet()) {
            try {
                ResourceLocation location = entry.getKey();
                JsonObject json = entry.getValue().getAsJsonObject();
                
                LOGGER.info("Loading fatigue config from: {}", location);
                
                // Load fatigue system settings
                if (json.has("max_fatigue_minutes")) {
                    int minutes = json.get("max_fatigue_minutes").getAsInt();
                    maxFatigueTicks = minutes * 60 * 20;
                    LOGGER.info("Set max fatigue to {} minutes ({} ticks)", minutes, maxFatigueTicks);
                }
                
                if (json.has("bed_rest_time_seconds")) {
                    int seconds = json.get("bed_rest_time_seconds").getAsInt();
                    bedRestTime = seconds * 20;
                    LOGGER.info("Set bed rest time to {} seconds ({} ticks)", seconds, bedRestTime);
                }
                
                if (json.has("proximity_recovery_interval_seconds")) {
                    int seconds = json.get("proximity_recovery_interval_seconds").getAsInt();
                    proximityRecoveryInterval = seconds * 20;
                    LOGGER.info("Set proximity recovery interval to {} seconds", seconds);
                }
                
                if (json.has("proximity_recovery_amount_seconds")) {
                    int seconds = json.get("proximity_recovery_amount_seconds").getAsInt();
                    proximityRecoveryAmount = seconds * 20;
                    LOGGER.info("Set proximity recovery amount to {} seconds of fatigue", seconds);
                }
                
                if (json.has("bed_proximity_range")) {
                    bedProximityRange = json.get("bed_proximity_range").getAsDouble();
                    LOGGER.info("Set bed proximity range to {} blocks", bedProximityRange);
                }
                
                // Load performance settings
                if (json.has("movement_check_interval")) {
                    movementCheckInterval = json.get("movement_check_interval").getAsInt();
                    LOGGER.info("Set movement check interval to {} ticks", movementCheckInterval);
                }
                
                if (json.has("bed_check_interval")) {
                    bedCheckInterval = json.get("bed_check_interval").getAsInt();
                    LOGGER.info("Set bed check interval to {} ticks", bedCheckInterval);
                }
                
                if (json.has("player_batch_size")) {
                    playerBatchSize = json.get("player_batch_size").getAsInt();
                    LOGGER.info("Set player batch size to {}", playerBatchSize);
                }
                
                // Load warning thresholds
                if (json.has("warning_threshold_5_minutes")) {
                    warningThreshold5Min = json.get("warning_threshold_5_minutes").getAsInt();
                    LOGGER.info("Set 5-minute warning threshold to {} minutes", warningThreshold5Min);
                }
                
                if (json.has("warning_threshold_1_minute")) {
                    warningThreshold1Min = json.get("warning_threshold_1_minute").getAsInt();
                    LOGGER.info("Set 1-minute warning threshold to {} minutes", warningThreshold1Min);
                }
                
                // Load exhaustion effect settings
                if (json.has("exhaustion_slowness_level")) {
                    exhaustionSlownessLevel = json.get("exhaustion_slowness_level").getAsInt();
                    LOGGER.info("Set exhaustion slowness level to {}", exhaustionSlownessLevel);
                }
                
                if (json.has("exhaustion_effect_duration")) {
                    exhaustionEffectDuration = json.get("exhaustion_effect_duration").getAsInt();
                    LOGGER.info("Set exhaustion effect duration to {} ticks", exhaustionEffectDuration);
                }
                
                if (json.has("apply_nausea_effect")) {
                    applyNauseaEffect = json.get("apply_nausea_effect").getAsBoolean();
                    LOGGER.info("Set apply nausea effect to {}", applyNauseaEffect);
                }
                
                // Load day/night cycle settings
                if (json.has("day_cycle_length")) {
                    dayCycleLength = json.get("day_cycle_length").getAsInt();
                    LOGGER.info("Set day cycle length to {} ticks", dayCycleLength);
                }
                
                if (json.has("day_end_time")) {
                    dayEndTime = json.get("day_end_time").getAsInt();
                    LOGGER.info("Set day end time to {} ticks", dayEndTime);
                }
                
                loaded++;
            } catch (Exception e) {
                LOGGER.error("Error loading fatigue config from {}", entry.getKey(), e);
            }
        }
        
        if (loaded == 0) {
            LOGGER.info("No custom fatigue configs found, using defaults");
        }
        
        LOGGER.info("Fatigue configuration loaded: max={} ticks, bed rest={} ticks, proximity range={} blocks",
            maxFatigueTicks, bedRestTime, bedProximityRange);
    }
    
    // Getters for all configuration values
    
    public static int getMaxFatigueTicks() {
        return maxFatigueTicks;
    }
    
    public static int getBedRestTime() {
        return bedRestTime;
    }
    
    public static int getProximityRecoveryInterval() {
        return proximityRecoveryInterval;
    }
    
    public static int getProximityRecoveryAmount() {
        return proximityRecoveryAmount;
    }
    
    public static double getBedProximityRange() {
        return bedProximityRange;
    }
    
    public static int getMovementCheckInterval() {
        return movementCheckInterval;
    }
    
    public static int getBedCheckInterval() {
        return bedCheckInterval;
    }
    
    public static int getPlayerBatchSize() {
        return playerBatchSize;
    }
    
    public static int getWarningThreshold5MinTicks() {
        return warningThreshold5Min * 60 * 20;
    }
    
    public static int getWarningThreshold1MinTicks() {
        return warningThreshold1Min * 60 * 20;
    }
    
    public static int getExhaustionSlownessLevel() {
        return exhaustionSlownessLevel;
    }
    
    public static int getExhaustionEffectDuration() {
        return exhaustionEffectDuration;
    }
    
    public static boolean shouldApplyNauseaEffect() {
        return applyNauseaEffect;
    }
    
    public static int getDayCycleLength() {
        return dayCycleLength;
    }
    
    public static int getDayEndTime() {
        return dayEndTime;
    }
}
