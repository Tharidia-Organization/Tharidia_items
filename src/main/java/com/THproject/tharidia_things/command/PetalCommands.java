package com.THproject.tharidia_things.command;

import com.THproject.tharidia_things.block.alchemist.PetalColorRegistry;
import com.THproject.tharidia_things.block.alchemist.PetalColorRegistry.PetalColor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * /tharidia petal <colore> [quantità] [giocatore]
 *
 * Dà al giocatore target un petalo dell'alchimista nel colore specificato.
 * Richiede permesso livello 2 (op).
 *
 * Colori disponibili: rosso (1), giallo (2), blu (3),
 *                     arancione (12), verde (23), viola (13)
 */
public class PetalCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tharidia")
                .then(Commands.literal("petal")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("colore", StringArgumentType.word())
                                .suggests((ctx, builder) ->
                                        SharedSuggestionProvider.suggest(PetalColorRegistry.allNames(), builder))
                                // /tharidia petal <colore>  — al mittente, quantità 1
                                .executes(ctx -> givePetal(ctx.getSource(), null, 1,
                                        StringArgumentType.getString(ctx, "colore")))
                                // /tharidia petal <colore> <quantità>
                                .then(Commands.argument("quantita", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> givePetal(ctx.getSource(), null,
                                                IntegerArgumentType.getInteger(ctx, "quantita"),
                                                StringArgumentType.getString(ctx, "colore")))
                                        // /tharidia petal <colore> <quantità> <giocatore>
                                        .then(Commands.argument("giocatore", EntityArgument.player())
                                                .executes(ctx -> givePetal(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "giocatore"),
                                                        IntegerArgumentType.getInteger(ctx, "quantita"),
                                                        StringArgumentType.getString(ctx, "colore")))
                                        )
                                )
                        )
                )
        );
    }

    private static int givePetal(CommandSourceStack source, ServerPlayer target, int amount, String colorName) {
        // Resolve target player
        ServerPlayer player = target;
        if (player == null) {
            try {
                player = source.getPlayerOrException();
            } catch (CommandSyntaxException e) {
                source.sendFailure(Component.literal("Specifica un giocatore oppure esegui il comando come player."));
                return 0;
            }
        }

        // Resolve color
        PetalColor color = PetalColorRegistry.fromName(colorName).orElse(null);
        if (color == null) {
            source.sendFailure(Component.literal(
                    "Colore '" + colorName + "' non riconosciuto. Colori validi: " +
                    String.join(", ", PetalColorRegistry.allNames())));
            return 0;
        }

        // Create and give the petal stack
        ItemStack stack = PetalColorRegistry.createPetalStack(color, amount);
        boolean added = player.getInventory().add(stack);
        if (!added) {
            player.drop(stack, false);
        }

        final ServerPlayer finalPlayer = player;
        source.sendSuccess(() -> Component.literal(
                "Dato " + amount + "x petalo " + color.name +
                " (valore=" + color.value + ") a " + finalPlayer.getName().getString()), true);
        return 1;
    }
}
