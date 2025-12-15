package com.tharidia.tharidia_things.registry;

import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStats {
    public static final DeferredRegister<ResourceLocation> CUSTOM_STATS = DeferredRegister
            .create(Registries.CUSTOM_STAT, TharidiaThings.MODID);

    public static final DeferredHolder<ResourceLocation, ResourceLocation> LAMA_CORTA_KILL = CUSTOM_STATS.register(
            "lama_corta_kill",
            () -> ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "lama_corta_kill"));

    public static final DeferredHolder<ResourceLocation, ResourceLocation> LANCIA_KILL = CUSTOM_STATS.register(
            "lancia_kill",
            () -> ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "lancia_kill"));

    public static final DeferredHolder<ResourceLocation, ResourceLocation> MARTELLI_KILL = CUSTOM_STATS.register(
            "martelli_kill",
            () -> ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "martelli_kill"));

    public static final DeferredHolder<ResourceLocation, ResourceLocation> MAZZE_KILL = CUSTOM_STATS.register(
            "mazze_kill",
            () -> ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "mazze_kill"));

    public static final DeferredHolder<ResourceLocation, ResourceLocation> SPADE_2_MANI_KILL = CUSTOM_STATS.register(
            "spade_2_mani_kill",
            () -> ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "spade_2_mani_kill"));

    public static final DeferredHolder<ResourceLocation, ResourceLocation> ASCE_KILL = CUSTOM_STATS.register(
            "asce_kill",
            () -> ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "asce_kill"));

    public static final DeferredHolder<ResourceLocation, ResourceLocation> SOCCHI_KILL = CUSTOM_STATS.register(
            "socchi_kill",
            () -> ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "socchi_kill"));

    public static final DeferredHolder<ResourceLocation, ResourceLocation> ARCHI_KILL = CUSTOM_STATS.register(
            "archi_kill",
            () -> ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "archi_kill"));

    public static final DeferredHolder<ResourceLocation, ResourceLocation> ARMI_DA_FUOCO_KILL = CUSTOM_STATS.register(
            "armi_da_fuoco_kill",
            () -> ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "armi_da_fuoco_kill"));

    public static void register(IEventBus eventBus) {
        CUSTOM_STATS.register(eventBus);
    }

    public static String[] getAllStatNames() {
        return new String[] {
                "lama_corta_kill",
                "lancia_kill",
                "martelli_kill",
                "mazze_kill",
                "spade_2_mani_kill",
                "asce_kill",
                "socchi_kill",
                "archi_kill",
                "armi_da_fuoco_kill"
        };        
    }
}
