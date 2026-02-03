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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    // Executor for async database operations - prevents blocking server thread
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GodEye-Sync-Thread");
        t.setDaemon(true); // Allow JVM to exit even if this thread is running
        return t;
    });

    // Timeout for database operations in milliseconds
    private static final long SYNC_TIMEOUT_MS = 5000;
    
    /**
     * Initialize the integration with Tharidia Features
     */
    private static void initialize() {
        if (initialized || initializationFailed) {
            return;
        }
        
        try {
            // Get the GodEyeDatabase instance from Tharidia Features
            Class<?> mainClass = Class.forName("com.THproject.tharidia_features.main");
            Method getGodEyeDatabaseMethod = mainClass.getMethod("getGodEyeDatabase");
            godEyeDatabase = getGodEyeDatabaseMethod.invoke(null);
            
            if (godEyeDatabase != null) {
                Class<?> godEyeDatabaseClass = Class.forName("com.THproject.tharidia_features.database.GodEyeDatabase");
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
     *
     * IMPORTANT: This method runs asynchronously to prevent blocking the server thread
     * during world save operations.
     */
    public static void syncPlayerClaims(UUID playerUUID, ServerLevel level) {
        if (!initialized && !initializationFailed) {
            initialize();
        }

        if (!initialized || godEyeDatabase == null || updatePlayerClaimsMethod == null) {
            return;
        }

        // Collect claim data on the main thread (fast operation)
        final List<ClaimRegistry.DetailedClaimData> claims;
        try {
            claims = ClaimRegistry.getPlayerClaimsWithDetails(playerUUID, level);
            if (claims == null || claims.isEmpty()) {
                return;
            }
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed to collect claim data for player {}", playerUUID, e);
            return;
        }

        // Build JSON on main thread (fast, CPU-only operation)
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

        final String claimPositionsJson = GSON.toJson(claimsArray);
        final int claimCount = claims.size();

        // Run database update asynchronously with timeout
        CompletableFuture.runAsync(() -> {
            try {
                updatePlayerClaimsMethod.invoke(godEyeDatabase, playerUUID, claimCount, claimPositionsJson);
                TharidiaThings.LOGGER.debug("Synced {} claims to GodEye database for player {}", claimCount, playerUUID);
            } catch (Exception e) {
                TharidiaThings.LOGGER.error("Failed to sync player claims to GodEye database", e);
            }
        }, executor).orTimeout(SYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .exceptionally(throwable -> {
            if (throwable instanceof java.util.concurrent.TimeoutException) {
                TharidiaThings.LOGGER.warn("GodEye sync timed out for player {} after {}ms", playerUUID, SYNC_TIMEOUT_MS);
            } else {
                TharidiaThings.LOGGER.error("GodEye sync failed for player {}", playerUUID, throwable);
            }
            return null;
        });
    }

    /**
     * Synchronous version for shutdown - blocks until complete or timeout
     * Use only during server shutdown when async isn't appropriate
     */
    public static void syncPlayerClaimsBlocking(UUID playerUUID, ServerLevel level) {
        if (!initialized || godEyeDatabase == null || updatePlayerClaimsMethod == null) {
            return;
        }

        try {
            List<ClaimRegistry.DetailedClaimData> claims = ClaimRegistry.getPlayerClaimsWithDetails(playerUUID, level);
            if (claims == null || claims.isEmpty()) {
                return;
            }

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
            updatePlayerClaimsMethod.invoke(godEyeDatabase, playerUUID, claims.size(), claimPositionsJson);
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Failed blocking sync for player {}", playerUUID, e);
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

    /**
     * Shutdown the executor service - call this during server stop
     * Waits up to 3 seconds for pending tasks to complete
     */
    public static void shutdown() {
        TharidiaThings.LOGGER.info("Shutting down GodEye sync executor...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                TharidiaThings.LOGGER.warn("GodEye executor did not terminate gracefully, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        TharidiaThings.LOGGER.info("GodEye sync executor shutdown complete");
    }
}
