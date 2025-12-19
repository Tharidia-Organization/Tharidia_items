package com.THproject.tharidia_things.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.THproject.tharidia_things.config.ItemCatalogueConfig;
import com.THproject.tharidia_things.registry.ModStats;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

public class PlayerStatsIncrementHandler {

    private static final Map<Object, DeferredHolder<ResourceLocation, ResourceLocation>> WEAPON_STATS = new HashMap<>();

    public static void reload() {
        WEAPON_STATS.clear();
        WEAPON_STATS.put(ItemCatalogueConfig.config.LAMA_CORTA_ITEMS.get("Value"),
                ModStats.LAMA_CORTA_KILL);
        WEAPON_STATS.put(ItemCatalogueConfig.config.LANCIA_ITEMS.get("Value"),
                ModStats.LANCIA_KILL);
        WEAPON_STATS.put(ItemCatalogueConfig.config.MARTELLI_ITEMS.get("Value"),
                ModStats.MARTELLI_KILL);
        WEAPON_STATS.put(ItemCatalogueConfig.config.MAZZE_ITEMS.get("Value"),
                ModStats.MAZZE_KILL);
        WEAPON_STATS.put(ItemCatalogueConfig.config.SPADE_2_MANI_ITEMS.get("Value"),
                ModStats.SPADE_2_MANI_KILL);
        WEAPON_STATS.put(ItemCatalogueConfig.config.ASCE_ITEMS.get("Value"),
                ModStats.ASCE_KILL);
        WEAPON_STATS.put(ItemCatalogueConfig.config.SOCCHI_ITEMS.get("Value"),
                ModStats.SOCCHI_KILL);
        WEAPON_STATS.put(ItemCatalogueConfig.config.ARCHI_ITEMS.get("Value"),
                ModStats.ARCHI_KILL);
        WEAPON_STATS.put(ItemCatalogueConfig.config.ARMI_DA_FUOCO_ITEMS.get("Value"),
                ModStats.ARMI_DA_FUOCO_KILL);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.level().isClientSide()) {
                return;
            }

            WEAPON_STATS.forEach((list, stat) -> {
                if (listContains(list, player.getMainHandItem())) {
                    ((ServerPlayer) player).awardStat(stat.get());
                }
            });
        }
    }

    private static boolean listContains(Object list, ItemStack item) {
        for (Object obj : (ArrayList<?>) list) {
            if (obj instanceof String) {
                if (String.valueOf(obj).equals(item.getItem().toString())) {
                    return true;
                }
            }
        }
        return false;
    }
}
