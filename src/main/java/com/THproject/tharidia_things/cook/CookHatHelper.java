package com.THproject.tharidia_things.cook;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class CookHatHelper {
    public static boolean isCookHat(ItemStack stack) {
        for (ItemStack hats : getHats()) {
            if (hats.getItem() == stack.getItem())
                return true;
        }
        return false;
    }

    public static List<ItemStack> getHats() {
        return List.of(
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("age_of_fight:cook_hat_1_helmet"))),
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("age_of_fight:cook_hat_2_helmet"))),
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("age_of_fight:cook_hat_3_helmet"))));
    }
}
