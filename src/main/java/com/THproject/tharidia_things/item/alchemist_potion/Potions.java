package com.THproject.tharidia_things.item.alchemist_potion;

import java.util.List;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

public class Potions {
    public static final DeferredItem<Item> POTION_1 = TharidiaThings.ITEMS.register(
        "potion_1",() -> new PotionItem(PotionTypes.TIPO_1, 0x00FF00));

    public static void register() {}

    public static List<ItemStack> getAllPotions() {
        return List.of(
            POTION_1.get().getDefaultInstance()
        );
    }
}
