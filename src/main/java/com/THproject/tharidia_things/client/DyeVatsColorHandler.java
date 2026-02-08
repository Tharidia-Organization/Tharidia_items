package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.DyeVatsBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DyeVatsColorHandler {

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 1 && level != null && pos != null) {
                if (level.getBlockEntity(pos) instanceof DyeVatsBlockEntity dyeVats) {
                    return 0xFF000000 | dyeVats.getCurrentColor();
                }
                return 0xFF000000 | DyeVatsBlockEntity.DEFAULT_WATER_COLOR;
            }
            return 0xFFFFFFFF;
        }, TharidiaThings.DYE_VATS.get());
    }
}
