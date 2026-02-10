package com.THproject.tharidia_things;

import com.THproject.tharidia_things.client.WeightHudOverlay;
import com.THproject.tharidia_things.client.video.VideoScreenRenderHandler;
import com.THproject.tharidia_things.client.ClaimBoundaryRenderer;
import com.THproject.tharidia_things.client.ClientConnectionHandler;
import com.THproject.tharidia_things.client.RealmBoundaryRenderer;
import com.THproject.tharidia_things.client.RealmClientHandler;
import com.THproject.tharidia_things.client.RealmOverlay;
import com.THproject.tharidia_things.client.StaminaHudOverlay;
import com.THproject.tharidia_things.client.ZoneMusicPlayer;
import com.THproject.tharidia_things.client.video.DependencyCheckHandler;
import com.THproject.tharidia_things.client.renderer.PietroBlockRenderer;
import com.THproject.tharidia_things.client.renderer.PulverizerRenderer;
import com.THproject.tharidia_things.client.renderer.SieveRenderer;
import com.THproject.tharidia_things.client.renderer.TankRenderer;
import com.THproject.tharidia_things.client.renderer.SinkRenderer;
import com.THproject.tharidia_things.client.renderer.HotIronAnvilRenderer;
import com.THproject.tharidia_things.client.renderer.HotGoldAnvilRenderer;
import com.THproject.tharidia_things.client.renderer.HotCopperAnvilRenderer;
import com.THproject.tharidia_things.client.renderer.StableBlockRenderer;
import com.THproject.tharidia_things.client.renderer.SmithingFurnaceRenderer;
import com.THproject.tharidia_things.diet.ClientDietProfileCache;
import com.THproject.tharidia_things.diet.DietRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
// import com.THproject.tharidia_things.command.EquipClientCommand;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = TharidiaThings.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods
// in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class TharidiaThingsClient {
    private static ClientDietProfileCache clientDietCache = null;

    public TharidiaThingsClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your
        // mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json
        // file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // Initialize zone music player
        ZoneMusicPlayer.initialize();

        // Register client-side event handlers to the NeoForge event bus
        NeoForge.EVENT_BUS.register(RealmBoundaryRenderer.class);
        NeoForge.EVENT_BUS.register(ClaimBoundaryRenderer.class);
        NeoForge.EVENT_BUS.register(RealmClientHandler.class);
        NeoForge.EVENT_BUS.register(ClientConnectionHandler.class);
        NeoForge.EVENT_BUS.register(VideoScreenRenderHandler.class);
        NeoForge.EVENT_BUS.register(DependencyCheckHandler.class);

        // Debug: Verify the new code is running
        TharidiaThings.LOGGER.info("[VIDEO DEPENDENCIES] DependencyCheckHandler registered on NeoForge EVENT_BUS");
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Register block entity renderers
        event.enqueueWork(() -> {
            // Trigger dependency check immediately after client setup
            TharidiaThings.LOGGER.info("[VIDEO DEPENDENCIES] Triggering dependency check from client setup");
            DependencyCheckHandler.forceRecheck();

            // Initialize client diet profile cache
            initializeClientDietCache();
        });
    }

    private static void initializeClientDietCache() {
        try {
            clientDietCache = new ClientDietProfileCache();
            clientDietCache.load();

            // Start background calculation if needed
            if (clientDietCache.needsRecalculation()) {
                TharidiaThings.LOGGER.info("[DIET CLIENT] Starting background calculation of diet profiles...");
                clientDietCache.calculateAsync(DietRegistry.getSettings());
            } else {
                TharidiaThings.LOGGER.info("[DIET CLIENT] Using cached diet profiles");
            }
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("[DIET CLIENT] Failed to initialize client diet cache", e);
            clientDietCache = null;
        }
    }

    public static ClientDietProfileCache getClientDietCache() {
        return clientDietCache;
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register block entity renderers using the event method
        event.registerBlockEntityRenderer(TharidiaThings.PIETRO_BLOCK_ENTITY.get(), PietroBlockRenderer::new);
        event.registerBlockEntityRenderer(TharidiaThings.HOT_IRON_ANVIL_ENTITY.get(), HotIronAnvilRenderer::new);
        event.registerBlockEntityRenderer(TharidiaThings.HOT_GOLD_ANVIL_ENTITY.get(), HotGoldAnvilRenderer::new);
        event.registerBlockEntityRenderer(TharidiaThings.HOT_COPPER_ANVIL_ENTITY.get(), HotCopperAnvilRenderer::new);
        event.registerBlockEntityRenderer(TharidiaThings.STABLE_BLOCK_ENTITY.get(), StableBlockRenderer::new);
        event.registerBlockEntityRenderer(TharidiaThings.SIEVE_BLOCK_ENTITY.get(), context -> new SieveRenderer());
        event.registerBlockEntityRenderer(TharidiaThings.TANK_BLOCK_ENTITY.get(), context -> new TankRenderer());
        event.registerBlockEntityRenderer(TharidiaThings.SINK_BLOCK_ENTITY.get(), context -> new SinkRenderer());
        event.registerBlockEntityRenderer(TharidiaThings.SMITHING_FURNACE_BLOCK_ENTITY.get(), SmithingFurnaceRenderer::new);
        event.registerBlockEntityRenderer(TharidiaThings.PULVERIZER_BLOCK_ENTITY.get(), context -> new PulverizerRenderer());
    }

    @SubscribeEvent
    static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Set render type for Pietro block to support transparency
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                TharidiaThings.PIETRO.get(),
                RenderType.cutout());
        // Set render type for Stable block to support transparency
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
            TharidiaThings.STABLE.get(),
            RenderType.cutout()
        );

        // Set render type for Dungeon Portal block to support translucency
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                TharidiaThings.DUNGEON_PORTAL.get(),
                RenderType.translucent()
        );

        // Set render type for Smithing Furnace block (GeckoLib) to support transparency
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
            TharidiaThings.SMITHING_FURNACE.get(),
            RenderType.cutout()
        );
    }

    @SubscribeEvent
    static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // Register progressive hot iron 3D models for anvil renderer (0-4 strikes)
        for (int i = 0; i <= 4; i++) {
            event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/hot_iron_anvil_" + i)));
        }

        // Register progressive hot gold 3D models for anvil renderer (0-4 strikes)
        for (int i = 0; i <= 4; i++) {
            event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/hot_gold_anvil_" + i)));
        }

        // Register progressive hot copper 3D models for anvil renderer (0-4 strikes)
        for (int i = 0; i <= 4; i++) {
            event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/hot_copper_anvil_" + i)));
        }

        // Register stall overlay models for conditional rendering (hay and water)
        event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/stall_hay")));
        event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/stall_water")));
        event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/stall_milk")));

        // Register stall manure overlay models (10 stages for 10%, 20%, ..., 100%)
        for (int i = 1; i <= 10; i++) {
            event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath("tharidiathings", "block/stall_shit_" + i)));
        }

        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath("tharidiathings", "geo/tank_water.geo.json")));
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                TharidiaThings.modLoc("realm_overlay"),
                new RealmOverlay());

        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                TharidiaThings.modLoc("weight_overlay"),
                new WeightHudOverlay());

        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                TharidiaThings.modLoc("stamina_overlay"),
                new StaminaHudOverlay());
    }

    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Register custom armor layer for Default (Steve) and Slim (Alex) player models
        // Using var to resolve type inference issues with getSkins()
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.EntityRenderer<?> rawRenderer = event.getSkin(skin);
            if (rawRenderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> livingRenderer) {
                if (livingRenderer.getModel() instanceof net.minecraft.client.model.PlayerModel) {
                    @SuppressWarnings("unchecked")
                    net.minecraft.client.renderer.entity.LivingEntityRenderer<net.minecraft.client.player.AbstractClientPlayer, net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer>> typedRenderer = (net.minecraft.client.renderer.entity.LivingEntityRenderer<net.minecraft.client.player.AbstractClientPlayer, net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer>>) livingRenderer;

                    typedRenderer.addLayer(new com.THproject.tharidia_things.client.renderer.CustomArmorLayer(
                            typedRenderer,
                            event.getEntityModels()));
                }
            }
        }
    }
}
