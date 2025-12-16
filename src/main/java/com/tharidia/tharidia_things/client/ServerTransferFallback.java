package com.tharidia.tharidia_things.client;

import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ServerTransferFallback {
    
    private static final Set<String> ALLOWED_UUIDS_HASH = new HashSet<>();
    private static final String SALT = "tharidia_fallback_salt_2024_secure";
    private static boolean isTransferring = false;
    private static String attemptedServer = null;
    private static String fallbackServer = null;
    
    static {
        // Hashed UUIDs of allowed players (SHA-256)
        // These are pre-computed hashes of the UUIDs that should have fallback access
        ALLOWED_UUIDS_HASH.add("ecdcd66e118ffc6f2fd8a15e1cc2d6647f1899d6757f3f9a35cd08c80787337c");
        ALLOWED_UUIDS_HASH.add("2b445f5c032fa0611f4144590faf872cee1581771abd5ce39afce8152aa18003");
    }
    
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        Screen screen = event.getScreen();
        
        // Check if we're getting a disconnect screen during or after transfer
        if (screen instanceof DisconnectedScreen && isTransferring) {
            DisconnectedScreen disconnectScreen = (DisconnectedScreen) screen;
            // Get the disconnect reason from narration message (contains title + reason)
            String fullMessage = disconnectScreen.getNarrationMessage().getString();
            String reasonStr = fullMessage.toLowerCase();
            
            // Check if it's a connection failure (not a kick/ban)
            if (reasonStr.contains("connection") || reasonStr.contains("timeout") || 
                reasonStr.contains("refused") || reasonStr.contains("unreachable") ||
                reasonStr.contains("timed out")) {
                
                TharidiaThings.LOGGER.info("Connection failure detected to {}, attempting fallback to {}", 
                    attemptedServer, fallbackServer);
                
                // Prevent the disconnect screen from showing
                event.setNewScreen(null);
                
                // Attempt fallback connection
                attemptFallbackConnection();
            }
        }
    }
    
    public static boolean isTransferring() {
        return isTransferring;
    }
    
    public static void setTransferring(boolean transferring, String fromServer, String toServer) {
        isTransferring = transferring;
        attemptedServer = toServer;
        fallbackServer = fromServer.equals("main") ? "dev" : "main";
    }
    
    private static void attemptFallbackConnection() {
        Minecraft minecraft = Minecraft.getInstance();
        
        // Get the current server data
        ServerData currentServer = minecraft.getCurrentServer();
        if (currentServer == null) {
            TharidiaThings.LOGGER.error("No current server data available for fallback");
            return;
        }
        
        // Get fallback address
        String fallbackAddress = getFallbackAddress(fallbackServer);
        if (fallbackAddress == null) {
            TharidiaThings.LOGGER.error("No fallback address configured for server: {}", fallbackServer);
            return;
        }
        
        // Create new server data for fallback
        ServerData fallbackServerData = new ServerData(
            "Tharidia " + fallbackServer.toUpperCase(),
            fallbackAddress,
            ServerData.Type.OTHER
        );
        
        // Reset transfer state
        isTransferring = false;
        
        // Show connecting screen
        ConnectScreen.startConnecting(minecraft.screen, minecraft, 
            ServerAddress.parseString(fallbackAddress), fallbackServerData, false, null);
        
        minecraft.player.sendSystemMessage(Component.literal(
            "§cConnection to " + attemptedServer + " failed. §aAttempting fallback to " + fallbackServer + "..."
        ));
    }
    
    private static String getFallbackAddress(String serverName) {
        return switch (serverName.toLowerCase()) {
            case "main" -> "172.18.0.10:25772";
            case "dev" -> "172.18.0.10:25566";
            default -> null;
        };
    }
    
    public static boolean canAttemptFallback(UUID playerUUID) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String uuidWithSalt = playerUUID.toString() + SALT;
            byte[] hash = digest.digest(uuidWithSalt.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return ALLOWED_UUIDS_HASH.contains(hexString.toString());
        } catch (NoSuchAlgorithmException e) {
            TharidiaThings.LOGGER.error("Failed to check UUID fallback permission", e);
            return false;
        }
    }
    
    public static void attemptDirectLoginFallback(UUID playerUUID) {
        if (!canAttemptFallback(playerUUID)) {
            TharidiaThings.LOGGER.info("Player {} not in fallback whitelist", playerUUID);
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        
        // Check if we're on the main server and need to fallback to dev
        ServerData currentServer = minecraft.getCurrentServer();
        if (currentServer == null) return;
        
        String currentAddress = currentServer.ip;
        
        // If connecting to main and it fails, try dev
        if (currentAddress.contains("25772")) { // Main server port
            TharidiaThings.LOGGER.info("Direct login to main failed, attempting fallback to dev for whitelisted player");
            
            ServerData devServerData = new ServerData(
                "Tharidia DEV",
                "172.18.0.10:25566",
                ServerData.Type.OTHER
            );
            
            ConnectScreen.startConnecting(minecraft.screen, minecraft, 
                ServerAddress.parseString("172.18.0.10:25566"), devServerData, false, null);
            
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal(
                    "§cConnection to main server failed. §aAttempting fallback to DEV server..."
                ));
            }
        }
    }
}
