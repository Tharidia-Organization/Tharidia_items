package com.THproject.tharidia_things.command;

import com.THproject.tharidia_things.stable.StableDebugLogger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Command to control stable debug logging.
 * Requires admin permission (op level 2+).
 * /stabledebug - Toggle debug logging
 * /stabledebug on - Enable debug logging
 * /stabledebug off - Disable debug logging
 */
public class StableDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stabledebug")
            .requires(source -> source.hasPermission(2)) // Requires op level 2 (admin)
            .executes(context -> {
                // Toggle
                boolean newState = !StableDebugLogger.isEnabled();
                StableDebugLogger.setEnabled(newState);
                context.getSource().sendSuccess(() ->
                    Component.literal("Stable debug logging " + (newState ? "§aENABLED" : "§cDISABLED")), false);
                return 1;
            })
            .then(Commands.literal("on")
                .executes(context -> {
                    StableDebugLogger.setEnabled(true);
                    context.getSource().sendSuccess(() ->
                        Component.literal("Stable debug logging §aENABLED"), false);
                    return 1;
                }))
            .then(Commands.literal("off")
                .executes(context -> {
                    StableDebugLogger.setEnabled(false);
                    context.getSource().sendSuccess(() ->
                        Component.literal("Stable debug logging §cDISABLED"), false);
                    return 1;
                }))
        );
    }
}
