package com.THproject.tharidia_things.dungeon_query;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active dungeon instances and ticks them every server tick.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID)
public class DungeonInstanceManager {

    private static final List<DungeonQueryInstance> activeInstances = new CopyOnWriteArrayList<>();

    /**
     * Registers a new dungeon instance to be ticked.
     */
    public static void registerInstance(DungeonQueryInstance instance) {
        activeInstances.add(instance);
        TharidiaThings.LOGGER.debug("[DUNGEON] Registered dungeon instance. Total active: {}", activeInstances.size());
    }

    /**
     * Unregisters a dungeon instance.
     */
    public static void unregisterInstance(DungeonQueryInstance instance) {
        activeInstances.remove(instance);
        TharidiaThings.LOGGER.debug("[DUNGEON] Unregistered dungeon instance. Total active: {}", activeInstances.size());
    }

    /**
     * Gets the count of active dungeon instances.
     */
    public static int getActiveInstanceCount() {
        return activeInstances.size();
    }

    /**
     * Clears all dungeon instances (for server shutdown).
     */
    public static void clearAll() {
        activeInstances.clear();
    }

    /**
     * Ticks all active dungeon instances.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Tick all active instances
        List<DungeonQueryInstance> toRemove = new ArrayList<>();

        for (DungeonQueryInstance instance : activeInstances) {
            instance.tick();

            // Check if dungeon has ended
            if (instance.getStatus() == DungeonStatus.IDLE && instance.getPlayers().isEmpty()) {
                toRemove.add(instance);
            }
        }

        // Remov e finished instances
        for (DungeonQueryInstance instance : toRemove) {
            unregisterInstance(instance);
        }
    }
}
