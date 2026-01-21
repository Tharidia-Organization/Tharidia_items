package com.THproject.tharidia_things.registry;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.BabyMobItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.HashMap;
import java.util.Map;

public class BabyMobRegistry {
    
    // Map of EntityType to BabyMobItem
    private static final Map<EntityType<?>, DeferredItem<Item>> BABY_MOB_ITEMS = new HashMap<>();
    
    // Map of BabyMobItem to EntityType (reverse lookup)
    private static final Map<Item, EntityType<?>> ITEM_TO_ENTITY = new HashMap<>();
    
    /**
     * Scans all registered items and creates baby mob items for all peaceful mob spawn eggs
     */
    public static void registerBabyMobs() {
        TharidiaThings.LOGGER.info("[BABY MOBS] Starting dynamic baby mob registration...");
        
        int count = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SpawnEggItem spawnEgg) {
                // Create a valid ItemStack for the spawn egg
                ItemStack eggStack = new ItemStack(item);
                EntityType<?> entityType = spawnEgg.getType(eggStack);
                
                // Check if the entity is a peaceful mob (CREATURE or AMBIENT or WATER_CREATURE)
                if (entityType.getCategory() == MobCategory.CREATURE || 
                    entityType.getCategory() == MobCategory.AMBIENT ||
                    entityType.getCategory() == MobCategory.WATER_CREATURE ||
                    entityType.getCategory() == MobCategory.WATER_AMBIENT) {
                    
                    // Get entity ID
                    String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath();
                    String babyItemId = "baby_" + entityId;
                    
                    // Register the baby mob item
                    DeferredItem<Item> babyItem = TharidiaThings.ITEMS.register(babyItemId, 
                        () -> new BabyMobItem(entityType, new Item.Properties().stacksTo(16)));
                    
                    BABY_MOB_ITEMS.put(entityType, babyItem);
                    count++;
                    
                    TharidiaThings.LOGGER.info("[BABY MOBS] Registered: {} for entity {}", babyItemId, entityId);
                }
            }
        }
        
        TharidiaThings.LOGGER.info("[BABY MOBS] Registered {} baby mob items", count);
    }
    
    /**
     * Called after items are registered to build the reverse lookup map
     */
    public static void buildReverseLookup() {
        ITEM_TO_ENTITY.clear();
        for (Map.Entry<EntityType<?>, DeferredItem<Item>> entry : BABY_MOB_ITEMS.entrySet()) {
            Item item = entry.getValue().get();
            ITEM_TO_ENTITY.put(item, entry.getKey());
        }
        TharidiaThings.LOGGER.info("[BABY MOBS] Built reverse lookup map with {} entries", ITEM_TO_ENTITY.size());
    }
    
    /**
     * Gets the EntityType for a given baby mob item
     */
    public static EntityType<?> getEntityTypeForItem(Item item) {
        if (item instanceof BabyMobItem babyMobItem) {
            return babyMobItem.getEntityType();
        }
        return ITEM_TO_ENTITY.get(item);
    }
    
    /**
     * Gets the baby mob item for a given EntityType
     */
    public static Item getBabyItemForEntity(EntityType<?> entityType) {
        DeferredItem<Item> item = BABY_MOB_ITEMS.get(entityType);
        return item != null ? item.get() : null;
    }
    
    /**
     * Checks if an item is a baby mob item
     */
    public static boolean isBabyMobItem(Item item) {
        return item instanceof BabyMobItem || ITEM_TO_ENTITY.containsKey(item);
    }
    
    /**
     * Gets all registered baby mob items
     */
    public static Map<EntityType<?>, DeferredItem<Item>> getAllBabyMobs() {
        return BABY_MOB_ITEMS;
    }
    
    /**
     * Adds baby mob items to creative tab
     */
    public static void addToCreativeTab(net.minecraft.world.item.CreativeModeTab.Output output) {
        for (DeferredItem<Item> item : BABY_MOB_ITEMS.values()) {
            output.accept(item.get());
        }
    }
}
