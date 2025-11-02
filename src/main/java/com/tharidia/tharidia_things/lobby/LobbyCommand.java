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
        // /queue - Check your position in queue
        dispatcher.register(Commands.literal("queue")
            .executes(context -> {
                if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                    context.getSource().sendFailure(Component.literal("§cThis command can only be used by players!"));
                    return 0;
                }
                
                int position = queueManager.getQueuePosition(player.getUUID());
                if (position > 0) {
                    long waitTime = queueManager.getWaitTime(player.getUUID());
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§e§l[QUEUE] §7You are in position §6#" + position
                    ), false);
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§7Queue size: §6" + queueManager.getQueueSize() + " §7| Wait time: §6" + waitTime + "s"
                    ), false);
                } else {
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§e§l[QUEUE] §7You are not in the queue"
                    ), false);
                }
                return 1;
            })
        );
        
        // /play - Join the main server (or queue)
        dispatcher.register(Commands.literal("play")
            .executes(context -> {
                if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                    context.getSource().sendFailure(Component.literal("§cThis command can only be used by players!"));
                    return 0;
                }
                
                if (!queueManager.isQueueEnabled()) {
                    transferManager.transferToMain(player);
                } else {
                    if (!queueManager.isInQueue(player.getUUID())) {
                        queueManager.addToQueue(player);
                    } else {
                        queueManager.updateQueuePosition(player);
                    }
                }
                return 1;
            })
        );
        
        // /queueadmin - Admin commands
        dispatcher.register(Commands.literal("queueadmin")
            .requires(source -> source.hasPermission(2)) // OP level 2
            
            // /queueadmin enable
            .then(Commands.literal("enable")
                .executes(context -> {
                    queueManager.setQueueEnabled(true);
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§a§l[QUEUE] §7Queue system enabled"
                    ), true);
                    return 1;
                })
            )
            
            // /queueadmin disable
            .then(Commands.literal("disable")
                .executes(context -> {
                    queueManager.setQueueEnabled(false);
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§c§l[QUEUE] §7Queue system disabled"
                    ), true);
                    return 1;
                })
            )
            
            // /queueadmin clear
            .then(Commands.literal("clear")
                .executes(context -> {
                    queueManager.clearQueue();
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§a§l[QUEUE] §7Queue cleared"
                    ), true);
                    return 1;
                })
            )
            
            // /queueadmin list
            .then(Commands.literal("list")
                .executes(context -> {
                    return listQueue(context);
                })
            )
            
            // /queueadmin send <player>
            .then(Commands.literal("send")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                        transferManager.transferToMain(target);
                        queueManager.removeFromQueue(target.getUUID());
                        context.getSource().sendSuccess(() -> Component.literal(
                            "§a§l[QUEUE] §7Sent §6" + target.getName().getString() + "§7 to main server"
                        ), true);
                        return 1;
                    })
                )
            )
            
            // /queueadmin sendnext
            .then(Commands.literal("sendnext")
                .executes(context -> {
                    return sendNextPlayer(context);
                })
            )
            
            // /queueadmin sendall
            .then(Commands.literal("sendall")
                .executes(context -> {
                    return sendAllPlayers(context);
                })
            )
            
            // /queueadmin autotransfer <on|off>
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
                        context.getSource().sendSuccess(() -> Component.literal(
                            "§a§l[QUEUE] §7Auto-transfer " + (enabled ? "enabled" : "disabled")
                        ), true);
                        return 1;
                    })
                )
            )
            
            // /queueadmin maxplayers <number>
            .then(Commands.literal("maxplayers")
                .then(Commands.argument("max", IntegerArgumentType.integer(1, 1000))
                    .executes(context -> {
                        int max = IntegerArgumentType.getInteger(context, "max");
                        queueManager.setMaxMainServerPlayers(max);
                        context.getSource().sendSuccess(() -> Component.literal(
                            "§a§l[QUEUE] §7Max main server players set to: §6" + max
                        ), true);
                        return 1;
                    })
                )
            )
            
            // /queueadmin info
            .then(Commands.literal("info")
                .executes(context -> {
                    return showQueueInfo(context);
                })
            )
            
            // /queueadmin lobbymode <on|off>
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
                        context.getSource().sendSuccess(() -> Component.literal(
                            "§a§l[LOBBY] §7Lobby mode " + (enabled ? "enabled" : "disabled")
                        ), true);
                        context.getSource().sendSuccess(() -> Component.literal(
                            "§7Players will " + (enabled ? "now" : "no longer") + " spawn in spectator mode"
                        ), true);
                        return 1;
                    })
                )
            )
        );
    }
    
    private static int listQueue(CommandContext<CommandSourceStack> context) {
        List<UUID> queue = queueManager.getQueuedPlayers();
        
        if (queue.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal(
                "§e§l[QUEUE] §7Queue is empty"
            ), false);
            return 1;
        }
        
        context.getSource().sendSuccess(() -> Component.literal(
            "§6§l=== Queue List (" + queue.size() + " players) ==="
        ), false);
        
        int position = 1;
        for (UUID uuid : queue) {
            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(uuid);
            String name = player != null ? player.getName().getString() : "Unknown";
            long waitTime = queueManager.getWaitTime(uuid);
            
            final int pos = position;
            context.getSource().sendSuccess(() -> Component.literal(
                "§7" + pos + ". §6" + name + " §7(waited: §6" + waitTime + "s§7)"
            ), false);
            position++;
        }
        return 1;
    }
    
    private static int sendNextPlayer(CommandContext<CommandSourceStack> context) {
        UUID nextUuid = queueManager.pollNextInQueue();
        
        if (nextUuid == null) {
            context.getSource().sendSuccess(() -> Component.literal(
                "§e§l[QUEUE] §7Queue is empty"
            ), false);
            return 1;
        }
        
        ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(nextUuid);
        if (player != null) {
            transferManager.transferToMain(player);
            context.getSource().sendSuccess(() -> Component.literal(
                "§a§l[QUEUE] §7Sent §6" + player.getName().getString() + "§7 to main server"
            ), true);
        } else {
            context.getSource().sendSuccess(() -> Component.literal(
                "§c§l[QUEUE] §7Player not found (offline?)"
            ), false);
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
        
        final int finalSent = sent;
        context.getSource().sendSuccess(() -> Component.literal(
            "§a§l[QUEUE] §7Sent §6" + finalSent + "§7 players to main server"
        ), true);
        return 1;
    }
    
    private static int showQueueInfo(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6§l=== Queue Information ==="), false);
        context.getSource().sendSuccess(() -> Component.literal(
            "§7Lobby Mode: " + (LobbyEvents.isLobbyMode() ? "§aEnabled" : "§cDisabled")
        ), false);
        context.getSource().sendSuccess(() -> Component.literal(
            "§7Queue Status: " + (queueManager.isQueueEnabled() ? "§aEnabled" : "§cDisabled")
        ), false);
        context.getSource().sendSuccess(() -> Component.literal(
            "§7Auto-transfer: " + (queueManager.isAutoTransfer() ? "§aEnabled" : "§cDisabled")
        ), false);
        context.getSource().sendSuccess(() -> Component.literal(
            "§7Max players: §6" + queueManager.getMaxMainServerPlayers()
        ), false);
        context.getSource().sendSuccess(() -> Component.literal(
            "§7Current queue size: §6" + queueManager.getQueueSize()
        ), false);
        return 1;
    }
}
