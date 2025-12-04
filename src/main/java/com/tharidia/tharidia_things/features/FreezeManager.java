package com.tharidia.tharidia_things.features;

import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
    private static final Map<UUID, Vec3> frozenPlayers = new HashMap<>();

    private static final ResourceLocation FREEZE_MOVEMENT_ID = ResourceLocation
            .fromNamespaceAndPath(TharidiaThings.MODID, "freeze_movement");
    private static final ResourceLocation FREEZE_JUMP_ID = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
            "freeze_jump");

    /**
     * Freeze a player at their current position
     */
    public static void freezePlayer(ServerPlayer player) {
        Vec3 position = player.position();
        frozenPlayers.put(player.getUUID(), position);

        // Apply attributes to prevent movement and jumping
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null && movement.getModifier(FREEZE_MOVEMENT_ID) == null) {
            movement.addTransientModifier(
                    new AttributeModifier(FREEZE_MOVEMENT_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null && jump.getModifier(FREEZE_JUMP_ID) == null) {
            jump.addTransientModifier(
                    new AttributeModifier(FREEZE_JUMP_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /**
     * Unfreeze a player
     */
    public static void unfreezePlayer(ServerPlayer player) {
        if (frozenPlayers.remove(player.getUUID()) != null) {
            // Remove attributes
            AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement != null) {
                movement.removeModifier(FREEZE_MOVEMENT_ID);
            }

            AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
            if (jump != null) {
                jump.removeModifier(FREEZE_JUMP_ID);
            }
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
    }
}
