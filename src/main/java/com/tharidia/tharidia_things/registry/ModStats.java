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

    public static void register(IEventBus eventBus) {
        CUSTOM_STATS.register(eventBus);
    }
}
