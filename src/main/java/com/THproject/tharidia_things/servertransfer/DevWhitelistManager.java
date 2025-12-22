package com.THproject.tharidia_things.servertransfer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles whitelist state for direct access to the development server.
 */
public final class DevWhitelistManager {

    private static DatabaseManager databaseManager;

    private DevWhitelistManager() {}

    public static void setDatabaseManager(DatabaseManager dbManager) {
        databaseManager = dbManager;
    }

    public static boolean isInitialized() {
        return databaseManager != null && databaseManager.isInitialized();
    }

    public static void ensureTableExists() {
        if (!isInitialized()) {
            return;
        }
        String sql = """
            CREATE TABLE IF NOT EXISTS dev_whitelist (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                added_by VARCHAR(36),
                added_by_name VARCHAR(16),
                added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                reason VARCHAR(255),
                INDEX idx_username (username)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Unable to create dev whitelist table: {}", e.getMessage());
        }
    }

    public static boolean isWhitelisted(UUID playerUUID) {
        if (!isInitialized()) {
            return false;
        }
        String sql = "SELECT 1 FROM dev_whitelist WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Failed to check dev whitelist for {}: {}", playerUUID, e.getMessage());
            return false;
        }
    }

    public static boolean addToWhitelist(UUID playerUUID, String username, UUID addedBy, String addedByName, String reason) {
        if (!isInitialized()) {
            return false;
        }
        ensureTableExists();
        String sql = """
            INSERT INTO dev_whitelist (uuid, username, added_by, added_by_name, reason)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                added_by = VALUES(added_by),
                added_by_name = VALUES(added_by_name),
                reason = VALUES(reason),
                added_at = CURRENT_TIMESTAMP
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, username);
            stmt.setString(3, addedBy != null ? addedBy.toString() : null);
            stmt.setString(4, addedByName);
            stmt.setString(5, reason);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Failed to add {} to dev whitelist: {}", username, e.getMessage());
            return false;
        }
    }

    public static boolean removeFromWhitelist(UUID playerUUID) {
        if (!isInitialized()) {
            return false;
        }
        String sql = "DELETE FROM dev_whitelist WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Failed to remove {} from dev whitelist: {}", playerUUID, e.getMessage());
            return false;
        }
    }

    public static Optional<WhitelistEntry> getEntry(UUID playerUUID) {
        if (!isInitialized()) {
            return Optional.empty();
        }
        String sql = "SELECT uuid, username, added_by_name, added_at, reason FROM dev_whitelist WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapEntry(rs));
                }
            }
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Failed to fetch dev whitelist entry for {}: {}", playerUUID, e.getMessage());
        }
        return Optional.empty();
    }

    public static List<WhitelistEntry> listEntries() {
        List<WhitelistEntry> entries = new ArrayList<>();
        if (!isInitialized()) {
            return entries;
        }
        String sql = "SELECT uuid, username, added_by_name, added_at, reason FROM dev_whitelist ORDER BY added_at DESC";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                entries.add(mapEntry(rs));
            }
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Failed to list dev whitelist entries: {}", e.getMessage());
        }
        return entries;
    }

    private static WhitelistEntry mapEntry(ResultSet rs) throws SQLException {
        return new WhitelistEntry(
            UUID.fromString(rs.getString("uuid")),
            rs.getString("username"),
            rs.getString("added_by_name"),
            rs.getTimestamp("added_at").getTime(),
            rs.getString("reason")
        );
    }

    public record WhitelistEntry(UUID uuid, String username, String addedByName, long addedAtMillis, String reason) {}
}
