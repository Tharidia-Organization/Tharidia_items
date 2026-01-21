package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.BabyMobItem;
import com.THproject.tharidia_things.registry.BabyMobRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Handles dynamic color tinting for BabyMobItems.
 * Uses the same colors as the corresponding spawn egg for each entity type.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BabyMobColorHandler {

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        TharidiaThings.LOGGER.info("[BABY MOBS] Registering color handlers for baby mob items...");
        TharidiaThings.LOGGER.info("[BABY MOBS] Total baby mobs in registry: {}", BabyMobRegistry.getAllBabyMobs().size());

        int count = 0;
        for (DeferredItem<Item> deferredItem : BabyMobRegistry.getAllBabyMobs().values()) {
            Item item = deferredItem.get();
            TharidiaThings.LOGGER.info("[BABY MOBS] Processing item: {} (class: {})", deferredItem.getId(), item.getClass().getSimpleName());

            if (item instanceof BabyMobItem babyMobItem) {
                EntityType<?> entityType = babyMobItem.getEntityType();
                SpawnEggItem spawnEgg = SpawnEggItem.byId(entityType);

                TharidiaThings.LOGGER.info("[BABY MOBS] Entity: {}, SpawnEgg found: {}", entityType, spawnEgg != null);

                if (spawnEgg != null) {
                    // Capture spawn egg for lambda - get colors from the spawn egg
                    final SpawnEggItem egg = spawnEgg;

                    // Colors in NeoForge 1.21.1 are ARGB format!
                    // SpawnEggItem.getColor() returns RGB, we need to add full alpha (0xFF000000)
                    event.register((stack, tintIndex) -> {
                        // Use primary color (tintIndex 0) for layer0
                        int rgbColor = egg.getColor(tintIndex);
                        // Add full alpha to make it ARGB format
                        return 0xFF000000 | rgbColor;
                    }, item);

                    TharidiaThings.LOGGER.info("[BABY MOBS] Registered color handler for {} with colors: primary=0x{}, secondary=0x{}",
                        entityType, Integer.toHexString(egg.getColor(0)), Integer.toHexString(egg.getColor(1)));
                } else {
                    // No spawn egg found - use a default gray color
                    event.register((stack, tintIndex) -> {
                        return 0xFF888888; // Gray fallback (ARGB)
                    }, item);

                    TharidiaThings.LOGGER.warn("[BABY MOBS] No spawn egg found for {}, using gray fallback", entityType);
                }

                count++;
            }
        }

        TharidiaThings.LOGGER.info("[BABY MOBS] Registered {} color handlers", count);
    }
}
