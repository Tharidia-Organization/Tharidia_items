package com.THproject.tharidia_things.util;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Helper class to get player's chosen name from tharidia_tweaks NameService.
 * Falls back to Minecraft username if NameService is not available.
 * Reflection results are cached for performance.
 */
public class PlayerNameHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerNameHelper.class);

    // Cached reflection — initialized once
    private static boolean reflectionInitialized = false;
    private static boolean reflectionAvailable = false;
    private static Method getChosenNameMethod;
    private static Method getChosenNameByUUIDMethod;

    private static boolean ensureReflection() {
        if (reflectionInitialized) return reflectionAvailable;
        reflectionInitialized = true;

        try {
            Class<?> nameServiceClass = Class.forName("com.THproject.tharidia_tweaks.name.NameService");
            getChosenNameMethod = nameServiceClass.getMethod("getChosenName", ServerPlayer.class);
            getChosenNameByUUIDMethod = nameServiceClass.getMethod("getChosenNameByUUID", UUID.class);
            reflectionAvailable = true;
        } catch (ClassNotFoundException e) {
            LOGGER.warn("tharidia_tweaks mod not found! Using Minecraft usernames as fallback.");
            reflectionAvailable = false;
        } catch (NoSuchMethodException e) {
            LOGGER.warn("NameService API changed! Using Minecraft usernames as fallback.", e);
            reflectionAvailable = false;
        }

        return reflectionAvailable;
    }

    /**
     * Gets the chosen name for a player using NameService.
     * Falls back to Minecraft username if not available.
     */
    public static String getChosenName(ServerPlayer player) {
        if (!ensureReflection()) {
            return player.getName().getString();
        }

        try {
            String chosenName = (String) getChosenNameMethod.invoke(null, player);
            if (chosenName != null && !chosenName.isEmpty()) {
                return chosenName;
            }
        } catch (Exception e) {
            LOGGER.error("Error getting chosen name for player {}", player.getName().getString(), e);
        }

        return player.getName().getString();
    }

    /**
     * Gets the chosen name for a player by UUID.
     * Useful when the player is offline.
     */
    public static String getChosenNameByUUID(UUID playerUUID, net.minecraft.server.MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
        if (player != null) {
            return getChosenName(player);
        }

        if (!ensureReflection()) {
            return "Unknown";
        }

        try {
            String chosenName = (String) getChosenNameByUUIDMethod.invoke(null, playerUUID);
            if (chosenName != null && !chosenName.isEmpty()) {
                return chosenName;
            }
        } catch (Exception e) {
            LOGGER.error("Error getting chosen name for UUID {}", playerUUID, e);
        }

        return "Unknown";
    }
}
