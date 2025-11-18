package com.tharidia.tharidia_things.features;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages frozen players - prevents them from moving
 */
public class FreezeManager {
    private static Logger logger;
    private static final Map<UUID, Vec3> frozenPlayers = new HashMap<>();

    public static void initialize(Logger log) {
        logger = log;
        logger.info("FreezeManager initialized");
    }

    /**
     * Freeze a player at their current position
     */
    public static void freezePlayer(ServerPlayer player) {
        Vec3 position = player.position();
        frozenPlayers.put(player.getUUID(), position);
        logger.info("Froze player {} at position {}", player.getName().getString(), position);
    }

    /**
     * Unfreeze a player
     */
    public static void unfreezePlayer(ServerPlayer player) {
        if (frozenPlayers.remove(player.getUUID()) != null) {
            logger.info("Unfroze player {}", player.getName().getString());
        }
    }

    /**
     * Check if a player is frozen
     */
    public static boolean isFrozen(UUID playerUuid) {
        return frozenPlayers.containsKey(playerUuid);
    }

    /**
     * Get the frozen position of a player
     */
    public static Vec3 getFrozenPosition(UUID playerUuid) {
        return frozenPlayers.get(playerUuid);
    }

    /**
     * Unfreeze all players (called on server stop)
     */
    public static void unfreezeAll() {
        frozenPlayers.clear();
        logger.info("Unfroze all players");
    }

    /**
     * Event handler to keep frozen players in place
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            if (frozenPlayers.containsKey(uuid)) {
                Vec3 frozenPos = frozenPlayers.get(uuid);

                // Teleport player back to frozen position if they moved
                Vec3 currentPos = player.position();
                double distance = currentPos.distanceTo(frozenPos);

                if (distance > 0.1) { // Allow small movements due to physics
                    player.teleportTo(frozenPos.x, frozenPos.y, frozenPos.z);
                    player.setDeltaMovement(Vec3.ZERO);
                    player.hurtMarked = true; // Force position update to client
                }

                // Also cancel all movement to prevent jumping
                player.setDeltaMovement(Vec3.ZERO);
            }
        }
    }
}
