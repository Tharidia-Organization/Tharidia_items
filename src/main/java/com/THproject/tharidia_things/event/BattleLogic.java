package com.THproject.tharidia_things.event;

import javax.annotation.Nullable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.BattleGauntleAttachments;
import com.THproject.tharidia_things.features.FreezeManager;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID)
public class BattleLogic {
    private static final ResourceLocation DUNGEON_DIMENSION = ResourceLocation.fromNamespaceAndPath("tharidia", "dungeon");

    private static boolean isInDungeonDimension(Player player) {
        return player.level().dimension().equals(ResourceKey.create(Registries.DIMENSION, DUNGEON_DIMENSION));
    }
    @SubscribeEvent
    public static void onPlayerKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide())
            return;

        if (event.getEntity() instanceof Player loser) {
            BattleGauntleAttachments targetAttachments = loser.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
            if (targetAttachments.getInBattle()) {
                finischBattle(null, loser);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void winTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide())
            return;

        ServerLevel level = ((ServerLevel) event.getEntity().level());
        Player player = event.getEntity();
        BattleGauntleAttachments playerAttachments = player.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

        if (playerAttachments.getWinTick() >= 1) {
            playerAttachments.setWinTick(playerAttachments.getWinTick() - 1);
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    player.getX(), player.getY(), player.getZ(),
                    1,
                    0.3, 1, 0.3,
                    0.1);
        }
    }

    @SubscribeEvent
    public static void loseTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide())
            return;

        ServerLevel level = ((ServerLevel) event.getEntity().level());
        Player player = event.getEntity();
        BattleGauntleAttachments playerAttachments = player.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

        ResourceLocation particleId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blood");
        ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleId);

        if (playerAttachments.getLoseTick() >= 2) {
            playerAttachments.setLoseTick(playerAttachments.getLoseTick() - 1);
            if (particleType instanceof SimpleParticleType simpleParticle) {
                level.sendParticles(
                        simpleParticle,
                        player.getX(), player.getY(), player.getZ(),
                        10,
                        0.3, 1, 0.3,
                        0.1);
            }
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

    public static void startBattle(Player player1, Player player2) {
        BattleGauntleAttachments player1Attachments = player1.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
        BattleGauntleAttachments player2Attachments = player2.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

        player1Attachments.setInBattle(true);
        player1Attachments.setChallengerUUID(player2.getUUID());
        player1Attachments.setPlayerHealth(player1.getHealth());

        player2Attachments.setInBattle(true);
        player2Attachments.setChallengerUUID(player1.getUUID());
        player2Attachments.setPlayerHealth(player2.getHealth());

        player1.setHealth(player1.getMaxHealth());
        player2.setHealth(player2.getMaxHealth());

        ((ServerPlayer) player1).connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("message.tharidiathings.battle.start").withColor(0xA89700)));
        ((ServerPlayer) player2).connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("message.tharidiathings.battle.start").withColor(0xA89700)));

        player1.level().playSound(
                null,
                player1.blockPosition(),
                SoundEvents.RAID_HORN.value(),
                SoundSource.PLAYERS,
                60.0f, 1.0f);
    }

    public static void finischBattle(@Nullable Player winnerPlayer, @Nullable Player loserPlayer) {
        if (winnerPlayer == null && loserPlayer == null)
            return;

        BattleGauntleAttachments winnerAttachments = null;
        BattleGauntleAttachments loserAttachments = null;

        if (winnerPlayer == null) {
            loserAttachments = loserPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
            if (loserPlayer instanceof ServerPlayer sp) {
                winnerPlayer = sp.getServer().getPlayerList().getPlayer(loserAttachments.getChallengerUUID());
                if (winnerPlayer != null) {
                    winnerAttachments = winnerPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
                }
            }
        } else {
            winnerAttachments = winnerPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
        }

        if (loserPlayer == null) {
            if (winnerPlayer != null) {
                winnerAttachments = winnerPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
                if (winnerPlayer instanceof ServerPlayer sp) {
                    loserPlayer = sp.getServer().getPlayerList().getPlayer(winnerAttachments.getChallengerUUID());
                    if (loserPlayer != null) {
                        loserAttachments = loserPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
                    }
                }
            }
        } else {
            loserAttachments = loserPlayer.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
        }

        if (winnerPlayer != null) {
            exitPlayerBattle(winnerPlayer);
        }
        if (loserPlayer != null) {
            exitPlayerBattle(loserPlayer);
        }

        if (winnerPlayer instanceof ServerPlayer) {
            ((ServerPlayer) winnerPlayer).connection.send(new ClientboundSetTitleTextPacket(
                    Component.translatable("message.tharidiathings.battle.win").withColor(0x00FF00)));
        }
        if (loserPlayer instanceof ServerPlayer) {
            ((ServerPlayer) loserPlayer).connection.send(new ClientboundSetTitleTextPacket(
                    Component.translatable("message.tharidiathings.battle.lose").withColor(0xFF0000)));
        }

        if (loserPlayer != null && loserAttachments != null) {
            loserPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 1, false, false, false));
            loserPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 1, false, false, false));
            loserAttachments.setLoseTick(200);
        }
        if (winnerAttachments != null) {
            winnerAttachments.setWinTick(200);
        }

        if (loserPlayer instanceof ServerPlayer serverLoser) {
            FreezeManager.freezePlayer(serverLoser);
        }
    }

    public static void exitPlayerBattle(Player player) {
        BattleGauntleAttachments playerAttachments = player.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

        playerAttachments.setInBattle(false);
        playerAttachments.setChallengerUUID(null);
        playerAttachments.setLoseTick(0);
        player.setHealth(playerAttachments.getPlayerHealth());
    }

    public static Player getChallengerPlayer(Player player) {
        BattleGauntleAttachments playerAttachments = player.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
        if (player instanceof ServerPlayer serverPlayer) {
            if (playerAttachments.getInBattle())
                return serverPlayer.getServer().getPlayerList()
                        .getPlayer(playerAttachments.getChallengerUUID());
            return null;
        }
        return null;
    }
}
