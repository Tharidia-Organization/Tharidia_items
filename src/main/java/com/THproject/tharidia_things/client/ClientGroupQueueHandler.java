package com.THproject.tharidia_things.client;

import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side handler for group queue state.
 * Receives sync packets from the server and stores the current queue state.
 */
public class ClientGroupQueueHandler {

    // Map of Pietro block positions to their current queue data
    private static final Map<BlockPos, QueueData> queueDataMap = new ConcurrentHashMap<>();

    // The current Pietro position the player is viewing (set when opening PietroScreen)
    private static BlockPos currentPietroPos = null;

    /**
     * Stores queue data for a Pietro block.
     */
    public static class QueueData {
        public final List<UUID> playerUUIDs;
        public final UUID leaderUUID;
        public final long lastUpdated;

        public QueueData(List<UUID> players, UUID leader) {
            this.playerUUIDs = new ArrayList<>(players);
            this.leaderUUID = leader;
            this.lastUpdated = System.currentTimeMillis();
        }

        public boolean hasPlayers() {
            return !playerUUIDs.isEmpty();
        }

        public int getPlayerCount() {
            return playerUUIDs.size();
        }

        public boolean isLeader(UUID uuid) {
            return leaderUUID != null && leaderUUID.equals(uuid);
        }

        public boolean containsPlayer(UUID uuid) {
            return playerUUIDs.contains(uuid);
        }
    }

    /**
     * Called when receiving a sync packet from the server.
     */
    public static void handleSync(BlockPos pos, List<UUID> playerUUIDs, UUID leaderUUID) {
        if (playerUUIDs.isEmpty()) {
            // Queue was disbanded
            queueDataMap.remove(pos);
        } else {
            queueDataMap.put(pos, new QueueData(playerUUIDs, leaderUUID));
        }
    }

    /**
     * Sets the current Pietro position being viewed.
     */
    public static void setCurrentPietroPos(BlockPos pos) {
        currentPietroPos = pos;
    }

    /**
     * Clears the current Pietro position.
     */
    public static void clearCurrentPietroPos() {
        currentPietroPos = null;
    }

    /**
     * Gets the current Pietro position.
     */
    public static BlockPos getCurrentPietroPos() {
        return currentPietroPos;
    }

    /**
     * Gets queue data for the specified position.
     */
    public static QueueData getQueueData(BlockPos pos) {
        return queueDataMap.get(pos);
    }

    /**
     * Gets queue data for the current Pietro position.
     */
    public static QueueData getCurrentQueueData() {
        if (currentPietroPos == null) return null;
        return queueDataMap.get(currentPietroPos);
    }

    /**
     * Checks if there's an active queue at the position.
     */
    public static boolean hasActiveQueue(BlockPos pos) {
        QueueData data = queueDataMap.get(pos);
        return data != null && data.hasPlayers();
    }

    /**
     * Gets all positions with active queues.
     */
    public static Set<BlockPos> getActiveQueuePositions() {
        Set<BlockPos> positions = new HashSet<>();
        for (Map.Entry<BlockPos, QueueData> entry : queueDataMap.entrySet()) {
            if (entry.getValue().hasPlayers()) {
                positions.add(entry.getKey());
            }
        }
        return positions;
    }

    /**
     * Clears all cached queue data.
     */
    public static void clearAll() {
        queueDataMap.clear();
        currentPietroPos = null;
    }

    /**
     * Checks if the local player is in the queue at the given position.
     */
    public static boolean isLocalPlayerInQueue(BlockPos pos, UUID localPlayerUUID) {
        QueueData data = queueDataMap.get(pos);
        return data != null && data.containsPlayer(localPlayerUUID);
    }

    /**
     * Checks if the local player is the leader of the queue at the given position.
     */
    public static boolean isLocalPlayerLeader(BlockPos pos, UUID localPlayerUUID) {
        QueueData data = queueDataMap.get(pos);
        return data != null && data.isLeader(localPlayerUUID);
    }
}
