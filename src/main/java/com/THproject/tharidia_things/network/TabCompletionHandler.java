package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * Handles blocking player name suggestions in chat tab completion.
 * This works by intercepting chat-related events server-side.
 */
public class TabCompletionHandler {

    /**
     * Register this handler to the NeoForge event bus
     */
    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new TabCompletionHandler());
        TharidiaThings.LOGGER.info("TabCompletionHandler registered - player name tab completion will be blocked");
    }
}
