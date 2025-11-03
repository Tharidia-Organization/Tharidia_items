package com.tharidia.tharidia_things.lobby;

import com.tharidia.tharidia_things.database.DatabaseCommandQueue;
import com.tharidia.tharidia_things.database.DatabaseManager;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Polls the database for new commands and executes them on the lobby server
 * Runs on a separate thread to avoid blocking the main server thread
 */
public class CommandPoller {
    
    private final DatabaseCommandQueue commandQueue;
    private final DatabaseManager databaseManager;
    private final MinecraftServer server;
    private final Logger logger;
    private final ScheduledExecutorService scheduler;
    private boolean running = false;
    
    public CommandPoller(DatabaseCommandQueue commandQueue, DatabaseManager databaseManager, MinecraftServer server, Logger logger) {
        this.commandQueue = commandQueue;
        this.databaseManager = databaseManager;
        this.server = server;
        this.logger = logger;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "CommandPoller");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * Start polling for commands
     */
    public void start() {
        if (running) {
            logger.warn("CommandPoller is already running");
            return;
        }
        
        running = true;
        logger.info("Starting command poller (checking every 2 seconds)...");
        
        // Poll every 2 seconds
        scheduler.scheduleAtFixedRate(this::pollCommands, 2, 2, TimeUnit.SECONDS);
        
        // Cleanup old commands every hour
        scheduler.scheduleAtFixedRate(this::cleanupOldCommands, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Stop polling for commands
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        logger.info("Stopping command poller...");
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Command poller stopped");
    }
    
    /**
     * Poll for new commands and execute them
     */
    private void pollCommands() {
        try {
            List<DatabaseCommandQueue.QueuedCommand> commands = commandQueue.getPendingCommands();
            
            if (commands.isEmpty()) {
                return;
            }
            
            logger.debug("Found {} pending commands", commands.size());
            
            for (DatabaseCommandQueue.QueuedCommand cmd : commands) {
                // Execute on server thread
                server.execute(() -> {
                    try {
                        logger.info("Executing command '{}' from {} (ID: {})", 
                                cmd.getCommand(), cmd.getSenderName(), cmd.getId());
                        
                        // Execute the command
                        QueueCommandHandler.handleCommand(
                                server,
                                cmd.getCommand(),
                                cmd.getArgs(),
                                cmd.getSenderUuid(),
                                cmd.getSenderName()
                        );
                        
                        // Mark as executed
                        commandQueue.markAsExecuted(cmd.getId());
                        
                    } catch (Exception e) {
                        logger.error("Error executing command {}: {}", cmd.getId(), e.getMessage(), e);
                    }
                });
            }
            
        } catch (Exception e) {
            logger.error("Error polling commands: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Cleanup old executed commands
     */
    private void cleanupOldCommands() {
        try {
            if (databaseManager != null && databaseManager.isInitialized()) {
                databaseManager.cleanupOldCommands();
            }
        } catch (Exception e) {
            logger.error("Error cleaning up old commands: {}", e.getMessage(), e);
        }
    }
}
