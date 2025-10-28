package com.tharidia.tharidia_things.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

/**
 * Hot Iron item created from casting molten iron into an ingot mold.
 * This item is hot and must be handled with tongs (Pinza).
 * It should NEVER be in a player's inventory.
 */
public class HotIronItem extends Item {
    
    public HotIronItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Make it glow to indicate it's hot
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // If hot iron somehow ends up in a player's inventory, remove it
        if (entity instanceof Player player && !level.isClientSide) {
            // Remove the item from inventory
            player.getInventory().removeItem(stack);
            stack.shrink(stack.getCount());
            
            // Hurt the player (it's hot!)
            player.hurt(level.damageSources().inFire(), 2.0F);
            
            // Show message
            player.displayClientMessage(
                Component.translatable("item.tharidiathings.hot_iron.burned_hands"), 
                true
            );
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
