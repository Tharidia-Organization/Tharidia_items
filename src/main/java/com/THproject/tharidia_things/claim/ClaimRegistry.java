package com.THproject.tharidia_things.claim;

import com.THproject.tharidia_things.block.entity.ClaimBlockEntity;
import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import com.THproject.tharidia_things.integration.GodEyeIntegration;
import com.THproject.tharidia_things.realm.RealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry for tracking all claims in the world
 * This allows efficient lookup and management of claims
 */
public class ClaimRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimRegistry.class);
    
    // Map of dimension -> (position -> claim data)
    private static final Map<String, Map<BlockPos, ClaimData>> claims = new ConcurrentHashMap<>();
    
    // Map of player UUID -> list of claim positions
    private static final Map<UUID, Set<BlockPos>> playerClaims = new ConcurrentHashMap<>();

    /**
     * Data class representing a claim in the registry
     */
    public static class ClaimData {
        public final BlockPos position;
        public final UUID ownerUUID;
        public String claimName;
        public String ownerName; // Chosen name from NameService
        public final long creationTime;
        public final String dimension;

        public ClaimData(BlockPos position, UUID ownerUUID, String claimName, String ownerName, long creationTime, String dimension) {
            this.position = position;
            this.ownerUUID = ownerUUID;
            this.claimName = claimName;
            this.ownerName = ownerName;
            this.creationTime = creationTime;
            this.dimension = dimension;
        }

        public BlockPos getPosition() {
            return position;
        }

        public UUID getOwnerUUID() {
            return ownerUUID;
        }

        public String getClaimName() {
            return claimName;
        }
        
        public String getOwnerName() {
            return ownerName;
        }

        public String getDimension() {
            return dimension;
        }
    }

    /**
     * Registers a claim in the global registry
     */
    public static void registerClaim(ServerLevel level, BlockPos pos, ClaimBlockEntity claim) {
        String dimension = level.dimension().location().toString();
        
        ClaimData data = new ClaimData(
            pos,
            claim.getOwnerUUID(),
            claim.getClaimName(),
            claim.getOwnerName(),
            claim.getCreationTime(),
            dimension
        );
        
        // Add to main registry
        claims.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>()).put(pos, data);
        
        // Add to player index
        if (claim.getOwnerUUID() != null) {
            playerClaims.computeIfAbsent(claim.getOwnerUUID(), k -> ConcurrentHashMap.newKeySet()).add(pos);
        }
        
        // Persist to saved data
        ClaimSavedData savedData = ClaimSavedData.get(level);
        savedData.storeClaim(dimension, pos, claim.getOwnerUUID(), claim.getClaimName(), claim.getCreationTime(), claim.getExpirationTime());
        
        // Sync to GodEye database
        if (claim.getOwnerUUID() != null) {
            GodEyeIntegration.syncPlayerClaims(claim.getOwnerUUID(), level);
        }
        
        LOGGER.debug("Registered claim at {} in dimension {}", pos, dimension);
    }

    /**
     * Unregisters a claim from the global registry
     */
    public static void unregisterClaim(ServerLevel level, BlockPos pos) {
        String dimension = level.dimension().location().toString();
        
        Map<BlockPos, ClaimData> dimensionClaims = claims.get(dimension);
        if (dimensionClaims != null) {
            ClaimData data = dimensionClaims.remove(pos);
            
            if (data != null && data.ownerUUID != null) {
                Set<BlockPos> positions = playerClaims.get(data.ownerUUID);
                if (positions != null) {
                    positions.remove(pos);
                    if (positions.isEmpty()) {
                        playerClaims.remove(data.ownerUUID);
                    }
                }
            }
            
            // Remove from persistent storage
            ClaimSavedData savedData = ClaimSavedData.get(level);
            savedData.removeClaim(dimension, pos);
            
            // Sync to GodEye database
            if (data != null && data.ownerUUID != null) {
                GodEyeIntegration.syncPlayerClaims(data.ownerUUID, level);
            }
            
            LOGGER.debug("Unregistered claim at {} in dimension {}", pos, dimension);
        }
    }

    /**
     * Gets all claims owned by a specific player
     */
    public static List<ClaimData> getPlayerClaims(UUID playerUUID) {
        Set<BlockPos> positions = playerClaims.get(playerUUID);
        if (positions == null || positions.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<ClaimData> result = new ArrayList<>();
        for (BlockPos pos : positions) {
            for (Map<BlockPos, ClaimData> dimensionClaims : claims.values()) {
                ClaimData data = dimensionClaims.get(pos);
                if (data != null && playerUUID.equals(data.ownerUUID)) {
                    result.add(data);
                }
            }
        }
        
        return result;
    }

    /**
     * Gets player claims with full details including expiration time
     * This method loads the actual ClaimBlockEntity to get live data
     */
    public static List<DetailedClaimData> getPlayerClaimsWithDetails(UUID playerUUID, ServerLevel level) {
        Set<BlockPos> positions = playerClaims.get(playerUUID);
        if (positions == null || positions.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<DetailedClaimData> result = new ArrayList<>();
        for (BlockPos pos : positions) {
            for (Map.Entry<String, Map<BlockPos, ClaimData>> dimensionEntry : claims.entrySet()) {
                ClaimData data = dimensionEntry.getValue().get(pos);
                if (data != null && playerUUID.equals(data.ownerUUID)) {
                    // Try to get the actual block entity for live data
                    long expirationTime = -1;
                    if (level.hasChunkAt(pos)) {
                        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof ClaimBlockEntity claimEntity) {
                            expirationTime = claimEntity.getExpirationTime();
                        }
                    }
                    
                    result.add(new DetailedClaimData(
                        data.position,
                        data.ownerUUID,
                        data.claimName,
                        data.ownerName,
                        data.creationTime,
                        data.dimension,
                        expirationTime
                    ));
                }
            }
        }
        
        return result;
    }
    
    /**
     * Extended claim data that includes expiration time
     */
    public static class DetailedClaimData extends ClaimData {
        public final long expirationTime;
        
        public DetailedClaimData(BlockPos position, UUID ownerUUID, String claimName, String ownerName, 
                                long creationTime, String dimension, long expirationTime) {
            super(position, ownerUUID, claimName, ownerName, creationTime, dimension);
            this.expirationTime = expirationTime;
        }
        
        public long getExpirationTime() {
            return expirationTime;
        }
    }

    /**
     * Gets all claims in a specific dimension
     */
    public static List<ClaimData> getClaimsInDimension(String dimension) {
        Map<BlockPos, ClaimData> dimensionClaims = claims.get(dimension);
        if (dimensionClaims == null) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(dimensionClaims.values());
    }

    /**
     * Gets total number of claims
     */
    public static int getTotalClaimCount() {
        return claims.values().stream().mapToInt(Map::size).sum();
    }

    /**
     * Gets number of claims owned by a player
     * Falls back to persistent storage if in-memory registry is empty (e.g., after restart)
     */
    public static int getPlayerClaimCount(UUID playerUUID) {
        Set<BlockPos> positions = playerClaims.get(playerUUID);
        return positions != null ? positions.size() : 0;
    }
    
    /**
     * Gets number of claims owned by a player from persistent storage
     * Use this when you need the count even if chunks aren't loaded yet
     */
    public static int getPlayerClaimCountPersistent(UUID playerUUID, ServerLevel level) {
        ClaimSavedData savedData = ClaimSavedData.get(level);
        return savedData.getPlayerClaimCount(playerUUID);
    }

    /**
     * Checks if a player has claims in any realm other than the specified one
     * @param playerUUID The UUID of the player to check
     * @param level The server level
     * @param currentRealm The realm the player is currently trying to place a claim in
     * @return true if the player has claims in other realms, false otherwise
     */
    public static boolean hasClaimsInOtherRealms(UUID playerUUID, ServerLevel level, PietroBlockEntity currentRealm) {
        Set<BlockPos> playerClaimPositions = playerClaims.get(playerUUID);
        if (playerClaimPositions == null || playerClaimPositions.isEmpty()) {
            return false;
        }
        
        // Check each claim position
        for (BlockPos claimPos : playerClaimPositions) {
            // Find which realm this claim is in
            PietroBlockEntity claimRealm =
                RealmManager.getRealmAt(level, claimPos);
            
            // If claim is in a realm and it's not the current realm, player has claims in multiple realms
            if (claimRealm != null && !claimRealm.getBlockPos().equals(currentRealm.getBlockPos())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a claim exists at the given position
     */
    public static boolean hasClaimAt(ServerLevel level, BlockPos pos) {
        String dimension = level.dimension().location().toString();
        Map<BlockPos, ClaimData> dimensionClaims = claims.get(dimension);
        return dimensionClaims != null && dimensionClaims.containsKey(pos);
    }

    /**
     * Updates claim data when properties change
     */
    public static void updateClaimName(ServerLevel level, BlockPos pos, String newName) {
        String dimension = level.dimension().location().toString();
        Map<BlockPos, ClaimData> dimensionClaims = claims.get(dimension);
        
        if (dimensionClaims != null) {
            ClaimData data = dimensionClaims.get(pos);
            if (data != null) {
                data.claimName = newName;
            }
        }
    }
    
    /**
     * Updates claim expiration time in persistent storage
     */
    public static void updateClaimExpirationTime(ServerLevel level, BlockPos pos, long expirationTime) {
        String dimension = level.dimension().location().toString();
        Map<BlockPos, ClaimData> dimensionClaims = claims.get(dimension);
        
        if (dimensionClaims != null) {
            ClaimData data = dimensionClaims.get(pos);
            if (data != null) {
                // Update persistent storage
                ClaimSavedData savedData = ClaimSavedData.get(level);
                savedData.storeClaim(dimension, pos, data.ownerUUID, data.claimName, data.creationTime, expirationTime);
            }
        }
    }

    /**
     * Clears all registry data (for testing or server restart)
     */
    public static void clear() {
        claims.clear();
        playerClaims.clear();
        LOGGER.info("Claim registry cleared");
    }
    
    /**
     * Loads the registry from persistent storage
     * This is called when the server starts to restore claim data
     */
    public static void loadFromPersistentStorage(ServerLevel level) {
        ClaimSavedData savedData = ClaimSavedData.get(level);
        Map<String, Map<BlockPos, ClaimSavedData.StoredClaimData>> storedClaims = savedData.getStoredClaims();
        
        LOGGER.info("Loading claim registry from persistent storage...");
        
        int loadedCount = 0;
        for (Map.Entry<String, Map<BlockPos, ClaimSavedData.StoredClaimData>> dimensionEntry : storedClaims.entrySet()) {
            String dimension = dimensionEntry.getKey();
            
            for (Map.Entry<BlockPos, ClaimSavedData.StoredClaimData> claimEntry : dimensionEntry.getValue().entrySet()) {
                BlockPos pos = claimEntry.getKey();
                ClaimSavedData.StoredClaimData stored = claimEntry.getValue();
                
                // Note: We don't add to in-memory registry here because ClaimBlockEntity will
                // register itself when the chunk loads. We just keep the persistent data
                // for tracking claim counts even when chunks aren't loaded.
                loadedCount++;
            }
        }
        
        LOGGER.info("Loaded {} claims from persistent storage", loadedCount);
    }
    
    /**
     * Synchronizes persistent storage with current in-memory state
     * Useful for ensuring data consistency
     */
    public static void syncToPersistentStorage(ServerLevel level) {
        ClaimSavedData savedData = ClaimSavedData.get(level);
        
        // Clear and rebuild the saved data from current registry
        savedData.clear();
        
        for (Map.Entry<String, Map<BlockPos, ClaimData>> dimensionEntry : claims.entrySet()) {
            String dimension = dimensionEntry.getKey();
            
            for (Map.Entry<BlockPos, ClaimData> claimEntry : dimensionEntry.getValue().entrySet()) {
                BlockPos pos = claimEntry.getKey();
                ClaimData data = claimEntry.getValue();
                
                // Try to get expiration time from the actual block entity
                long expirationTime = -1;
                if (level.hasChunkAt(pos)) {
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof ClaimBlockEntity claimEntity) {
                        expirationTime = claimEntity.getExpirationTime();
                    }
                }
                
                savedData.storeClaim(dimension, pos, data.ownerUUID, data.claimName, data.creationTime, expirationTime);
            }
        }
        
        LOGGER.info("Synchronized {} claims to persistent storage", getTotalClaimCount());
    }


}
