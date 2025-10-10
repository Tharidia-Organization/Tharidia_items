package com.tharidia.tharidia_things.weight;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Manages weight calculations for players
 */
public class WeightManager {
    
    /**
     * Checks if a player should be exempt from weight restrictions
     * Masters (OP level 2+) are exempt from weight
     */
    public static boolean isMaster(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.hasPermissions(2);
        }
        return false;
    }
    
    /**
     * Calculates the total weight of a player's inventory
     * Returns 0 if player is a master
     */
    public static double calculatePlayerWeight(Player player) {
        // Masters bypass weight system
        if (isMaster(player)) {
            return 0.0;
        }
        Inventory inventory = player.getInventory();
        double totalWeight = 0.0;
        
        // Main inventory (including hotbar)
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                double itemWeight = WeightRegistry.getItemWeight(stack.getItem());
                totalWeight += itemWeight * stack.getCount();
            }
        }
        
        // Armor slots
        for (ItemStack armorStack : inventory.armor) {
            if (!armorStack.isEmpty()) {
                double itemWeight = WeightRegistry.getItemWeight(armorStack.getItem());
                totalWeight += itemWeight * armorStack.getCount();
            }
        }
        
        // Offhand
        ItemStack offhandStack = inventory.offhand.get(0);
        if (!offhandStack.isEmpty()) {
            double itemWeight = WeightRegistry.getItemWeight(offhandStack.getItem());
            totalWeight += itemWeight * offhandStack.getCount();
        }
        
        return totalWeight;
    }
    
    /**
     * Gets the weight status for a player
     */
    public static WeightData.WeightStatus getPlayerWeightStatus(Player player) {
        double weight = calculatePlayerWeight(player);
        return WeightRegistry.getWeightStatus(weight);
    }
    
    /**
     * Gets the speed multiplier for a player based on their weight
     */
    public static double getSpeedMultiplier(Player player) {
        WeightData.WeightStatus status = getPlayerWeightStatus(player);
        return WeightRegistry.getDebuffs().getSpeedMultiplier(status);
    }
    
    /**
     * Checks if a player should be unable to swim up
     */
    public static boolean isSwimUpDisabled(Player player) {
        WeightData.WeightStatus status = getPlayerWeightStatus(player);
        return WeightRegistry.getDebuffs().isSwimUpDisabled(status);
    }
}
