package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.features.Revive;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class ReviveLogic {
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide())
            return;

        if (event.getEntity() instanceof Player player) {
            if (!Revive.isPlayerFallen(player)) {
                ReviveAttachments playerAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
                playerAttachments.setResTime(100);
                Revive.fallPlayer(player, true);
                event.setCanceled(true);
                event.getEntity().setHealth(1);
            } else {
                Revive.revivePlayer(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerInterractFallen(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().level().isClientSide())
            return;

        if (event.getTarget() instanceof Player interractedPlayer) {
            if (Revive.isPlayerFallen(interractedPlayer)) {
                ReviveAttachments playerReviveAttachments = interractedPlayer
                        .getData(ReviveAttachments.REVIVE_DATA.get());

                if (playerReviveAttachments.canRevive()) {
                    long timeDiff = event.getEntity().level().getGameTime()
                            - playerReviveAttachments.getLastRevivedTime();
                    if (timeDiff > 5) {
                        playerReviveAttachments.setResTime(100);
                    }

                    playerReviveAttachments.decreaseResTime();
                    playerReviveAttachments.setLastRevivedTime(event.getEntity().level().getGameTime());
                    event.getEntity().displayClientMessage(
                            Component.literal(String.valueOf(playerReviveAttachments.getResTime())), true);

                    if (playerReviveAttachments.getResTime() == 0)
                        Revive.revivePlayer(interractedPlayer);
                }
            }
        }
    }
}
