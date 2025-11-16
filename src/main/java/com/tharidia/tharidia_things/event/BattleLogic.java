package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.compoundTag.BattleGauntleAttachments;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

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
                        Component.literal("You won").withColor(0x00FF00)));
                ((ServerPlayer) target).connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("You lose").withColor(0xFF0000)));

                level.sendParticles(
                        ParticleTypes.END_ROD,
                        source.getX(), source.getY(), source.getZ(),
                        100,
                        0.3, 1, 0.3,
                        0.1);

                event.setCanceled(true);
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
                            Component.literal("You can't attack this player, he is in battle").withColor(0x857700),
                            false);
                    event.setCanceled(true);
                }
            }
        }
    }
}
