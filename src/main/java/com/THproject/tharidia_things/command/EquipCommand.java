package com.THproject.tharidia_things.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.THproject.tharidia_things.network.EquipActionPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class EquipCommand {

    // Server-side cache of client files for suggestions
    private static final Map<UUID, List<String>> EQUIP_CACHE = new HashMap<>();
    private static final Map<UUID, List<String>> PENDING_SHARE_CACHE = new HashMap<>();

    public static void updateEquipCache(UUID playerId, List<String> equips) {
        EQUIP_CACHE.put(playerId, equips);
    }

    public static void updatePendingCache(UUID playerId, List<String> pending) {
        PENDING_SHARE_CACHE.put(playerId, pending);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("thmaster")
                .then(Commands.literal("equip")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("save")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(EquipCommand::saveEquip)))
                        .then(Commands.literal("load")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(EquipCommand::suggestEquipNames)
                                        .executes(EquipCommand::loadEquip)))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(EquipCommand::suggestEquipNames)
                                        .executes(EquipCommand::deleteEquip)))
                        .then(Commands.literal("rename")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(EquipCommand::suggestEquipNames)
                                        .then(Commands.argument("newName", StringArgumentType.string())
                                                .executes(EquipCommand::renameEquip))))
                        .then(Commands.literal("share")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .suggests(EquipCommand::suggestEquipNames)
                                                .executes(EquipCommand::shareEquip))))
                        .then(Commands.literal("accept")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(EquipCommand::suggestPendingNames)
                                        .executes(EquipCommand::acceptEquip)
                                        .then(Commands.argument("saveName", StringArgumentType.string())
                                                .executes(EquipCommand::acceptEquipWithRename))))
                        .then(Commands.literal("decline")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(EquipCommand::suggestPendingNames)
                                        .executes(EquipCommand::declineEquip)))
                        .then(Commands.literal("list")
                                .executes(EquipCommand::listEquips))));
    }

    private static CompletableFuture<Suggestions> suggestEquipNames(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            List<String> list = EQUIP_CACHE.get(player.getUUID());
            if (list != null) {
                for (String name : list) {
                    builder.suggest(name);
                }
            } else {
                // If cache is empty, request sync (though unlikely to complete in time for this
                // suggestion)
                PacketDistributor.sendToPlayer(player,
                        new EquipActionPacket(EquipActionPacket.ACTION_SYNC_REQUEST, "", ""));
            }
        } catch (Exception e) {
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPendingNames(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            List<String> list = PENDING_SHARE_CACHE.get(player.getUUID());
            if (list != null) {
                for (String name : list) {
                    builder.suggest(name);
                }
            } else {
                PacketDistributor.sendToPlayer(player,
                        new EquipActionPacket(EquipActionPacket.ACTION_SYNC_REQUEST, "", ""));
            }
        } catch (Exception e) {
        }
        return builder.buildFuture();
    }

    private static int saveEquip(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");

            PacketDistributor.sendToPlayer(player,
                    new EquipActionPacket(EquipActionPacket.ACTION_SAVE, name, ""));

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int loadEquip(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");

            PacketDistributor.sendToPlayer(player,
                    new EquipActionPacket(EquipActionPacket.ACTION_LOAD, name, ""));

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int deleteEquip(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");

            PacketDistributor.sendToPlayer(player,
                    new EquipActionPacket(EquipActionPacket.ACTION_DELETE, name, ""));

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int renameEquip(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");
            String newName = StringArgumentType.getString(context, "newName");

            PacketDistributor.sendToPlayer(player,
                    new EquipActionPacket(EquipActionPacket.ACTION_RENAME, name, newName));

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int shareEquip(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer sender = context.getSource().getPlayerOrException();
            String targetName = EntityArgument.getPlayer(context, "player").getName().getString();
            String name = StringArgumentType.getString(context, "name");

            PacketDistributor.sendToPlayer(sender,
                    new EquipActionPacket(EquipActionPacket.ACTION_SHARE_REQUEST, name, targetName));

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int acceptEquip(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");

            PacketDistributor.sendToPlayer(player,
                    new EquipActionPacket(EquipActionPacket.ACTION_ACCEPT, name, ""));

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int acceptEquipWithRename(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");
            String saveName = StringArgumentType.getString(context, "saveName");

            PacketDistributor.sendToPlayer(player,
                    new EquipActionPacket(EquipActionPacket.ACTION_ACCEPT, name, saveName));

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int declineEquip(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");

            PacketDistributor.sendToPlayer(player,
                    new EquipActionPacket(EquipActionPacket.ACTION_DECLINE, name, ""));

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int listEquips(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();

            // Request fresh sync
            PacketDistributor.sendToPlayer(player,
                    new EquipActionPacket(EquipActionPacket.ACTION_SYNC_REQUEST, "", ""));

            List<String> list = EQUIP_CACHE.get(player.getUUID());
            if (list == null || list.isEmpty()) {
                context.getSource().sendSuccess(
                        () -> Component.literal("Equips empty"), false);
            } else {
                context.getSource().sendSuccess(
                        () -> Component.literal("Equips: " + String.join(", ", list)), false);
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
