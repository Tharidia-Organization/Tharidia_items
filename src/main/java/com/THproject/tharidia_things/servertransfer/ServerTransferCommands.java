package com.THproject.tharidia_things.servertransfer;

import com.THproject.tharidia_things.TharidiaThings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class ServerTransferCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Comando /tharidiathings transfer <server> [player]
        dispatcher.register(Commands.literal("tharidia")
            .then(Commands.literal("transfer")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("server", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest("main");
                        builder.suggest("dev");
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            String targetServer = StringArgumentType.getString(context, "server");
                            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
                            return transferPlayerToServer(context.getSource(), targetServer, targetPlayer);
                        })
                    )
                    .executes(context -> {
                        String targetServer = StringArgumentType.getString(context, "server");
                        return transferToServer(context.getSource(), targetServer);
                    })
                )
                .executes(context -> {
                    showServerList(context.getSource());
                    return 1;
                })
            )
            .then(Commands.literal("devaccess")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("add")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                            .executes(context -> addToDevWhitelist(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "player"),
                                StringArgumentType.getString(context, "reason")
                            ))
                        )
                        .executes(context -> addToDevWhitelist(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "player"),
                            "Nessuna motivazione fornita"
                        ))
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> removeFromDevWhitelist(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "player")
                        ))
                    )
                )
                .then(Commands.literal("check")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> checkDevWhitelist(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "player")
                        ))
                    )
                )
                .then(Commands.literal("list")
                    .executes(context -> listDevWhitelist(context.getSource()))
                )
            )
        );
    }
    
    private static int transferToServer(CommandSourceStack source, String targetServer) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cSolo i giocatori possono usare questo comando"));
            return 0;
        }
        
        // Verifica che il server sia valido
        if (!targetServer.equalsIgnoreCase("main") && !targetServer.equalsIgnoreCase("dev")) {
            player.sendSystemMessage(Component.literal("§cServer non valido. Server disponibili: main, dev"));
            return 0;
        }
        
        // Verifica che non si stia già trasferendo a se stesso
        String currentServer = ServerTransferManager.getCurrentServerName();
        if (targetServer.equalsIgnoreCase(currentServer)) {
            player.sendSystemMessage(Component.literal("§cSei già connesso al server " + targetServer));
            return 0;
        }
        
        // Crea token di trasferimento
        TransferTokenManager.createToken(player.getUUID(), targetServer);
        
        // Salva i dati del giocatore
        if (!ServerTransferManager.savePlayerDataForTransfer(player, targetServer)) {
            player.sendSystemMessage(Component.literal("§cErrore durante il salvataggio dei dati. Riprova più tardi."));
            return 0;
        }
        
        // Ottieni l'indirizzo del server di destinazione
        String serverAddress = ServerTransferManager.getServerAddress(targetServer);
        if (serverAddress == null) {
            player.sendSystemMessage(Component.literal("§cServer di destinazione non configurato"));
            return 0;
        }
        
        // Parse the address to get host and port
        String[] parts = serverAddress.split(":");
        if (parts.length != 2) {
            player.sendSystemMessage(Component.literal("§cIndirizzo server non valido: " + serverAddress));
            return 0;
        }
        
        String host = parts[0];
        int port;
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cPorta server non valida: " + parts[1]));
            return 0;
        }
        
        // Invia messaggio di trasferimento
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6=== Trasferimento Server ==="));
        player.sendSystemMessage(Component.literal("§eDati salvati correttamente!"));
        player.sendSystemMessage(Component.literal("§aTrasferimento in corso verso: §f" + targetServer));
        player.sendSystemMessage(Component.literal("§7Indirizzo: §b" + serverAddress));
        player.sendSystemMessage(Component.literal(""));
        
        // Log the full address for debugging
        TharidiaThings.LOGGER.info("Player {} transferring to server {} with address {}:{}", 
                player.getName().getString(), targetServer, host, port);
        
        // Invia il pacchetto di trasferimento al client
        player.connection.send(new ClientboundTransferPacket(host, port));
        
        return 1;
    }
    
    private static int transferPlayerToServer(CommandSourceStack source, String targetServer, ServerPlayer targetPlayer) {
        // Verifica che il server sia valido
        if (!targetServer.equalsIgnoreCase("main") && !targetServer.equalsIgnoreCase("dev")) {
            source.sendFailure(Component.literal("§cServer non valido. Server disponibili: main, dev"));
            return 0;
        }
        
        // Verifica che non si stia già trasferendo a se stesso
        String currentServer = ServerTransferManager.getCurrentServerName();
        if (targetServer.equalsIgnoreCase(currentServer)) {
            source.sendFailure(Component.literal("§cIl giocatore " + targetPlayer.getName().getString() + " è già connesso al server " + targetServer));
            return 0;
        }
        
        // Crea token di trasferimento
        TransferTokenManager.createToken(targetPlayer.getUUID(), targetServer);
        
        // Salva i dati del giocatore
        if (!ServerTransferManager.savePlayerDataForTransfer(targetPlayer, targetServer)) {
            source.sendFailure(Component.literal("§cErrore durante il salvataggio dei dati del giocatore. Riprova più tardi."));
            return 0;
        }
        
        // Ottieni l'indirizzo del server di destinazione
        String serverAddress = ServerTransferManager.getServerAddress(targetServer);
        if (serverAddress == null) {
            source.sendFailure(Component.literal("§cServer di destinazione non configurato"));
            return 0;
        }
        
        // Parse the address to get host and port
        String[] parts = serverAddress.split(":");
        if (parts.length != 2) {
            source.sendFailure(Component.literal("§cIndirizzo server non valido: " + serverAddress));
            return 0;
        }
        
        String host = parts[0];
        int port;
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("§cPorta server non valida: " + parts[1]));
            return 0;
        }
        
        // Invia messaggio di conferma al command sender
        source.sendSuccess(() -> Component.literal("§aTrasferimento di " + targetPlayer.getName().getString() + " verso " + targetServer + " in corso..."), true);
        
        // Invia messaggio al giocatore target
        targetPlayer.sendSystemMessage(Component.literal(""));
        targetPlayer.sendSystemMessage(Component.literal("§6=== Trasferimento Server ==="));
        targetPlayer.sendSystemMessage(Component.literal("§eDati salvati correttamente!"));
        targetPlayer.sendSystemMessage(Component.literal("§aTrasferimento in corso verso: §f" + targetServer));
        targetPlayer.sendSystemMessage(Component.literal("§7Indirizzo: §b" + serverAddress));
        targetPlayer.sendSystemMessage(Component.literal(""));
        
        // Log the full address for debugging
        TharidiaThings.LOGGER.info("Player {} being transferred to server {} with address {}:{} by {}", 
                targetPlayer.getName().getString(), targetServer, host, port, source.getTextName());
        
        // Invia il pacchetto di trasferimento al client del giocatore target
        targetPlayer.connection.send(new ClientboundTransferPacket(host, port));
        
        return 1;
    }
    
    private static void showServerList(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cSolo i giocatori possono usare questo comando"));
            return;
        }
        
        String currentServer = ServerTransferManager.getCurrentServerName();
        
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6=== Server Disponibili ==="));
        player.sendSystemMessage(Component.literal("§aServer attuale: §e" + currentServer));
        player.sendSystemMessage(Component.literal(""));
        
        if (!currentServer.equalsIgnoreCase("main")) {
            String mainAddress = ServerTransferManager.getServerAddress("main");
            player.sendSystemMessage(Component.literal("§7• §fmain §7- §b" + mainAddress));
        }
        
        if (!currentServer.equalsIgnoreCase("dev")) {
            String devAddress = ServerTransferManager.getServerAddress("dev");
            player.sendSystemMessage(Component.literal("§7• §fdev §7- §b" + devAddress));
        }
        
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Usa /tharidiathings transfer <server> per iniziare il trasferimento"));
    }

    private static int addToDevWhitelist(CommandSourceStack source, ServerPlayer targetPlayer, String reason) {
        UUIDWrapper executor = UUIDWrapper.fromSource(source);
        boolean success = DevWhitelistManager.addToWhitelist(
            targetPlayer.getUUID(),
            targetPlayer.getName().getString(),
            executor.uuid(),
            executor.name(),
            reason
        );

        if (success) {
            source.sendSuccess(() -> Component.literal(
                "§a✓ " + targetPlayer.getName().getString() + " aggiunto alla whitelist dev.\n" +
                "§7Motivo: " + reason
            ), true);
            targetPlayer.sendSystemMessage(Component.literal(
                "§a✓ Sei stato autorizzato ad accedere al server dev.\n" +
                "§7Puoi connetterti direttamente se hai l'IP configurato."
            ));
            return 1;
        } else {
            source.sendFailure(Component.literal("§cErrore durante l'aggiunta alla dev whitelist"));
            return 0;
        }
    }

    private static int removeFromDevWhitelist(CommandSourceStack source, ServerPlayer targetPlayer) {
        boolean success = DevWhitelistManager.removeFromWhitelist(targetPlayer.getUUID());
        if (success) {
            source.sendSuccess(() -> Component.literal(
                "§c✗ " + targetPlayer.getName().getString() + " rimosso dalla whitelist dev."
            ), true);
            targetPlayer.sendSystemMessage(Component.literal(
                "§c✗ Il tuo accesso diretto al server dev è stato revocato."
            ));
            return 1;
        } else {
            source.sendFailure(Component.literal("§cQuel giocatore non è nella whitelist dev"));
            return 0;
        }
    }

    private static int checkDevWhitelist(CommandSourceStack source, ServerPlayer targetPlayer) {
        boolean whitelisted = DevWhitelistManager.isWhitelisted(targetPlayer.getUUID());
        if (whitelisted) {
            source.sendSuccess(() -> Component.literal(
                "§a✓ " + targetPlayer.getName().getString() + " è autorizzato per l'accesso dev."
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                "§c✗ " + targetPlayer.getName().getString() + " NON è nella dev whitelist."
            ), false);
        }
        return 1;
    }

    private static int listDevWhitelist(CommandSourceStack source) {
        List<DevWhitelistManager.WhitelistEntry> entries = DevWhitelistManager.listEntries();
        source.sendSuccess(() -> Component.literal(
            "§6=== Dev Whitelist (" + entries.size() + ") ==="
        ), false);
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7(La whitelist è vuota)"), false);
            return 1;
        }
        for (DevWhitelistManager.WhitelistEntry entry : entries) {
            source.sendSuccess(() -> Component.literal(
                "§7• §f" + entry.username() +
                " §8(aggiunto da " + (entry.addedByName() != null ? entry.addedByName() : "sconosciuto") + ")" +
                (entry.reason() != null && !entry.reason().isEmpty() ? " §7- " + entry.reason() : "")
            ), false);
        }
        return 1;
    }

    private record UUIDWrapper(String name, java.util.UUID uuid) {
        static UUIDWrapper fromSource(CommandSourceStack source) {
            if (source.getEntity() instanceof ServerPlayer sp) {
                return new UUIDWrapper(sp.getName().getString(), sp.getUUID());
            }
            return new UUIDWrapper(source.getTextName(), null);
        }
    }
}
