package com.THproject.tharidia_things.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

/**
 * Pitchfork item used for removing bedding (straw) from stables.
 * A farmer's tool made of iron with wooden handle.
 */
public class PitchforkItem extends Item {

    public PitchforkItem(Properties properties) {
        super(properties
            .durability(250)  // Similar to iron tools
            .stacksTo(1));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return Tiers.IRON.getEnchantmentValue();
    }

    public boolean canBeDepleted() {
        return true;
    }
}
