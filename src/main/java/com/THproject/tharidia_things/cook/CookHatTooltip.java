package com.THproject.tharidia_things.cook;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class CookHatTooltip {
    @SubscribeEvent
    public static void AddCookHatTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (CookHatHelper.isCookHat(stack) && stack.has(CookHatData.PLAYER_UUID)) {
            String playerName = stack.get(CookHatData.PLAYER_NAME);
            event.getToolTip().add(3, Component.translatable("tooltip.tharidiathings.cook_hat.owner", playerName));
        }
    }
}
