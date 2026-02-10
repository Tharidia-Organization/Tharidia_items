package com.THproject.tharidia_things.item;

import net.minecraft.world.item.Item;

public class Grinder extends Item {
    public Grinder() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(120));
    }
}
