package com.tharidia.tharidia_things.lobby;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Handles queue management commands received from the main server via Velocity
 */
public class QueueCommandHandler {
    
    private static QueueManager queueManager;
    private static ServerTransferManager transferManager;
    private static Logger logger;
    
    public static void initialize(QueueManager qm, ServerTransferManager tm, Logger log) {
        queueManager = qm;
        transferManager = tm;
        logger = log;
    }
    
    /**
     * Process a queue command received from the main server
     */
    public static void handleCommand(MinecraftServer server, String command, String[] args, UUID senderUuid, String senderName) {
        if (queueManager == null || transferManager == null) {
            logger.error("QueueCommandHandler not initialized!");
            return;
        }
        
        logger.info("Processing queue command '{}' from {} (main server)", command, senderName);
        
        // Find the sender to send feedback (if they're online on lobby)
        ServerPlayer sender = server.getPlayerList().getPlayer(senderUuid);
        
        try {
            switch (command) {
                case "enable" -> {
                    queueManager.setQueueEnabled(true);
                    broadcastToOps(server, "§a§l[QUEUE] §7Queue system enabled by §6" + senderName + "§7 (from main server)");
                }
                case "disable" -> {
                    queueManager.setQueueEnabled(false);
                    broadcastToOps(server, "§c§l[QUEUE] §7Queue system disabled by §6" + senderName + "§7 (from main server)");
                }
                case "clear" -> {
                    queueManager.clearQueue();
                    broadcastToOps(server, "§a§l[QUEUE] §7Queue cleared by §6" + senderName + "§7 (from main server)");
                }
                case "list" -> {
                    List<UUID> queue = queueManager.getQueuedPlayers();
                    if (queue.isEmpty()) {
                        sendFeedbackToSender(sender, "§e§l[QUEUE] §7Queue is empty");
                    } else {
                        sendFeedbackToSender(sender, "§6§l=== Queue List (" + queue.size() + " players) ===");
                        int position = 1;
                        for (UUID uuid : queue) {
                            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                            String name = player != null ? player.getName().getString() : "Unknown";
                            long waitTime = queueManager.getWaitTime(uuid);
                            sendFeedbackToSender(sender, "§7" + position + ". §6" + name + " §7(waited: §6" + waitTime + "s§7)");
                            position++;
                        }
                    }
                }
                case "sendnext" -> {
                    UUID nextUuid = queueManager.pollNextInQueue();
                    if (nextUuid == null) {
                        sendFeedbackToSender(sender, "§e§l[QUEUE] §7Queue is empty");
                    } else {
                        ServerPlayer player = server.getPlayerList().getPlayer(nextUuid);
                        if (player != null) {
                            transferManager.transferToMain(player);
                            broadcastToOps(server, "§a§l[QUEUE] §7Sent §6" + player.getName().getString() + "§7 to main server (by " + senderName + ")");
                        } else {
                            sendFeedbackToSender(sender, "§c§l[QUEUE] §7Player not found (offline?)");
                        }
                    }
                }
                case "sendall" -> {
                    List<UUID> queue = queueManager.getQueuedPlayers();
                    int sent = 0;
                    for (UUID uuid : queue) {
                        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                        if (player != null) {
                            transferManager.transferToMain(player);
                            queueManager.removeFromQueue(uuid);
                            sent++;
                        }
                    }
                    broadcastToOps(server, "§a§l[QUEUE] §7Sent §6" + sent + "§7 players to main server (by " + senderName + ")");
                }
                case "autotransfer" -> {
                    if (args.length > 0) {
                        boolean enabled = args[0].equalsIgnoreCase("on");
                        queueManager.setAutoTransfer(enabled);
                        broadcastToOps(server, "§a§l[QUEUE] §7Auto-transfer " + (enabled ? "enabled" : "disabled") + " by §6" + senderName);
                    }
                }
                case "maxplayers" -> {
                    if (args.length > 0) {
                        try {
                            int max = Integer.parseInt(args[0]);
                            queueManager.setMaxMainServerPlayers(max);
                            broadcastToOps(server, "§a§l[QUEUE] §7Max main server players set to: §6" + max + " §7by §6" + senderName);
                        } catch (NumberFormatException e) {
                            sendFeedbackToSender(sender, "§c§l[ERROR] §7Invalid number: " + args[0]);
                        }
                    }
                }
                case "info" -> {
                    sendFeedbackToSender(sender, "§6§l=== Queue Information ===");
                    sendFeedbackToSender(sender, "§7Lobby Mode: " + (LobbyEvents.isLobbyMode() ? "§aEnabled" : "§cDisabled"));
                    sendFeedbackToSender(sender, "§7Queue Status: " + (queueManager.isQueueEnabled() ? "§aEnabled" : "§cDisabled"));
                    sendFeedbackToSender(sender, "§7Auto-transfer: " + (queueManager.isAutoTransfer() ? "§aEnabled" : "§cDisabled"));
                    sendFeedbackToSender(sender, "§7Max players: §6" + queueManager.getMaxMainServerPlayers());
                    sendFeedbackToSender(sender, "§7Current queue size: §6" + queueManager.getQueueSize());
                }
                case "lobbymode" -> {
                    if (args.length > 0) {
                        boolean enabled = args[0].equalsIgnoreCase("on");
                        LobbyEvents.setLobbyMode(enabled);
                        broadcastToOps(server, "§a§l[LOBBY] §7Lobby mode " + (enabled ? "enabled" : "disabled") + " by §6" + senderName);
                        broadcastToOps(server, "§7Players will " + (enabled ? "now" : "no longer") + " spawn in spectator mode");
                    }
                }
                case "send" -> {
                    if (args.length > 0) {
                        String targetName = args[0];
                        ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
                        if (target != null) {
                            transferManager.transferToMain(target);
                            queueManager.removeFromQueue(target.getUUID());
                            broadcastToOps(server, "§a§l[QUEUE] §7Sent §6" + target.getName().getString() + "§7 to main server (by " + senderName + ")");
                        } else {
                            sendFeedbackToSender(sender, "§c§l[ERROR] §7Player not found: " + targetName);
                        }
                    }
                }
                case "sendtolobby" -> {
                    // This command is handled on the main server directly
                    sendFeedbackToSender(sender, "§e§l[INFO] §7This command should be executed on the main server");
                }
                default -> {
                    logger.warn("Unknown queue command: {}", command);
                    sendFeedbackToSender(sender, "§c§l[ERROR] §7Unknown command: " + command);
                }
            }
        } catch (Exception e) {
            logger.error("Error executing queue command '{}': {}", command, e.getMessage(), e);
            sendFeedbackToSender(sender, "§c§l[ERROR] §7Failed to execute command: " + e.getMessage());
        }
    }
    
    /**
     * Send feedback to the command sender if they're online on the lobby
     */
    private static void sendFeedbackToSender(ServerPlayer sender, String message) {
        if (sender != null) {
            sender.sendSystemMessage(Component.literal(message));
        }
    }
    
    /**
     * Broadcast a message to all online operators
     */
    private static void broadcastToOps(MinecraftServer server, String message) {
        Component component = Component.literal(message);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                player.sendSystemMessage(component);
            }
        }
        logger.info(message);
    }
}
