package com.THproject.tharidia_things.command;

import com.mojang.brigadier.CommandDispatcher;
import com.THproject.tharidia_things.gui.TradeMenu;
import com.THproject.tharidia_things.trade.TradeManager;
import com.THproject.tharidia_things.trade.TradeSession;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/**
 * Commands for market testing and management
 */
public class MarketCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tharidia")
            .requires(source -> source.hasPermission(2)) // Requires OP level 2
            .then(Commands.literal("market")
                .then(Commands.literal("testTrigger")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        return openTestTrade(context.getSource(), player);
                    })
                )
            )
        );
    }

    private static int openTestTrade(CommandSourceStack source, ServerPlayer player) {
        // Check if player is already in a trade
        if (TradeManager.isPlayerInTrade(player.getUUID())) {
            source.sendFailure(Component.literal("§cSei già in uno scambio attivo! Usa /trade reset per cancellarlo."));
            return 0;
        }

        // Create a test session using TradeManager
        TradeSession testSession = TradeManager.createTestSession(player);
        if (testSession == null) {
            source.sendFailure(Component.literal("§cImpossibile creare una sessione di test."));
            return 0;
        }

        // Open the trade GUI using SimpleMenuProvider
        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, p) -> new TradeMenu(containerId, playerInventory, testSession, p),
            Component.literal("Scambio di Test")
        ), buf -> {
            buf.writeUUID(testSession.getSessionId());
            buf.writeUUID(player.getUUID()); // Same player for test
            buf.writeUtf("Giocatore di Test");
        });
        
        source.sendSuccess(() -> Component.literal("§aGUI di test aperta! Puoi testare l'interfaccia di scambio."), true);
        player.sendSystemMessage(Component.literal("§6Modalità test: stai simulando uno scambio con un giocatore fittizio."));
        
        return 1;
    }
}
