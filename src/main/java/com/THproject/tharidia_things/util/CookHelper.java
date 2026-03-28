package com.THproject.tharidia_things.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class CookHelper {
    public static boolean hasCookHat(Player player){
        Item hat1 = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("age_of_fight", "cook_hat_1"));
        Item hat2 = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("age_of_fight", "cook_hat_2"));
        Item hat3 = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("age_of_fight", "cook_hat_3"));
        // Item hat4 = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("age_of_fight", "cook_hat_4"));
        // Item hat5 = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("age_of_fight", "cook_hat_5"));

        Item item = player.getInventory().getArmor(3).getItem();
        return (item != hat1
                && item != hat2
                && item != hat3
                // && item != hat4
                // && item != hat5
            );
    }
}
