package com.THproject.tharidia_things.stable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Configuration for stable block entity parameters.
 * All time values are in ticks (20 ticks = 1 second).
 * Loaded from datapack: data/[namespace]/stable_config/default.json
 */
public record StableConfig(
    // Animal capacity
    int maxAnimals,

    // Breeding
    int feedRequiredForBreeding,

    // Growth
    int growthTimeTicks,

    // Production (chicken eggs)
    int eggProductionTimeTicks,
    int maxEggsPerChicken,

    // Production (milk - cows, goats, etc.)
    int milkProductionTimeTicks,

    // Water
    int waterDurationTicks,

    // Food/Feeder
    int maxFoodItems,
    int foodConsumptionRateTicks,
    int feedUsesRequired,

    // Manure
    int maxManure,
    int adultManureRateTicks,
    int babyManureRateTicks,
    int manureCollectAmount,

    // Bedding (Houseboundry)
    int beddingDecayIntervalTicks,
    int beddingStartFreshness,

    // Day/Night cycle (Houseboundry)
    // Animals rest during night and don't produce
    int dayStartTick,    // When day begins (animals wake up and start producing)
    int dayEndTick       // When day ends (animals rest and stop producing)
) {
    // Default values (matching original hardcoded values)
    public static final StableConfig DEFAULT = new StableConfig(
        3,                      // maxAnimals
        3,                      // feedRequiredForBreeding
        20 * 60 * 2,           // growthTimeTicks (2 minutes)
        20 * 30,               // eggProductionTimeTicks (30 seconds)
        3,                      // maxEggsPerChicken (legacy, chickens no longer die)
        20 * 60,               // milkProductionTimeTicks (1 minute cooldown)
        20 * 60 * 10,          // waterDurationTicks (10 minutes)
        64,                     // maxFoodItems
        20 * 10,               // foodConsumptionRateTicks (10 seconds)
        5,                      // feedUsesRequired
        100,                    // maxManure
        20 * 10,               // adultManureRateTicks (10 seconds)
        20 * 20,               // babyManureRateTicks (20 seconds)
        10,                     // manureCollectAmount
        20 * 60 * 60,          // beddingDecayIntervalTicks (1 hour)
        100,                    // beddingStartFreshness
        0,                      // dayStartTick (dawn - 6:00 AM in MC)
        12000                   // dayEndTick (dusk - 6:00 PM in MC)
    );

    // RecordCodecBuilder.group() supports max 16 parameters
    // We decode 16 fields with the codec, then handle dayStartTick and dayEndTick in the loader
    public static final Codec<StableConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.optionalFieldOf("max_animals", DEFAULT.maxAnimals())
                .forGetter(StableConfig::maxAnimals),
            Codec.INT.optionalFieldOf("feed_required_for_breeding", DEFAULT.feedRequiredForBreeding())
                .forGetter(StableConfig::feedRequiredForBreeding),
            Codec.INT.optionalFieldOf("growth_time_ticks", DEFAULT.growthTimeTicks())
                .forGetter(StableConfig::growthTimeTicks),
            Codec.INT.optionalFieldOf("egg_production_time_ticks", DEFAULT.eggProductionTimeTicks())
                .forGetter(StableConfig::eggProductionTimeTicks),
            Codec.INT.optionalFieldOf("max_eggs_per_chicken", DEFAULT.maxEggsPerChicken())
                .forGetter(StableConfig::maxEggsPerChicken),
            Codec.INT.optionalFieldOf("milk_production_time_ticks", DEFAULT.milkProductionTimeTicks())
                .forGetter(StableConfig::milkProductionTimeTicks),
            Codec.INT.optionalFieldOf("water_duration_ticks", DEFAULT.waterDurationTicks())
                .forGetter(StableConfig::waterDurationTicks),
            Codec.INT.optionalFieldOf("max_food_items", DEFAULT.maxFoodItems())
                .forGetter(StableConfig::maxFoodItems),
            Codec.INT.optionalFieldOf("food_consumption_rate_ticks", DEFAULT.foodConsumptionRateTicks())
                .forGetter(StableConfig::foodConsumptionRateTicks),
            Codec.INT.optionalFieldOf("feed_uses_required", DEFAULT.feedUsesRequired())
                .forGetter(StableConfig::feedUsesRequired),
            Codec.INT.optionalFieldOf("max_manure", DEFAULT.maxManure())
                .forGetter(StableConfig::maxManure),
            Codec.INT.optionalFieldOf("adult_manure_rate_ticks", DEFAULT.adultManureRateTicks())
                .forGetter(StableConfig::adultManureRateTicks),
            Codec.INT.optionalFieldOf("baby_manure_rate_ticks", DEFAULT.babyManureRateTicks())
                .forGetter(StableConfig::babyManureRateTicks),
            Codec.INT.optionalFieldOf("manure_collect_amount", DEFAULT.manureCollectAmount())
                .forGetter(StableConfig::manureCollectAmount),
            Codec.INT.optionalFieldOf("bedding_decay_interval_ticks", DEFAULT.beddingDecayIntervalTicks())
                .forGetter(StableConfig::beddingDecayIntervalTicks),
            Codec.INT.optionalFieldOf("bedding_start_freshness", DEFAULT.beddingStartFreshness())
                .forGetter(StableConfig::beddingStartFreshness)
        ).apply(instance, (maxAnimals, feedRequired, growthTicks, eggTicks, maxEggs, milkTicks,
                          waterTicks, maxFood, foodRate, feedUses, maxManure, adultManure,
                          babyManure, manureCollect, beddingDecay, beddingFresh) ->
            new StableConfig(maxAnimals, feedRequired, growthTicks, eggTicks, maxEggs, milkTicks,
                           waterTicks, maxFood, foodRate, feedUses, maxManure, adultManure,
                           babyManure, manureCollect, beddingDecay, beddingFresh,
                           DEFAULT.dayStartTick(), DEFAULT.dayEndTick())
        )
    );

    /**
     * Creates a new config with custom day/night tick values.
     * Used by the loader to override defaults after parsing.
     */
    public StableConfig withDayNightTicks(int dayStart, int dayEnd) {
        return new StableConfig(
            maxAnimals, feedRequiredForBreeding, growthTimeTicks, eggProductionTimeTicks,
            maxEggsPerChicken, milkProductionTimeTicks, waterDurationTicks, maxFoodItems,
            foodConsumptionRateTicks, feedUsesRequired, maxManure, adultManureRateTicks,
            babyManureRateTicks, manureCollectAmount, beddingDecayIntervalTicks, beddingStartFreshness,
            dayStart, dayEnd
        );
    }

    // Helper methods for time conversion
    public int growthTimeSeconds() {
        return growthTimeTicks / 20;
    }

    public int waterDurationSeconds() {
        return waterDurationTicks / 20;
    }

    public int foodConsumptionRateSeconds() {
        return foodConsumptionRateTicks / 20;
    }

    public int beddingDecayIntervalSeconds() {
        return beddingDecayIntervalTicks / 20;
    }

    public int milkProductionTimeSeconds() {
        return milkProductionTimeTicks / 20;
    }

    /**
     * Checks if the given game time (in ticks) is during daytime.
     * Animals produce during day, rest during night.
     *
     * @param dayTime the current day time from level.getDayTime() % 24000
     * @return true if it's daytime (animals should produce), false if nighttime (animals rest)
     */
    public boolean isDaytime(long dayTime) {
        long timeOfDay = dayTime % 24000;
        return timeOfDay >= dayStartTick && timeOfDay < dayEndTick;
    }
}
