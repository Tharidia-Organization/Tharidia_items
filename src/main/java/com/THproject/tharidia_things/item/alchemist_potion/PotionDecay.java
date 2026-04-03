package com.THproject.tharidia_things.item.alchemist_potion;

import java.util.List;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class PotionDecay {
    @SubscribeEvent
    public static void test(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().level().isClientSide)
            return;

        if (event.getHand() != InteractionHand.MAIN_HAND)
            return;

        List<ItemStack> givePotions = List.of(
                new ItemStack(AlchemistPotions.BALL_POTION.get()),
                new ItemStack(AlchemistPotions.DROP_POTION.get()),
                new ItemStack(AlchemistPotions.FANTASY_POTION.get()),
                new ItemStack(AlchemistPotions.TRIANG_POTION.get()));

        givePotions.forEach(potion -> {
            potion.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFF0000, false));
            potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.REGENERATION));
            potion.set(PotionComponents.CRAFTED_TIME, System.currentTimeMillis());

            event.getEntity().getInventory().add(potion);
        });
    }

    @SubscribeEvent
    public static void onPotionStack(ItemStackedOnOtherEvent event) {
        if (event.getSlot().container instanceof Player player && player.level().isClientSide)
            return;
        ItemStack carried = event.getCarriedItem();
        ItemStack stackedOn = event.getStackedOnItem();

        // 1. Validation
        if (!AlchemistPotions.isPotion(carried) || !AlchemistPotions.isPotion(stackedOn))
            return;

        if(carried.getItem() != stackedOn.getItem())
            return;

        long mouseTime = carried.getOrDefault(PotionComponents.CRAFTED_TIME.get(), -1L);
        long inventoryTime = stackedOn.getOrDefault(PotionComponents.CRAFTED_TIME.get(), -1L);

        if (mouseTime == -1L || inventoryTime == -1L)
            return;

        // 2. Logic Check
        long minTime = Math.min(mouseTime, inventoryTime);
        long diffTime = Math.abs(mouseTime - inventoryTime);

        if (diffTime < AlchemistPotions.DECAY_TOLERANCE) {
            int maxStack = stackedOn.getMaxStackSize();

            if (stackedOn.getCount() < maxStack) {
                int transfer = Math.min(carried.getCount(), maxStack - stackedOn.getCount());

                // Perform the merge
                stackedOn.grow(transfer);
                carried.shrink(transfer);

                // Update time (take the minimum of the two)
                stackedOn.set(PotionComponents.CRAFTED_TIME, minTime);

                event.setCanceled(true); // Prevent vanilla behavior
            }
        }
    }
}
