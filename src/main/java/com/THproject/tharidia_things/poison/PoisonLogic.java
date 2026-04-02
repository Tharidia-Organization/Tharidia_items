package com.THproject.tharidia_things.poison;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class PoisonLogic {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide)
            return;

        if (player.tickCount % 20 != 0)
            return;

        PoisonHelper.calcPoisonProgress(player);
        PoisonHelper.fallIfTimeExceed(player);

        PoisonSyncPacket.syncSelf(player);
    }

    @SubscribeEvent
    public static void onFoodEat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player))
            return;
        PoisonAttachments attachment = PoisonHelper.getAttachment(player);
        if (attachment == null)
            return;

        attachment.setSoftPoisoned();
    }
}
