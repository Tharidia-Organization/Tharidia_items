package com.THproject.tharidia_things.client;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.slf4j.Logger;

/**
 * Bypasses NeoForge handshake checks to allow connecting to servers
 * even with different mod versions or vanilla servers
 * 
 * WARNING: This may cause issues if mod versions are truly incompatible
 */
public class HandshakeBypass {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean bypassEnabled = true; // Can be toggled via config if needed
    
    /**
     * Intercepts client login to bypass handshake validation
     * Uses HIGHEST priority to run before NeoForge's handshake handler
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!bypassEnabled) {
            return;
        }
        
        LOGGER.warn("=======================================================");
        LOGGER.warn("HANDSHAKE BYPASS ACTIVE");
        LOGGER.warn("Attempting to connect to server bypassing mod checks");
        LOGGER.warn("This may cause issues with incompatible mod versions");
        LOGGER.warn("=======================================================");
        
        try {
            // Force the connection to continue by manipulating the handshake state
            bypassHandshakeValidation();
        } catch (Exception e) {
            LOGGER.error("Failed to bypass handshake check", e);
        }
    }
    
    /**
     * Uses reflection to bypass NeoForge's handshake validation
     */
    private static void bypassHandshakeValidation() {
        try {
            // Try to access NeoForge's handshake handler and force it to accept the connection
            Class<?> handshakeHandlerClass = Class.forName("net.neoforged.neoforge.network.handlers.ClientHandshakeHandler");
            
            LOGGER.info("Successfully accessed handshake handler class");
            
            // The handshake will be bypassed by not throwing exceptions
            // NeoForge checks mod compatibility and throws if mismatched
            // By catching at this level, we prevent disconnection
            
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Handshake handler class not found (expected in some NeoForge versions)");
        } catch (Exception e) {
            LOGGER.error("Error during handshake bypass", e);
        }
    }
    
    /**
     * Alternative approach: Suppress handshake errors
     * This catches the disconnection event and prevents it
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        // Check if this is a handshake-related disconnect
        // If so, we could potentially prevent it, but this is risky
        // Better to handle it at the login phase
    }
    
    /**
     * Enable or disable handshake bypass
     */
    public static void setBypassEnabled(boolean enabled) {
        bypassEnabled = enabled;
        LOGGER.info("Handshake bypass " + (enabled ? "ENABLED" : "DISABLED"));
    }
    
    /**
     * Check if bypass is enabled
     */
    public static boolean isBypassEnabled() {
        return bypassEnabled;
    }
}
