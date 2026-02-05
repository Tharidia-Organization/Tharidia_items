package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class RedirectArmorEquip {
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();

        if (stack.getItem() instanceof ArmorItem || stack.getItem() instanceof Equipable) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.PASS);
        }
    }
}
