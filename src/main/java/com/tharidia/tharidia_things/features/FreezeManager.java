package com.tharidia.tharidia_things.features;

import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (isFrozen(player.getUUID())) {
                event.setCanceled(true);
            }
        }
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
                Vec3 currentPos = player.position();

                // Check horizontal movement
                double dx = currentPos.x - frozenPos.x;
                double dz = currentPos.z - frozenPos.z;
                double distSqr = dx * dx + dz * dz;

                if (distSqr > 0.01) {
                    player.teleportTo(frozenPos.x, currentPos.y, frozenPos.z);
                    player.hurtMarked = true;
                }

                // Stop horizontal velocity and upward vertical velocity
                Vec3 currentDelta = player.getDeltaMovement();
                if (currentDelta.x != 0 || currentDelta.z != 0 || currentDelta.y > 0) {
                    player.setDeltaMovement(new Vec3(0, Math.min(0, currentDelta.y), 0));
                }
            }
        }
    }
}
