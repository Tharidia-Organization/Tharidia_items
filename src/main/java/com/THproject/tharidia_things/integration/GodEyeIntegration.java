package com.THproject.tharidia_things.integration;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.claim.ClaimRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Integration with Tharidia Features GodEye database
 * Syncs claim data when claims are created, updated, or removed
 */
public class GodEyeIntegration {
    private static final Gson GSON = new Gson();
    private static Object godEyeDatabase = null;
    private static Method updatePlayerClaimsMethod = null;
    private static boolean initialized = false;
    private static boolean initializationFailed = false;
    
    /**
     * Initialize the integration with Tharidia Features
     */
    private static void initialize() {
        if (initialized || initializationFailed) {
            return;
        }
        
        try {
            // Get the GodEyeDatabase instance from Tharidia Features
            Class<?> mainClass = Class.forName("com.lucab.tharidia_features.main");
            Method getGodEyeDatabaseMethod = mainClass.getMethod("getGodEyeDatabase");
            godEyeDatabase = getGodEyeDatabaseMethod.invoke(null);
            
            if (godEyeDatabase != null) {
                Class<?> godEyeDatabaseClass = Class.forName("com.lucab.tharidia_features.database.GodEyeDatabase");
                updatePlayerClaimsMethod = godEyeDatabaseClass.getMethod("updatePlayerClaims", UUID.class, int.class, String.class);
                initialized = true;
                TharidiaThings.LOGGER.info("GodEye integration initialized successfully");
            } else {
                TharidiaThings.LOGGER.warn("GodEye database not available yet");
            }
        } catch (ClassNotFoundException e) {
            TharidiaThings.LOGGER.info("Tharidia Features mod not found, GodEye integration disabled");
            initializationFailed = true;
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to initialize GodEye integration", e);
            initializationFailed = true;
        }
    }
    
    /**
     * Sync a player's claim data to the GodEye database
     * Called when claims are created, updated, or removed
     */
    public static void syncPlayerClaims(UUID playerUUID, ServerLevel level) {
        if (!initialized && !initializationFailed) {
            initialize();
        }
        
        if (!initialized || godEyeDatabase == null || updatePlayerClaimsMethod == null) {
            return;
        }
        
        try {
            // Get detailed claim data for this player (includes expiration time)
            List<ClaimRegistry.DetailedClaimData> claims = ClaimRegistry.getPlayerClaimsWithDetails(playerUUID, level);
            
            if (claims == null) {
                return;
            }
            
            // Build JSON array with all claim data including expiration time
            JsonArray claimsArray = new JsonArray();
            
            for (ClaimRegistry.DetailedClaimData claim : claims) {
                BlockPos position = claim.getPosition();
                
                JsonObject claimObj = new JsonObject();
                claimObj.addProperty("x", position.getX());
                claimObj.addProperty("y", position.getY());
                claimObj.addProperty("z", position.getZ());
                claimObj.addProperty("dimension", claim.getDimension());
                claimObj.addProperty("name", claim.getClaimName());
                claimObj.addProperty("expirationTime", claim.getExpirationTime());
                
                claimsArray.add(claimObj);
            }
            
            String claimPositionsJson = GSON.toJson(claimsArray);
            
            // Update the database
            updatePlayerClaimsMethod.invoke(godEyeDatabase, playerUUID, claims.size(), claimPositionsJson);
            
            TharidiaThings.LOGGER.debug("Synced {} claims to GodEye database for player {}", claims.size(), playerUUID);
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to sync player claims to GodEye database", e);
        }
    }
    
    /**
     * Force re-initialization (useful after server restart or mod reload)
     */
    public static void reset() {
        initialized = false;
        initializationFailed = false;
        godEyeDatabase = null;
        updatePlayerClaimsMethod = null;
    }
}
