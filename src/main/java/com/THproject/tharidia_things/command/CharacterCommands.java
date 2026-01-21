package com.THproject.tharidia_things.command;

import com.THproject.tharidia_things.character.CharacterAttachments;
import com.THproject.tharidia_things.character.CharacterEventHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                                        return completeCharacterCreation(context.getSource(), player);
                                    } catch (CommandSyntaxException e) {
                                        context.getSource().sendFailure(
                                                Component.literal("This command can only be run by a player"));
                                        return 0;
                                    }
                                })
                                // /tharidia character create <player> - for other player
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            return completeCharacterCreation(context.getSource(), target);
                                        })
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

    private static int completeCharacterCreation(CommandSourceStack source, ServerPlayer player) {
        if (CharacterEventHandler.hasCompletedCharacterCreation(player)) {
            source.sendFailure(Component.literal("Player " + player.getName().getString() +
                    " has already created their character"));
            return 0;
        }

        CharacterEventHandler.completeCharacterCreation(player);
        source.sendSuccess(() -> Component.literal("Character creation completed for " +
                player.getName().getString()), true);
        return 1;
    }

    private static int resetCharacterCreation(CommandSourceStack source, ServerPlayer player) {
        CharacterEventHandler.resetCharacterCreation(player);
        source.sendSuccess(() -> Component.literal("Character creation status reset for " +
                player.getName().getString()), true);
        return 1;
    }

    private static int showCharacterStatus(CommandSourceStack source, ServerPlayer player) {
        boolean hasCreated = CharacterEventHandler.hasCompletedCharacterCreation(player);
        String status = hasCreated ? "§aCompleted" : "§cNot completed";

        source.sendSuccess(() -> Component.literal("Character status for " +
                player.getName().getString() + ": " + status), false);
        return 1;
    }

    private static int teleportToCharacterCreation(CommandSourceStack source, ServerPlayer player) {
        // Reset character creation status first
        CharacterEventHandler.resetCharacterCreation(player);

        // The event handler will automatically teleport the player
        // We need to trigger a re-check
        player.server.execute(() -> {
            player.server.execute(() -> {
                // Force a dimension change check which will trigger teleport
                if (!CharacterEventHandler.hasCompletedCharacterCreation(player)) {
                    var characterLevel = player.server.getLevel(CharacterEventHandler.CHARACTER_DIMENSION);
                    if (characterLevel != null) {
                        var spawnPos = CharacterEventHandler.getPlayerSpawnPos(player.getUUID());
                        player.teleportTo(characterLevel, spawnPos.getX() + 0.5, spawnPos.getY() + 1,
                                spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                    }
                }
            });
        });

        source.sendSuccess(() -> Component.literal("Teleporting " + player.getName().getString() +
                " to character creation dimension"), true);
        return 1;
    }
}
