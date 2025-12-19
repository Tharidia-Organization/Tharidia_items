package com.THproject.tharidia_things.command;

import com.THproject.tharidia_things.character.CharacterAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.THproject.tharidia_things.character.CharacterEventHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for character creation
 */
public class CharacterCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tharidia")
            .then(Commands.literal("character")
                .requires(source -> source.hasPermission(4)) // Admin only
                .then(Commands.literal("create")
                    .executes(context -> {
                        try {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            CharacterEventHandler.completeCharacterCreation(player);
                            return 1;
                        } catch (CommandSyntaxException e) {
                            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("This command can only be run by a player"));
                            return 0;
                        }
                    })
                )
                .then(Commands.literal("reset")
                    .executes(context -> {
                        try {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            player.getData(CharacterAttachments.CHARACTER_DATA)
                                  .setCharacterCreated(false);
                            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Character creation status reset"), true);
                            return 1;
                        } catch (CommandSyntaxException e) {
                            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("This command can only be run by a player"));
                            return 0;
                        }
                    })
                )
            )
        );
    }
}
