package com.THproject.tharidia_things.client;

import java.util.List;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.item.alchemist_potion.AlchemistPotions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class AlchemistPotionColorHandler {
    @SubscribeEvent
    public static void registerItemColor(RegisterColorHandlersEvent.Item event) {
        List<Item> potionItems = List.of(
            AlchemistPotions.BALL_POTION.get(),
            AlchemistPotions.TRIANG_POTION.get(),
            AlchemistPotions.DROP_POTION.get(),
            AlchemistPotions.FANTASY_POTION.get()
        );

        potionItems.forEach(potion -> {
            event.register((stack, tintIndex) -> {
                if (tintIndex == 0) {
                    DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
                    if (dyedColor != null) {
                        // Filled: tint overlay with the potion's colour
                        return 0xFF000000 | dyedColor.rgb();
                    }
                    // Empty: make overlay fully transparent so only the base bottle shows
                    return 0;
                }
                return 0xFFFFFFFF; // layer1 (base bottle) — no tint
            }, potion);
        });
    }
}
