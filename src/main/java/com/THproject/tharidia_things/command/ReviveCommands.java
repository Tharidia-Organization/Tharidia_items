package com.THproject.tharidia_things.command;

import java.util.concurrent.CompletableFuture;

import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.features.Revive;
import com.THproject.tharidia_things.features.Revive.FallState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ReviveCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("thmaster")
                        .then(Commands.literal("fall")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> fallPlayer(context, false))
                                        .then(Commands.argument("fallState", StringArgumentType.string())
                                                .suggests(ReviveCommands::suggestFallState)
                                                .executes(context -> fallPlayer(context, true))))));

        dispatcher.register(
                Commands.literal("thmaster")
                        .then(Commands.literal("revive")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ReviveCommands::revivePlayer))));

        dispatcher.register(
                Commands.literal("thmaster")
                        .then(Commands.literal("can_fall")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("can_fall", BoolArgumentType.bool())
                                                .executes(ReviveCommands::setCanFall)))));
    }

    private static CompletableFuture<Suggestions> suggestFallState(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (FallState state : FallState.values())
            builder.suggest(state.toString().toLowerCase());
        return builder.buildFuture();
    }

    private static int fallPlayer(CommandContext<CommandSourceStack> context, boolean has_to_read_value) {
        try {
            var player = EntityArgument.getPlayer(context, "player");
            String fallState = FallState.NONE.toString();
            if (has_to_read_value) {
                fallState = StringArgumentType.getString(context, "fallState");
            }
            Revive.fallPlayer(player, FallState.valueOf(fallState.toUpperCase()));
            context.getSource().sendSuccess(
                    () -> Component.literal(String.format("Fallen %s", player.getName().getString())), false);
        } catch (CommandSyntaxException e) {
            return 0;
        }
        return 1;
    }

    private static int revivePlayer(CommandContext<CommandSourceStack> context) {
        try {
            var player = EntityArgument.getPlayer(context, "player");
            Revive.revivePlayer(player);
            context.getSource().sendSuccess(
                    () -> Component.literal(String.format("Revived player %s", player.getName().getString())), false);
        } catch (CommandSyntaxException e) {
            return 0;
        }
        return 1;
    }

    private static int setCanFall(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            boolean can_fall = BoolArgumentType.getBool(context, "can_fall");
            player.getData(ReviveAttachments.REVIVE_DATA.get()).setCanFall(can_fall);

            if (can_fall)
                context.getSource().sendSuccess(
                        () -> Component.literal(String.format("Player %s can now fall", player.getName().getString())),
                        can_fall);
            else
                context.getSource().sendSuccess(
                        () -> Component
                                .literal(String.format("Player %s can no longer fall", player.getName().getString())),
                        can_fall);
        } catch (CommandSyntaxException e) {
            return 0;
        }

        return 1;
    }
}
