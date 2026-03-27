package com.THproject.tharidia_things.command;

import com.THproject.tharidia_things.compoundTag.CookAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class CookCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("thmaster")
            .requires(source -> source.hasPermission(4))
            .then(Commands.literal("cook")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.literal("level_up")
                        .executes(CookCommand::levelUp))
                    .then(Commands.literal("level")
                        .then(Commands.literal("get")
                            .executes(CookCommand::getLevel))
                        .then(Commands.literal("set")
                            .then(Commands.argument("level", IntegerArgumentType.integer(1, CookAttachments.MAX_LEVEL))
                                .executes(CookCommand::setLevel))))
                    .then(Commands.literal("progress")
                        .then(Commands.literal("set")
                            .then(Commands.argument("progress", IntegerArgumentType.integer(0, CookAttachments.MAX_PROGRESS))
                                .executes(CookCommand::setProgress)))
                        .then(Commands.literal("get")
                            .executes(CookCommand::getProgress))))));
    }

    private static int levelUp(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            Player player = EntityArgument.getPlayer(context, "player");
            CookAttachments cook_data = player.getData(CookAttachments.COOK_DATA.get());
            if (cook_data != null) {
                if (cook_data.levelUp()) {
                    source.sendSuccess(() -> Component.literal("Leveled up " + player.getName().getString()).withColor(0x00FF00), false);
                } else {
                    source.sendSuccess(() -> Component.literal("Player cannot level up").withColor(0xFF0000), false);
                }
                return 1;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setProgress(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            Player player = EntityArgument.getPlayer(context, "player");
            int progress = IntegerArgumentType.getInteger(context, "progress");
            CookAttachments cook_data = player.getData(CookAttachments.COOK_DATA.get());
            if (cook_data != null) {
                cook_data.setProgress(progress);
                source.sendSuccess(
                        () -> Component.literal(
                                String.format("Set %s's progress at %d", player.getName().getString(), progress)),
                        false);
                return 1;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getProgress(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            Player player = EntityArgument.getPlayer(context, "player");
            CookAttachments cook_data = player.getData(CookAttachments.COOK_DATA.get());
            if (cook_data != null) {
                int progress = cook_data.getProgress();
                source.sendSuccess(
                        () -> Component.literal(
                                String.format("%s has progress %d", player.getName().getString(), progress)),
                        false);
                return 1;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setLevel(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            Player player = EntityArgument.getPlayer(context, "player");
            int level = IntegerArgumentType.getInteger(context, "level");
            CookAttachments cook_data = player.getData(CookAttachments.COOK_DATA.get());
            if (cook_data != null) {
                cook_data.setLevel(level);
                source.sendSuccess(
                        () -> Component.literal(
                                String.format("Set %s's level at %d", player.getName().getString(), level)),
                        false);
                return 1;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getLevel(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            Player player = EntityArgument.getPlayer(context, "player");
            CookAttachments cook_data = player.getData(CookAttachments.COOK_DATA.get());
            if (cook_data != null) {
                int level = cook_data.getLevel();
                source.sendSuccess(
                        () -> Component.literal(
                                String.format("%s has level %d", player.getName().getString(), level)),
                        false);
                return 1;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
