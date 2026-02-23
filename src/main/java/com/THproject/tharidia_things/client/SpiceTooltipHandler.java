package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.spice.SpiceData;
import com.THproject.tharidia_things.spice.SpiceDataComponents;
import com.THproject.tharidia_things.spice.SpiceType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Adds spice info to food tooltips on the client side.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class SpiceTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        SpiceData spiceData = stack.get(SpiceDataComponents.SPICE_DATA.get());
        if (spiceData == null || spiceData.isEmpty()) {
            return;
        }

        event.getToolTip().add(Component.literal(" "));
        event.getToolTip().add(Component.translatable("spice.tharidiathings.header")
                .withStyle(style -> style.withColor(0xFFAA00)));

        for (SpiceType type : SpiceType.VALUES) {
            if (spiceData.hasSpice(type)) {
                event.getToolTip().add(
                        Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                                .append(Component.translatable(type.getTranslationKey())
                                        .withStyle(Style.EMPTY.withColor(type.getColor())))
                );
            }
        }
    }
}
