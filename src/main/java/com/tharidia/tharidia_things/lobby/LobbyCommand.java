package com.tharidia.tharidia_things.lobby;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Commands for managing the lobby queue system
 */
public class LobbyCommand {
    
    private static QueueManager queueManager;
    private static ServerTransferManager transferManager;
    
    public static void initialize(QueueManager qm, ServerTransferManager tm) {
        queueManager = qm;
        transferManager = tm;
    }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Only register admin commands - no commands for non-OP players
        registerQueueAdmin(dispatcher, "thqueueadmin");
    }


    private static void registerQueueAdmin(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
        // /<root> - Admin commands (requires OP level 4)
        dispatcher.register(Commands.literal(root)
            .requires(source -> source.hasPermission(4))
            
            // /thqueueadmin enable
            .then(Commands.literal("enable")
                .executes(context -> {
                    queueManager.setQueueEnabled(true);
                    sendFeedback(context, "§a§l[QUEUE] §7Queue system enabled");
                    return 1;
                })
            )
            
            // /thqueueadmin disable
            .then(Commands.literal("disable")
                .executes(context -> {
                    queueManager.setQueueEnabled(false);
                    sendFeedback(context, "§c§l[QUEUE] §7Queue system disabled");
                    return 1;
                })
            )
            
            // /thqueueadmin clear
            .then(Commands.literal("clear")
                .executes(context -> {
                    queueManager.clearQueue();
                    sendFeedback(context, "§a§l[QUEUE] §7Queue cleared");
                    return 1;
                })
            )
            
            // /thqueueadmin list
            .then(Commands.literal("list")
                .executes(context -> {
                    return listQueue(context);
                })
            )
            
            // /thqueueadmin send <player>
            .then(Commands.literal("send")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                        transferManager.transferToMain(target);
                        queueManager.removeFromQueue(target.getUUID());
                        sendFeedback(context, "§a§l[QUEUE] §7Sent §6" + target.getName().getString() + "§7 to main server");
                        return 1;
                    })
                )
            )
            
            // /thqueueadmin sendnext
            .then(Commands.literal("sendnext")
                .executes(context -> {
                    return sendNextPlayer(context);
                })
            )
            
            // /thqueueadmin sendall
            .then(Commands.literal("sendall")
                .executes(context -> {
                    return sendAllPlayers(context);
                })
            )
            
            // /thqueueadmin autotransfer <on|off>
            .then(Commands.literal("autotransfer")
                .then(Commands.argument("state", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest("on");
                        builder.suggest("off");
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        String state = StringArgumentType.getString(context, "state");
                        boolean enabled = state.equalsIgnoreCase("on");
                        queueManager.setAutoTransfer(enabled);
                        sendFeedback(context, "§a§l[QUEUE] §7Auto-transfer " + (enabled ? "enabled" : "disabled"));
                        return 1;
                    })
                )
            )
            
            // /thqueueadmin maxplayers <number>
            .then(Commands.literal("maxplayers")
                .then(Commands.argument("max", IntegerArgumentType.integer(1, 1000))
                    .executes(context -> {
                        int max = IntegerArgumentType.getInteger(context, "max");
                        queueManager.setMaxMainServerPlayers(max);
                        sendFeedback(context, "§a§l[QUEUE] §7Max main server players set to: §6" + max);
                        return 1;
                    })
                )
            )
            
            // /thqueueadmin info
            .then(Commands.literal("info")
                .executes(context -> {
                    return showQueueInfo(context);
                })
            )
            
            // /thqueueadmin lobbymode <on|off>
            .then(Commands.literal("lobbymode")
                .then(Commands.argument("state", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest("on");
                        builder.suggest("off");
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        String state = StringArgumentType.getString(context, "state");
                        boolean enabled = state.equalsIgnoreCase("on");
                        LobbyEvents.setLobbyMode(enabled);
                        sendFeedback(context, "§a§l[LOBBY] §7Lobby mode " + (enabled ? "enabled" : "disabled"));
                        sendFeedback(context, "§7Players will " + (enabled ? "now" : "no longer") + " spawn in spectator mode");
                        return 1;
                    })
                )
            )
            
            // /thqueueadmin play - Admin command to manually join/queue (bypasses restrictions)
            .then(Commands.literal("play")
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        context.getSource().sendFailure(Component.literal("§cThis command can only be used by players!"));
                        return 0;
                    }
                    
                    if (!queueManager.isQueueEnabled()) {
                        transferManager.transferToMain(player);
                        sendFeedback(context, "§a§l[ADMIN] §7Transferring to main server...");
                    } else {
                        if (!queueManager.isInQueue(player.getUUID())) {
                            queueManager.addToQueue(player);
                        } else {
                            queueManager.updateQueuePosition(player);
                        }
                    }
                    return 1;
                })
            )
            
            // /thqueueadmin sendtolobby <player> - Send player from main to lobby
            .then(Commands.literal("sendtolobby")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                        transferManager.transferToServer(target, "lobby");
                        sendFeedback(context, "§a§l[QUEUE] §7Sent §6" + target.getName().getString() + "§7 to lobby server");
                        return 1;
                    })
                )
            )
            
            // /thqueueadmin queue - Check queue position (admin only)
            .then(Commands.literal("queue")
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        context.getSource().sendFailure(Component.literal("§cThis command can only be used by players!"));
                        return 0;
                    }
                    
                    int position = queueManager.getQueuePosition(player.getUUID());
                    if (position > 0) {
                        long waitTime = queueManager.getWaitTime(player.getUUID());
                        sendFeedback(context, "§e§l[QUEUE] §7You are in position §6#" + position);
                        sendFeedback(context, "§7Queue size: §6" + queueManager.getQueueSize() + " §7| Wait time: §6" + waitTime + "s");
                    } else {
                        sendFeedback(context, "§e§l[QUEUE] §7You are not in the queue");
                    }
                    return 1;
                })
            )
        );
    }
    
    private static int listQueue(CommandContext<CommandSourceStack> context) {
        List<UUID> queue = queueManager.getQueuedPlayers();
        
        if (queue.isEmpty()) {
            sendFeedback(context, "§e§l[QUEUE] §7Queue is empty");
            return 1;
        }
        
        sendFeedback(context, "§6§l=== Queue List (" + queue.size() + " players) ===");
        
        int position = 1;
        for (UUID uuid : queue) {
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(uuid);
            String name = player != null ? player.getName().getString() : "Unknown";
            long waitTime = queueManager.getWaitTime(uuid);
            
            sendFeedback(context, "§7" + position + ". §6" + name + " §7(waited: §6" + waitTime + "s§7)");
            position++;
        }
        return 1;
    }
    
    private static int sendNextPlayer(CommandContext<CommandSourceStack> context) {
        UUID nextUuid = queueManager.pollNextInQueue();
        
        if (nextUuid == null) {
            sendFeedback(context, "§e§l[QUEUE] §7Queue is empty");
            return 1;
        }
        
        ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(nextUuid);
        if (player != null) {
            transferManager.transferToMain(player);
            sendFeedback(context, "§a§l[QUEUE] §7Sent §6" + player.getName().getString() + "§7 to main server");
        } else {
            sendFeedback(context, "§c§l[QUEUE] §7Player not found (offline?)");
        }
        return 1;
    }
    
    private static int sendAllPlayers(CommandContext<CommandSourceStack> context) {
        List<UUID> queue = queueManager.getQueuedPlayers();
        int sent = 0;
        
        for (UUID uuid : queue) {
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                transferManager.transferToMain(player);
                queueManager.removeFromQueue(uuid);
                sent++;
            }
        }
        
        sendFeedback(context, "§a§l[QUEUE] §7Sent §6" + sent + "§7 players to main server");
        return 1;
    }
    
    private static int showQueueInfo(CommandContext<CommandSourceStack> context) {
        sendFeedback(context, "§6§l=== Queue Information ===");
        sendFeedback(context, "§7Lobby Mode: " + (LobbyEvents.isLobbyMode() ? "§aEnabled" : "§cDisabled"));
        sendFeedback(context, "§7Queue Status: " + (queueManager.isQueueEnabled() ? "§aEnabled" : "§cDisabled"));
        sendFeedback(context, "§7Auto-transfer: " + (queueManager.isAutoTransfer() ? "§aEnabled" : "§cDisabled"));
        sendFeedback(context, "§7Max players: §6" + queueManager.getMaxMainServerPlayers());
        sendFeedback(context, "§7Current queue size: §6" + queueManager.getQueueSize());
        return 1;
    }
    
    /**
     * Helper method to send feedback that works in both game chat and console
     */
    private static void sendFeedback(CommandContext<CommandSourceStack> context, String message) {
        Component component = Component.literal(message);
        // Send to command source (works for both player and console)
        context.getSource().sendSuccess(() -> component, false);
        // If executed by a player, also ensure they see it in chat
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(component);
        }
    }
}
