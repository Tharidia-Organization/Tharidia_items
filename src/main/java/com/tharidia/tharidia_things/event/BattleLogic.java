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
                targetAttachments.setInBattle(false);

                ((ServerPlayer) source).connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("You won")));
                ((ServerPlayer) target).connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("You lose")));

                level.sendParticles(
                        ParticleTypes.END_ROD,
                        source.getX(), source.getY(), source.getZ(),
                        100,
                        0.5, 1, 0.5,
                        0.1);
            }

            event.setCanceled(true);
        }
    }
}
