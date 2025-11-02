package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.network.RequestNamePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles pre-login name selection check (SERVER SIDE ONLY)
 * Sends a packet to client to show name selection dialog before world loads
 */
public class PreLoginNameHandler {
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Only run on server side
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Check if player needs to choose a name
            boolean needsName = RequestNamePacket.checkIfPlayerNeedsName(serverPlayer);
            
            TharidiaThings.LOGGER.info("Player {} login - needs name: {}", 
                serverPlayer.getName().getString(), needsName);
            
            // Send packet to client to trigger name selection screen if needed
            PacketDistributor.sendToPlayer(serverPlayer, new RequestNamePacket(needsName));
        }
    }
}
