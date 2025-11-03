package com.tharidia.tharidia_things.database;

import com.tharidia.tharidia_things.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages database connections using HikariCP connection pooling
 * Handles initialization and cleanup of database resources
 */
public class DatabaseManager {
    
    private final Logger logger;
    private HikariDataSource dataSource;
    private boolean initialized = false;
    
    public DatabaseManager(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Initialize the database connection pool
     */
    public boolean initialize() {
        if (!Config.DATABASE_ENABLED.get()) {
            logger.info("Database is disabled in config");
            return false;
        }
        
        try {
            String dbHost = Config.DATABASE_HOST.get();
            int dbPort = Config.DATABASE_PORT.get();
            String dbName = Config.DATABASE_NAME.get();
            String dbUser = Config.DATABASE_USERNAME.get();
            
            logger.info("Initializing database connection to {}:{} (database: {}, user: {})", 
                dbHost, dbPort, dbName, dbUser);
            
            HikariConfig config = new HikariConfig();
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    dbHost, dbPort, dbName);
            
            logger.info("JDBC URL: {}", jdbcUrl);
            
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(dbUser);
            config.setPassword(Config.DATABASE_PASSWORD.get());
            
            // Connection pool settings
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000); // 30 seconds
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            
            // Connection test query
            config.setConnectionTestQuery("SELECT 1");
            
            dataSource = new HikariDataSource(config);
            
            // Mark as initialized before testing connection
            initialized = true;
            
            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                String dbProduct = conn.getMetaData().getDatabaseProductName();
                String dbVersion = conn.getMetaData().getDatabaseProductVersion();
                logger.info("Database connection established successfully to {} version {}", 
                    dbProduct, dbVersion);
            } catch (SQLException e) {
                logger.error("Failed to test database connection: {}", e.getMessage(), e);
                initialized = false;
                throw e;
            }
            
            // Create tables if they don't exist
            createTables();
            
            logger.info("Database initialized successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to initialize database: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Create necessary database tables
     */
    private void createTables() {
        String createCommandsTable = """
            CREATE TABLE IF NOT EXISTS queue_commands (
                id INT AUTO_INCREMENT PRIMARY KEY,
                command VARCHAR(255) NOT NULL,
                args TEXT,
                sender_uuid VARCHAR(36) NOT NULL,
                sender_name VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                executed BOOLEAN DEFAULT FALSE,
                executed_at TIMESTAMP NULL,
                INDEX idx_executed (executed),
                INDEX idx_created (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(createCommandsTable);
            logger.info("Database tables created/verified successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to create database tables: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get a connection from the pool
     */
    public Connection getConnection() throws SQLException {
        if (!initialized || dataSource == null) {
            throw new SQLException("Database not initialized");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Check if database is initialized and ready
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Cleanup old executed commands (older than 1 hour)
     */
    public void cleanupOldCommands() {
        if (!initialized) {
            return;
        }
        
        String sql = "DELETE FROM queue_commands WHERE executed = TRUE AND executed_at < DATE_SUB(NOW(), INTERVAL 1 HOUR)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                logger.info("Cleaned up {} old executed commands", deleted);
            }
            
        } catch (SQLException e) {
            logger.error("Failed to cleanup old commands: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Shutdown the database connection pool
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Shutting down database connection pool...");
            dataSource.close();
            initialized = false;
            logger.info("Database connection pool closed");
        }
    }
}
