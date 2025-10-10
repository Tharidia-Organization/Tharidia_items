package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.weight.WeightManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles weight-based debuffs for players
 */
public class WeightDebuffHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeightDebuffHandler.class);
    private static final ResourceLocation WEIGHT_SPEED_MODIFIER_ID = 
        ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "weight_speed_penalty");
    
    /**
     * Apply speed debuff based on weight
     */
    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // Only update every 20 ticks (1 second) to reduce performance impact
        if (player.tickCount % 20 != 0) {
            return;
        }
        
        // Server-side only
        if (player.level().isClientSide) {
            return;
        }
        
        // Masters bypass weight system
        if (WeightManager.isMaster(player)) {
            // Remove any existing weight modifiers for masters
            removeWeightModifier(player);
            return;
        }
        
        applyWeightDebuffs(player);
    }
    
    /**
     * Prevent swimming up when overencumbered
     */
    @SubscribeEvent
    public static void onPlayerSwim(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        if (player.level().isClientSide) {
            return;
        }
        
        // Masters bypass weight system
        if (WeightManager.isMaster(player)) {
            return;
        }
        
        // Check if player is in water and trying to swim up
        if (player.isInWater() && WeightManager.isSwimUpDisabled(player)) {
            // Prevent upward movement in water
            if (player.getDeltaMovement().y > 0) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
            }
        }
    }
    
    /**
     * Remove weight modifiers when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        removeWeightModifier(player);
    }
    
    /**
     * Apply weight-based speed debuffs
     */
    private static void applyWeightDebuffs(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }
        
        // Remove existing modifier
        removeWeightModifier(player);
        
        // Calculate speed multiplier based on weight
        double speedMultiplier = WeightManager.getSpeedMultiplier(player);
        
        // Only apply modifier if speed is reduced
        if (speedMultiplier < 1.0) {
            // Create a new modifier (multiplicative)
            double modifierValue = speedMultiplier - 1.0; // Convert to modifier format (-0.05, -0.15, etc.)
            AttributeModifier modifier = new AttributeModifier(
                WEIGHT_SPEED_MODIFIER_ID,
                modifierValue,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            
            movementSpeed.addPermanentModifier(modifier);
        }
    }
    
    /**
     * Remove weight modifier from player
     */
    private static void removeWeightModifier(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null && movementSpeed.getModifier(WEIGHT_SPEED_MODIFIER_ID) != null) {
            movementSpeed.removeModifier(WEIGHT_SPEED_MODIFIER_ID);
        }
    }
}
