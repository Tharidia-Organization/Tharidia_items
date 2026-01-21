package com.THproject.tharidia_things.command;

import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.features.Revive;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

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
                                        .then(Commands.argument("can_revive", BoolArgumentType.bool())
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

    private static int fallPlayer(CommandContext<CommandSourceStack> context, boolean has_to_read_value) {
        try {
            var player = EntityArgument.getPlayer(context, "player");
            boolean can_revive = true;
            if (has_to_read_value) {
                can_revive = BoolArgumentType.getBool(context, "can_revive");
            }
            Revive.fallPlayer(player, can_revive);
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
