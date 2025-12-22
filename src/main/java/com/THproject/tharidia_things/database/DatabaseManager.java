package com.THproject.tharidia_things.database;

import com.THproject.tharidia_things.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.sql.Connection;
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
            String dbName = "market";
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
            
            // Connection pool settings - aggressive timeouts for faster shutdown
            config.setMaximumPoolSize(5); // Reduced pool size
            config.setMinimumIdle(1); // Reduced minimum idle
            config.setConnectionTimeout(10000); // 10 seconds (reduced from 30)
            config.setIdleTimeout(300000); // 5 minutes (reduced from 10)
            config.setMaxLifetime(600000); // 10 minutes (reduced from 30)
            config.setLeakDetectionThreshold(60000); // 1 minute leak detection
            
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
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create transfer_tokens table
            String createTransferTokensTable = """
                CREATE TABLE IF NOT EXISTS transfer_tokens (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    target_server VARCHAR(50) NOT NULL,
                    expiration_time BIGINT NOT NULL,
                    INDEX idx_expiration (expiration_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
            
            // Create player_transfers table
            String createPlayerTransfersTable = """
                CREATE TABLE IF NOT EXISTS player_transfers (
                    uuid VARCHAR(36) NOT NULL,
                    server_name VARCHAR(50) NOT NULL,
                    from_server VARCHAR(50),
                    to_server VARCHAR(50),
                    serialized_data LONGBLOB,
                    pending_transfer BOOLEAN DEFAULT false,
                    transfer_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (uuid, server_name),
                    INDEX idx_transfer_time (transfer_time),
                    INDEX idx_pending_transfer (pending_transfer)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
            
            String createDevWhitelistTable = """
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
            
            stmt.execute(createTransferTokensTable);
            stmt.execute(createPlayerTransfersTable);
            stmt.execute(createDevWhitelistTable);
            
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
     * Cleanup old data (placeholder for future database maintenance)
     */
    public void cleanupOldData() {
        if (!initialized) {
            return;
        }
        
        // No cross-server command cleanup needed - lobby system removed
        logger.debug("Database cleanup completed - no old data to remove");
    }
    
    /**
     * Shutdown the database connection pool
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Shutting down database connection pool...");
            
            try {
                // Set a short timeout for shutdown
                dataSource.setLoginTimeout(2); // 2 seconds max for any pending operations
                
                // Close the pool - this should be fast with the aggressive timeouts
                dataSource.close();
                initialized = false;
                logger.info("Database connection pool closed successfully");
            } catch (Exception e) {
                logger.error("Error during database shutdown (forcing close): {}", e.getMessage());
                // Force close even if there's an error
                try {
                    dataSource.close();
                } catch (Exception ignored) {
                    // Ignore any further errors
                }
                initialized = false;
            }
        }
    }
}
