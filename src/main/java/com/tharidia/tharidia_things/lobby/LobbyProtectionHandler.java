package com.tharidia.tharidia_things.lobby;

import com.tharidia.tharidia_things.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures non-OP players in lobby stay in spectator mode and cannot exploit game mechanics
 */
public class LobbyProtectionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyProtectionHandler.class);
    
    /**
     * Enforce spectator mode for non-OP players in lobby
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Only enforce on lobby servers
        if (!Config.IS_LOBBY_SERVER.get()) {
            return;
        }
        
        // Skip OP players
        if (player.hasPermissions(2)) {
            return;
        }
        
        // Check every 20 ticks (1 second) to reduce overhead
        if (player.tickCount % 20 != 0) {
            return;
        }
        
        // Force spectator mode if player somehow changed it
        if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            LOGGER.warn("Non-OP player {} was not in spectator mode in lobby - forcing spectator", 
                player.getName().getString());
            player.setGameMode(GameType.SPECTATOR);
        }
        
        // Reset any flight abilities that shouldn't exist
        if (!player.getAbilities().mayfly && player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }
    
    /**
     * Ensure players respawn in spectator mode in lobby
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Only enforce on lobby servers
        if (!Config.IS_LOBBY_SERVER.get()) {
            return;
        }
        
        // Skip OP players
        if (player.hasPermissions(2)) {
            return;
        }
        
        // Force spectator mode on respawn
        player.setGameMode(GameType.SPECTATOR);
        LOGGER.info("Player {} respawned in lobby - set to spectator mode", player.getName().getString());
    }
}
