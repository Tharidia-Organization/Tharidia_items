package com.tharidia.tharidia_things.event;

import javax.annotation.Nullable;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.compoundTag.BattleGauntleAttachments;
import com.tharidia.tharidia_things.features.FreezeManager;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class BattleLogic {
    @SubscribeEvent
    public static void onPlayerKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide())
            return;

        if (event.getEntity() instanceof Player loser) {
            if (event.getSource().getEntity() instanceof Player winner) {
                BattleGauntleAttachments sourceAttachments = winner
                        .getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
                BattleGauntleAttachments targetAttachments = loser
                        .getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

                if (sourceAttachments.getInBattle() && targetAttachments.getInBattle()) {
                    finischBattle(winner, loser);
                    event.setCanceled(true);
                }
            } else {
                if (loser instanceof ServerPlayer serverPlayer) {
                    BattleGauntleAttachments targetAttachments = loser
                            .getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
                    Player challengerPlayer = serverPlayer.getServer().getPlayerList()
                            .getPlayer(targetAttachments.getChallengerUUID());
                    exitPlayerBattle(challengerPlayer);
                }
                finischBattle(null, loser);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void loseFreezeTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        BattleGauntleAttachments playerAttachments = player.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

        if (playerAttachments.getLoseTick() >= 2) {
            playerAttachments.setLoseTick(playerAttachments.getLoseTick() - 1);
        } else if (playerAttachments.getLoseTick() == 1) {
            playerAttachments.setLoseTick(0);
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                FreezeManager.unfreezePlayer(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerAttach(AttackEntityEvent event) {
        if (event.getTarget().level().isClientSide())
            return;

        Player attacker = event.getEntity();
        if (event.getTarget() instanceof Player target) {
            BattleGauntleAttachments attackerAttachments = attacker
                    .getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
            BattleGauntleAttachments targetAttachments = target
                    .getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

            if (attackerAttachments.getInBattle()) {
                if (!attackerAttachments.getChallengerUUID().equals(target.getUUID())) {
                    attacker.displayClientMessage(
                            Component.translatable("message.tharidiathings.battle.unable_to_attack_1")
                                    .withColor(0x857700),
                            true);
                    event.setCanceled(true);
                }
            } else if (targetAttachments.getInBattle()) {
                if (!targetAttachments.getChallengerUUID().equals(attacker.getUUID())) {
                    attacker.displayClientMessage(
                            Component.translatable("message.tharidiathings.battle.unable_to_attack_2")
                                    .withColor(0x857700),
                            true);
                    event.setCanceled(true);
                }
            } else if (attackerAttachments.getLoseTick() > 0) {
                event.setCanceled(true);
            } else if (targetAttachments.getLoseTick() > 0) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide())
            return;

        Player player = event.getEntity();

        BattleGauntleAttachments playerAttachments = player.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

        if (playerAttachments.getInBattle()) {
            if (player instanceof ServerPlayer serverPlayer) {
                Player challengerPlayer = serverPlayer.getServer().getPlayerList()
                        .getPlayer(playerAttachments.getChallengerUUID());
                exitPlayerBattle(challengerPlayer);
            }
            exitPlayerBattle(player);
        }
    }

    private static void finischBattle(@Nullable Player winnerPlayer, @Nullable Player loserPlayer) {
        if (winnerPlayer == null && loserPlayer == null)
            return;

        ServerLevel level = (ServerLevel) (winnerPlayer != null ? winnerPlayer.level() : loserPlayer.level());
        BattleGauntleAttachments winnerAttachments = null;
        BattleGauntleAttachments loserAttachments = null;

        if (winnerPlayer == null) {
            loserAttachments = loserPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
            if (loserPlayer instanceof ServerPlayer sp) {
                winnerPlayer = sp.getServer().getPlayerList().getPlayer(loserAttachments.getChallengerUUID());
                winnerAttachments = winnerPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
            }
        } else {
            winnerAttachments = winnerPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
        }

        if (loserPlayer == null) {
            winnerAttachments = winnerPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
            if (loserPlayer instanceof ServerPlayer sp) {
                loserPlayer = sp.getServer().getPlayerList().getPlayer(winnerAttachments.getChallengerUUID());
                loserAttachments = loserPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
            }
        } else {
            loserAttachments = loserPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
        }

        exitPlayerBattle(winnerPlayer);
        exitPlayerBattle(loserPlayer);

        ((ServerPlayer) winnerPlayer).connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("message.tharidiathings.battle.win").withColor(0x00FF00)));
        ((ServerPlayer) loserPlayer).connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("message.tharidiathings.battle.lose").withColor(0xFF0000)));

        level.sendParticles(
                ParticleTypes.END_ROD,
                winnerPlayer.getX(), winnerPlayer.getY(), winnerPlayer.getZ(),
                100,
                0.3, 1, 0.3,
                0.1);

        loserPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 1, false, false, false));
        loserPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 1, false, false, false));
        loserAttachments.setLoseTick(200);

        if (loserPlayer instanceof ServerPlayer serverLoser) {
            FreezeManager.freezePlayer(serverLoser);
        }
    }

    private static void exitPlayerBattle(Player player) {
        BattleGauntleAttachments playerAttachments = player.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

        playerAttachments.setInBattle(false);
        playerAttachments.setChallengerUUID(null);
        playerAttachments.setLoseTick(0);
        player.setHealth(playerAttachments.getPlayerHealth());
    }
}
