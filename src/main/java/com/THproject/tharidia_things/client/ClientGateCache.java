package com.THproject.tharidia_things.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Client-side cache of currently blocked items.
 * Updated via packets from the server whenever player's permissions change.
 */
public class ClientGateCache {
    
    private static Set<Item> blockedItems = Collections.emptySet();
    
    /**
     * Updates the cache with a new set of blocked items (called when receiving server packet)
     */
    public static void updateBlockedItems(Set<Item> items) {
        blockedItems = new HashSet<>(items);
    }
    
    /**
     * Checks if an item is currently blocked for the local player
     */
    public static boolean isBlocked(Item item) {
        return blockedItems.contains(item);
    }
    
    /**
     * Checks if an item stack is currently blocked for the local player
     */
    public static boolean isBlocked(ItemStack stack) {
        return !stack.isEmpty() && isBlocked(stack.getItem());
    }
    
    /**
     * Clears the cache (called on disconnect)
     */
    public static void clear() {
        blockedItems = Collections.emptySet();
    }
    
    /**
     * Gets all currently blocked items (for debugging)
     */
    public static Set<Item> getBlockedItems() {
        return Collections.unmodifiableSet(blockedItems);
    }
}
