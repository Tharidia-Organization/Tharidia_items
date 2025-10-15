package com.tharidia.tharidia_things;

import com.tharidia.tharidia_things.client.ClaimBoundaryRenderer;
import com.tharidia.tharidia_things.client.ClientConnectionHandler;
import com.tharidia.tharidia_things.client.RealmBoundaryRenderer;
import com.tharidia.tharidia_things.client.RealmClientHandler;
import com.tharidia.tharidia_things.client.RealmOverlay;
import com.tharidia.tharidia_things.client.renderer.PietroBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = TharidiaThings.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class TharidiaThingsClient {
    public TharidiaThingsClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        
        // Register client-side event handlers to the NeoForge event bus
        NeoForge.EVENT_BUS.register(RealmBoundaryRenderer.class);
        NeoForge.EVENT_BUS.register(ClaimBoundaryRenderer.class);
        NeoForge.EVENT_BUS.register(RealmClientHandler.class);
        NeoForge.EVENT_BUS.register(ClientConnectionHandler.class);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        TharidiaThings.LOGGER.info("HELLO FROM CLIENT SETUP");
        TharidiaThings.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        // Register AzureLib block entity renderer
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(TharidiaThings.PIETRO_BLOCK_ENTITY.get(), context -> new PietroBlockRenderer());
        });
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // Register the realm overlay to be rendered above the hotbar
        event.registerAbove(
            VanillaGuiLayers.HOTBAR,
            TharidiaThings.modLoc("realm_overlay"),
            new RealmOverlay()
        );
        TharidiaThings.LOGGER.info("Registered Realm Overlay");
        
        // Register the weight HUD overlay in the lower left corner
        event.registerAbove(
            VanillaGuiLayers.HOTBAR,
            TharidiaThings.modLoc("weight_overlay"),
            new com.tharidia.tharidia_things.client.WeightHudOverlay()
        );
        TharidiaThings.LOGGER.info("Registered Weight HUD Overlay");
    }
}
