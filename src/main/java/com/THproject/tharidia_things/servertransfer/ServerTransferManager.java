package com.THproject.tharidia_things.servertransfer;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.database.DatabaseManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ServerTransferManager {
    private static DatabaseManager databaseManager;
    private static String currentServerName = "main";
    private static String mainServerIP = "172.18.0.10:25772";
    private static String devServerIP = "172.18.0.10:25566";
    
    public static void setDatabaseManager(DatabaseManager dbManager) {
        databaseManager = dbManager;
    }
    
    public static void setCurrentServerName(String serverName) {
        currentServerName = serverName;
    }
    
    public static void setServerAddresses(String mainIP, String devIP) {
        mainServerIP = mainIP;
        devServerIP = devIP;
    }
    
    public static String getCurrentServerName() {
        return currentServerName;
    }
    
    public static boolean savePlayerDataForTransfer(ServerPlayer player, String targetServer) {
        if (databaseManager == null || !databaseManager.isInitialized()) {
            TharidiaThings.LOGGER.warn("Database non disponibile per il salvataggio dati transfer");
            return false;
        }
        
        try {
            createTransferTableIfNotExists();
            
            // First, save the current position for the current server
            // This ensures we don't lose the position when transferring
            if (savePlayerPosition(player)) {
                TharidiaThings.LOGGER.debug("Correctly saved current player position");
            } else {
                TharidiaThings.LOGGER.error("Failed to save current player position");
            }
            
            CompoundTag playerData = serializePlayerData(player);
            byte[] serializedData = playerData.toString().getBytes();
            
            String sql = "INSERT INTO player_transfers (uuid, server_name, from_server, to_server, serialized_data, pending_transfer) " +
                        "VALUES (?, ?, ?, ?, ?, true) ON DUPLICATE KEY UPDATE " +
                        "from_server = VALUES(from_server), to_server = VALUES(to_server), " +
                        "serialized_data = VALUES(serialized_data), transfer_time = CURRENT_TIMESTAMP, " +
                        "pending_transfer = VALUES(pending_transfer)";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUUID().toString());
                stmt.setString(2, targetServer);
                stmt.setString(3, currentServerName);
                stmt.setString(4, targetServer);
                stmt.setBytes(5, serializedData);
                stmt.executeUpdate();
                
                TharidiaThings.LOGGER.info("Dati transfer salvati per {} verso {}", player.getName().getString(), targetServer);
                return true;
            }
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Errore salvataggio dati transfer: {}", e.getMessage());
            return false;
        }
    }
    
    public static boolean restorePlayerData(ServerPlayer player) {
        if (databaseManager == null || !databaseManager.isInitialized()) {
            TharidiaThings.LOGGER.warn("Database non disponibile per il ripristino dati transfer");
            return false;
        }
        
        try {
            String sql = "SELECT serialized_data, from_server, to_server FROM player_transfers " +
                        "WHERE uuid = ? AND server_name = ? AND pending_transfer = true AND transfer_time > DATE_SUB(NOW(), INTERVAL 5 MINUTE) " +
                        "ORDER BY transfer_time DESC LIMIT 1";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUUID().toString());
                stmt.setString(2, currentServerName);
                TharidiaThings.LOGGER.info("Checking transfer data for {} on server {}", 
                    player.getName().getString(), currentServerName);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    byte[] serializedData = rs.getBytes("serialized_data");
                    String fromServer = rs.getString("from_server");
                    String toServer = rs.getString("to_server");
                    
                    TharidiaThings.LOGGER.info("Found transfer data for {}: from {} to {}", 
                        player.getName().getString(), fromServer, toServer);
                    
                    // Verifica che il server corrente sia il server di destinazione
                    if (!currentServerName.equalsIgnoreCase(toServer)) {
                        TharidiaThings.LOGGER.info("Player {} si è riconnesso al server {} ma era destinato al {}", 
                                player.getName().getString(), currentServerName, toServer);
                        return false;
                    }
                    
                    String nbtString = new String(serializedData);
                    CompoundTag playerData = TagParser.parseTag(nbtString);
                    
                    restorePlayerData(player, playerData);
                    
                    TharidiaThings.LOGGER.info("Dati transfer ripristinati per {} da {} a {}", 
                            player.getName().getString(), fromServer, currentServerName);
                    
                    deleteTransferData(player.getUUID());
                    player.sendSystemMessage(Component.literal("§aDati ripristinati dal server: " + fromServer));
                    return true;
                } else {
                    TharidiaThings.LOGGER.info("No transfer data found for {} on server {}", 
                        player.getName().getString(), currentServerName);
                }
            }
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Errore ripristino dati transfer: {}", e.getMessage());
        }
        return false;
    }
    
    public static String getServerAddress(String serverName) {
        return switch (serverName.toLowerCase()) {
            case "main" -> mainServerIP;
            case "dev" -> devServerIP;
            default -> null;
        };
    }
    
    private static void createTransferTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS player_transfers (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "server_name VARCHAR(50) NOT NULL," +
                    "from_server VARCHAR(50)," +
                    "to_server VARCHAR(50)," +
                    "serialized_data LONGBLOB," +
                    "pending_transfer BOOLEAN DEFAULT false," +
                    "transfer_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (uuid, server_name)," +
                    "INDEX idx_transfer_time (transfer_time)," +
                    "INDEX idx_pending_transfer (pending_transfer)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }
    
    private static CompoundTag serializePlayerData(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        
        // Dati base
        tag.putDouble("health", player.getHealth());
        tag.putFloat("food", player.getFoodData().getFoodLevel());
        tag.putFloat("saturation", player.getFoodData().getSaturationLevel());
        // NOTA: La posizione non viene salvata nel trasferimento
        // Verrà ripristinata dalla posizione salvata del server di destinazione
        
        // Inventario
        ListTag inventoryList = new ListTag();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("slot", i);
                itemTag.put("item", stack.save(player.registryAccess()));
                inventoryList.add(itemTag);
            }
        }
        tag.put("inventory", inventoryList);
        
        // Armor e offhand
        ListTag equipmentList = new ListTag();
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("slot", i + 100);
                itemTag.put("item", stack.save(player.registryAccess()));
                equipmentList.add(itemTag);
            }
        }
        
        ItemStack offhand = player.getInventory().offhand.get(0);
        if (!offhand.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("slot", 104);
            itemTag.put("item", offhand.save(player.registryAccess()));
            equipmentList.add(itemTag);
        }
        tag.put("equipment", equipmentList);
        
        // XP e livelli
        tag.putInt("experienceLevel", player.experienceLevel);
        tag.putInt("experienceProgress", (int)(player.experienceProgress * 100));
        tag.putInt("totalExperience", player.totalExperience);
        
        // Dati Tharidia specifici
        CompoundTag tharidiaData = new CompoundTag();
        // Aggiungere qui dati specifici delle mod Tharidia
        tag.put("tharidia_data", tharidiaData);
        
        return tag;
    }
    
    private static void restorePlayerData(ServerPlayer player, CompoundTag tag) {
        try {
            // Ripristinare dati base
            player.setHealth((float)tag.getDouble("health"));
            player.getFoodData().setFoodLevel((int)tag.getFloat("food"));
            player.getFoodData().setSaturation(tag.getFloat("saturation"));
            
            // NOTA: La posizione non viene ripristinata dai dati di trasferimento
            // Verrà ripristinata dalla posizione salvata del server corrente
            
            // Ripristinare inventario
            ListTag inventoryList = tag.getList("inventory", 10);
            for (int i = 0; i < inventoryList.size(); i++) {
                CompoundTag itemTag = inventoryList.getCompound(i);
                int slot = itemTag.getInt("slot");
                ItemStack stack = ItemStack.parse(player.registryAccess(), itemTag.getCompound("item")).orElse(ItemStack.EMPTY);
                player.getInventory().setItem(slot, stack);
            }
            
            // Ripristinare equipment
            ListTag equipmentList = tag.getList("equipment", 10);
            for (int i = 0; i < equipmentList.size(); i++) {
                CompoundTag itemTag = equipmentList.getCompound(i);
                int slot = itemTag.getInt("slot");
                ItemStack stack = ItemStack.parse(player.registryAccess(), itemTag.getCompound("item")).orElse(ItemStack.EMPTY);
                
                if (slot >= 100 && slot <= 103) {
                    player.getInventory().armor.set(slot - 100, stack);
                } else if (slot == 104) {
                    player.getInventory().offhand.set(0, stack);
                }
            }
            
            // Ripristinare XP
            player.experienceLevel = tag.getInt("experienceLevel");
            player.experienceProgress = tag.getInt("experienceProgress") / 100.0f;
            player.totalExperience = tag.getInt("totalExperience");
            
            // Ripristinare dati Tharidia
            CompoundTag tharidiaData = tag.getCompound("tharidia_data");
            // Implementare ripristino dati specifici
            
            // Ripristina la posizione salvata del server corrente
            restorePlayerPosition(player);
            
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Errore durante il ripristino dati giocatore: {}", e.getMessage());
        }
    }
    
    public static boolean savePlayerPosition(ServerPlayer player) {
        if (databaseManager == null || !databaseManager.isInitialized()) {
            TharidiaThings.LOGGER.warn("Database non disponibile per il salvataggio posizione");
            return false;
        }
        
        try {
            createTransferTableIfNotExists();
            
            CompoundTag positionData = new CompoundTag();
            positionData.putDouble("x", player.getX());
            positionData.putDouble("y", player.getY());
            positionData.putDouble("z", player.getZ());
            positionData.putFloat("yaw", player.getYRot());
            positionData.putFloat("pitch", player.getXRot());
            positionData.putString("dimension", player.level().dimension().location().toString());
            
            byte[] serializedData = positionData.toString().getBytes();
            
            String sql = "INSERT INTO player_transfers (uuid, server_name, from_server, to_server, serialized_data, pending_transfer) " +
                        "VALUES (?, ?, ?, ?, ?, false) ON DUPLICATE KEY UPDATE " +
                        "serialized_data = VALUES(serialized_data), transfer_time = CURRENT_TIMESTAMP, " +
                        "pending_transfer = false";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUUID().toString());
                stmt.setString(2, currentServerName);
                stmt.setString(3, currentServerName);
                stmt.setString(4, currentServerName);
                stmt.setBytes(5, serializedData);
                stmt.executeUpdate();
                
                TharidiaThings.LOGGER.debug("Posizione salvata per {} sul server {}", player.getName().getString(), currentServerName);
                return true;
            }
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Errore salvataggio posizione: {}", e.getMessage());
            return false;
        }
    }
    
    public static boolean restorePlayerPosition(ServerPlayer player) {
        if (databaseManager == null || !databaseManager.isInitialized()) {
            TharidiaThings.LOGGER.warn("Database non disponibile per il ripristino posizione");
            return false;
        }
        
        try {
            String sql = "SELECT serialized_data FROM player_transfers " +
                        "WHERE uuid = ? AND server_name = ? AND pending_transfer = false " +
                        "ORDER BY transfer_time DESC LIMIT 1";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUUID().toString());
                stmt.setString(2, currentServerName);
                TharidiaThings.LOGGER.info("Querying position for player {} on server {}", 
                    player.getName().getString(), currentServerName);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    byte[] serializedData = rs.getBytes("serialized_data");
                    String nbtString = new String(serializedData);
                    CompoundTag positionData = TagParser.parseTag(nbtString);
                    
                    double x = positionData.getDouble("x");
                    double y = positionData.getDouble("y");
                    double z = positionData.getDouble("z");
                    float yaw = positionData.getFloat("yaw");
                    float pitch = positionData.getFloat("pitch");
                    
                    TharidiaThings.LOGGER.info("Found position for {}: ({}, {}, {}) on server {}", 
                        player.getName().getString(), x, y, z, currentServerName);
                    
                    player.server.execute(() -> {
                        player.teleportTo(x, y, z);
                        player.setYRot(yaw);
                        player.setXRot(pitch);
                    });
                    
                    TharidiaThings.LOGGER.debug("Posizione ripristinata per {} sul server {}", player.getName().getString(), currentServerName);
                    return true;
                } else {
                    TharidiaThings.LOGGER.warn("No saved position found for player {} on server {}", 
                        player.getName().getString(), currentServerName);
                }
            }
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Errore ripristino posizione: {}", e.getMessage());
        }
        return false;
    }
    
    public static void cleanOldTransferData() {
        if (databaseManager == null || !databaseManager.isInitialized()) {
            TharidiaThings.LOGGER.warn("Database non disponibile per la pulizia dati");
            return;
        }
        
        try {
            // Delete all transfer data older than 1 hour
            String sql = "DELETE FROM player_transfers WHERE transfer_time < DATE_SUB(NOW(), INTERVAL 1 HOUR)";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                int deleted = stmt.executeUpdate();
                TharidiaThings.LOGGER.info("Puliti {} vecchi record di transfer", deleted);
            }
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Errore pulizia vecchi dati transfer: {}", e.getMessage());
        }
    }
    
    private static void deleteTransferData(UUID playerUUID) {
        try {
            String sql = "DELETE FROM player_transfers WHERE uuid = ? AND server_name = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, currentServerName);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            TharidiaThings.LOGGER.error("Errore eliminazione dati transfer: {}", e.getMessage());
        }
    }
}
