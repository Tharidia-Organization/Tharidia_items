package com.tharidia.tharidia_things.client;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/**
 * Handles client connection events to properly clear synced realm data
 */
public class ClientConnectionHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track if we're in the middle of logging in to avoid clearing data prematurely
    private static boolean isLoggingIn = false;
    
    // Track boundary visibility state across logout/login (made public for ClientPacketHandler)
    public static boolean boundariesWereVisible = false;
    
    /**
     * Called when client starts logging in
     */
    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        isLoggingIn = true;
        // Save boundary visibility state before clearing
        boundariesWereVisible = RealmBoundaryRenderer.areBoundariesVisible();
        LOGGER.info("[REALM SYNC] Client logging in - boundaries were visible: {}", boundariesWereVisible);
        if (com.tharidia.tharidia_things.Config.DEBUG_REALM_SYNC.get()) {
            LOGGER.info("[DEBUG] Setting isLoggingIn = true, clearing old realm data");
        }
        // Clear old data in preparation for new sync
        ClientPacketHandler.syncedRealms.clear();
        RealmClientHandler.reset();
    }
    
    /**
     * Clears synced realm data when the client disconnects from a server
     */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        LOGGER.info("[REALM SYNC] Client disconnecting - clearing synced realm data");
        if (com.tharidia.tharidia_things.Config.DEBUG_REALM_SYNC.get()) {
            LOGGER.info("[DEBUG] Clearing {} synced realms on disconnect", ClientPacketHandler.syncedRealms.size());
        }
        isLoggingIn = false;
        ClientPacketHandler.syncedRealms.clear();
        RealmClientHandler.reset();
    }
    
    /**
     * Clears synced realm data when changing dimensions
     * Don't clear if we're in the middle of logging in
     */
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().level().isClientSide) {
            // Don't clear during login process
            if (isLoggingIn) {
                LOGGER.info("[REALM SYNC] Dimension change during login - NOT clearing realm data (boundariesWereVisible: {})", boundariesWereVisible);
                if (com.tharidia.tharidia_things.Config.DEBUG_REALM_SYNC.get()) {
                    LOGGER.info("[DEBUG] Login complete, {} realms in memory", ClientPacketHandler.syncedRealms.size());
                }
                isLoggingIn = false; // Login complete
                
                // Visibility restoration is now handled in ClientPacketHandler after sync completes
                // This ensures the realm data is available when we try to show boundaries
                LOGGER.info("[REALM SYNC] Visibility will be restored in ClientPacketHandler if boundariesWereVisible={}", boundariesWereVisible);
                return;
            }
            
            LOGGER.info("[REALM SYNC] Player changed dimension on client - clearing synced realm data");
            if (com.tharidia.tharidia_things.Config.DEBUG_REALM_SYNC.get()) {
                LOGGER.info("[DEBUG] Clearing {} realms for dimension change", ClientPacketHandler.syncedRealms.size());
            }
            ClientPacketHandler.syncedRealms.clear();
            RealmClientHandler.reset();
        }
    }
    
    /**
     * Clears synced realm data when respawning
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity().level().isClientSide) {
            LOGGER.info("Player respawned on client - resetting realm handler");
            RealmClientHandler.reset();
        }
    }
}
