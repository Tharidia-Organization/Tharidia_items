package com.THproject.tharidia_things.command;

import java.util.UUID;

import com.THproject.tharidia_things.compoundTag.CookAttachments;
import com.THproject.tharidia_things.cook.CookHatData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CookCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("thmaster")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("cook")
                        .then(Commands.literal("give_hat")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CookCommand::giveHat)
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                                                .executes(CookCommand::giveHat))))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("level_up")
                                        .executes(CookCommand::levelUp))
                                .then(Commands.literal("level")
                                        .then(Commands.literal("get")
                                                .executes(CookCommand::getLevel))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("level",IntegerArgumentType.integer(1, CookAttachments.MAX_LEVEL))
                                                        .executes(CookCommand::setLevel))))
                                .then(Commands.literal("progress")
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("progress", IntegerArgumentType.integer(0, CookAttachments.MAX_PROGRESS))
                                                        .executes(CookCommand::setProgress)))
                                        .then(Commands.literal("get")
                                                .executes(CookCommand::getProgress))))));
    }

    private static int giveHat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player sourcePlayer = source.getPlayer();
        Player player = EntityArgument.getPlayer(context, "player");
        CookAttachments attachment = player.getData(CookAttachments.COOK_DATA.get());
        int playerLevel = attachment.getLevel();
        int givenHatLevel = 1; // Fallback set to 1

        /*
         * The command is called with an option "level" argument.
         * If this argument is not given it launch the IllegalArgumentException
         * and we handle it by setting the hat's level with the player cook level
         */
        try {
            givenHatLevel = IntegerArgumentType.getInteger(context, "level");
            int temp = givenHatLevel;
            if (temp != playerLevel)
                source.sendSuccess(
                        () -> Component.literal(String.format("Warning: the player cook level is not %d", temp))
                                .withColor(0xE8BE33),
                        false);
        } catch (IllegalArgumentException e) {
            givenHatLevel = attachment.getLevel();
        }

        UUID playerUUID = player.getUUID();

        ItemStack hat = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("age_of_fight:cook_hat_" + givenHatLevel + "_helmet")));
        hat.set(CookHatData.PLAYER_UUID, playerUUID);
        hat.set(CookHatData.PLAYER_NAME, player.getName().getString());
        sourcePlayer.getInventory().add(hat);

        source.sendSuccess(() -> Component.literal(String.format("Give %s's hat", player.getName().getString())), false);

        return 1;
    }

    private static int levelUp(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            Player player = EntityArgument.getPlayer(context, "player");
            CookAttachments cook_data = player.getData(CookAttachments.COOK_DATA.get());
            if (cook_data != null) {
                if (cook_data.levelUp()) {
                    source.sendSuccess(
                            () -> Component.literal("Leveled up " + player.getName().getString()).withColor(0x00FF00),
                            false);
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
