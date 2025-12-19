package com.THproject.tharidia_things.diet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;

/**
 * Represents the data-driven configuration for the diet system loaded from datapacks.
 */
public record DietPackConfig(Map<ResourceLocation, DietProfile> items,
                             DietProfile decayRates,
                             DietProfile maxValues,
                             DietSystemSettings settings) {

    private static final DietProfile DEFAULT_DECAY = DietProfile.of(
            0.15f, // grain
            0.2f,  // protein
            0.25f, // vegetable
            0.25f, // fruit
            0.3f,  // sugar
            0.35f  // water
    );

    private static final DietProfile DEFAULT_MAX = DietProfile.of(
            100.0f, 100.0f, 100.0f, 100.0f, 100.0f, 100.0f
    );

    public static final DietPackConfig DEFAULT = new DietPackConfig(
            Collections.emptyMap(),
            DEFAULT_DECAY,
            DEFAULT_MAX,
            DietSystemSettings.DEFAULT
    );

    public static final Codec<DietPackConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, DietProfile.CODEC)
                    .optionalFieldOf("items", Collections.emptyMap())
                    .forGetter(DietPackConfig::items),
            DietProfile.CODEC.optionalFieldOf("decay_rates", DEFAULT_DECAY)
                    .forGetter(DietPackConfig::decayRates),
            DietProfile.CODEC.optionalFieldOf("max_values", DEFAULT_MAX)
                    .forGetter(DietPackConfig::maxValues),
            DietSystemSettings.CODEC.optionalFieldOf("settings", DietSystemSettings.DEFAULT)
                    .forGetter(DietPackConfig::settings)
    ).apply(instance, DietPackConfig::new));
}
