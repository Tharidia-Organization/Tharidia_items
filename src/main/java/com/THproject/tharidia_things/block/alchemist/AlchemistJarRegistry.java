package com.THproject.tharidia_things.block.alchemist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static value matrices used by the Alchemist Table crafting sequence.
 *
 * <h3>Flower matrix</h3>
 * Each distinct vanilla flower species is assigned a unique integer 1..N
 * in registration order. Mixed-flower jars (e.g. two different species)
 * cannot occur because {@link AlchemistTableBlockEntity#tryInsertIntoJar}
 * enforces single-type jars.
 *
 * <h3>Manure matrix</h3>
 * Manure items are assigned positive even integers: 2, 4, 6, …
 * To add a new manure type extend {@link #MANURE_VALUES}.
 */
public final class AlchemistJarRegistry {

    private AlchemistJarRegistry() {}

    // ==================== Flower Matrix (values 1 .. N) ====================

    /**
     * All vanilla flower items in a fixed, stable order.
     * Position in this list determines the value: index 0 → value 1, etc.
     * Add custom flowers at the end to avoid shifting existing values.
     */
    private static final List<Item> FLOWER_ORDER = List.of(
            Items.DANDELION,           // 1
            Items.POPPY,               // 2
            Items.BLUE_ORCHID,         // 3
            Items.ALLIUM,              // 4
            Items.AZURE_BLUET,         // 5
            Items.RED_TULIP,           // 6
            Items.ORANGE_TULIP,        // 7
            Items.WHITE_TULIP,         // 8
            Items.PINK_TULIP,          // 9
            Items.OXEYE_DAISY,         // 10
            Items.CORNFLOWER,          // 11
            Items.LILY_OF_THE_VALLEY,  // 12
            Items.WITHER_ROSE,         // 13
            Items.SUNFLOWER,           // 14
            Items.LILAC,               // 15
            Items.ROSE_BUSH,           // 16
            Items.PEONY,               // 17
            Items.TORCHFLOWER,         // 18
            Items.PITCHER_PLANT        // 19
    );

    private static final Map<Item, Integer> FLOWER_VALUES;

    static {
        FLOWER_VALUES = new LinkedHashMap<>();
        for (int i = 0; i < FLOWER_ORDER.size(); i++) {
            FLOWER_VALUES.put(FLOWER_ORDER.get(i), i + 1);
        }
    }

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
     * Returns 0 if the item is unknown (not in any matrix).
     */
    public static int getItemValue(ItemStack jar) {
        if (jar.isEmpty()) return 0;

        Integer flowerVal = FLOWER_VALUES.get(jar.getItem());
        if (flowerVal != null) return flowerVal;

        if (jar.is(MANURE_TAG)) {
            ResourceLocation id = jar.getItemHolder().unwrapKey()
                    .map(k -> k.location())
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
