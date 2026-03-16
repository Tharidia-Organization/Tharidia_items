package com.THproject.tharidia_things.item.alchemist_potion;

import net.minecraft.world.item.Item;

public class PotionItem extends Item {
    private final PotionTypes potionTypes;
    private final int color;
    
    public PotionItem(PotionTypes potionTypes, int color){
        super(new Item.Properties());
        this.potionTypes = potionTypes;
        this.color = color;
    }

    public PotionTypes getPotionTypes() {
        return potionTypes;
    }

    public int getColor() {
        return color;
    }
}
