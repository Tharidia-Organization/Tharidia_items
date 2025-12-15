package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.registry.ModStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public class PlayerKillHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            // Check if the player is holding an iron sword in their main hand
            if (player.getMainHandItem().is(Items.IRON_SWORD)) {
                player.awardStat(ModStats.LAMA_CORTA_KILL.get());
            }
        }
    }
}
