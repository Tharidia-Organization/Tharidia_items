package com.tharidia.tharidia_things.lobby;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blocks /server commands for all players (use /thqueueadmin instead)
 */
public class ServerCommandBlocker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCommandBlocker.class);
    
    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        String command = event.getParseResults().getReader().getString().toLowerCase();
        
        // Block /server commands (from Velocity or other plugins)
        if (command.startsWith("server ") || command.equals("server")) {
            // Allow OP level 4 to use it
            if (event.getParseResults().getContext().getSource().hasPermission(4)) {
                return;
            }
            
            event.setCanceled(true);
            
            // Send feedback if it's a player
            if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.literal(
                    "§c§l[BLOCKED] §7The /server command is disabled. " +
                    "§7Use §6/thqueueadmin play§7 or §6/thqueueadmin send§7 instead."
                ));
            }
            
            LOGGER.info("Blocked /server command from {}", 
                event.getParseResults().getContext().getSource().getTextName());
        }
    }
}
