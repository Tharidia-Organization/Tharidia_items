package com.THproject.tharidia_things.dungeon_query;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side manager for group dungeon queues.
 * Each Pietro block can have one active group queue at a time.
 */
public class GroupDungeonQueueManager {

    private static final Map<BlockPos, GroupQueue> activeQueues = new ConcurrentHashMap<>();
    private static final int MAX_PLAYERS_PER_QUEUE = 10;

    /**
     * Represents a group queue for a specific Pietro block
     */
    public static class GroupQueue {
        private final BlockPos pietroPos;
        private final UUID leaderUUID;
        private final List<UUID> players = new ArrayList<>();
        private final long createdAt;

        public GroupQueue(BlockPos pos, UUID leader) {
            this.pietroPos = pos;
            this.leaderUUID = leader;
            this.players.add(leader);
            this.createdAt = System.currentTimeMillis();
        }

        public BlockPos getPosition() {
            return pietroPos;
        }

        public UUID getLeaderUUID() {
            return leaderUUID;
        }

        public List<UUID> getPlayers() {
            return Collections.unmodifiableList(players);
        }

        public int getPlayerCount() {
            return players.size();
        }

        public boolean isFull() {
            return players.size() >= MAX_PLAYERS_PER_QUEUE;
        }

        public boolean containsPlayer(UUID uuid) {
            return players.contains(uuid);
        }

        public boolean addPlayer(UUID uuid) {
            if (isFull() || containsPlayer(uuid)) {
                return false;
            }
            players.add(uuid);
            return true;
        }

        public boolean removePlayer(UUID uuid) {
            return players.remove(uuid);
        }

        public boolean isLeader(UUID uuid) {
            return leaderUUID.equals(uuid);
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }

    /**
     * Creates a new group queue for the given Pietro block position.
     * If a queue already exists, the player joins it instead.
     *
     * @param pos The Pietro block position
     * @param player The player creating/joining the queue
     * @return true if successfully created or joined
     */
    public static boolean createOrJoinQueue(BlockPos pos, ServerPlayer player) {
        UUID playerUUID = player.getUUID();

        // Check if player is already in any queue
        for (GroupQueue queue : activeQueues.values()) {
            if (queue.containsPlayer(playerUUID)) {
                // Player already in a queue
                return false;
            }
        }

        GroupQueue existingQueue = activeQueues.get(pos);
        if (existingQueue != null) {
            // Join existing queue
            return existingQueue.addPlayer(playerUUID);
        } else {
            // Create new queue
            GroupQueue newQueue = new GroupQueue(pos, playerUUID);
            activeQueues.put(pos, newQueue);
            return true;
        }
    }

    /**
     * Gets the queue at the given position, if any.
     */
    public static GroupQueue getQueue(BlockPos pos) {
        return activeQueues.get(pos);
    }

    /**
     * Gets the queue a player is currently in, if any.
     */
    public static GroupQueue getPlayerQueue(UUID playerUUID) {
        for (GroupQueue queue : activeQueues.values()) {
            if (queue.containsPlayer(playerUUID)) {
                return queue;
            }
        }
        return null;
    }

    /**
     * Removes a player from their current queue.
     * If the player was the leader and there are other players,
     * the next player becomes the leader (queue is disbanded).
     *
     * @return true if the player was removed from a queue
     */
    public static boolean leaveQueue(UUID playerUUID) {
        GroupQueue queue = getPlayerQueue(playerUUID);
        if (queue == null) {
            return false;
        }

        queue.removePlayer(playerUUID);

        // If queue is empty, remove it
        if (queue.getPlayerCount() == 0) {
            activeQueues.remove(queue.getPosition());
        }
        // If leader left, disband the queue
        else if (queue.isLeader(playerUUID)) {
            activeQueues.remove(queue.getPosition());
        }

        return true;
    }

    /**
     * Removes a queue entirely.
     */
    public static void removeQueue(BlockPos pos) {
        activeQueues.remove(pos);
    }

    /**
     * Checks if a queue exists at the given position.
     */
    public static boolean hasQueue(BlockPos pos) {
        return activeQueues.containsKey(pos);
    }

    /**
     * Gets all players in the queue at the given position.
     */
    public static List<UUID> getQueuePlayers(BlockPos pos) {
        GroupQueue queue = activeQueues.get(pos);
        if (queue != null) {
            return queue.getPlayers();
        }
        return Collections.emptyList();
    }

    /**
     * Starts the dungeon for all players in the queue.
     * Only the leader can start the dungeon.
     *
     * @param pos The Pietro block position
     * @param requestingPlayer The player requesting to start
     * @param level The server level
     * @return List of players to teleport, or empty list if failed
     */
    public static List<ServerPlayer> startDungeon(BlockPos pos, ServerPlayer requestingPlayer, ServerLevel level) {
        GroupQueue queue = activeQueues.get(pos);
        if (queue == null) {
            return Collections.emptyList();
        }

        // Only leader can start
        if (!queue.isLeader(requestingPlayer.getUUID())) {
            return Collections.emptyList();
        }

        // Gather all online players
        List<ServerPlayer> playersToTeleport = new ArrayList<>();
        for (UUID uuid : queue.getPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                playersToTeleport.add(player);
            }
        }

        // Remove the queue
        activeQueues.remove(pos);

        return playersToTeleport;
    }

    /**
     * Clears all queues (for server shutdown, etc.)
     */
    public static void clearAllQueues() {
        activeQueues.clear();
    }

    /**
     * Gets the count of active queues.
     */
    public static int getActiveQueueCount() {
        return activeQueues.size();
    }
}
