package com.tharidia.tharidia_things.client;

import com.mojang.logging.LogUtils;
import com.tharidia.tharidia_things.client.gui.PreLoginNameScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/**
 * Handles client connection events to properly clear synced realm data
 * and manage pre-login name selection
 */
public class ClientConnectionHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Track if we're in the middle of logging in to avoid clearing data prematurely
    private static boolean isLoggingIn = false;
    
    // Track boundary visibility state across logout/login (made public for ClientPacketHandler)
    public static boolean boundariesWereVisible = false;
    
    // Track if we're waiting for name selection
    private static boolean waitingForNameSelection = false;
    
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
        waitingForNameSelection = false;
        ClientPacketHandler.syncedRealms.clear();
        ClientPacketHandler.clearHierarchyCache();
        RealmClientHandler.reset();
        // Clear gate restriction cache
        ClientGateCache.clear();
        LOGGER.info("[GATE CACHE] Cleared gate restrictions cache on disconnect");
    }
    
    /**
     * Called when server requests name selection
     */
    public static void handleNameRequest(boolean needsName) {
        if (needsName && !waitingForNameSelection) {
            waitingForNameSelection = true;
            LOGGER.info("[NAME SELECTION] Server requires name selection - showing pre-login screen");
            
            // Show the pre-login name selection screen
            Minecraft.getInstance().execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new PreLoginNameScreen());
            });
        } else if (!needsName) {
            LOGGER.info("[NAME SELECTION] Player already has a name, proceeding with login");
            waitingForNameSelection = false;
        }
    }
    
    /**
     * Called when name has been submitted successfully
     */
    public static void onNameSubmitted() {
        waitingForNameSelection = false;
        LOGGER.info("[NAME SELECTION] Name submitted, continuing login");
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
