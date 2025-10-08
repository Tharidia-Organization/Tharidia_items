package com.tharidia.tharidia_things.claim;

import com.tharidia.tharidia_things.block.entity.ClaimBlockEntity;
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
     * Registers a claim in the global registry
     */
    public static void registerClaim(ServerLevel level, BlockPos pos, ClaimBlockEntity claim) {
        String dimension = level.dimension().location().toString();
        
        ClaimData data = new ClaimData(
            pos,
            claim.getOwnerUUID(),
            claim.getClaimName(),
            claim.getCreationTime(),
            dimension
        );
        
        // Add to main registry
        claims.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>()).put(pos, data);
        
        // Add to player index
        if (claim.getOwnerUUID() != null) {
            playerClaims.computeIfAbsent(claim.getOwnerUUID(), k -> ConcurrentHashMap.newKeySet()).add(pos);
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
     */
    public static int getPlayerClaimCount(UUID playerUUID) {
        Set<BlockPos> positions = playerClaims.get(playerUUID);
        return positions != null ? positions.size() : 0;
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
     * Clears all registry data (for testing or server restart)
     */
    public static void clear() {
        claims.clear();
        playerClaims.clear();
        LOGGER.info("Claim registry cleared");
    }

    /**
     * Data class representing a claim in the registry
     */
    public static class ClaimData {
        public final BlockPos position;
        public final UUID ownerUUID;
        public String claimName;
        public final long creationTime;
        public final String dimension;
        
        public ClaimData(BlockPos position, UUID ownerUUID, String claimName, long creationTime, String dimension) {
            this.position = position;
            this.ownerUUID = ownerUUID;
            this.claimName = claimName;
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
        
        public String getDimension() {
            return dimension;
        }
    }
}
