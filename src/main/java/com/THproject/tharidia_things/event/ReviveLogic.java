package com.THproject.tharidia_things.event;

import java.util.UUID;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.config.ReviveConfig;
import com.THproject.tharidia_things.features.Revive;
import com.THproject.tharidia_things.network.RightClickReleasePayload;
import com.THproject.tharidia_things.network.revive.ReviveSyncPayload;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags.DamageTypes;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class ReviveLogic {

    // Sync revive data of all players to loggedIn player
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer joinedPlayer) {
            for (ServerPlayer otherPlayer : joinedPlayer.server.getPlayerList().getPlayers()) {
                if (Revive.isPlayerFallen(otherPlayer)) {
                    ReviveSyncPayload.sync(otherPlayer, joinedPlayer);
                }
            }
        }
    }

    // Cancel death event and fall player
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide())
            return;

        if (event.getEntity() instanceof Player player) {
            if (!Revive.isPlayerFallen(player)) {
                ReviveAttachments playerAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
                if (playerAttachments.canFall()) {
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
        if (Revive.isPlayerFallen(event.getEntity())) {
            event.getEntity().kill();
            Revive.revivePlayer(event.getEntity());
        }
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
                    if ((playerReviveAttachments.getResTick()
                            - player.tickCount) < ReviveAttachments.INVULNERABILITY_TICK
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
        if (event.getHand() != InteractionHand.MAIN_HAND)
            return;

        // Run strictly on server side to manage authoritative state
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getTarget() instanceof Player interactedPlayer) {
            ReviveAttachments interractReviveAttachments = interactedPlayer
                    .getData(ReviveAttachments.REVIVE_DATA.get());
            ReviveAttachments playerReviveAttachments = event.getEntity().getData(ReviveAttachments.REVIVE_DATA.get());

            boolean targetIsFallen = interractReviveAttachments.isFallen();
            boolean selfIsFallen = playerReviveAttachments.isFallen();

            if (targetIsFallen && !selfIsFallen) {
                if (interractReviveAttachments.canRevive()) {
                    // Check item config on server side
                    String item_to_revive = "";
                    try {
                        item_to_revive = ReviveConfig.config.REVIVE_ITEM.get("Value").toString();
                    } catch (Exception e) {
                        item_to_revive = "";
                    }
                    if (item_to_revive == null)
                        item_to_revive = "";

                    boolean hasItem = item_to_revive.equals("") ||
                            event.getEntity().getMainHandItem().getItem().toString().equals(item_to_revive);

                    if (hasItem) {
                        // Start reviving state
                        playerReviveAttachments.setRevivingPlayer(interactedPlayer.getUUID());

                        // Sync state to client so overlay appears
                        // We must sync SELF so the reviver sees the bar
                        ReviveSyncPayload.syncSelf(event.getEntity());
                    }
                }
            }
        }
    }

    public static void onStopReviving(final RightClickReleasePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null)
                return;

            ReviveAttachments playerReviveAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());

            if (playerReviveAttachments.getRevivingPlayer() != null) {
                UUID targetUUID = playerReviveAttachments.getRevivingPlayer();
                playerReviveAttachments.setRevivingPlayer(null);
                ReviveSyncPayload.syncSelf(player); // Update client to hide bar

                Player interactedPlayer = player.level().getPlayerByUUID(targetUUID);
                if (interactedPlayer != null) {
                    ReviveAttachments interReviveAttachments = interactedPlayer
                            .getData(ReviveAttachments.REVIVE_DATA.get());
                    if (Revive.isPlayerFallen(interactedPlayer)) {
                        interReviveAttachments.resetResTime();
                        ReviveSyncPayload.sync(interactedPlayer); // Sync progress reset
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Handle Continuous Reviving Logic (Server Only)
        if (!player.level().isClientSide()) {
            ReviveAttachments reviverAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
            UUID revivingUUID = reviverAttachments.getRevivingPlayer();

            if (revivingUUID != null) {
                Player target = player.level().getPlayerByUUID(revivingUUID);
                // Validate target exists, is fallen, and is close enough
                if (target != null && target.getData(ReviveAttachments.REVIVE_DATA.get()).isFallen()
                        && player.distanceTo(target) <= 6.0) {
                    ReviveAttachments targetAttachments = target.getData(ReviveAttachments.REVIVE_DATA.get());

                    // Check Item Requirement again (in case they switched item)
                    String item_to_revive = "";
                    try {
                        item_to_revive = ReviveConfig.config.REVIVE_ITEM.get("Value").toString();
                    } catch (Exception e) {
                        item_to_revive = "";
                    }
                    if (item_to_revive == null)
                        item_to_revive = "";

                    boolean hasItem = item_to_revive.equals("") ||
                            player.getMainHandItem().getItem().toString().equals(item_to_revive);

                    if (hasItem) {
                        // Progress revival
                        targetAttachments.decreaseResTick();
                        targetAttachments.decreaseTimeFallen();
                        ReviveSyncPayload.sync(target, player); // Sync target progress to reviver
                        ReviveSyncPayload.syncSelf(target);

                        if (targetAttachments.getResTick() <= 0) {
                            Revive.revivePlayer(target);
                            targetAttachments.resetResTime();
                            reviverAttachments.setRevivingPlayer(null);

                            ReviveSyncPayload.sync(target);
                            ReviveSyncPayload.syncSelf(player);

                            if (!item_to_revive.equals("")) {
                                player.getMainHandItem().shrink(1);
                            }
                        }
                    } else {
                        // Lost item -> Stop reviving
                        reviverAttachments.setRevivingPlayer(null);
                        targetAttachments.resetResTime();
                        ReviveSyncPayload.syncSelf(player);
                        ReviveSyncPayload.sync(target);
                    }
                } else {
                    // Invalid target/range -> Stop reviving
                    reviverAttachments.setRevivingPlayer(null);
                    ReviveSyncPayload.syncSelf(player);

                    if (target != null) {
                        target.getData(ReviveAttachments.REVIVE_DATA.get()).resetResTime();
                        ReviveSyncPayload.sync(target);
                    }
                }
            }
        }

        if (player.level().isClientSide()) {
            if (player.getData(ReviveAttachments.REVIVE_DATA.get()).isFallen()) {
                player.setForcedPose(Pose.SWIMMING);
                player.setSwimming(true);
            } else {
                player.setForcedPose(null);
                player.setSwimming(false);
            }

            ReviveAttachments clientAttachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
            if (clientAttachments.isFallen())
                clientAttachments.increaseTimeFallen();
            return;
        }

        if (Revive.isPlayerFallen(player)) {
            ReviveAttachments attachments = player.getData(ReviveAttachments.REVIVE_DATA.get());
            attachments.increaseTimeFallen();

            if (attachments.getTimeFallen() >= ReviveAttachments.MAX_FALLEN_TICK) {
                player.kill();
                Revive.revivePlayer(player);
            }
        }
    }

}
