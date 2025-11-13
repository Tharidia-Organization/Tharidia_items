package com.tharidia.tharidia_things.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tharidia.tharidia_things.trade.TradeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for managing trades
 */
public class TradeCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trade")
            .requires(source -> source.hasPermission(2)) // Requires OP level 2
            .then(Commands.literal("reset")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                        return resetPlayerTrade(context.getSource(), player);
                    })
                )
            )
            .then(Commands.literal("resetbetween")
                .then(Commands.argument("player1", EntityArgument.player())
                    .then(Commands.argument("player2", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer player1 = EntityArgument.getPlayer(context, "player1");
                            ServerPlayer player2 = EntityArgument.getPlayer(context, "player2");
                            return resetTradeBetween(context.getSource(), player1, player2);
                        })
                    )
                )
            )
        );
    }

    private static int resetPlayerTrade(CommandSourceStack source, ServerPlayer player) {
        if (TradeManager.isPlayerInTrade(player.getUUID())) {
            TradeManager.cancelPlayerSession(player.getUUID());
            player.closeContainer();
            source.sendSuccess(() -> Component.literal("§aScambio di " + player.getName().getString() + " resettato con successo."), true);
            player.sendSystemMessage(Component.literal("§cIl tuo scambio è stato annullato da un amministratore."));
            return 1;
        } else {
            source.sendFailure(Component.literal("§c" + player.getName().getString() + " non è in uno scambio attivo."));
            return 0;
        }
    }

    private static int resetTradeBetween(CommandSourceStack source, ServerPlayer player1, ServerPlayer player2) {
        boolean player1InTrade = TradeManager.isPlayerInTrade(player1.getUUID());
        boolean player2InTrade = TradeManager.isPlayerInTrade(player2.getUUID());

        if (!player1InTrade && !player2InTrade) {
            source.sendFailure(Component.literal("§cNessuno dei due giocatori è in uno scambio attivo."));
            return 0;
        }

        // Cancel both players' trades
        if (player1InTrade) {
            TradeManager.cancelPlayerSession(player1.getUUID());
            player1.closeContainer();
            player1.sendSystemMessage(Component.literal("§cIl tuo scambio è stato annullato da un amministratore."));
        }

        if (player2InTrade) {
            TradeManager.cancelPlayerSession(player2.getUUID());
            player2.closeContainer();
            player2.sendSystemMessage(Component.literal("§cIl tuo scambio è stato annullato da un amministratore."));
        }

        source.sendSuccess(() -> Component.literal("§aScambi resettati con successo per " + 
            player1.getName().getString() + " e " + player2.getName().getString() + "."), true);
        return 1;
    }
}
