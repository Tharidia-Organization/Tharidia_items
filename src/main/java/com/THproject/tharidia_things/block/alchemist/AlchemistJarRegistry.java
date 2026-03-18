package com.THproject.tharidia_things.block.alchemist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static value matrices used by the Alchemist Table crafting sequence.
 *
 * <h3>Petal matrix</h3>
 * Petals produced by the herbalist tree are classified by their {@code DYED_COLOR}
 * component into one of six named colors, each with a fixed integer value.
 * See {@link PetalColorRegistry} for the full mapping and classification logic.
 *
 * <h3>Manure matrix</h3>
 * Manure items are assigned positive even integers: 2, 4, 6, …
 * To add a new manure type extend {@link #MANURE_VALUES}.
 */
public final class AlchemistJarRegistry {

    private AlchemistJarRegistry() {}

    // ==================== Manure Matrix (positive even values: 2, 4, 6 …) ====================

    /**
     * Manure item IDs mapped to their even-number values.
     * To add another manure type: {@code "namespace:item_id" → next even number}.
     */
    private static final Map<ResourceLocation, Integer> MANURE_VALUES = new LinkedHashMap<>(Map.of(
            ResourceLocation.fromNamespaceAndPath("tharidiathings", "manure"), 2
            // ResourceLocation.fromNamespaceAndPath("tharidiathings", "rich_manure"), 4
    ));

    private static final TagKey<Item> MANURE_TAG =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("tharidiathings", "manure"));

    // ==================== Public API ====================

    /**
     * Returns the integer value assigned to the item inside {@code jar}.
     * <ul>
     *   <li>Petals: classified by colour via {@link PetalColorRegistry} → values 1, 2, 3, 12, 13, 23</li>
     *   <li>Manure: positive even integers (2, 4, 6 …)</li>
     * </ul>
     * Returns 0 if the item is unknown or unclassifiable.
     */
    public static int getItemValue(ItemStack jar) {
        if (jar.isEmpty()) return 0;

        // Petal: classified by DYED_COLOR hue
        int petalVal = PetalColorRegistry.getPetalValue(jar);
        if (petalVal != 0) return petalVal;

        // Manure
        if (jar.is(MANURE_TAG)) {
            ResourceLocation id = jar.getItemHolder().unwrapKey()
                    .map(net.minecraft.resources.ResourceKey::location)
                    .orElse(null);
            if (id != null) return MANURE_VALUES.getOrDefault(id, 2);
            return 2; // fallback for any unspecified manure
        }

        return 0;
    }

    /** Returns a display name for the value of {@code jar}'s contents, for player messages. */
    public static String describeJar(ItemStack jar) {
        int value = getItemValue(jar);
        return jar.getHoverName().getString() + " → " + value;
    }
}
