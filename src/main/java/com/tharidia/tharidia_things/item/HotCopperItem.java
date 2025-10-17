package com.tharidia.tharidia_things.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Hot Copper - Fresh from the forge, too hot to hold bare-handed.
 * Must be handled with Pinza (tongs).
 */
public class HotCopperItem extends Item {
    
    public HotCopperItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Makes it glow like enchanted items
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // If a player somehow gets this in their inventory, burn them and remove it
        if (entity instanceof Player player && !level.isClientSide) {
            player.getInventory().removeItem(stack);
            stack.shrink(stack.getCount());
            player.hurt(level.damageSources().inFire(), 2.0F);
            player.displayClientMessage(
                Component.translatable("item.tharidiathings.hot_copper.burned_hands"), 
                true
            );
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
