package com.THproject.tharidia_things.item.alchemist_potion;

import java.util.ArrayList;
import java.util.List;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class PotionDecay {
    @SubscribeEvent
    public static void asd(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().level().isClientSide)
            return;

        ItemStack stack = new ItemStack(AlchemistPotions.BALL_POTION.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFF0000, false));
        stack.set(PotionComponents.CRAFTED_TIME, System.currentTimeMillis());

        event.getEntity().getInventory().add(stack);
    }

    @SubscribeEvent
    public static void onPotionDecay(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 20 != 0)
            return;

        List<ItemStack> potionItems = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (AlchemistPotions.isPotion(player.getInventory().getItem(i))) {
                potionItems.add(player.getInventory().getItem(i));
            }
        }

        if (potionItems.size() == 0)
            return;

        potionItems.forEach(potion -> {
            Long time = potion.getOrDefault(PotionComponents.CRAFTED_TIME.get(), -1L);
            if (time == -1L)
                return;
            if ((System.currentTimeMillis() - time) > 10000L)
                potion.setCount(0);
        });
    }
}
