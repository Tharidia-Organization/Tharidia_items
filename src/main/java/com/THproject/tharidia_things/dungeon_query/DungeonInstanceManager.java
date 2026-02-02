package com.THproject.tharidia_things.dungeon_query;

import com.THproject.tharidia_things.TharidiaThings;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
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
    // Waiting queue for groups when all instances are full
    private static final List<DungeonQueryInstance> waitingQueue = new ArrayList<>();

    /**
     * Registers a new dungeon instance to be ticked.
     */
    public static void registerInstance(DungeonQueryInstance instance) {
        activeInstances.add(instance);
        TharidiaThings.LOGGER.debug("[DUNGEON] Registered dungeon instance. Total active: {}", activeInstances.size());
    }

    /**
     * Adds a DungeonQueryInstance to the waiting queue if all instances are full.
     */
    public static void addToWaitingQueue(DungeonQueryInstance instance) {
        waitingQueue.add(instance);
        TharidiaThings.LOGGER.info("[DUNGEON] Added group to waiting queue. Queue size: {}", waitingQueue.size());
    }

    /**
     * Unregisters a dungeon instance.
     */
    public static void unregisterInstance(DungeonQueryInstance instance) {
        if (activeInstances.contains(instance)) {
            activeInstances.remove(instance);
            TharidiaThings.LOGGER.debug("[DUNGEON] Unregistered dungeon instance. Total active: {}",
                    activeInstances.size());
        }
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
        waitingQueue.clear();
    }

    /**
     * Ticks all active dungeon instances.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // event.getServer().getPlayerList().getPlayers().forEach(player -> {
        //     player.sendSystemMessage(
        //             Component.literal(String.valueOf(DungeonManager.getInstance().getActiveInstanceCount())));
        // });
        // Tick all active instances
        List<DungeonQueryInstance> toRemove = new ArrayList<>();

        for (DungeonQueryInstance instance : activeInstances) {
            instance.tick();

            // Check if dungeon has ended
            if (instance.getStatus() == DungeonStatus.IDLE) {
                toRemove.add(instance);
            }
        }

        // Remove finished instances
        for (DungeonQueryInstance instance : toRemove) {
            unregisterInstance(instance);
        }

        // If there are waiting groups and free instance slots, start next group
        // Only process waiting queue if tharidia_features is loaded (provides DungeonManager)
        if (!waitingQueue.isEmpty() && ModList.get().isLoaded("tharidia_features")) {
            int maxInstances = getMaxInstancesFromFeatures();
            while (activeInstances.size() < maxInstances && !waitingQueue.isEmpty()) {
                DungeonQueryInstance nextGroup = waitingQueue.remove(0);
                registerInstance(nextGroup);
                nextGroup.callPlayers();
                TharidiaThings.LOGGER.info("[DUNGEON] Called next group from waiting queue.");
            }
        }
    }

    /**
     * Gets max instances from DungeonManager via reflection to avoid hard dependency.
     * Returns Integer.MAX_VALUE if tharidia_features is not available.
     */
    private static int getMaxInstancesFromFeatures() {
        try {
            Class<?> dungeonManagerClass = Class.forName("com.THproject.tharidia_features.dungeon.DungeonManager");
            Object manager = dungeonManagerClass.getMethod("getInstance").invoke(null);
            return (int) dungeonManagerClass.getMethod("getMaxInstances").invoke(manager);
        } catch (Exception e) {
            TharidiaThings.LOGGER.debug("[DUNGEON] Could not get max instances from DungeonManager: {}", e.getMessage());
            return Integer.MAX_VALUE;
        }
    }
}
