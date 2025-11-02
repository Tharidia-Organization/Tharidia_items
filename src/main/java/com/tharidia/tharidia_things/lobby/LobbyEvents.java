package com.tharidia.tharidia_things.lobby;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/**
 * Event handlers for lobby functionality
 */
public class LobbyEvents {
    
    private static QueueManager queueManager;
    private static ServerTransferManager transferManager;
    private static Logger logger;
    private static boolean lobbyMode = false;
    
    public static void initialize(QueueManager qm, ServerTransferManager tm, Logger log) {
        queueManager = qm;
        transferManager = tm;
        logger = log;
    }
    
    /**
     * Enable or disable lobby mode
     * When enabled, players spawn in spectator and are added to queue
     */
    public static void setLobbyMode(boolean enabled) {
        lobbyMode = enabled;
        logger.info("Lobby mode {}", enabled ? "enabled" : "disabled");
    }
    
    public static boolean isLobbyMode() {
        return lobbyMode;
    }
    
    /**
     * Handle player join - add to queue if lobby mode is active
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!lobbyMode) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            logger.info("Player {} joined lobby", player.getName().getString());
            
            // Set spectator mode
            player.setGameMode(GameType.SPECTATOR);
            
            // Send welcome message
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            ));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§l         Welcome to Tharidia!"
            ));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§7"
            ));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§7You are in the lobby. Please wait..."
            ));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§7Type §6/play§7 to join the queue"
            ));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            ));
            
            // Add to queue if enabled
            if (queueManager.isQueueEnabled()) {
                queueManager.addToQueue(player);
                
                // Auto-transfer if enabled and queue is small
                if (queueManager.isAutoTransfer() && queueManager.getQueueSize() <= 1) {
                    transferManager.transferToMain(player);
                    queueManager.removeFromQueue(player.getUUID());
                }
            }
        }
    }
    
    /**
     * Handle player disconnect - remove from queue
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!lobbyMode) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            queueManager.removeFromQueue(player.getUUID());
            logger.info("Player {} left lobby, removed from queue", player.getName().getString());
        }
    }
}
