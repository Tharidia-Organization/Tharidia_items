package com.tharidia.tharidia_things;

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
import com.tharidia.tharidia_things.item.BattleGauntlet;
import com.tharidia.tharidia_things.item.CopperElsaItem;
import com.tharidia.tharidia_things.client.ClientPacketHandler;
import com.tharidia.tharidia_things.command.BattleCommands;
import com.tharidia.tharidia_things.command.ClaimCommands;
import com.tharidia.tharidia_things.command.FatigueCommands;
import com.tharidia.tharidia_things.compoundTag.BattleGauntleAttachments;
import com.tharidia.tharidia_things.event.ClaimProtectionHandler;
import com.tharidia.tharidia_things.fatigue.FatigueAttachments;
import com.tharidia.tharidia_things.features.FreezeManager;
import com.tharidia.tharidia_things.network.BattlePackets;
import com.tharidia.tharidia_things.network.ClaimOwnerSyncPacket;
import com.tharidia.tharidia_things.network.FatigueSyncPacket;
import com.tharidia.tharidia_things.network.DungeonQueuePacket;
import com.tharidia.tharidia_things.network.HierarchySyncPacket;
import com.tharidia.tharidia_things.network.RealmSyncPacket;
import com.tharidia.tharidia_things.network.UpdateHierarchyPacket;
import com.tharidia.tharidia_things.network.SelectComponentPacket;
import com.tharidia.tharidia_things.network.SubmitNamePacket;
import com.tharidia.tharidia_things.network.SyncGateRestrictionsPacket;
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

    
    // Database system for cross-server communication
    private static com.tharidia.tharidia_things.database.DatabaseManager databaseManager;

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
    
    // Creates a MenuType for the Trade GUI
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.tharidia.tharidia_things.gui.TradeMenu>> TRADE_MENU =
        MENU_TYPES.register("trade_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.tharidia.tharidia_things.gui.TradeMenu::new));

    // Creates a MenuType for the Battle Invite GUI
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.tharidia.tharidia_things.gui.BattleInviteMenu>> BATTLE_INVITE_MENU =
        MENU_TYPES.register("battle_invite_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.tharidia.tharidia_things.gui.BattleInviteMenu::new));
    
    
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

    // Battle Gauntlet
    public static final DeferredItem<Item> BATTLE_GAUNTLE = ITEMS.register("battle_gauntlet", ()->new BattleGauntlet(new Item.Properties().stacksTo(1)));

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
                output.accept(BATTLE_GAUNTLE.get());
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
        // Register server stopping event
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
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
        // Register the freeze manager
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.features.FreezeManager.class);
        // Register the pre-login name handler
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.event.PreLoginNameHandler.class);
        // Register the fatigue handler
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.event.FatigueHandler.class);
        // Register the trade interaction handler
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.event.TradeInteractionHandler.class);
        // Register the trade inventory blocker
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.event.TradeInventoryBlocker.class);
        // Register the currency protection handler
        NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.event.CurrencyProtectionHandler.class);

        // Register Freeze Manager for master freeze command
        NeoForge.EVENT_BUS.register(FreezeManager.class);

        BattleGauntleAttachments.register(modEventBus);

        modEventBus.addListener(BattlePackets::register);

        // Register handshake bypass (CLIENT ONLY)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(com.tharidia.tharidia_things.client.HandshakeBypass.class);
            LOGGER.warn("Handshake bypass registered - you can connect to servers with different mod versions");

            // Check for video tools on Windows
            com.tharidia.tharidia_things.client.video.VideoToolsManager.getInstance().checkAndInstallTools();
        }
        
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
        
        // Network payload registration for mod features
        
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
            // RPG Gates sync from tharidiatweaks
            registrar.playToClient(
                SyncGateRestrictionsPacket.TYPE,
                SyncGateRestrictionsPacket.STREAM_CODEC,
                ClientPacketHandler::handleGateRestrictionsSync
            );
            // Name request packet
            registrar.playToClient(
                com.tharidia.tharidia_things.network.RequestNamePacket.TYPE,
                com.tharidia.tharidia_things.network.RequestNamePacket.STREAM_CODEC,
                ClientPacketHandler::handleRequestName
            );
            // Zone music packet from tharidiatweaks
            registrar.playToClient(
                com.tharidia.tharidia_things.network.ZoneMusicPacket.TYPE,
                com.tharidia.tharidia_things.network.ZoneMusicPacket.STREAM_CODEC,
                ClientPacketHandler::handleZoneMusic
            );
            // Music file data packet
            registrar.playToClient(
                com.tharidia.tharidia_things.network.MusicFileDataPacket.TYPE,
                com.tharidia.tharidia_things.network.MusicFileDataPacket.STREAM_CODEC,
                ClientPacketHandler::handleMusicFileData
            );
            // Trade packets (client-bound)
            registrar.playToClient(
                com.tharidia.tharidia_things.network.TradeRequestPacket.TYPE,
                com.tharidia.tharidia_things.network.TradeRequestPacket.STREAM_CODEC,
                (packet, context) -> com.tharidia.tharidia_things.client.TradeClientHandler.handleTradeRequest(packet)
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.TradeCompletePacket.TYPE,
                com.tharidia.tharidia_things.network.TradeCompletePacket.STREAM_CODEC,
                (packet, context) -> com.tharidia.tharidia_things.client.TradeClientHandler.handleTradeComplete(packet)
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.TradeSyncPacket.TYPE,
                com.tharidia.tharidia_things.network.TradeSyncPacket.STREAM_CODEC,
                (packet, context) -> com.tharidia.tharidia_things.client.TradeClientHandler.handleTradeSync(packet)
            );

            // Register bungeecord:main channel to satisfy server requirement (dummy handler)
            registrar.playToClient(
                com.tharidia.tharidia_things.network.BungeeCordPacket.TYPE,
                com.tharidia.tharidia_things.network.BungeeCordPacket.STREAM_CODEC,
                (packet, context) -> {} // Dummy handler - we don't process bungeecord messages
            );

            // Video screen packets
            registrar.playToClient(
                com.tharidia.tharidia_things.network.VideoScreenSyncPacket.TYPE,
                com.tharidia.tharidia_things.network.VideoScreenSyncPacket.STREAM_CODEC,
                ClientPacketHandler::handleVideoScreenSync
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.VideoScreenDeletePacket.TYPE,
                com.tharidia.tharidia_things.network.VideoScreenDeletePacket.STREAM_CODEC,
                ClientPacketHandler::handleVideoScreenDelete
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.VideoScreenSeekPacket.TYPE,
                com.tharidia.tharidia_things.network.VideoScreenSeekPacket.STREAM_CODEC,
                com.tharidia.tharidia_things.client.ClientSeekPacketHandler::handleSeekPacket
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.VideoScreenVolumePacket.TYPE,
                com.tharidia.tharidia_things.network.VideoScreenVolumePacket.STREAM_CODEC,
                com.tharidia.tharidia_things.network.VideoScreenVolumePacket::handle
            );

            LOGGER.info("Client packet handlers registered");
        } else {
            
            // Server-side packet registration

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
            // RPG Gates sync from tharidiatweaks (dummy handler)
            registrar.playToClient(
                SyncGateRestrictionsPacket.TYPE,
                SyncGateRestrictionsPacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            // Name request packet (dummy handler)
            registrar.playToClient(
                com.tharidia.tharidia_things.network.RequestNamePacket.TYPE,
                com.tharidia.tharidia_things.network.RequestNamePacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            // Zone music packet (dummy handler)
            registrar.playToClient(
                com.tharidia.tharidia_things.network.ZoneMusicPacket.TYPE,
                com.tharidia.tharidia_things.network.ZoneMusicPacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            // Music file data packet (dummy handler)
            registrar.playToClient(
                com.tharidia.tharidia_things.network.MusicFileDataPacket.TYPE,
                com.tharidia.tharidia_things.network.MusicFileDataPacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            // Trade packets (client-bound, dummy handlers)
            registrar.playToClient(
                com.tharidia.tharidia_things.network.TradeRequestPacket.TYPE,
                com.tharidia.tharidia_things.network.TradeRequestPacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.TradeCompletePacket.TYPE,
                com.tharidia.tharidia_things.network.TradeCompletePacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.TradeSyncPacket.TYPE,
                com.tharidia.tharidia_things.network.TradeSyncPacket.STREAM_CODEC,
                (packet, context) -> {}
            );

            // Register bungeecord:main channel on server side (dummy handler for consistency)
            registrar.playToClient(
                com.tharidia.tharidia_things.network.BungeeCordPacket.TYPE,
                com.tharidia.tharidia_things.network.BungeeCordPacket.STREAM_CODEC,
                (packet, context) -> {} // Dummy handler
            );

            // Video screen packets (dummy handlers for server)
            registrar.playToClient(
                com.tharidia.tharidia_things.network.VideoScreenSyncPacket.TYPE,
                com.tharidia.tharidia_things.network.VideoScreenSyncPacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.VideoScreenDeletePacket.TYPE,
                com.tharidia.tharidia_things.network.VideoScreenDeletePacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.VideoScreenSeekPacket.TYPE,
                com.tharidia.tharidia_things.network.VideoScreenSeekPacket.STREAM_CODEC,
                (packet, context) -> {}
            );
            registrar.playToClient(
                com.tharidia.tharidia_things.network.VideoScreenVolumePacket.TYPE,
                com.tharidia.tharidia_things.network.VideoScreenVolumePacket.STREAM_CODEC,
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
            DungeonQueuePacket.TYPE,
            DungeonQueuePacket.STREAM_CODEC,
            DungeonQueuePacket::handle
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
        // Trade packets (server-bound)
        registrar.playToServer(
            com.tharidia.tharidia_things.network.TradeResponsePacket.TYPE,
            com.tharidia.tharidia_things.network.TradeResponsePacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> 
                com.tharidia.tharidia_things.network.TradePacketHandler.handleTradeResponse(packet, (ServerPlayer) context.player()))
        );
        registrar.playToServer(
            com.tharidia.tharidia_things.network.TradeUpdatePacket.TYPE,
            com.tharidia.tharidia_things.network.TradeUpdatePacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> 
                com.tharidia.tharidia_things.network.TradePacketHandler.handleTradeUpdate(packet, (ServerPlayer) context.player()))
        );
        registrar.playToServer(
            com.tharidia.tharidia_things.network.TradeCancelPacket.TYPE,
            com.tharidia.tharidia_things.network.TradeCancelPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> 
                com.tharidia.tharidia_things.network.TradePacketHandler.handleTradeCancel(packet, (ServerPlayer) context.player()))
        );
        registrar.playToServer(
            com.tharidia.tharidia_things.network.TradeFinalConfirmPacket.TYPE,
            com.tharidia.tharidia_things.network.TradeFinalConfirmPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> 
                com.tharidia.tharidia_things.network.TradePacketHandler.handleTradeFinalConfirm(packet, (ServerPlayer) context.player()))
        );
        // Music file request packet (server-bound)
        registrar.playToServer(
            com.tharidia.tharidia_things.network.RequestMusicFilePacket.TYPE,
            com.tharidia.tharidia_things.network.RequestMusicFilePacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> 
                com.tharidia.tharidia_things.network.ServerMusicFileHandler.handleMusicFileRequest(packet, (ServerPlayer) context.player()))
        );
    }

    private void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(CLAIM_MENU.get(), com.tharidia.tharidia_things.client.gui.ClaimScreen::new);
        event.register(PIETRO_MENU.get(), com.tharidia.tharidia_things.client.gui.PietroScreen::new);
        event.register(COMPONENT_SELECTION_MENU.get(), com.tharidia.tharidia_things.client.gui.ComponentSelectionScreen::new);
        event.register(TRADE_MENU.get(), com.tharidia.tharidia_things.client.gui.TradeScreen::new);
        event.register(BATTLE_INVITE_MENU.get(), com.tharidia.tharidia_things.client.gui.BattleInviteScreen::new);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // Send a full sync on login - this will clear and replace all client-side realm data
            syncAllRealmsToPlayer((ServerPlayer) event.getEntity(), serverLevel);

            // Sync all video screens to the player
            syncAllVideoScreensToPlayer((ServerPlayer) event.getEntity(), serverLevel);
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

    /**
     * Sends all video screens to a specific player
     */
    private void syncAllVideoScreensToPlayer(ServerPlayer player, net.minecraft.server.level.ServerLevel serverLevel) {
        String dimension = serverLevel.dimension().location().toString();
        com.tharidia.tharidia_things.video.VideoScreenRegistry registry =
            com.tharidia.tharidia_things.video.VideoScreenRegistry.get(serverLevel);

        java.util.Collection<com.tharidia.tharidia_things.video.VideoScreen> screens =
            registry.getScreensInDimension(dimension);

        for (com.tharidia.tharidia_things.video.VideoScreen screen : screens) {
            com.tharidia.tharidia_things.network.VideoScreenSyncPacket packet =
                new com.tharidia.tharidia_things.network.VideoScreenSyncPacket(
                    screen.getId(),
                    dimension,
                    screen.getCorner1(),
                    screen.getCorner2(),
                    screen.getVideoUrl(),
                    screen.getPlaybackState(),
                    screen.getVolume()
                );
            PacketDistributor.sendToPlayer(player, packet);
        }

        LOGGER.info("Synced {} video screens to player {}", screens.size(), player.getName().getString());
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
        
        // Initialize database system
        initializeDatabaseSystem(event.getServer());
        
            }
    
    /**
     * Initializes the database system for cross-server communication
     */
    private void initializeDatabaseSystem(net.minecraft.server.MinecraftServer server) {
        try {
            LOGGER.info("Initializing database system...");
            
            // Create database manager
            databaseManager = new com.tharidia.tharidia_things.database.DatabaseManager(LOGGER);
            
            // Initialize database connection
            if (databaseManager.initialize()) {
                LOGGER.info("Database system initialized successfully");
            } else {
                LOGGER.warn("Database initialization failed or disabled");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize database system: {}", e.getMessage(), e);
        }
    }
    
    

    /**
     * Called when the server is stopping
     */
    public void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        LOGGER.info("Server stopping, cleaning up resources...");
        

        // Then shutdown database
        if (databaseManager != null) {
            try {
                LOGGER.info("Shutting down database...");
                databaseManager.shutdown();
                LOGGER.info("Database shutdown complete");
            } catch (Exception e) {
                LOGGER.error("Error shutting down database: {}", e.getMessage(), e);
            }
        }
        
        LOGGER.info("Resource cleanup completed");
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
        com.tharidia.tharidia_things.command.TradeCommands.register(event.getDispatcher());
        BattleCommands.register(event.getDispatcher());
        com.tharidia.tharidia_things.command.MarketCommands.register(event.getDispatcher());
        com.tharidia.tharidia_things.command.VideoScreenCommands.register(event.getDispatcher());
    }

    /**
     * Helper method to create a ResourceLocation for this mod
     */
    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
