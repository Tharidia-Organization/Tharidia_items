package com.THproject.tharidia_things.util;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Helper class to get player's chosen name from tharidia_tweaks NameService
 * Falls back to Minecraft username if NameService is not available
 */
public class PlayerNameHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerNameHelper.class);
    
    /**
     * Gets the chosen name for a player using NameService
     * Falls back to Minecraft username if not available
     */
    public static String getChosenName(ServerPlayer player) {
        try {
            Class<?> nameServiceClass = Class.forName("com.THproject.tharidia_tweaks.name.NameService");
            java.lang.reflect.Method getChosenNameMethod = nameServiceClass.getMethod("getChosenName", ServerPlayer.class);
            String chosenName = (String) getChosenNameMethod.invoke(null, player);
            
            if (chosenName != null && !chosenName.isEmpty()) {
                return chosenName;
            }
        } catch (ClassNotFoundException e) {
            LOGGER.warn("tharidia_tweaks mod not found! Using Minecraft username as fallback.");
        } catch (Exception e) {
            LOGGER.error("Error getting chosen name for player {}", player.getName().getString(), e);
        }
        
        // Fallback to Minecraft username
        return player.getName().getString();
    }
    
    /**
     * Gets the chosen name for a player by UUID
     * This is useful when the player is offline
     */
    public static String getChosenNameByUUID(UUID playerUUID, net.minecraft.server.MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
        if (player != null) {
            return getChosenName(player);
        }
        
        // Player is offline, try to get from NameService storage
        try {
            Class<?> nameServiceClass = Class.forName("com.THproject.tharidia_tweaks.name.NameService");
            java.lang.reflect.Method getChosenNameByUUIDMethod = nameServiceClass.getMethod("getChosenNameByUUID", UUID.class);
            String chosenName = (String) getChosenNameByUUIDMethod.invoke(null, playerUUID);
            
            if (chosenName != null && !chosenName.isEmpty()) {
                return chosenName;
            }
        } catch (ClassNotFoundException e) {
            LOGGER.warn("tharidia_tweaks mod not found! Cannot get chosen name for offline player.");
        } catch (Exception e) {
            LOGGER.error("Error getting chosen name for UUID {}", playerUUID, e);
        }
        
        // Fallback to UUID string if player is offline and NameService unavailable
        return "Unknown";
    }
}
