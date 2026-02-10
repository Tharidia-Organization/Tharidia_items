package com.THproject.tharidia_things.command;

import com.THproject.tharidia_things.character.CharacterAttachments;
import com.THproject.tharidia_things.character.CharacterData;
import com.THproject.tharidia_things.character.CharacterEventHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for character creation management.
 * Provides admin tools to manage player character creation status.
 */
public class CharacterCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tharidia")
                .then(Commands.literal("character")
                        .requires(source -> source.hasPermission(4)) // Admin only
                        // /tharidia character create - for self
                        .then(Commands.literal("create")
                                .executes(context -> {
                                    try {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        return completeCharacterCreation(context.getSource(), player, "umano");
                                    } catch (CommandSyntaxException e) {
                                        context.getSource().sendFailure(
                                                Component.literal("This command can only be run by a player"));
                                        return 0;
                                    }
                                })
                                // /tharidia character create <player> [race]
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            return completeCharacterCreation(context.getSource(), target, "umano");
                                        })
                                        .then(Commands.argument("race", StringArgumentType.word())
                                                .suggests((context, builder) ->
                                                        SharedSuggestionProvider.suggest(
                                                                com.THproject.tharidia_things.character.RaceData.getValidRaceNames(), builder))
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    String race = StringArgumentType.getString(context, "race");
                                                    return completeCharacterCreation(context.getSource(), target, race);
                                                })
                                        )
                                )
                        )
                        // /tharidia character reset - for self
                        .then(Commands.literal("reset")
                                .executes(context -> {
                                    try {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        return resetCharacterCreation(context.getSource(), player);
                                    } catch (CommandSyntaxException e) {
                                        context.getSource().sendFailure(
                                                Component.literal("This command can only be run by a player"));
                                        return 0;
                                    }
                                })
                                // /tharidia character reset <player> - for other player
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            return resetCharacterCreation(context.getSource(), target);
                                        })
                                )
                        )
                        // /tharidia character status [player] - check status
                        .then(Commands.literal("status")
                                .executes(context -> {
                                    try {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        return showCharacterStatus(context.getSource(), player);
                                    } catch (CommandSyntaxException e) {
                                        context.getSource().sendFailure(
                                                Component.literal("This command can only be run by a player"));
                                        return 0;
                                    }
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            return showCharacterStatus(context.getSource(), target);
                                        })
                                )
                        )
                        // /tharidia character teleport [player] - teleport to character creation
                        .then(Commands.literal("teleport")
                                .executes(context -> {
                                    try {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        return teleportToCharacterCreation(context.getSource(), player);
                                    } catch (CommandSyntaxException e) {
                                        context.getSource().sendFailure(
                                                Component.literal("This command can only be run by a player"));
                                        return 0;
                                    }
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            return teleportToCharacterCreation(context.getSource(), target);
                                        })
                                )
                        )
                )
        );
    }

    private static int completeCharacterCreation(CommandSourceStack source, ServerPlayer player, String race) {
        if (CharacterEventHandler.hasCompletedCharacterCreation(player)) {
            source.sendFailure(Component.literal("Player " + player.getName().getString() +
                    " has already created their character"));
            return 0;
        }

        if (!com.THproject.tharidia_things.character.RaceData.isValidRace(race)) {
            source.sendFailure(Component.literal("Invalid race: " + race));
            return 0;
        }

        // Save race before completing
        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
        characterData.setSelectedRace(race);

        CharacterEventHandler.completeCharacterCreation(player);
        source.sendSuccess(() -> Component.literal("Character creation completed for " +
                player.getName().getString() + " (race: " + race + ")"), true);
        return 1;
    }

    private static int resetCharacterCreation(CommandSourceStack source, ServerPlayer player) {
        CharacterEventHandler.resetCharacterCreation(player);
        source.sendSuccess(() -> Component.literal("Character creation status reset for " +
                player.getName().getString()), true);
        return 1;
    }

    private static int showCharacterStatus(CommandSourceStack source, ServerPlayer player) {
        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
        CharacterData.CreationStage stage = characterData.getStage();
        String race = characterData.getSelectedRace();

        String stageColor = switch (stage) {
            case COMPLETED -> "§a";
            case AWAITING_RACE -> "§e";
            case NOT_STARTED -> "§c";
        };

        source.sendSuccess(() -> Component.literal("Character status for " +
                player.getName().getString() + ": " + stageColor + stage.name() +
                (race != null ? " §7(race: " + race + ")" : "")), false);
        return 1;
    }

    private static int teleportToCharacterCreation(CommandSourceStack source, ServerPlayer player) {
        // Reset to AWAITING_RACE so the player enters the race selection flow
        CharacterData characterData = player.getData(CharacterAttachments.CHARACTER_DATA);
        characterData.setStage(CharacterData.CreationStage.AWAITING_RACE);
        characterData.setSelectedRace(null);

        // Use the public teleport method
        CharacterEventHandler.teleportToCharacterDimension(player);

        source.sendSuccess(() -> Component.literal("Teleporting " + player.getName().getString() +
                " to character creation dimension"), true);
        return 1;
    }
}
