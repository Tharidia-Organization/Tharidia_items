package com.THproject.tharidia_things.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.THproject.tharidia_things.compoundTag.BattleGauntleAttachments;
import com.THproject.tharidia_things.event.BattleLogic;
import com.THproject.tharidia_things.util.PlayerNameHelper;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BattleCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("battle")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("start")
                                .then(Commands.argument("player1", EntityArgument.player())
                                        .then(Commands.argument("player2", EntityArgument.player())
                                                .executes(BattleCommands::startBattle))))
                        .then(Commands.literal("end")
                                .then(Commands.argument("winner", EntityArgument.player())
                                        .executes(BattleCommands::endBattle)))
                        .then(Commands.literal("stop")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(BattleCommands::stopBattle)))
                        .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player())
                                .executes(BattleCommands::getBattle)))
                        .then(Commands.literal("help")
                                .executes(BattleCommands::help)));

    }

    private static int startBattle(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        try {
            var player1 = EntityArgument.getPlayer(context, "player1");
            var player2 = EntityArgument.getPlayer(context, "player2");

            if (player1.getUUID().equals(player2.getUUID())) {
                source.sendSuccess(() -> Component.literal("A player cannot battle themselves!"), false);
                return 1;
            }

            BattleGauntleAttachments player1Attachments = player1
                    .getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
            BattleGauntleAttachments player2Attachments = player2
                    .getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

            if (player1Attachments.getInBattle() || player2Attachments.getInBattle()) {
                source.sendSuccess(() -> Component.literal("One of the players is already in a battle!"), false);
                return 1;
            }

            BattleLogic.startBattle(player1, player2);
            source.sendSuccess(() -> Component.literal("Battle started between " +
                    PlayerNameHelper.getChosenName((ServerPlayer) player1) +
                    " and " +
                    PlayerNameHelper.getChosenName((ServerPlayer) player2)),
                    false);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }

        return 1;
    }

    private static int endBattle(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        try {
            var winner = EntityArgument.getPlayer(context, "winner");
            var loser = BattleLogic.getChallengerPlayer(winner);
            if (loser == null) {
                source.sendSuccess(() -> Component.literal("The player is not in a battle!"), false);
                return 1;
            }

            BattleLogic.finischBattle(winner, loser);
            source.sendSuccess(() -> Component.literal("Battle ended between " +
                    PlayerNameHelper.getChosenName((ServerPlayer) winner) +
                    " and " +
                    PlayerNameHelper.getChosenName((ServerPlayer) loser)),
                    false);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }

        return 1;
    }

    private static int stopBattle(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        try {
            var player1 = EntityArgument.getPlayer(context, "player");
            var player2 = BattleLogic.getChallengerPlayer(player1);
            if (player2 == null) {
                source.sendSuccess(() -> Component.literal("The player is not in a battle!"), false);
                return 1;
            }
            BattleLogic.exitPlayerBattle(player1);
            BattleLogic.exitPlayerBattle(player2);
            source.sendSuccess(() -> Component.literal("Battle stopped between " +
                    PlayerNameHelper.getChosenName((ServerPlayer) player1) +
                    " and " +
                    PlayerNameHelper.getChosenName((ServerPlayer) player2)),
                    false);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }

        return 1;
    }

    private static int getBattle(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        try {
            var player = EntityArgument.getPlayer(context, "player");
            BattleGauntleAttachments playerAttachments = player
                    .getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

            if (playerAttachments.getInBattle()) {
                var challenger = BattleLogic.getChallengerPlayer(player);
                source.sendSuccess(() -> Component.literal(
                        PlayerNameHelper.getChosenName((ServerPlayer) player) +
                                " is in a battle with "
                                + PlayerNameHelper.getChosenName((ServerPlayer) challenger)),
                        false);
            } else {
                source.sendSuccess(() -> Component.literal(player.getName().getString() + " is not in a battle."),
                        false);
            }
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
            return 0;
        }
        return 1;
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        source.sendSuccess(() -> Component.literal(
                "Battle Commands:\n" +
                        "/battle start <player1> <player2> - Start a battle between two players.\n" +
                        "/battle end <winner> - End the battle, specifying the winner.\n" +
                        "/battle stop <player> - Forcefully stop the battle for the specified player.\n" +
                        "/battle get <player> - Get the battle status of the specified player.\n" +
                        "/battle help - Show this help message."),
                false);
        return 1;
    }
}
