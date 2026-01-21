package com.THproject.tharidia_things.houseboundry.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for an animal type's production, lifecycle, breeding, and slaughter.
 * Loaded from datapack JSON files.
 */
public record AnimalProductionConfig(
    ResourceLocation entityType,
    LifecycleConfig lifecycle,
    ProductionConfig production,
    BreedingConfig breeding,
    SlaughterConfig slaughter
) {
    public static final Codec<AnimalProductionConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ResourceLocation.CODEC.fieldOf("entity").forGetter(AnimalProductionConfig::entityType),
            LifecycleConfig.CODEC.fieldOf("lifecycle").forGetter(AnimalProductionConfig::lifecycle),
            ProductionConfig.CODEC.optionalFieldOf("production").forGetter(c -> Optional.ofNullable(c.production())),
            BreedingConfig.CODEC.optionalFieldOf("breeding").forGetter(c -> Optional.ofNullable(c.breeding())),
            SlaughterConfig.CODEC.optionalFieldOf("slaughter_loot").forGetter(c -> Optional.ofNullable(c.slaughter()))
        ).apply(instance, (entity, lifecycle, production, breeding, slaughter) ->
            new AnimalProductionConfig(entity, lifecycle, production.orElse(null), breeding.orElse(null), slaughter.orElse(null))
        )
    );

    /**
     * Lifecycle configuration - baby duration and productive duration.
     */
    public record LifecycleConfig(
        float babyDurationHours,
        float productiveDurationDays
    ) {
        public static final Codec<LifecycleConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.FLOAT.optionalFieldOf("baby_duration_hours", 1.0f).forGetter(LifecycleConfig::babyDurationHours),
                Codec.FLOAT.optionalFieldOf("productive_duration_days", 10.0f).forGetter(LifecycleConfig::productiveDurationDays)
            ).apply(instance, LifecycleConfig::new)
        );

        public static final LifecycleConfig DEFAULT = new LifecycleConfig(1.0f, 10.0f);
    }

    /**
     * Production configuration - interval, max per day, and products.
     */
    public record ProductionConfig(
        float intervalHours,
        int maxPerDay,
        List<ProductItem> products
    ) {
        public static final Codec<ProductionConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.FLOAT.optionalFieldOf("interval_hours", 1.0f).forGetter(ProductionConfig::intervalHours),
                Codec.INT.optionalFieldOf("max_per_day", 4).forGetter(ProductionConfig::maxPerDay),
                ProductItem.CODEC.listOf().fieldOf("products").forGetter(ProductionConfig::products)
            ).apply(instance, ProductionConfig::new)
        );
    }

    /**
     * A single product item with count.
     */
    public record ProductItem(
        ResourceLocation item,
        int count
    ) {
        public static final Codec<ProductItem> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(ProductItem::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(ProductItem::count)
            ).apply(instance, ProductItem::new)
        );
    }

    /**
     * Breeding configuration - item required and one-time flag.
     */
    public record BreedingConfig(
        ResourceLocation breedingItem,
        boolean oneTimeOnly
    ) {
        public static final Codec<BreedingConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(BreedingConfig::breedingItem),
                Codec.BOOL.optionalFieldOf("one_time_only", true).forGetter(BreedingConfig::oneTimeOnly)
            ).apply(instance, BreedingConfig::new)
        );
    }

    /**
     * Slaughter loot configuration.
     */
    public record SlaughterConfig(
        List<LootItem> items
    ) {
        public static final Codec<SlaughterConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                LootItem.CODEC.listOf().fieldOf("items").forGetter(SlaughterConfig::items)
            ).apply(instance, SlaughterConfig::new)
        );
    }

    /**
     * A single loot item with min/max range.
     */
    public record LootItem(
        ResourceLocation item,
        int min,
        int max
    ) {
        public static final Codec<LootItem> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(LootItem::item),
                Codec.INT.optionalFieldOf("min", 1).forGetter(LootItem::min),
                Codec.INT.optionalFieldOf("max", 1).forGetter(LootItem::max)
            ).apply(instance, LootItem::new)
        );
    }
}
