package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.config.ReviveConfig;
import com.THproject.tharidia_things.features.Revive;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
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
                playerAttachments.resetResTime();
                Revive.fallPlayer(player, true);
                event.setCanceled(true);
                event.getEntity().setHealth(1);
            } else {
                Revive.revivePlayer(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerInteractFallen(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().level().isClientSide())
            return;

        if (event.getHand() != InteractionHand.MAIN_HAND)
            return;

        if (event.getTarget() instanceof Player interractedPlayer) {
            if (Revive.isPlayerFallen(interractedPlayer) && !Revive.isPlayerFallen(event.getEntity())) {
                ReviveAttachments playerReviveAttachments = interractedPlayer
                        .getData(ReviveAttachments.REVIVE_DATA.get());

                if (playerReviveAttachments.canRevive()) {
                    String item_to_revive = ReviveConfig.config.REVIVE_ITEM.get("Value").toString();
                    if (item_to_revive.equals("")
                            || event.getEntity().getMainHandItem().getItem().toString().equals(item_to_revive)) {

                        long timeDiff = event.getEntity().level().getGameTime()
                                - playerReviveAttachments.getLastRevivedTime();
                        if (timeDiff > 5) {
                            playerReviveAttachments.resetResTime();
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
}
