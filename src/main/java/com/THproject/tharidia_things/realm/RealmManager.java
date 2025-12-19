package com.THproject.tharidia_things.realm;

import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all realms in the world
 * This class can be used to check if positions/chunks are in any realm
 * and to get realm information for gameplay mechanics
 */
public class RealmManager {
    private static final Map<ServerLevel, List<PietroBlockEntity>> realms = new HashMap<>();
    
    /**
     * Registers a realm (Pietro block) in the manager
     */
    public static void registerRealm(ServerLevel level, PietroBlockEntity realm) {
        realms.computeIfAbsent(level, k -> new ArrayList<>()).add(realm);
    }
    
    /**
     * Unregisters a realm when the block is broken
     */
    public static void unregisterRealm(ServerLevel level, PietroBlockEntity realm) {
        List<PietroBlockEntity> levelRealms = realms.get(level);
        if (levelRealms != null) {
            levelRealms.remove(realm);
        }
    }
    
    /**
     * Gets all realms in a level
     */
    public static List<PietroBlockEntity> getRealms(ServerLevel level) {
        return realms.getOrDefault(level, new ArrayList<>());
    }
    
    /**
     * Checks if a position is within any realm in the level
     * @return The realm block entity if position is in a realm, null otherwise
     */
    public static PietroBlockEntity getRealmAt(ServerLevel level, BlockPos pos) {
        List<PietroBlockEntity> levelRealms = getRealms(level);
        for (PietroBlockEntity realm : levelRealms) {
            if (realm.isPositionInRealm(pos)) {
                return realm;
            }
        }
        return null;
    }
    
    /**
     * Checks if a chunk is within any realm in the level
     * @return The realm block entity if chunk is in a realm, null otherwise
     */
    public static PietroBlockEntity getRealmAtChunk(ServerLevel level, ChunkPos chunkPos) {
        List<PietroBlockEntity> levelRealms = getRealms(level);
        for (PietroBlockEntity realm : levelRealms) {
            if (realm.isChunkInRealm(chunkPos)) {
                return realm;
            }
        }
        return null;
    }
    
    /**
     * Gets all realms that contain the given position
     * (in case of overlapping realms)
     */
    public static List<PietroBlockEntity> getAllRealmsAt(ServerLevel level, BlockPos pos) {
        List<PietroBlockEntity> result = new ArrayList<>();
        List<PietroBlockEntity> levelRealms = getRealms(level);
        for (PietroBlockEntity realm : levelRealms) {
            if (realm.isPositionInRealm(pos)) {
                result.add(realm);
            }
        }
        return result;
    }
    
    /**
     * Clears all realms for a level (useful for cleanup)
     */
    public static void clearLevel(ServerLevel level) {
        realms.remove(level);
    }
    
    /**
     * Checks if a player already owns a realm in the given level
     * @param level The server level to check
     * @param playerUUID The UUID of the player to check
     * @return true if the player already owns a realm, false otherwise
     */
    public static boolean playerOwnsRealm(ServerLevel level, UUID playerUUID) {
        List<PietroBlockEntity> levelRealms = getRealms(level);
        for (PietroBlockEntity realm : levelRealms) {
            if (playerUUID.equals(realm.getOwnerUUID())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Refreshes the realm list for a level by scanning for Pietro blocks
     * This can be called on world load to rebuild the realm registry
     */
    public static void refreshRealms(ServerLevel level) {
        // Clear existing realms for this level
        clearLevel(level);
        
        // Note: This is a placeholder for future implementation
        // You would need to iterate through loaded chunks and find Pietro blocks
        // For now, realms will be registered as chunks load and blocks are accessed
    }
}
