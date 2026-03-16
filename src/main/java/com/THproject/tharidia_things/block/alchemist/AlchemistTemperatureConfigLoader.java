package com.THproject.tharidia_things.block.alchemist;

import com.THproject.tharidia_things.TharidiaThings;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class AlchemistTemperatureConfigLoader
        extends SimplePreparableReloadListener<AlchemistTemperatureConfig> {

    public static final AlchemistTemperatureConfigLoader INSTANCE =
            new AlchemistTemperatureConfigLoader();

    private static final ResourceLocation RESOURCE = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "alchemist_table/temperature_config.json");

    @Override
    protected AlchemistTemperatureConfig prepare(ResourceManager manager, ProfilerFiller profiler) {
        var resource = manager.getResource(RESOURCE);
        if (resource.isEmpty()) {
            TharidiaThings.LOGGER.info("[AlchemistTable] temperature_config.json not found, using defaults.");
            return new AlchemistTemperatureConfig();
        }
        try (var reader = resource.get().openAsReader()) {
            JsonObject json = GsonHelper.parse(reader);
            TharidiaThings.LOGGER.info("[AlchemistTable] Loaded temperature_config.json");
            return AlchemistTemperatureConfig.fromJson(json);
        } catch (Exception e) {
            TharidiaThings.LOGGER.warn("[AlchemistTable] Failed to parse temperature_config.json: {}", e.getMessage());
            return new AlchemistTemperatureConfig();
        }
    }

    @Override
    protected void apply(AlchemistTemperatureConfig config, ResourceManager manager, ProfilerFiller profiler) {
        AlchemistTemperatureConfig.INSTANCE = config;
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
