package com.THproject.tharidia_things.command;

import java.util.concurrent.CompletableFuture;

import com.THproject.tharidia_things.poison.PoisonAttachments;
import com.THproject.tharidia_things.poison.PoisonHelper;
import com.THproject.tharidia_things.poison.PoisonHelper.PoisonType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class PoisonCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("thmaster")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("poison")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests(PoisonCommand::suggestType)
                                                .executes(PoisonCommand::setPoison)))
                                .then(Commands.literal("cure")
                                        .executes(PoisonCommand::removePoison))
                                .then(Commands.literal("get")
                                        .then(Commands.literal("progress")
                                                .executes(PoisonCommand::getProgress))
                                        .then(Commands.literal("type")
                                                .executes(PoisonCommand::getType))))));
    }

    private static CompletableFuture<Suggestions> suggestType(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        for (PoisonType poisonType : PoisonType.values()) {
            if (poisonType != PoisonType.NONE)
                builder.suggest(poisonType.toString().toLowerCase());
        }
        return builder.buildFuture();
    }

    private static int setPoison(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player player = EntityArgument.getPlayer(context, "player");
        String type = StringArgumentType.getString(context, "type");
        PoisonAttachments attachments = PoisonHelper.getAttachment(player);

        if (attachments == null) {
            source.sendSuccess(() -> Component.literal("Failed to get player's poison data").withColor(0xFF0000),
                    false);
            return 1;
        }

        attachments.setPoisoned(PoisonType.valueOf(type.toUpperCase()));
        source.sendSuccess(
                () -> Component
                        .literal(String.format("Set poison state to %s at %s", type, player.getName().getString())),
                false);
        return 1;
    }

    private static int removePoison(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player player = EntityArgument.getPlayer(context, "player");
        PoisonAttachments attachments = PoisonHelper.getAttachment(player);

        if (attachments == null) {
            source.sendSuccess(() -> Component.literal("Failed to get player's poison data").withColor(0xFF0000),
                    false);
            return 1;
        }

        attachments.removePoison();
        source.sendSuccess(
                () -> Component.literal(String.format("Removed poison state at %s", player.getName().getString())),
                false);
        return 1;
    }

    private static int getProgress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player player = EntityArgument.getPlayer(context, "player");
        PoisonAttachments attachments = PoisonHelper.getAttachment(player);

        if (attachments == null) {
            source.sendSuccess(() -> Component.literal("Failed to get player's poison data").withColor(0xFF0000),
                    false);
            return 1;
        }

        float progress = attachments.getProgress();
        source.sendSuccess(
                () -> Component
                        .literal(String.format("%s has %.2f poison type", player.getName().getString(), progress)),
                false);

        return 1;
    }

    private static int getType(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player player = EntityArgument.getPlayer(context, "player");
        PoisonAttachments attachments = PoisonHelper.getAttachment(player);

        if (attachments == null) {
            source.sendSuccess(() -> Component.literal("Failed to get player's poison data").withColor(0xFF0000),
                    false);
            return 1;
        }

        String type = attachments.getPoisonType().toString();
        source.sendSuccess(
                () -> Component
                        .literal(String.format("%s has %s poison type", player.getName().getString(), type)),
                false);

        return 1;
    }
}
