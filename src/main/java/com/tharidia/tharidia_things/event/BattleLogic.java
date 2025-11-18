package com.tharidia.tharidia_things.event;

import java.util.UUID;
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

        ServerLevel level = (ServerLevel) event.getEntity().level();

        if (event.getSource().getEntity() instanceof Player source &&
                event.getEntity() instanceof Player target) {
            BattleGauntleAttachments sourceAttachments = source.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());
            BattleGauntleAttachments targetAttachments = target.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

            if (sourceAttachments.getInBattle() && targetAttachments.getInBattle()) {
                source.setHealth(sourceAttachments.getPlayerHealth());
                target.setHealth(targetAttachments.getPlayerHealth());

                sourceAttachments.setInBattle(false);
                sourceAttachments.setChallengerUUID(null);
                targetAttachments.setInBattle(false);
                targetAttachments.setChallengerUUID(null);

                ((ServerPlayer) source).connection.send(new ClientboundSetTitleTextPacket(
                        Component.translatable("message.tharidiathings.battle.win").withColor(0x00FF00)));
                ((ServerPlayer) target).connection.send(new ClientboundSetTitleTextPacket(
                        Component.translatable("message.tharidiathings.battle.lose").withColor(0xFF0000)));

                level.sendParticles(
                        ParticleTypes.END_ROD,
                        source.getX(), source.getY(), source.getZ(),
                        100,
                        0.3, 1, 0.3,
                        0.1);

                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 1, false, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 1, false, false, false));

                if (target instanceof ServerPlayer serverTarget) {
                    target.displayClientMessage(Component.literal("FreezingPlayer"), false);
                    FreezeManager.freezePlayer(serverTarget);
                    target.displayClientMessage(Component.literal("FreezePlayer"), false);
                }

                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.tickCount % 220 == 0) {
                FreezeManager.unfreezePlayer(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerAttach(AttackEntityEvent event) {
        if (event.getTarget().level().isClientSide())
            return;

        if (event.getTarget() instanceof Player target) {
            BattleGauntleAttachments targetAttachments = target.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

            if (targetAttachments.getInBattle()) {
                if (!targetAttachments.getChallengerUUID().equals(event.getEntity().getUUID())) {
                    event.getEntity().displayClientMessage(
                            Component.translatable("message.tharidiathings.battle.unable_to_attack")
                                    .withColor(0x857700),
                            false);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggout(PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide())
            return;

        Player player = event.getEntity();

        BattleGauntleAttachments playerAttachments = player.getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

        if (playerAttachments.getInBattle()) {
            UUID challengerUUID = playerAttachments.getChallengerUUID();
            if (player instanceof ServerPlayer serverPlayer) {
                Player challengerPlayer = serverPlayer.getServer().getPlayerList().getPlayer(challengerUUID);

                BattleGauntleAttachments challengerAttachments = challengerPlayer
                        .getData(BattleGauntleAttachments.BATTLE_GAUNTLE.get());

                challengerAttachments.setInBattle(false);
                challengerAttachments.setChallengerUUID(null);
                challengerPlayer.setHealth(challengerAttachments.getPlayerHealth());
            }

            playerAttachments.setInBattle(false);
            playerAttachments.setChallengerUUID(null);
            player.setHealth(playerAttachments.getPlayerHealth());

        }
    }
}
