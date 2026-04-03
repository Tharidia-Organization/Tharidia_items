package com.THproject.tharidia_things.item.alchemist_potion;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class PotionDecay {
    @SubscribeEvent
    public static void asd(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().level().isClientSide)
            return;

        ItemStack stack = new ItemStack(AlchemistPotions.DROP_POTION.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFF0000, false));
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.REGENERATION));
        stack.set(PotionComponents.CRAFTED_TIME, System.currentTimeMillis());

        event.getEntity().getInventory().add(stack);
    }

    @SubscribeEvent
    public static void onUseDecayPotion(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity().level().isClientSide)
            return;

        ItemStack stack = event.getItem();
        if (!AlchemistPotions.isPotion(stack))
            return;

        Long time = stack.getOrDefault(PotionComponents.CRAFTED_TIME.get(), -1L);
        if (time == -1L)
            return;

        int duration = event.getDuration();
        if ((System.currentTimeMillis() - time) > 10000L && duration <= 5) {
            event.setCanceled(true);
            stack.shrink(1);
        }
    }
}
