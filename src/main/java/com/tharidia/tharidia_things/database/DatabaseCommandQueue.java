package com.tharidia.tharidia_things.database;

import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages queue commands stored in the database
 * Allows cross-server communication by storing and retrieving commands
 */
public class DatabaseCommandQueue {
    
    private final DatabaseManager databaseManager;
    private final Logger logger;
    
    public DatabaseCommandQueue(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }
    
    /**
     * Add a command to the queue
     */
    public boolean addCommand(String command, String[] args, UUID senderUuid, String senderName) {
        if (!databaseManager.isInitialized()) {
            logger.error("Cannot add command: database not initialized");
            return false;
        }
        
        String sql = "INSERT INTO queue_commands (command, args, sender_uuid, sender_name) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, command);
            stmt.setString(2, String.join("|", args)); // Join args with pipe separator
            stmt.setString(3, senderUuid.toString());
            stmt.setString(4, senderName);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                logger.info("Command '{}' added to queue by {} ({})", command, senderName, senderUuid);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Failed to add command to queue: {}", e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * Get all pending commands (not yet executed)
     */
    public List<QueuedCommand> getPendingCommands() {
        List<QueuedCommand> commands = new ArrayList<>();
        
        if (!databaseManager.isInitialized()) {
            return commands;
        }
        
        String sql = "SELECT id, command, args, sender_uuid, sender_name, created_at FROM queue_commands WHERE executed = FALSE ORDER BY created_at ASC";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String command = rs.getString("command");
                String argsStr = rs.getString("args");
                String[] args = argsStr != null && !argsStr.isEmpty() ? argsStr.split("\\|") : new String[0];
                UUID senderUuid = UUID.fromString(rs.getString("sender_uuid"));
                String senderName = rs.getString("sender_name");
                
                commands.add(new QueuedCommand(id, command, args, senderUuid, senderName));
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get pending commands: {}", e.getMessage(), e);
        }
        
        return commands;
    }
    
    /**
     * Mark a command as executed
     */
    public boolean markAsExecuted(int commandId) {
        if (!databaseManager.isInitialized()) {
            return false;
        }
        
        String sql = "UPDATE queue_commands SET executed = TRUE, executed_at = NOW() WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, commandId);
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                logger.debug("Command {} marked as executed", commandId);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Failed to mark command as executed: {}", e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * Delete a command from the queue
     */
    public boolean deleteCommand(int commandId) {
        if (!databaseManager.isInitialized()) {
            return false;
        }
        
        String sql = "DELETE FROM queue_commands WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, commandId);
            int result = stmt.executeUpdate();
            
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Failed to delete command: {}", e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * Represents a queued command
     */
    public static class QueuedCommand {
        private final int id;
        private final String command;
        private final String[] args;
        private final UUID senderUuid;
        private final String senderName;
        
        public QueuedCommand(int id, String command, String[] args, UUID senderUuid, String senderName) {
            this.id = id;
            this.command = command;
            this.args = args;
            this.senderUuid = senderUuid;
            this.senderName = senderName;
        }
        
        public int getId() {
            return id;
        }
        
        public String getCommand() {
            return command;
        }
        
        public String[] getArgs() {
            return args;
        }
        
        public UUID getSenderUuid() {
            return senderUuid;
        }
        
        public String getSenderName() {
            return senderName;
        }
    }
}
