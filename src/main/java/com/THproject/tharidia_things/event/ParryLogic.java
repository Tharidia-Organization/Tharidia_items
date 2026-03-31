package com.THproject.tharidia_things.event;

import java.util.Map;
import java.util.UUID;

import com.THproject.tharidia_things.sounds.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber
public class ParryLogic {
    public static final long MIN_PARRY_TIME = 1500L;
    public static final long MAX_PARRY_TIME = 2000L;

    public static Map<UUID, Long> entityStartAttackTime = new java.util.HashMap<>();
    public static Map<UUID, Long> playerParryTime = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onParry(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getItemStack().getItem() instanceof SwordItem) {
            parry(player);
        }
    }

    @SubscribeEvent
    public static void attackedOnParry(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.level().isClientSide)
                return;
            Level level = player.level();

            if (isParry(player, event.getSource().getEntity())) {
                event.setCanceled(true);
                level.playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        ModSounds.WEAPON_PARRY.get(), SoundSource.PLAYERS);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            5, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
    }

    public static void parry(Player player) {
        playerParryTime.put(player.getUUID(), System.currentTimeMillis());
    }

    public static boolean isParry(Player player, Entity entity) {
        long timeAttackStart = entityStartAttackTime.getOrDefault(entity.getUUID(), 0L);
        long timePlayerParry = playerParryTime.getOrDefault(player.getUUID(), 0L);
        if (timePlayerParry == 0L || timeAttackStart == 0L)
            return false;

        if (timePlayerParry - timeAttackStart < MIN_PARRY_TIME || timePlayerParry - timeAttackStart > MAX_PARRY_TIME)
            return false;

        return true;
    }
}
