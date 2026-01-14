package com.THproject.tharidia_things.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent storage for claim registry data
 * This ensures player claim counts survive server restarts
 */
public class ClaimSavedData extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimSavedData.class);
    private static final String DATA_NAME = "tharidia_claims";
    
    // Map of player UUID -> claim count
    private final Map<UUID, Integer> playerClaimCounts = new HashMap<>();
    
    // Map of dimension -> (position -> claim info)
    private final Map<String, Map<BlockPos, StoredClaimData>> storedClaims = new HashMap<>();
    
    public static class StoredClaimData {
        public final UUID ownerUUID;
        public final String claimName;
        public final long creationTime;
        public final long expirationTime;
        
        public StoredClaimData(UUID ownerUUID, String claimName, long creationTime, long expirationTime) {
            this.ownerUUID = ownerUUID;
            this.claimName = claimName;
            this.creationTime = creationTime;
            this.expirationTime = expirationTime;
        }
    }
    
    public ClaimSavedData() {
        super();
    }
    
    /**
     * Creates a new instance from NBT data
     */
    public static ClaimSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ClaimSavedData data = new ClaimSavedData();
        
        // Load player claim counts
        if (tag.contains("PlayerClaimCounts")) {
            CompoundTag countsTag = tag.getCompound("PlayerClaimCounts");
            for (String key : countsTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int count = countsTag.getInt(key);
                    data.playerClaimCounts.put(uuid, count);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid UUID in saved data: {}", key);
                }
            }
        }
        
        // Load stored claims
        if (tag.contains("StoredClaims")) {
            ListTag claimsListTag = tag.getList("StoredClaims", Tag.TAG_COMPOUND);
            for (int i = 0; i < claimsListTag.size(); i++) {
                CompoundTag claimTag = claimsListTag.getCompound(i);
                
                String dimension = claimTag.getString("Dimension");
                long posLong = claimTag.getLong("Position");
                BlockPos pos = BlockPos.of(posLong);
                
                UUID ownerUUID = null;
                if (claimTag.hasUUID("OwnerUUID")) {
                    ownerUUID = claimTag.getUUID("OwnerUUID");
                }
                
                String claimName = claimTag.getString("ClaimName");
                long creationTime = claimTag.getLong("CreationTime");
                long expirationTime = claimTag.getLong("ExpirationTime");
                
                StoredClaimData claimData = new StoredClaimData(ownerUUID, claimName, creationTime, expirationTime);
                data.storedClaims.computeIfAbsent(dimension, k -> new HashMap<>()).put(pos, claimData);
            }
        }
        
        LOGGER.info("Loaded claim data: {} players with claims, {} total stored claims", 
            data.playerClaimCounts.size(), 
            data.storedClaims.values().stream().mapToInt(Map::size).sum());
        
        return data;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        // Save player claim counts
        CompoundTag countsTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : playerClaimCounts.entrySet()) {
            countsTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("PlayerClaimCounts", countsTag);
        
        // Save stored claims
        ListTag claimsListTag = new ListTag();
        for (Map.Entry<String, Map<BlockPos, StoredClaimData>> dimensionEntry : storedClaims.entrySet()) {
            String dimension = dimensionEntry.getKey();
            for (Map.Entry<BlockPos, StoredClaimData> claimEntry : dimensionEntry.getValue().entrySet()) {
                BlockPos pos = claimEntry.getKey();
                StoredClaimData claimData = claimEntry.getValue();
                
                CompoundTag claimTag = new CompoundTag();
                claimTag.putString("Dimension", dimension);
                claimTag.putLong("Position", pos.asLong());
                if (claimData.ownerUUID != null) {
                    claimTag.putUUID("OwnerUUID", claimData.ownerUUID);
                }
                claimTag.putString("ClaimName", claimData.claimName);
                claimTag.putLong("CreationTime", claimData.creationTime);
                claimTag.putLong("ExpirationTime", claimData.expirationTime);
                
                claimsListTag.add(claimTag);
            }
        }
        tag.put("StoredClaims", claimsListTag);
        
        LOGGER.info("Saved claim data: {} players with claims, {} total stored claims", 
            playerClaimCounts.size(), 
            storedClaims.values().stream().mapToInt(Map::size).sum());
        
        return tag;
    }
    
    /**
     * Gets or creates the ClaimSavedData for the overworld
     * (We use overworld as the central storage for all dimensions)
     */
    public static ClaimSavedData get(ServerLevel level) {
        // Always use the overworld for storage
        ServerLevel overworld = level.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld == null) {
            LOGGER.error("Could not get overworld level!");
            return new ClaimSavedData();
        }
        
        return overworld.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                ClaimSavedData::new,
                ClaimSavedData::load
            ),
            DATA_NAME
        );
    }
    
    /**
     * Updates player claim count
     */
    public void setPlayerClaimCount(UUID playerUUID, int count) {
        playerClaimCounts.put(playerUUID, count);
        setDirty();
    }
    
    /**
     * Gets player claim count
     */
    public int getPlayerClaimCount(UUID playerUUID) {
        return playerClaimCounts.getOrDefault(playerUUID, 0);
    }
    
    /**
     * Stores a claim
     */
    public void storeClaim(String dimension, BlockPos pos, UUID ownerUUID, String claimName, long creationTime, long expirationTime) {
        Map<BlockPos, StoredClaimData> dimensionClaims = storedClaims.computeIfAbsent(dimension, k -> new HashMap<>());
        
        // Check if this is a new claim (not already stored)
        boolean isNewClaim = !dimensionClaims.containsKey(pos);
        
        StoredClaimData data = new StoredClaimData(ownerUUID, claimName, creationTime, expirationTime);
        dimensionClaims.put(pos, data);
        
        // Only increment player claim count if it's a new claim
        if (ownerUUID != null && isNewClaim) {
            int currentCount = getPlayerClaimCount(ownerUUID);
            setPlayerClaimCount(ownerUUID, currentCount + 1);
        }
        
        setDirty();
    }
    
    /**
     * Removes a stored claim
     */
    public void removeClaim(String dimension, BlockPos pos) {
        Map<BlockPos, StoredClaimData> dimensionClaims = storedClaims.get(dimension);
        if (dimensionClaims != null) {
            StoredClaimData removed = dimensionClaims.remove(pos);
            if (removed != null && removed.ownerUUID != null) {
                // Decrement player claim count
                int currentCount = getPlayerClaimCount(removed.ownerUUID);
                if (currentCount > 0) {
                    setPlayerClaimCount(removed.ownerUUID, currentCount - 1);
                }
            }
            
            if (dimensionClaims.isEmpty()) {
                storedClaims.remove(dimension);
            }
        }
        setDirty();
    }
    
    /**
     * Gets all stored claims
     */
    public Map<String, Map<BlockPos, StoredClaimData>> getStoredClaims() {
        return storedClaims;
    }
    
    /**
     * Clears all data (for testing)
     */
    public void clear() {
        playerClaimCounts.clear();
        storedClaims.clear();
        setDirty();
    }
}
