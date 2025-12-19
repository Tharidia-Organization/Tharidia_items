package com.THproject.tharidia_things.diet;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Central registry for diet profiles and configuration, backed by datapacks.
 */
public final class DietRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(DietRegistry.class);

    private static volatile DietPackConfig config = DietPackConfig.DEFAULT;

    // Fast cache per item stack (identity). Auto-clears when stack GC'ed.
    private static final Cache<ResourceLocation, DietProfile> PROFILE_CACHE =
            CacheBuilder.newBuilder()
                    .maximumSize(256)
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build();

    private DietRegistry() {}

    public static void loadConfig(DietPackConfig newConfig) {
        config = newConfig == null ? DietPackConfig.DEFAULT : newConfig;
        PROFILE_CACHE.invalidateAll();
        LOGGER.info("Diet config loaded with {} explicit item entries", config.items().size());
    }

    public static void reset() {
        config = DietPackConfig.DEFAULT;
        PROFILE_CACHE.invalidateAll();
    }

    public static DietProfile getProfile(ItemStack stack) {
        if (stack.isEmpty()) {
            return DietProfile.EMPTY;
        }
        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);

        DietProfile cached = PROFILE_CACHE.getIfPresent(id);
        if (cached != null) {
            return cached;
        }

        DietProfile profile = config.items().get(id);
        if (profile == null) {
            profile = deriveProfile(item);
        }
        PROFILE_CACHE.put(id, profile);
        return profile;
    }

    public static DietProfile getDecayRates() {
        return config.decayRates();
    }

    public static DietProfile getMaxValues() {
        return config.maxValues();
    }

    public static DietSystemSettings getSettings() {
        return config.settings();
    }

    private static DietProfile deriveProfile(Item item) {
        ItemStack sampleStack = item.getDefaultInstance();
        if (sampleStack.isEmpty()) {
            sampleStack = new ItemStack(item);
        }
        FoodProperties food = item.getFoodProperties(sampleStack, null);
        if (food == null) {
            return DietProfile.EMPTY;
        }

        DietSystemSettings settings = config.settings();
        float nutrition = food.nutrition();
        float saturation = food.saturation() * settings.saturationScale();
        boolean isMeat = item.builtInRegistryHolder().is(ItemTags.MEAT);
        boolean fast = food.saturation() <= settings.fastFoodSaturationThreshold(); // Fast food heuristic: low saturation
        boolean always = food.canAlwaysEat();

        float grain = (nutrition * settings.grainNutritionMultiplier())
                + (fast ? settings.fastFoodGrainBonus() : 0.0f);
        float protein = (isMeat ? settings.proteinMeatMultiplier() : settings.proteinBaseMultiplier()) * nutrition;
        float vegetable = saturation * (hasVegetableHints(item) ? settings.vegetableHintMultiplier() : settings.vegetableBaseMultiplier());
        float fruit = (hasFruitHints(item) ? (nutrition + saturation) * settings.fruitHintMultiplier()
                : saturation * settings.fruitBaseMultiplier());

        float sugar = fast
                ? settings.fastSugarFlatBonus() + saturation * settings.fastSugarSaturationMultiplier()
                : saturation * settings.sugarBaseMultiplier();

        float water = (always ? settings.waterAlwaysEatBonus() : settings.waterDefaultBonus())
                + (hasDrinkEffect(item) ? settings.drinkWaterBonus() : 0.0f);

        return DietProfile.of(
                Math.max(0.0f, grain),
                Math.max(0.0f, protein),
                Math.max(0.0f, vegetable),
                Math.max(0.0f, fruit),
                Math.max(0.0f, sugar),
                Math.max(0.0f, water)
        );
    }

    private static boolean hasDrinkEffect(Item item) {
        String namespace = BuiltInRegistries.ITEM.getKey(item).toString();
        return namespace.contains("soup") || namespace.contains("stew") || namespace.contains("tea") || namespace.contains("drink");
    }

    private static boolean hasVegetableHints(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        return path.contains("carrot") || path.contains("potato") || path.contains("salad") || path.contains("mushroom");
    }

    private static boolean hasFruitHints(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        return path.contains("berry") || path.contains("apple") || path.contains("melon") || path.contains("fruit");
    }
}
