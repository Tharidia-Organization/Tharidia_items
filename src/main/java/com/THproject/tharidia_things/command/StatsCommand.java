package com.THproject.tharidia_things.command;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.registry.ModStats;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;

public class StatsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("stats")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("stat", StringArgumentType.string())
                                        .suggests(StatsCommand::suggestStats)
                                        .then(Commands.literal("get")
                                                .executes(StatsCommand::getStat))
                                        .then(Commands.literal("set")
                                                .then(Commands
                                                        .argument("value",
                                                                IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                                        .executes(StatsCommand::setStat)))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                                        .executes(StatsCommand::addStat)))
                                        .then(Commands.literal("reset")
                                                .executes(StatsCommand::resetStat)))));
    }

    private static CompletableFuture<Suggestions> suggestStats(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {

        for (String statName : ModStats.getAllStatNames())
            builder.suggest(statName);

        return builder.buildFuture();
    }

    private static int getStat(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            var player = EntityArgument.getPlayer(context, "player");
            String statName = StringArgumentType.getString(context, "stat");

            var stat = statFromResourceLocation(statName);
            if (stat == null) {
                source.sendSuccess(() -> Component.literal("Stat not found").withColor(0xFF0000), false);
                return 1;
            }

            int value = player.getStats().getValue(stat);
            source.sendSuccess(() -> Component.literal(
                    String.format("Stat %s for %s is %d", statName, player.getName().getString(), value)), false);
            return 1;
        } catch (CommandSyntaxException e) {
            return 0;
        }
    }

    private static int setStat(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            var player = EntityArgument.getPlayer(context, "player");
            String statName = StringArgumentType.getString(context, "stat");
            int value = IntegerArgumentType.getInteger(context, "value");

            var stat = statFromResourceLocation(statName);
            if (stat == null) {
                source.sendSuccess(() -> Component.literal("Stat not found").withColor(0xFF0000), false);
                return 1;
            }

            player.getStats().setValue(player, stat, value);
            source.sendSuccess(
                    () -> Component.literal(
                            String.format("Stat %s set to %d for %s", statName, value, player.getName().getString())),
                    false);
            return 1;
        } catch (CommandSyntaxException e) {
            return 0;
        }
    }

    private static int addStat(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            var player = EntityArgument.getPlayer(context, "player");
            String statName = StringArgumentType.getString(context, "stat");
            int value = IntegerArgumentType.getInteger(context, "value");

            var stat = statFromResourceLocation(statName);
            if (stat == null) {
                source.sendSuccess(() -> Component.literal("Stat not found").withColor(0xFF0000), false);
                return 1;
            }

            player.getStats().setValue(player, stat,
                    player.getStats().getValue(stat) + value);
            source.sendSuccess(() -> Component.literal(
                    String.format("Added %d to stat %s for %s", value, statName, player.getName().getString())),
                    false);
            return 1;
        } catch (CommandSyntaxException e) {
            return 0;
        }
    }

    private static int resetStat(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            var player = EntityArgument.getPlayer(context, "player");
            String statName = StringArgumentType.getString(context, "stat");

            var stat = statFromResourceLocation(statName);
            if (stat == null) {
                source.sendSuccess(() -> Component.literal("Stat not found").withColor(0xFF0000), false);
                return 1;
            }

            player.getStats().setValue(player, stat, 0);
            source.sendSuccess(
                    () -> Component.literal(
                            String.format("Stat %s reset for %s", statName, player.getName().getString())),
                    false);
            return 1;
        } catch (CommandSyntaxException e) {
            return 0;
        }
    }

    private static Stat<ResourceLocation> statFromResourceLocation(String statName) {
        ResourceLocation statRL = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, statName);
        var stat = BuiltInRegistries.CUSTOM_STAT.get(statRL);
        if (stat == null) {
            return null;
        }
        return Stats.CUSTOM.get(stat);
    }
}
