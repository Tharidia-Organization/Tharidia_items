package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.features.Fallen;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class FallenLogic {
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide())
            return;

        if (event.getEntity() instanceof Player player) {
            if (!Fallen.isPlayerFallen(player)) {
                Fallen.fallPlayer(player);
                event.setCanceled(true);
                event.getEntity().setHealth(1);
            } else {
                Fallen.revivePlayer(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerInterractFallen(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().level().isClientSide())
            return;

        if (event.getTarget() instanceof Player interractedPlayer) {
            if (Fallen.isPlayerFallen(interractedPlayer))
                Fallen.revivePlayer(interractedPlayer);
        }
    }
}
