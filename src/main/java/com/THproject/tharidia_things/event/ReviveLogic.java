package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.config.ReviveConfig;
import com.THproject.tharidia_things.features.Revive;
import com.THproject.tharidia_things.network.RightClickReleasePayload;
import com.THproject.tharidia_things.network.ReviveProgressPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags.DamageTypes;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class ReviveLogic {
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide())
            return;

        if (event.getEntity() instanceof Player player) {
            if (!Revive.isPlayerFallen(player)) {
                ReviveAttachments playerAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
                if (playerAttachments.canFall()) {
                    playerAttachments.resetResTime();
                    Revive.fallPlayer(player, true);
                    event.setCanceled(true);
                    event.getEntity().setHealth(1);
                }
            } else {
                Revive.revivePlayer(player);
            }
        }
    }

    @SubscribeEvent
    public static void onFallenLogout(PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide())
            return;
        if (Revive.isPlayerFallen(event.getEntity()))
            event.getEntity().kill();
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
                    if ((player.tickCount - playerReviveAttachments.getInvulnerabilityTick()) < 200
                            && !event.getSource().is(DamageTypes.IS_TECHNICAL)) {
                        event.setCanceled(true);
                    }
                }
            } else {
                if (Revive.isPlayerFallen(player))
                    event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onReviving(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().level().isClientSide())
            return;

        if (event.getHand() != InteractionHand.MAIN_HAND)
            return;

        if (event.getTarget() instanceof Player interactedPlayer) {
            if (Revive.isPlayerFallen(interactedPlayer) && !Revive.isPlayerFallen(event.getEntity())) {
                ReviveAttachments interractReviveAttachments = interactedPlayer
                        .getData(ReviveAttachments.REVIVE_DATA.get());
                ReviveAttachments playerReviveAttachments = event.getEntity()
                        .getData(ReviveAttachments.REVIVE_DATA.get());

                if (interractReviveAttachments.canRevive()) {
                    String item_to_revive = ReviveConfig.config.REVIVE_ITEM.get("Value").toString();
                    if (item_to_revive.equals("")
                            || event.getEntity().getMainHandItem().getItem().toString().equals(item_to_revive)) {

                        playerReviveAttachments.setRevivingPlayer(interactedPlayer.getUUID());
                        interractReviveAttachments.decreaseResTime();

                        int maxTime = Integer.parseInt(ReviveConfig.config.TIME_TO_RES.get("Value").toString());
                        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                            PacketDistributor.sendToPlayer(
                                    serverPlayer,
                                    new ReviveProgressPacket(interractReviveAttachments.getResTime(), maxTime,
                                            "Reviving..."));
                        }

                        if (interractReviveAttachments.getResTime() == 0) {
                            Revive.revivePlayer(interactedPlayer);
                            interractReviveAttachments.resetResTime();
                            playerReviveAttachments.setRevivingPlayer(null);
                            if (!item_to_revive.equals(""))
                                event.getEntity().getMainHandItem().shrink(1);
                        }
                    }
                }
            }
        }
    }

    public static void onStopReviving(final RightClickReleasePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ReviveAttachments playerReviveAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());

            if (playerReviveAttachments.getRevivingPlayer() == null)
                return;

            Player interactedPlayer = player.level().getPlayerByUUID(playerReviveAttachments.getRevivingPlayer());
            if (interactedPlayer == null)
                return;
            ReviveAttachments interReviveAttachments = interactedPlayer.getData(ReviveAttachments.REVIVE_DATA.get());

            if (Revive.isPlayerFallen(interactedPlayer)) {
                interReviveAttachments.resetResTime();
                playerReviveAttachments.setRevivingPlayer(null);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide())
            return;

        Player player = event.getEntity();
        if (Revive.isPlayerFallen(player)) {
            ReviveAttachments attachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
            attachments.increaseTimeFallen();

            int maxTime = Integer.parseInt(ReviveConfig.config.TIME_FALLEN.get("Value").toString());

            if (attachments.getTimeFallen() >= maxTime) {
                player.kill();
                Revive.revivePlayer(player);
            } else {
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(
                            serverPlayer,
                            new ReviveProgressPacket(maxTime - attachments.getTimeFallen(), maxTime, "Dying..."));
                }
            }
        }
    }

}
