package com.THproject.tharidia_things.diet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Tunable values that govern automatic diet behavior.
 */
public record DietSystemSettings(
        float decayIntervalSeconds,
        HeuristicSettings heuristics
) {
    public DietSystemSettings {
        heuristics = heuristics == null ? HeuristicSettings.DEFAULT : heuristics;
    }

    public static final DietSystemSettings DEFAULT = new DietSystemSettings(
            120.0f,
            HeuristicSettings.DEFAULT
    );

    public static final Codec<DietSystemSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("decay_interval_seconds", DEFAULT.decayIntervalSeconds()).forGetter(DietSystemSettings::decayIntervalSeconds),
            HeuristicSettings.HEURISTIC_CODEC.forGetter(DietSystemSettings::heuristics)
    ).apply(instance, DietSystemSettings::new));

    public long decayIntervalMillis() {
        float clamped = Math.max(0.05f, decayIntervalSeconds);
        return (long) (clamped * 1000.0f);
    }

    public float saturationScale() {
        return heuristics.saturationScale();
    }

    public float fastFoodSaturationThreshold() {
        return heuristics.fastFoodSaturationThreshold();
    }

    public float grainNutritionMultiplier() {
        return heuristics.grainNutritionMultiplier();
    }

    public float fastFoodGrainBonus() {
        return heuristics.fastFoodGrainBonus();
    }

    public float proteinMeatMultiplier() {
        return heuristics.proteinMeatMultiplier();
    }

    public float proteinBaseMultiplier() {
        return heuristics.proteinBaseMultiplier();
    }

    public float vegetableHintMultiplier() {
        return heuristics.vegetableHintMultiplier();
    }

    public float vegetableBaseMultiplier() {
        return heuristics.vegetableBaseMultiplier();
    }

    public float fruitHintMultiplier() {
        return heuristics.fruitHintMultiplier();
    }

    public float fruitBaseMultiplier() {
        return heuristics.fruitBaseMultiplier();
    }

    public float sugarBaseMultiplier() {
        return heuristics.sugarBaseMultiplier();
    }

    public float fastSugarFlatBonus() {
        return heuristics.fastSugarFlatBonus();
    }

    public float fastSugarSaturationMultiplier() {
        return heuristics.fastSugarSaturationMultiplier();
    }

    public float waterAlwaysEatBonus() {
        return heuristics.waterAlwaysEatBonus();
    }

    public float waterDefaultBonus() {
        return heuristics.waterDefaultBonus();
    }

    public float drinkWaterBonus() {
        return heuristics.drinkWaterBonus();
    }

    public record HeuristicSettings(
            float saturationScale,
            float fastFoodSaturationThreshold,
            float grainNutritionMultiplier,
            float fastFoodGrainBonus,
            float proteinMeatMultiplier,
            float proteinBaseMultiplier,
            float vegetableHintMultiplier,
            float vegetableBaseMultiplier,
            float fruitHintMultiplier,
            float fruitBaseMultiplier,
            float sugarBaseMultiplier,
            float fastSugarFlatBonus,
            float fastSugarSaturationMultiplier,
            float waterAlwaysEatBonus,
            float waterDefaultBonus,
            float drinkWaterBonus
    ) {
        private static final HeuristicSettings DEFAULT = new HeuristicSettings(
                2.0f,   // multiplier applied to vanilla saturation when deriving stats
                2.0f,   // saturation value that distinguishes “fast food” items (norma food became "fast" when saturation is < of this value)
                0.4f,   // grain gained per point of nutrition
                1.5f,   // flat grain bonus added for fast foods
                0.6f,   // protein gained per nutrition from meat
                0.2f,   // protein gained per nutrition from non-meat
                1.5f,   // vegetable gain multiplier when the item hints veggies
                0.3f,   // vegetable gain multiplier when there is no veggie hint
                0.4f,   // fruit gain multiplier when the item hints fruit
                0.2f,   // fruit gain multiplier when there is no fruit hint
                0.4f,   // sugar gained per point of saturation on normal foods
                2.0f,   // flat sugar injection for fast foods
                1.0f,   // extra sugar from saturation contributed by fast foods
                1.0f,   // water bonus for foods you can always eat (soups, etc.)
                0.5f,   // baseline water bonus for regular foods
                0.6f    // additional water granted when the item is considered a drink
        );

        private static final MapCodec<HeuristicSettings> HEURISTIC_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("saturation_scale", DEFAULT.saturationScale()).forGetter(HeuristicSettings::saturationScale),
                Codec.FLOAT.optionalFieldOf("fast_food_saturation_threshold", DEFAULT.fastFoodSaturationThreshold()).forGetter(HeuristicSettings::fastFoodSaturationThreshold),
                Codec.FLOAT.optionalFieldOf("grain_nutrition_multiplier", DEFAULT.grainNutritionMultiplier()).forGetter(HeuristicSettings::grainNutritionMultiplier),
                Codec.FLOAT.optionalFieldOf("fast_food_grain_bonus", DEFAULT.fastFoodGrainBonus()).forGetter(HeuristicSettings::fastFoodGrainBonus),
                Codec.FLOAT.optionalFieldOf("protein_meat_multiplier", DEFAULT.proteinMeatMultiplier()).forGetter(HeuristicSettings::proteinMeatMultiplier),
                Codec.FLOAT.optionalFieldOf("protein_base_multiplier", DEFAULT.proteinBaseMultiplier()).forGetter(HeuristicSettings::proteinBaseMultiplier),
                Codec.FLOAT.optionalFieldOf("vegetable_hint_multiplier", DEFAULT.vegetableHintMultiplier()).forGetter(HeuristicSettings::vegetableHintMultiplier),
                Codec.FLOAT.optionalFieldOf("vegetable_base_multiplier", DEFAULT.vegetableBaseMultiplier()).forGetter(HeuristicSettings::vegetableBaseMultiplier),
                Codec.FLOAT.optionalFieldOf("fruit_hint_multiplier", DEFAULT.fruitHintMultiplier()).forGetter(HeuristicSettings::fruitHintMultiplier),
                Codec.FLOAT.optionalFieldOf("fruit_base_multiplier", DEFAULT.fruitBaseMultiplier()).forGetter(HeuristicSettings::fruitBaseMultiplier),
                Codec.FLOAT.optionalFieldOf("sugar_base_multiplier", DEFAULT.sugarBaseMultiplier()).forGetter(HeuristicSettings::sugarBaseMultiplier),
                Codec.FLOAT.optionalFieldOf("fast_sugar_flat_bonus", DEFAULT.fastSugarFlatBonus()).forGetter(HeuristicSettings::fastSugarFlatBonus),
                Codec.FLOAT.optionalFieldOf("fast_sugar_saturation_multiplier", DEFAULT.fastSugarSaturationMultiplier()).forGetter(HeuristicSettings::fastSugarSaturationMultiplier),
                Codec.FLOAT.optionalFieldOf("water_always_eat_bonus", DEFAULT.waterAlwaysEatBonus()).forGetter(HeuristicSettings::waterAlwaysEatBonus),
                Codec.FLOAT.optionalFieldOf("water_default_bonus", DEFAULT.waterDefaultBonus()).forGetter(HeuristicSettings::waterDefaultBonus),
                Codec.FLOAT.optionalFieldOf("drink_water_bonus", DEFAULT.drinkWaterBonus()).forGetter(HeuristicSettings::drinkWaterBonus)
        ).apply(instance, HeuristicSettings::new));
    }
}
