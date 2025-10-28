package com.tharidia.tharidia_things;

import com.tharidia.tharidia_tweaks.rpg_gates.network.SyncGateRestrictionsPacket;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.tharidia.tharidia_things.block.PietroBlock;
import com.tharidia.tharidia_things.block.ClaimBlock;
import com.tharidia.tharidia_things.block.HotIronMarkerBlock;
import com.tharidia.tharidia_things.block.HotGoldMarkerBlock;
import com.tharidia.tharidia_things.block.HotCopperMarkerBlock;
import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import com.tharidia.tharidia_things.block.entity.ClaimBlockEntity;
import com.tharidia.tharidia_things.block.entity.HotIronAnvilEntity;
import com.tharidia.tharidia_things.block.entity.HotGoldAnvilEntity;
import com.tharidia.tharidia_things.block.entity.HotCopperAnvilEntity;
import com.tharidia.tharidia_things.item.HotIronItem;
import com.tharidia.tharidia_things.item.HotGoldItem;
import com.tharidia.tharidia_things.item.HotCopperItem;
import com.tharidia.tharidia_things.item.PinzaItem;
import com.tharidia.tharidia_things.item.LamaLungaItem;
import com.tharidia.tharidia_things.item.LamaCortaItem;
import com.tharidia.tharidia_things.item.ElsaItem;
import com.tharidia.tharidia_things.item.GoldLamaLungaItem;
import com.tharidia.tharidia_things.item.GoldLamaCortaItem;
import com.tharidia.tharidia_things.item.CopperLamaLungaItem;
import com.tharidia.tharidia_things.item.CopperLamaCortaItem;
import com.tharidia.tharidia_things.item.CopperElsaItem;
import com.tharidia.tharidia_things.client.ClientPacketHandler;
import com.tharidia.tharidia_things.command.ClaimCommands;
import com.tharidia.tharidia_things.command.FatigueCommands;
import com.tharidia.tharidia_things.event.ClaimProtectionHandler;
import com.tharidia.tharidia_things.fatigue.FatigueAttachments;
import com.tharidia.tharidia_things.network.ClaimOwnerSyncPacket;
import com.tharidia.tharidia_things.network.FatigueSyncPacket;
import com.tharidia.tharidia_things.network.HierarchySyncPacket;
import com.tharidia.tharidia_things.network.RealmSyncPacket;
import com.tharidia.tharidia_things.network.UpdateHierarchyPacket;
import com.tharidia.tharidia_things.network.SelectComponentPacket;
import com.tharidia.tharidia_things.network.SubmitNamePacket;
import com.tharidia.tharidia_things.realm.RealmManager;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(TharidiaThings.MODID)
public class TharidiaThings {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "tharidiathings";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "tharidiathings" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "tharidiathings" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold BlockEntities which will all be registered under the "tharidiathings" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "tharidiathings" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold MenuTypes which will all be registered under the "tharidiathings" namespace
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, MODID);

    // Creates a new Block with the id "tharidiathings:pietro", combining the namespace and path
    public static final DeferredBlock<PietroBlock> PIETRO = BLOCKS.register("pietro", () -> new PietroBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 6.0F).noOcclusion()));
    // Creates a new BlockItem with the id "tharidiathings:pietro", combining the namespace and path
    public static final DeferredItem<BlockItem> PIETRO_ITEM = ITEMS.registerSimpleBlockItem("pietro", PIETRO);
    // Creates a new Block with the id "tharidiathings:claim"
    public static final DeferredBlock<ClaimBlock> CLAIM = BLOCKS.register("claim", () -> new ClaimBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 6.0F)));
    // Creates a new BlockItem with the id "tharidiathings:claim"
    public static final DeferredItem<BlockItem> CLAIM_ITEM = ITEMS.registerSimpleBlockItem("claim", CLAIM);
    // Hot Iron Marker Block (invisible, used for hot iron on anvil)
    public static final DeferredBlock<HotIronMarkerBlock> HOT_IRON_MARKER = BLOCKS.register("hot_iron_marker", () -> new HotIronMarkerBlock());
    // Hot Gold Marker Block (invisible, used for hot gold on anvil)
    public static final DeferredBlock<HotGoldMarkerBlock> HOT_GOLD_MARKER = BLOCKS.register("hot_gold_marker", () -> new HotGoldMarkerBlock());
    // Hot Copper Marker Block (invisible, used for hot copper on anvil)
    public static final DeferredBlock<HotCopperMarkerBlock> HOT_COPPER_MARKER = BLOCKS.register("hot_copper_marker", () -> new HotCopperMarkerBlock());
    
    // Creates a new BlockEntityType for the Pietro block
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PietroBlockEntity>> PIETRO_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("pietro", () -> BlockEntityType.Builder.of(PietroBlockEntity::new, PIETRO.get()).build(null));
    // Creates a new BlockEntityType for the Claim block
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClaimBlockEntity>> CLAIM_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("claim", () -> BlockEntityType.Builder.of(ClaimBlockEntity::new, CLAIM.get()).build(null));
    // Creates a new BlockEntityType for the Hot Iron on Anvil
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HotIronAnvilEntity>> HOT_IRON_ANVIL_ENTITY =
        BLOCK_ENTITIES.register("hot_iron_anvil", () -> BlockEntityType.Builder.of(HotIronAnvilEntity::new, HOT_IRON_MARKER.get()).build(null));
    // Creates a new BlockEntityType for the Hot Gold on Anvil
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HotGoldAnvilEntity>> HOT_GOLD_ANVIL_ENTITY =
        BLOCK_ENTITIES.register("hot_gold_anvil", () -> BlockEntityType.Builder.of(HotGoldAnvilEntity::new, HOT_GOLD_MARKER.get()).build(null));
    // Creates a new BlockEntityType for the Hot Copper on Anvil
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HotCopperAnvilEntity>> HOT_COPPER_ANVIL_ENTITY =
        BLOCK_ENTITIES.register("hot_copper_anvil", () -> BlockEntityType.Builder.of(HotCopperAnvilEntity::new, HOT_COPPER_MARKER.get()).build(null));
    
    // Creates a MenuType for the Claim GUI
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.tharidia.tharidia_things.gui.ClaimMenu>> CLAIM_MENU =
        MENU_TYPES.register("claim_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.tharidia.tharidia_things.gui.ClaimMenu::new));
    
    // Creates a MenuType for the Pietro GUI
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.tharidia.tharidia_things.gui.PietroMenu>> PIETRO_MENU =
        MENU_TYPES.register("pietro_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.tharidia.tharidia_things.gui.PietroMenu::new));
    
    // Creates a MenuType for the Component Selection GUI
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.tharidia.tharidia_things.gui.ComponentSelectionMenu>> COMPONENT_SELECTION_MENU =
        MENU_TYPES.register("component_selection_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.tharidia.tharidia_things.gui.ComponentSelectionMenu::new));
    
    // Creates a MenuType for the Name Selection GUI
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.tharidia.tharidia_things.gui.NameSelectionMenu>> NAME_SELECTION_MENU =
        MENU_TYPES.register("name_selection_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.tharidia.tharidia_things.gui.NameSelectionMenu::new));
    
    // Smithing items
    public static final DeferredItem<Item> HOT_IRON = ITEMS.register("hot_iron", () -> new HotIronItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> HOT_GOLD = ITEMS.register("hot_gold", () -> new HotGoldItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> HOT_COPPER = ITEMS.register("hot_copper", () -> new HotCopperItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> PINZA = ITEMS.register("pinza", () -> new PinzaItem(new Item.Properties()));
    public static final DeferredItem<Item> LAMA_LUNGA = ITEMS.register("lama_lunga", () -> new LamaLungaItem(new Item.Properties()));
    public static final DeferredItem<Item> LAMA_CORTA = ITEMS.register("lama_corta", () -> new LamaCortaItem(new Item.Properties()));
    public static final DeferredItem<Item> ELSA = ITEMS.register("elsa", () -> new ElsaItem(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_LAMA_LUNGA = ITEMS.register("gold_lama_lunga", () -> new GoldLamaLungaItem(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_LAMA_CORTA = ITEMS.register("gold_lama_corta", () -> new GoldLamaCortaItem(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_LAMA_LUNGA = ITEMS.register("copper_lama_lunga", () -> new CopperLamaLungaItem(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_LAMA_CORTA = ITEMS.register("copper_lama_corta", () -> new CopperLamaCortaItem(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_ELSA = ITEMS.register("copper_elsa", () -> new CopperElsaItem(new Item.Properties()));

    // Creates a creative tab with the id "tharidiathings:tharidia_tab" for the mod items, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THARIDIA_TAB = CREATIVE_MODE_TABS.register("tharidia_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tharidiathings")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> PIETRO_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(PIETRO_ITEM.get());
                output.accept(CLAIM_ITEM.get());
                output.accept(HOT_IRON.get());
                output.accept(HOT_GOLD.get());
                output.accept(HOT_COPPER.get());
                output.accept(PINZA.get());
                output.accept(LAMA_LUNGA.get());
                output.accept(LAMA_CORTA.get());
                output.accept(ELSA.get());
                output.accept(GOLD_LAMA_LUNGA.get());
                output.accept(GOLD_LAMA_CORTA.get());
                output.accept(COPPER_LAMA_LUNGA.get());
                output.accept(COPPER_LAMA_CORTA.get());
                output.accept(COPPER_ELSA.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public TharidiaThings(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Register network packets
        modEventBus.addListener(this::registerPayloads);
        // Register client-side screen handlers (only on client)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::registerScreens);
        }

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entities get registered
        BLOCK_ENTITIES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so menus get registered
        MENU_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so attachment types get registered
        FatigueAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (TharidiaThings) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        // Register the claim protection handler
        NeoForge.EVENT_BUS.register(ClaimProtectionHandler.class);
        // Register the claim expiration handler
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.event.ClaimExpirationHandler.class);
        // Register the realm placement handler
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.event.RealmPlacementHandler.class);
        // Register the weight debuff handler
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.event.WeightDebuffHandler.class);
        // Register the smithing handler
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.event.SmithingHandler.class);
        // Register the name selection handler
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.event.NameSelectionHandler.class);
        // Register the fatigue handler
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.event.FatigueHandler.class);
        
        // Log version for debugging
        LOGGER.info("=================================================");
        LOGGER.info("TharidiaThings v1.0.8 - NEW REST SYSTEM LOADED");
        LOGGER.info("Features: Rest Near Bed, No Force-Back, Time Skip Block");
        LOGGER.info("=================================================");
        
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Common setup
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        LOGGER.info("Registering network payloads (dist: {})", FMLEnvironment.dist);

        if (FMLEnvironment.dist.isClient()) {
            registrar.playToClient(
                ClaimOwnerSyncPacket.TYPE,
                ClaimOwnerSyncPacket.STREAM_CODEC,
                ClientPacketHandler::handleClaimOwnerSync
            );
            registrar.playToClient(
                RealmSyncPacket.TYPE,
                RealmSyncPacket.STREAM_CODEC,
                ClientPacketHandler::handleRealmSync
            );
            registrar.playToClient(
                HierarchySyncPacket.TYPE,
                HierarchySyncPacket.STREAM_CODEC,
                ClientPacketHandler::handleHierarchySync
            );
            registrar.playToClient(
                FatigueSyncPacket.TYPE,
                FatigueSyncPacket.STREAM_CODEC,
                ClientPacketHandler::handleFatigueSync
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.FatigueWarningPacket.TYPE,
                com.tharidia.tharidia_things.network.FatigueWarningPacket.STREAM_CODEC,
                ClientPacketHandler::handleFatigueWarning
            );
            // RPG Gates packet (from tharidia_tweaks integration)
            registrar.playToClient(
                    SyncGateRestrictionsPacket.TYPE,
                    SyncGateRestrictionsPacket.STREAM_CODEC,
                    ClientPacketHandler::handleSyncRestriciton
            );
            LOGGER.info("Client packet handlers registered (including RPG Gates)");
        } else {
            // On server, register dummy handlers (packets won't be received here anyway)
            registrar.playToClient(
                ClaimOwnerSyncPacket.TYPE,
                ClaimOwnerSyncPacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            registrar.playToClient(
                RealmSyncPacket.TYPE,
                RealmSyncPacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            registrar.playToClient(
                HierarchySyncPacket.TYPE,
                HierarchySyncPacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            registrar.playToClient(
                FatigueSyncPacket.TYPE,
                FatigueSyncPacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.FatigueWarningPacket.TYPE,
                com.tharidia.tharidia_things.network.FatigueWarningPacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            // RPG Gates packet (dummy handler on server)
            registrar.playToClient(
                    SyncGateRestrictionsPacket.TYPE,
                    SyncGateRestrictionsPacket.STREAM_CODEC,
                    (packet, context) -> {}
            );
            LOGGER.info("Server-side packet registration completed (dummy handlers)");
        }
        
        // Register server-bound packets (works on both sides)
        registrar.playToServer(
            UpdateHierarchyPacket.TYPE,
            UpdateHierarchyPacket.STREAM_CODEC,
            UpdateHierarchyPacket::handle
        );
        registrar.playToServer(
            SelectComponentPacket.TYPE,
            SelectComponentPacket.STREAM_CODEC,
            SelectComponentPacket::handle
        );
        registrar.playToServer(
            SubmitNamePacket.TYPE,
            SubmitNamePacket.STREAM_CODEC,
            SubmitNamePacket::handle
        );
    }

    private void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(CLAIM_MENU.get(), com.tharidia.tharidia_things.client.gui.ClaimScreen::new);
        event.register(PIETRO_MENU.get(), com.tharidia.tharidia_things.client.gui.PietroScreen::new);
        event.register(COMPONENT_SELECTION_MENU.get(), com.tharidia.tharidia_things.client.gui.ComponentSelectionScreen::new);
        event.register(NAME_SELECTION_MENU.get(), com.tharidia.tharidia_things.client.gui.NameSelectionScreen::new);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // Send a full sync on login - this will clear and replace all client-side realm data
            syncAllRealmsToPlayer((ServerPlayer) event.getEntity(), serverLevel);
        }
    }
    
    /**
     * Sends all realm data to a specific player (full sync)
     */
    private void syncAllRealmsToPlayer(ServerPlayer player, net.minecraft.server.level.ServerLevel serverLevel) {
        List<RealmSyncPacket.RealmData> realmDataList = new ArrayList<>();
        List<PietroBlockEntity> allRealms = RealmManager.getRealms(serverLevel);
        
        for (PietroBlockEntity realm : allRealms) {
            RealmSyncPacket.RealmData data = new RealmSyncPacket.RealmData(
                realm.getBlockPos(),
                realm.getRealmSize(),
                realm.getOwnerName(),
                realm.getCenterChunk().x,
                realm.getCenterChunk().z
            );
            realmDataList.add(data);
        }
        
        RealmSyncPacket packet = new RealmSyncPacket(realmDataList, true); // true = full sync
        PacketDistributor.sendToPlayer(player, packet);

        LOGGER.info("Synced {} realms to player {}", realmDataList.size(), player.getName().getString());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Load claim registry from persistent storage
        net.minecraft.server.level.ServerLevel overworld = event.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld != null) {
            com.tharidia.tharidia_things.claim.ClaimRegistry.loadFromPersistentStorage(overworld);
        } else {
            LOGGER.error("Could not load claim registry: overworld is null");
        }
        
        // Register weight data loader
        event.getServer().getResourceManager();
    }
    
    @SubscribeEvent
    public void onAddReloadListeners(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(new com.tharidia.tharidia_things.weight.WeightDataLoader());
        event.addListener(new com.tharidia.tharidia_things.config.CropProtectionConfig());
        event.addListener(new com.tharidia.tharidia_things.config.FatigueConfig());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ClaimCommands.register(event.getDispatcher());
        com.tharidia.tharidia_things.command.ClaimAdminCommands.register(event.getDispatcher());
        FatigueCommands.register(event.getDispatcher());
    }

    /**
     * Helper method to create a ResourceLocation for this mod
     */
    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
