package com.tharidia.tharidia_things.lobby;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.chat.Component;
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
    private final Map<UUID, ServerBossEvent> bossBars = new ConcurrentHashMap<>();
    
    private boolean queueEnabled = true;
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
        logger.info("[DEBUG] addToQueue called for player: {}, already in queue: {}", player.getName().getString(), queue.contains(uuid));
        if (!queue.contains(uuid)) {
            queue.add(uuid);
            queueJoinTime.put(uuid, System.currentTimeMillis());
            logger.info("[DEBUG] Player added to queue, calling updateQueuePosition and createOrUpdateBossBar");
            updateQueuePosition(player);
            createOrUpdateBossBar(player);
            logger.info("Added {} to queue. Position: {}", player.getName().getString(), getQueuePosition(uuid));
        }
    }
    
    /**
     * Remove a player from the queue
     */
    public void removeFromQueue(UUID uuid) {
        queue.remove(uuid);
        queueJoinTime.remove(uuid);
        removeBossBar(uuid);
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
            removeBossBar(uuid);
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
        // Remove all boss bars
        for (UUID uuid : new ArrayList<>(queue)) {
            removeBossBar(uuid);
        }
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
            updateBossBar(player, position);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§e§l[QUEUE] §7You are in position §6#" + position + "§7 in the queue"
            ));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§7Please wait while we process your connection..."
            ));
        } else {
            removeBossBar(player.getUUID());
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
    
    /**
     * Update all boss bars for players in queue (call this when queue order changes)
     */
    public void updateAllBossBars(net.minecraft.server.MinecraftServer server) {
        for (UUID uuid : queue) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                int position = getQueuePosition(uuid);
                updateBossBar(player, position);
            }
        }
    }
    
    /**
     * Create or update the boss bar for a player showing their queue position
     */
    private void createOrUpdateBossBar(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int position = getQueuePosition(uuid);
        int queueSize = getQueueSize();
        
        ServerBossEvent bossBar = bossBars.get(uuid);
        if (bossBar == null) {
            bossBar = new ServerBossEvent(
                Component.literal("§6Posizione in coda: #" + position + "/" + queueSize),
                BossEvent.BossBarColor.YELLOW,
                BossEvent.BossBarOverlay.PROGRESS
            );
            bossBar.setCreateWorldFog(false);
            bossBar.setVisible(true);
            bossBars.put(uuid, bossBar);
            bossBar.addPlayer(player);
            logger.info("[BossBar] Created for {} at position {}/{}", player.getName().getString(), position, queueSize);
        } else {
            bossBar.setName(Component.literal("§6Posizione in coda: #" + position + "/" + queueSize));
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
            logger.info("[BossBar] Updated for {} at position {}/{}", player.getName().getString(), position, queueSize);
        }
        bossBar.setProgress(1.0F);
    }
    
    /**
     * Update the boss bar for a player with new position
     */
    private void updateBossBar(ServerPlayer player, int position) {
        UUID uuid = player.getUUID();
        int queueSize = getQueueSize();
        
        ServerBossEvent bossBar = bossBars.get(uuid);
        if (bossBar == null) {
            createOrUpdateBossBar(player);
        } else {
            bossBar.setName(Component.literal("§6Queue Position: #" + position + "/" + queueSize));
            bossBar.setProgress(1.0F);
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
                logger.info("[BossBar] Re-added player {} to existing boss bar", player.getName().getString());
            }
        }
    }
    
    /**
     * Remove the boss bar for a player
     */
    private void removeBossBar(UUID uuid) {
        ServerBossEvent bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar.setVisible(false);
            logger.info("[BossBar] Removed for player {}", uuid);
        }
    }
}
