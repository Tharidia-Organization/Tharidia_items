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
import com.tharidia.tharidia_things.database.DatabaseCommandQueue;

/**
 * Remote commands for managing the lobby queue from the main server
 * These commands send requests to the lobby server via database
 */
public class LobbyRemoteCommand {
    
    private static DatabaseCommandQueue commandQueue;
    private static ServerTransferManager transferManager;
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("thqueueadmin")
            .requires(source -> source.hasPermission(4))
            
            // /thqueueadmin enable
            .then(Commands.literal("enable")
                .executes(context -> {
                    return sendCommand(context, "enable");
                })
            )
            
            // /thqueueadmin disable
            .then(Commands.literal("disable")
                .executes(context -> {
                    return sendCommand(context, "disable");
                })
            )
            
            // /thqueueadmin clear
            .then(Commands.literal("clear")
                .executes(context -> {
                    return sendCommand(context, "clear");
                })
            )
            
            // /thqueueadmin list
            .then(Commands.literal("list")
                .executes(context -> {
                    return sendCommand(context, "list");
                })
            )
            
            // /thqueueadmin sendnext
            .then(Commands.literal("sendnext")
                .executes(context -> {
                    return sendCommand(context, "sendnext");
                })
            )
            
            // /thqueueadmin sendall
            .then(Commands.literal("sendall")
                .executes(context -> {
                    return sendCommand(context, "sendall");
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
                        return sendCommand(context, "autotransfer", state);
                    })
                )
            )
            
            // /thqueueadmin maxplayers <number>
            .then(Commands.literal("maxplayers")
                .then(Commands.argument("max", IntegerArgumentType.integer(1, 1000))
                    .executes(context -> {
                        int max = IntegerArgumentType.getInteger(context, "max");
                        return sendCommand(context, "maxplayers", String.valueOf(max));
                    })
                )
            )
            
            // /thqueueadmin info
            .then(Commands.literal("info")
                .executes(context -> {
                    return sendCommand(context, "info");
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
                        return sendCommand(context, "lobbymode", state);
                    })
                )
            )
            
            // /thqueueadmin send <player>
            .then(Commands.literal("send")
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(context -> {
                        String playerName = StringArgumentType.getString(context, "player");
                        return sendCommand(context, "send", playerName);
                    })
                )
            )
            
            // /thqueueadmin sendtolobby <player> - Execute directly on main server
            .then(Commands.literal("sendtolobby")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        return executeSendToLobby(context);
                    })
                )
            )
        );
    }
    
    /**
     * Initialize the command queue and transfer manager
     */
    public static void initialize(DatabaseCommandQueue queue, ServerTransferManager tm) {
        commandQueue = queue;
        transferManager = tm;
    }
    
    /**
     * Execute sendtolobby command directly on main server
     */
    private static int executeSendToLobby(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cThis command can only be used by players!"));
            return 0;
        }
        
        if (transferManager == null) {
            player.sendSystemMessage(Component.literal("§c§l[ERROR] §7Transfer manager not initialized!"));
            return 0;
        }
        
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            transferManager.transferToServer(target, "lobby");
            player.sendSystemMessage(Component.literal("§a§l[QUEUE] §7Sent §6" + target.getName().getString() + "§7 to lobby server"));
            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c§l[ERROR] §7Failed to send player: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int sendCommand(CommandContext<CommandSourceStack> context, String command, String... args) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cThis command can only be used by players!"));
            return 0;
        }
        
        if (commandQueue == null) {
            player.sendSystemMessage(Component.literal("§c§l[ERROR] §7Database not initialized!"));
            return 0;
        }
        
        // Add command to database queue
        boolean success = commandQueue.addCommand(command, args, player.getUUID(), player.getName().getString());
        
        if (success) {
            player.sendSystemMessage(Component.literal("§e§l[QUEUE] §7Command sent to lobby server via database..."));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c§l[ERROR] §7Failed to send command to database!"));
            return 0;
        }
    }
}
