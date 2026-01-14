package com.THproject.tharidia_things.servertransfer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class TransferTokenManager {
    private static DatabaseManager databaseManager;
    private static final long TOKEN_VALIDITY_MS = 30000; // 30 secondi
    
    public static void setDatabaseManager(DatabaseManager dbManager) {
        databaseManager = dbManager;
    }
    
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
        if (databaseManager == null || !databaseManager.isInitialized()) {
            TharidiaThings.LOGGER.warn("Database non disponibile per creare token");
            return;
        }
        
        try {
            createTokenTableIfNotExists();
            
            long expirationTime = System.currentTimeMillis() + TOKEN_VALIDITY_MS;
            String sql = "INSERT INTO transfer_tokens (player_uuid, target_server, expiration_time) " +
                        "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE " +
                        "target_server = VALUES(target_server), expiration_time = VALUES(expiration_time)";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, targetServer);
                stmt.setLong(3, expirationTime);
                stmt.executeUpdate();
                
                TharidiaThings.LOGGER.info("Created transfer token for {} to server {}", playerUUID, targetServer);
            }
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Errore creazione token: {}", e.getMessage());
        }
    }
    
    public static boolean validateAndConsumeToken(UUID playerUUID, String currentServer) {
        if (databaseManager == null || !databaseManager.isInitialized()) {
            TharidiaThings.LOGGER.warn("Database non disponibile per validare token");
            return false;
        }
        
        try {
            String sql = "SELECT target_server, expiration_time FROM transfer_tokens WHERE player_uuid = ?";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (!rs.next()) {
                    TharidiaThings.LOGGER.warn("No transfer token found for {}", playerUUID);
                    return false;
                }
                
                String targetServer = rs.getString("target_server");
                long expirationTime = rs.getLong("expiration_time");
                
                // Delete token after reading
                deleteToken(playerUUID);
                
                if (System.currentTimeMillis() > expirationTime) {
                    TharidiaThings.LOGGER.warn("Transfer token expired for {}", playerUUID);
                    return false;
                }
                
                if (!targetServer.equalsIgnoreCase(currentServer)) {
                    TharidiaThings.LOGGER.warn("Transfer token server mismatch for {}. Expected: {}, Got: {}", 
                        playerUUID, targetServer, currentServer);
                    return false;
                }
                
                TharidiaThings.LOGGER.info("Transfer token validated for {} to server {}", playerUUID, currentServer);
                return true;
            }
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Errore validazione token: {}", e.getMessage());
        }
        return false;
    }
    
    public static void cleanupExpiredTokens() {
        if (databaseManager == null || !databaseManager.isInitialized()) {
            return;
        }
        
        try {
            String sql = "DELETE FROM transfer_tokens WHERE expiration_time < ?";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, System.currentTimeMillis());
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    TharidiaThings.LOGGER.debug("Cleaned up {} expired transfer tokens", deleted);
                }
            }
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Errore pulizia token: {}", e.getMessage());
        }
    }
    
    private static void deleteToken(UUID playerUUID) {
        try {
            String sql = "DELETE FROM transfer_tokens WHERE player_uuid = ?";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Errore eliminazione token: {}", e.getMessage());
        }
    }
    
    private static void createTokenTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS transfer_tokens (" +
                    "player_uuid VARCHAR(36) PRIMARY KEY," +
                    "target_server VARCHAR(50) NOT NULL," +
                    "expiration_time BIGINT NOT NULL," +
                    "INDEX idx_expiration (expiration_time)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }
}
