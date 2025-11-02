package com.tharidia.tharidia_things.lobby;

import com.tharidia.tharidia_things.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blocks chat for non-OP players in lobby mode
 */
public class LobbyChatBlocker {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyChatBlocker.class);
    
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        // Only block if this is a lobby server
        if (!Config.IS_LOBBY_SERVER.get()) {
            return;
        }
        
        ServerPlayer player = event.getPlayer();
        
        // Allow OP players to chat
        if (player.hasPermissions(2)) {
            return;
        }
        
        // Block non-OP players from chatting in lobby
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal("§c§l[LOBBY] §7Chat is disabled in the lobby. Please wait to be transferred to the main server."));
        LOGGER.debug("Blocked chat message from non-OP player {} in lobby", player.getName().getString());
    }
}
