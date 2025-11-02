package com.tharidia.tharidia_things.lobby;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages the queue system for players waiting to join the main server via Velocity
 */
public class QueueManager {
    
    private final Logger logger;
    private final Queue<UUID> queue = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Long> queueJoinTime = new ConcurrentHashMap<>();
    
    private boolean queueEnabled = false;
    private boolean autoTransfer = false;
    private int maxMainServerPlayers = 100;
    
    public QueueManager(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Add a player to the queue
     */
    public void addToQueue(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!queue.contains(uuid)) {
            queue.add(uuid);
            queueJoinTime.put(uuid, System.currentTimeMillis());
            updateQueuePosition(player);
            logger.info("Added {} to queue. Position: {}", player.getName().getString(), getQueuePosition(uuid));
        }
    }
    
    /**
     * Remove a player from the queue
     */
    public void removeFromQueue(UUID uuid) {
        queue.remove(uuid);
        queueJoinTime.remove(uuid);
    }
    
    /**
     * Get the position of a player in the queue (1-indexed)
     */
    public int getQueuePosition(UUID uuid) {
        int position = 1;
        for (UUID queuedUuid : queue) {
            if (queuedUuid.equals(uuid)) {
                return position;
            }
            position++;
        }
        return -1;
    }
    
    /**
     * Get the next player in queue
     */
    public UUID pollNextInQueue() {
        UUID uuid = queue.poll();
        if (uuid != null) {
            queueJoinTime.remove(uuid);
        }
        return uuid;
    }
    
    /**
     * Peek at the next player without removing
     */
    public UUID peekNextInQueue() {
        return queue.peek();
    }
    
    /**
     * Check if queue is empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    /**
     * Get queue size
     */
    public int getQueueSize() {
        return queue.size();
    }
    
    /**
     * Clear the entire queue
     */
    public void clearQueue() {
        queue.clear();
        queueJoinTime.clear();
        logger.info("Queue cleared");
    }
    
    /**
     * Update queue position message for a player
     */
    public void updateQueuePosition(ServerPlayer player) {
        int position = getQueuePosition(player.getUUID());
        if (position > 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§e§l[QUEUE] §7You are in position §6#" + position + "§7 in the queue"
            ));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§7Please wait while we process your connection..."
            ));
        }
    }
    
    /**
     * Get wait time for a player in seconds
     */
    public long getWaitTime(UUID uuid) {
        Long joinTime = queueJoinTime.get(uuid);
        if (joinTime == null) return 0;
        return (System.currentTimeMillis() - joinTime) / 1000;
    }
    
    /**
     * Check if a player is in queue
     */
    public boolean isInQueue(UUID uuid) {
        return queue.contains(uuid);
    }
    
    // Getters and setters
    
    public boolean isQueueEnabled() {
        return queueEnabled;
    }
    
    public void setQueueEnabled(boolean enabled) {
        this.queueEnabled = enabled;
        logger.info("Queue system {}", enabled ? "enabled" : "disabled");
    }
    
    public boolean isAutoTransfer() {
        return autoTransfer;
    }
    
    public void setAutoTransfer(boolean autoTransfer) {
        this.autoTransfer = autoTransfer;
        logger.info("Auto-transfer {}", autoTransfer ? "enabled" : "disabled");
    }
    
    public int getMaxMainServerPlayers() {
        return maxMainServerPlayers;
    }
    
    public void setMaxMainServerPlayers(int max) {
        this.maxMainServerPlayers = max;
        logger.info("Max main server players set to: {}", max);
    }
    
    /**
     * Get all players in queue
     */
    public List<UUID> getQueuedPlayers() {
        return new ArrayList<>(queue);
    }
}
