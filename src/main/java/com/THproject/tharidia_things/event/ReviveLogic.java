package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.config.ReviveConfig;
import com.THproject.tharidia_things.features.Revive;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

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
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide())
            return;
        if (Revive.isPlayerFallen(event.getEntity()))
            event.getEntity().kill();
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

                        if (playerReviveAttachments.getResTime() == 0) {
                            if (!item_to_revive.equals(""))
                                event.getEntity().getMainHandItem().shrink(1);
                            Revive.revivePlayer(interractedPlayer);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFallenPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.level().isClientSide())
                return;

            if (Revive.isPlayerFallen(player))
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFallenBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer().level().isClientSide())
            return;

        Player player = event.getPlayer();

        if (Revive.isPlayerFallen(player))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFallenAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide())
            return;

        if (Revive.isPlayerFallen(event.getEntity()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFallenBeingAttacked(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide())
            return;

        Entity attacker = event.getSource().getDirectEntity();
        if (attacker instanceof Projectile p) {
            attacker = p.getOwner();
        }
        if (event.getEntity() instanceof Player player) {
            if (!(attacker instanceof Player)) {
                if (Revive.isPlayerFallen(player)) {
                    ReviveAttachments playerReviveAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
                    if ((player.tickCount - playerReviveAttachments.getInvulnerabilityTick()) < 200) {
                        event.setCanceled(true);
                    }
                }
            } else {
                if (Revive.isPlayerFallen(player))
                    event.setCanceled(true);
            }
        }
    }

}
