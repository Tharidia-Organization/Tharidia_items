package com.THproject.tharidia_things.util;

import com.THproject.tharidia_things.Config;
import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for currency item validation.
 * Centralizes all currency checking logic to avoid duplication and inconsistencies.
 */
public final class CurrencyHelper {

    // Hardcoded currency items that are always considered currency
    private static final Set<String> HARDCODED_CURRENCIES = Set.of(
        "numismaticoverhaul:bronze_coin",
        "numismaticoverhaul:silver_coin",
        "numismaticoverhaul:gold_coin",
        "numismaticoverhaul:money_bag"
    );

    // Cache for config currencies to avoid repeated parsing
    private static Set<ResourceLocation> cachedConfigCurrencies = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_DURATION_MS = 60000; // 1 minute cache

    private CurrencyHelper() {
        // Utility class, no instantiation
    }

    /**
     * Check if an ItemStack is a currency item.
     * This is the single source of truth for currency checking.
     *
     * @param stack The ItemStack to check
     * @return true if the item is considered a currency
     */
    public static boolean isCurrencyItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return isCurrencyItem(itemId);
    }

    /**
     * Check if a ResourceLocation represents a currency item.
     *
     * @param itemId The ResourceLocation of the item
     * @return true if the item is considered a currency
     */
    public static boolean isCurrencyItem(ResourceLocation itemId) {
        if (itemId == null) {
            return false;
        }

        String itemIdString = itemId.toString();

        // Check hardcoded currencies first (fast path)
        if (HARDCODED_CURRENCIES.contains(itemIdString)) {
            return true;
        }

        // Check config currencies
        return getConfigCurrencies().contains(itemId);
    }

    /**
     * Get the set of currency ResourceLocations from config.
     * Results are cached for performance.
     */
    private static Set<ResourceLocation> getConfigCurrencies() {
        long now = System.currentTimeMillis();

        // Return cached value if still valid
        if (cachedConfigCurrencies != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
            return cachedConfigCurrencies;
        }

        // Rebuild cache
        Set<ResourceLocation> currencies = new HashSet<>();
        try {
            List<? extends String> configItems = Config.TRADE_CURRENCY_ITEMS.get();
            for (String currency : configItems) {
                try {
                    ResourceLocation currencyId = ResourceLocation.parse(currency);
                    currencies.add(currencyId);
                } catch (Exception e) {
                    TharidiaThings.LOGGER.warn("Invalid currency item in config: {}", currency);
                }
            }
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to load currency items from config", e);
        }

        cachedConfigCurrencies = currencies;
        cacheTimestamp = now;

        return currencies;
    }

    /**
     * Invalidate the currency cache.
     * Call this when config is reloaded.
     */
    public static void invalidateCache() {
        cachedConfigCurrencies = null;
        cacheTimestamp = 0;
    }

    /**
     * Get all known currency items (both hardcoded and config).
     *
     * @return Set of all currency ResourceLocations
     */
    public static Set<ResourceLocation> getAllCurrencies() {
        Set<ResourceLocation> all = new HashSet<>();

        // Add hardcoded currencies
        for (String hardcoded : HARDCODED_CURRENCIES) {
            try {
                all.add(ResourceLocation.parse(hardcoded));
            } catch (Exception e) {
                // Ignore invalid hardcoded entries
            }
        }

        // Add config currencies
        all.addAll(getConfigCurrencies());

        return all;
    }
}
