package com.tharidia.tharidia_things.servertransfer;

import com.tharidia.tharidia_things.TharidiaThings;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransferTokenManager {
    private static final Map<UUID, TransferToken> activeTokens = new ConcurrentHashMap<>();
    private static final long TOKEN_VALIDITY_MS = 30000; // 30 secondi
    
    public static class TransferToken {
        public final String targetServer;
        public final long expirationTime;
        
        public TransferToken(String targetServer) {
            this.targetServer = targetServer;
            this.expirationTime = System.currentTimeMillis() + TOKEN_VALIDITY_MS;
        }
        
        public boolean isValid() {
            return System.currentTimeMillis() < expirationTime;
        }
    }
    
    public static void createToken(UUID playerUUID, String targetServer) {
        TransferToken token = new TransferToken(targetServer);
        activeTokens.put(playerUUID, token);
        TharidiaThings.LOGGER.info("Created transfer token for {} to server {}", playerUUID, targetServer);
    }
    
    public static boolean validateAndConsumeToken(UUID playerUUID, String currentServer) {
        TransferToken token = activeTokens.remove(playerUUID);
        
        if (token == null) {
            TharidiaThings.LOGGER.warn("No transfer token found for {}", playerUUID);
            return false;
        }
        
        if (!token.isValid()) {
            TharidiaThings.LOGGER.warn("Transfer token expired for {}", playerUUID);
            return false;
        }
        
        if (!token.targetServer.equalsIgnoreCase(currentServer)) {
            TharidiaThings.LOGGER.warn("Transfer token server mismatch for {}. Expected: {}, Got: {}", 
                playerUUID, token.targetServer, currentServer);
            return false;
        }
        
        TharidiaThings.LOGGER.info("Transfer token validated for {} to server {}", playerUUID, currentServer);
        return true;
    }
    
    public static void cleanupExpiredTokens() {
        activeTokens.entrySet().removeIf(entry -> !entry.getValue().isValid());
    }
}
