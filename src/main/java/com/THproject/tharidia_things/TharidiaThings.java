package com.THproject.tharidia_things;

import com.THproject.tharidia_things.claim.ClaimRegistry;
import com.THproject.tharidia_things.client.ClientSeekPacketHandler;
import com.THproject.tharidia_things.client.HandshakeBypass;
import com.THproject.tharidia_things.client.TradeClientHandler;
import com.THproject.tharidia_things.client.gui.*;
import com.THproject.tharidia_things.client.video.VideoToolsManager;
import com.THproject.tharidia_things.command.*;
import com.THproject.tharidia_things.config.CropProtectionConfig;
import com.THproject.tharidia_things.config.FatigueConfig;
import com.THproject.tharidia_things.config.StaminaConfig;
import com.THproject.tharidia_things.database.DatabaseManager;
import com.THproject.tharidia_things.diet.DietAttachments;
import com.THproject.tharidia_things.diet.DietDataLoader;
import com.THproject.tharidia_things.diet.DietHandler;
import com.THproject.tharidia_things.event.*;
import com.THproject.tharidia_things.gui.*;
import com.THproject.tharidia_things.network.*;
import com.THproject.tharidia_things.video.VideoScreen;
import com.THproject.tharidia_things.video.VideoScreenRegistry;
import com.THproject.tharidia_things.weight.WeightDataLoader;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.THproject.tharidia_things.block.PietroBlock;
import com.THproject.tharidia_things.block.ClaimBlock;
import com.THproject.tharidia_things.block.HotIronMarkerBlock;
import com.THproject.tharidia_things.block.HotGoldMarkerBlock;
import com.THproject.tharidia_things.block.HotCopperMarkerBlock;
import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import com.THproject.tharidia_things.block.entity.ClaimBlockEntity;
import com.THproject.tharidia_things.block.entity.HotIronAnvilEntity;
import com.THproject.tharidia_things.block.entity.HotGoldAnvilEntity;
import com.THproject.tharidia_things.block.entity.HotCopperAnvilEntity;
import com.THproject.tharidia_things.item.HotIronItem;
import com.THproject.tharidia_things.item.HotGoldItem;
import com.THproject.tharidia_things.item.HotCopperItem;
import com.THproject.tharidia_things.item.PinzaItem;
import com.THproject.tharidia_things.item.LamaLungaItem;
import com.THproject.tharidia_things.item.LamaCortaItem;
import com.THproject.tharidia_things.item.ElsaItem;
import com.THproject.tharidia_things.item.GoldLamaLungaItem;
import com.THproject.tharidia_things.item.GoldLamaCortaItem;
import com.THproject.tharidia_things.item.CopperLamaLungaItem;
import com.THproject.tharidia_things.item.CopperLamaCortaItem;
import com.THproject.tharidia_things.item.BattleGauntlet;
import com.THproject.tharidia_things.item.CopperElsaItem;
import com.THproject.tharidia_things.item.DiceItem;
import com.THproject.tharidia_things.item.PietroBlockItem;
import com.THproject.tharidia_things.client.ClientPacketHandler;
import com.THproject.tharidia_things.entity.ModEntities;
import com.THproject.tharidia_things.compoundTag.BattleGauntleAttachments;
import com.THproject.tharidia_things.character.CharacterAttachments;
import com.THproject.tharidia_things.config.ItemCatalogueConfig;
import com.THproject.tharidia_things.event.ItemAttributeHandler;
import com.THproject.tharidia_things.event.PlayerStatsIncrementHandler;
import com.THproject.tharidia_things.fatigue.FatigueAttachments;
import com.THproject.tharidia_things.features.FreezeManager;
import com.THproject.tharidia_things.network.BattlePackets;
import com.THproject.tharidia_things.network.ClaimOwnerSyncPacket;
import com.THproject.tharidia_things.network.FatigueSyncPacket;
import com.THproject.tharidia_things.network.DungeonQueuePacket;
import com.THproject.tharidia_things.network.HierarchySyncPacket;
import com.THproject.tharidia_things.network.RealmSyncPacket;
import com.THproject.tharidia_things.network.UpdateHierarchyPacket;
import com.THproject.tharidia_things.network.SelectComponentPacket;
import com.THproject.tharidia_things.network.SubmitNamePacket;
import com.THproject.tharidia_things.network.SyncGateRestrictionsPacket;
import com.THproject.tharidia_things.realm.RealmManager;
import com.THproject.tharidia_things.registry.ModAttributes;
import com.THproject.tharidia_things.registry.ModStats;
import com.THproject.tharidia_things.stamina.StaminaAttachments;
import com.THproject.tharidia_things.stamina.StaminaTagMappingsLoader;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatFormatter;
import com.THproject.tharidia_things.servertransfer.ServerTransferManager;
import com.THproject.tharidia_things.servertransfer.ServerTransferCommands;
import com.THproject.tharidia_things.servertransfer.TransferTokenManager;
import com.THproject.tharidia_things.servertransfer.DevWhitelistManager;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
    // Create a Deferred Register to hold Blocks which will all be registered under
    // the "tharidiathings" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under
    // the "tharidiathings" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold BlockEntities which will all be registered
    // under the "tharidiathings" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be
    // registered under the "tharidiathings" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold MenuTypes which will all be registered
    // under the "tharidiathings" namespace
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENU_TYPES = DeferredRegister
            .create(BuiltInRegistries.MENU, MODID);

    // Database system for cross-server communication
    private DatabaseManager databaseManager;

    // Server transfer system
    private net.minecraft.server.MinecraftServer currentServer;
    private int tickCounter = 0;
    private final int CLEANUP_INTERVAL_TICKS = 1200; // 60 seconds at 20 ticks per second

    // Creates a new Block with the id "tharidiathings:pietro", combining the
    // namespace and path
    public static final DeferredBlock<PietroBlock> PIETRO = BLOCKS.register("pietro", () -> new PietroBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 6.0F).noOcclusion()));
    // Creates a new BlockItem with the id "tharidiathings:pietro", combining the
    // namespace and path. Uses custom PietroBlockItem for GeckoLib inventory rendering.
    public static final DeferredItem<PietroBlockItem> PIETRO_ITEM = ITEMS.register("pietro",
            () -> new PietroBlockItem(PIETRO.get(), new Item.Properties()));
    // Creates a new Block with the id "tharidiathings:claim"
    public static final DeferredBlock<ClaimBlock> CLAIM = BLOCKS.register("claim",
            () -> new ClaimBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 6.0F)));
    // Creates a new BlockItem with the id "tharidiathings:claim"
    public static final DeferredItem<BlockItem> CLAIM_ITEM = ITEMS.registerSimpleBlockItem("claim", CLAIM);
    // Hot Iron Marker Block (invisible, used for hot iron on anvil)
    public static final DeferredBlock<HotIronMarkerBlock> HOT_IRON_MARKER = BLOCKS.register("hot_iron_marker",
            () -> new HotIronMarkerBlock());
    // Hot Gold Marker Block (invisible, used for hot gold on anvil)
    public static final DeferredBlock<HotGoldMarkerBlock> HOT_GOLD_MARKER = BLOCKS.register("hot_gold_marker",
            () -> new HotGoldMarkerBlock());
    // Hot Copper Marker Block (invisible, used for hot copper on anvil)
    public static final DeferredBlock<HotCopperMarkerBlock> HOT_COPPER_MARKER = BLOCKS.register("hot_copper_marker",
            () -> new HotCopperMarkerBlock());

    // Creates a new BlockEntityType for the Pietro block
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PietroBlockEntity>> PIETRO_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("pietro", () -> BlockEntityType.Builder.of(PietroBlockEntity::new, PIETRO.get()).build(null));
    // Creates a new BlockEntityType for the Claim block
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClaimBlockEntity>> CLAIM_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("claim", () -> BlockEntityType.Builder.of(ClaimBlockEntity::new, CLAIM.get()).build(null));
    // Creates a new BlockEntityType for the Hot Iron on Anvil
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HotIronAnvilEntity>> HOT_IRON_ANVIL_ENTITY = BLOCK_ENTITIES
            .register("hot_iron_anvil",
                    () -> BlockEntityType.Builder.of(HotIronAnvilEntity::new, HOT_IRON_MARKER.get()).build(null));
    // Creates a new BlockEntityType for the Hot Gold on Anvil
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HotGoldAnvilEntity>> HOT_GOLD_ANVIL_ENTITY = BLOCK_ENTITIES
            .register("hot_gold_anvil",
                    () -> BlockEntityType.Builder.of(HotGoldAnvilEntity::new, HOT_GOLD_MARKER.get()).build(null));
    // Creates a new BlockEntityType for the Hot Copper on Anvil
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HotCopperAnvilEntity>> HOT_COPPER_ANVIL_ENTITY = BLOCK_ENTITIES
            .register("hot_copper_anvil",
                    () -> BlockEntityType.Builder.of(HotCopperAnvilEntity::new, HOT_COPPER_MARKER.get()).build(null));

    // Creates a MenuType for the Claim GUI
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<ClaimMenu>> CLAIM_MENU = MENU_TYPES
            .register("claim_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension
                    .create(ClaimMenu::new));

    // Creates a MenuType for the Pietro GUI
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<PietroMenu>> PIETRO_MENU = MENU_TYPES
            .register("pietro_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension
                    .create(PietroMenu::new));

    // Creates a MenuType for the Component Selection GUI
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<ComponentSelectionMenu>> COMPONENT_SELECTION_MENU = MENU_TYPES
            .register("component_selection_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension
                    .create(ComponentSelectionMenu::new));

    // Creates a MenuType for the Trade GUI
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<TradeMenu>> TRADE_MENU = MENU_TYPES
            .register("trade_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension
                    .create(TradeMenu::new));

    // Creates a MenuType for the Battle Invite GUI
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<BattleInviteMenu>> BATTLE_INVITE_MENU = MENU_TYPES
            .register("battle_invite_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension
                    .create(BattleInviteMenu::new));

    // Smithing items
    public static final DeferredItem<Item> HOT_IRON = ITEMS.register("hot_iron",
            () -> new HotIronItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> HOT_GOLD = ITEMS.register("hot_gold",
            () -> new HotGoldItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> HOT_COPPER = ITEMS.register("hot_copper",
            () -> new HotCopperItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> PINZA = ITEMS.register("pinza", () -> new PinzaItem(new Item.Properties()));
    public static final DeferredItem<Item> LAMA_LUNGA = ITEMS.register("lama_lunga",
            () -> new LamaLungaItem(new Item.Properties()));
    public static final DeferredItem<Item> LAMA_CORTA = ITEMS.register("lama_corta",
            () -> new LamaCortaItem(new Item.Properties()));
    public static final DeferredItem<Item> ELSA = ITEMS.register("elsa", () -> new ElsaItem(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_LAMA_LUNGA = ITEMS.register("gold_lama_lunga",
            () -> new GoldLamaLungaItem(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_LAMA_CORTA = ITEMS.register("gold_lama_corta",
            () -> new GoldLamaCortaItem(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_LAMA_LUNGA = ITEMS.register("copper_lama_lunga",
            () -> new CopperLamaLungaItem(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_LAMA_CORTA = ITEMS.register("copper_lama_corta",
            () -> new CopperLamaCortaItem(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_ELSA = ITEMS.register("copper_elsa",
            () -> new CopperElsaItem(new Item.Properties()));
    public static final DeferredItem<Item> DICE = ITEMS.register("dice",
            () -> new DiceItem(new Item.Properties().stacksTo(16)));

    // Battle Gauntlet
    public static final DeferredItem<Item> BATTLE_GAUNTLE = ITEMS.register("battle_gauntlet",
            () -> new BattleGauntlet(new Item.Properties().stacksTo(1)));

    // Creates a creative tab with the id "tharidiathings:tharidia_tab" for the mod
    // items, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THARIDIA_TAB = CREATIVE_MODE_TABS
            .register("tharidia_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tharidiathings")) // The language key for the title of your
                                                                               // CreativeModeTab
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
                        output.accept(DICE.get());
                        output.accept(BATTLE_GAUNTLE.get());
                    }).build());

    // The constructor for the mod class is the first code that is run when your mod
    // is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and
    // pass them in automatically.
    public TharidiaThings(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Register network packets
        modEventBus.addListener(this::registerPayloads);
        // Register client-side screen handlers (only on client)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::registerScreens);
        }

        // Register custom attributes
        ModAttributes.ATTRIBUTES.register(modEventBus);

        NeoForge.EVENT_BUS.register(new ItemAttributeHandler());

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entities get registered
        BLOCK_ENTITIES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so entities get registered
        ModEntities.ENTITIES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so menus get registered
        MENU_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so attachment types get
        // registered
        FatigueAttachments.ATTACHMENT_TYPES.register(modEventBus);
        DietAttachments.ATTACHMENT_TYPES.register(modEventBus);
        CharacterAttachments.ATTACHMENT_TYPES.register(modEventBus);
        StaminaAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // Register custom stats
        ModStats.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class
        // (TharidiaThings) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in
        // this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        // Register server stopping event
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        // Register the claim protection handler
        NeoForge.EVENT_BUS.register(ClaimProtectionHandler.class);
        // Register the player kill handler
        NeoForge.EVENT_BUS.register(PlayerStatsIncrementHandler.class);
        // Register the claim expiration handler
        NeoForge.EVENT_BUS.register(ClaimExpirationHandler.class);
        // Register the realm placement handler
        NeoForge.EVENT_BUS.register(RealmPlacementHandler.class);
        // Register the weight debuff handler
        NeoForge.EVENT_BUS.register(WeightDebuffHandler.class);
        // Register the smithing handler
        NeoForge.EVENT_BUS.register(SmithingHandler.class);
        // Register the freeze manager
        NeoForge.EVENT_BUS.register(FreezeManager.class);
        // Register the pre-login name handler
        NeoForge.EVENT_BUS.register(PreLoginNameHandler.class);
        // Register the fatigue handler
        NeoForge.EVENT_BUS.register(FatigueHandler.class);
        NeoForge.EVENT_BUS.register(StaminaHandler.class);
        NeoForge.EVENT_BUS.register(DietHandler.class);
        // Register the trade interaction handler
        NeoForge.EVENT_BUS.register(TradeInteractionHandler.class);
        // Register the trade inventory blocker
        NeoForge.EVENT_BUS.register(TradeInventoryBlocker.class);
        // Register the currency protection handler
        NeoForge.EVENT_BUS.register(CurrencyProtectionHandler.class);

        // Register Freeze Manager for master freeze command
        NeoForge.EVENT_BUS.register(FreezeManager.class);

        BattleGauntleAttachments.register(modEventBus);

        modEventBus.addListener(BattlePackets::register);

        // Register handshake bypass (CLIENT ONLY)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(HandshakeBypass.class);
            LOGGER.warn("Handshake bypass registered - you can connect to servers with different mod versions");

            // Check for video tools on Windows
            VideoToolsManager.getInstance().checkAndInstallTools();
        }

        // Log version for debugging
        LOGGER.info("=================================================");
        LOGGER.info("TharidiaThings v1.0.8 - NEW REST SYSTEM LOADED");
        LOGGER.info("Features: Rest Near Bed, No Force-Back, Time Skip Block");
        LOGGER.info("=================================================");

        // Register our mod's ModConfigSpec so that FML can create and load the config
        // file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        if (ModList.get().isLoaded("epicfight")) {
            StaminaHandler.registerEpicFightCompat();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Common setup
        LOGGER.info("HELLO FROM COMMON SETUP");

        event.enqueueWork(() -> {
            Stats.CUSTOM.get(ModStats.LAMA_CORTA_KILL.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(ModStats.LANCIA_KILL.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(ModStats.MARTELLI_KILL.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(ModStats.MAZZE_KILL.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(ModStats.SPADE_2_MANI_KILL.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(ModStats.ASCE_KILL.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(ModStats.SOCCHI_KILL.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(ModStats.ARCHI_KILL.get(), StatFormatter.DEFAULT);
            Stats.CUSTOM.get(ModStats.ARMI_DA_FUOCO_KILL.get(), StatFormatter.DEFAULT);
        });
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        LOGGER.info("Registering network payloads (dist: {})", FMLEnvironment.dist);

        // Network payload registration for mod features

        if (FMLEnvironment.dist.isClient()) {

            registrar.playToClient(
                    ClaimOwnerSyncPacket.TYPE,
                    ClaimOwnerSyncPacket.STREAM_CODEC,
                    ClientPacketHandler::handleClaimOwnerSync);
            registrar.playToClient(
                    RealmSyncPacket.TYPE,
                    RealmSyncPacket.STREAM_CODEC,
                    ClientPacketHandler::handleRealmSync);
            registrar.playToClient(
                    HierarchySyncPacket.TYPE,
                    HierarchySyncPacket.STREAM_CODEC,
                    ClientPacketHandler::handleHierarchySync);
            registrar.playToClient(
                    FatigueSyncPacket.TYPE,
                    FatigueSyncPacket.STREAM_CODEC,
                    ClientPacketHandler::handleFatigueSync);
            registrar.playToClient(
                    StaminaSyncPacket.TYPE,
                    StaminaSyncPacket.STREAM_CODEC,
                    ClientPacketHandler::handleStaminaSync);
            registrar.playToClient(
                    DietSyncPacket.TYPE,
                    DietSyncPacket.STREAM_CODEC,
                    ClientPacketHandler::handleDietSync);
            registrar.playToClient(
                    FatigueWarningPacket.TYPE,
                    FatigueWarningPacket.STREAM_CODEC,
                    ClientPacketHandler::handleFatigueWarning);
            // RPG Gates sync from tharidiatweaks
            registrar.playToClient(
                    SyncGateRestrictionsPacket.TYPE,
                    SyncGateRestrictionsPacket.STREAM_CODEC,
                    ClientPacketHandler::handleGateRestrictionsSync);
            // Name request packet
            registrar.playToClient(
                    RequestNamePacket.TYPE,
                    RequestNamePacket.STREAM_CODEC,
                    ClientPacketHandler::handleRequestName);
            // Race GUI packet
            registrar.playToClient(
                    OpenRaceGuiPacket.TYPE,
                    OpenRaceGuiPacket.STREAM_CODEC,
                    ClientPacketHandler::handleOpenRaceGui);
            // Race selection packet
            registrar.playToServer(
                    SelectRacePacket.TYPE,
                    SelectRacePacket.STREAM_CODEC,
                    SelectRacePacket::handle);
            // Zone music packet from tharidiatweaks
            registrar.playToClient(
                    ZoneMusicPacket.TYPE,
                    ZoneMusicPacket.STREAM_CODEC,
                    ClientPacketHandler::handleZoneMusic);
            // Music file data packet
            registrar.playToClient(
                    MusicFileDataPacket.TYPE,
                    MusicFileDataPacket.STREAM_CODEC,
                    ClientPacketHandler::handleMusicFileData);
            // Trade packets (client-bound)
            registrar.playToClient(
                    TradeRequestPacket.TYPE,
                    TradeRequestPacket.STREAM_CODEC,
                    (packet, context) -> TradeClientHandler
                            .handleTradeRequest(packet));
            registrar.playToClient(
                    TradeCompletePacket.TYPE,
                    TradeCompletePacket.STREAM_CODEC,
                    (packet, context) -> TradeClientHandler
                            .handleTradeComplete(packet));
            registrar.playToClient(
                    TradeSyncPacket.TYPE,
                    TradeSyncPacket.STREAM_CODEC,
                    (packet, context) -> TradeClientHandler
                            .handleTradeSync(packet));

            // Register bungeecord:main channel to satisfy server requirement (dummy
            // handler)
            registrar.playToClient(
                    BungeeCordPacket.TYPE,
                    BungeeCordPacket.STREAM_CODEC,
                    (packet, context) -> {
                    } // Dummy handler - we don't process bungeecord messages
            );

            // Video screen packets
            registrar.playToClient(
                    VideoScreenSyncPacket.TYPE,
                    VideoScreenSyncPacket.STREAM_CODEC,
                    ClientPacketHandler::handleVideoScreenSync);
            registrar.playToClient(
                    VideoScreenDeletePacket.TYPE,
                    VideoScreenDeletePacket.STREAM_CODEC,
                    ClientPacketHandler::handleVideoScreenDelete);
            registrar.playToClient(
                    VideoScreenSeekPacket.TYPE,
                    VideoScreenSeekPacket.STREAM_CODEC,
                    ClientSeekPacketHandler::handleSeekPacket);
            registrar.playToClient(
                    VideoScreenVolumePacket.TYPE,
                    VideoScreenVolumePacket.STREAM_CODEC,
                    VideoScreenVolumePacket::handle);

            // Diet profile sync packet
            registrar.playToClient(
                    DietProfileSyncPacket.TYPE,
                    DietProfileSyncPacket.STREAM_CODEC,
                    ClientPacketHandler::handleDietProfileSync);

            registrar.playToClient(
                    WeightConfigSyncPacket.TYPE,
                    WeightConfigSyncPacket.STREAM_CODEC,
                    ClientPacketHandler::handleWeightConfigSync);

            // Register dummy handlers for server-bound packets (client-side only for handshake)
            // Note: All server-bound packets are registered below with actual handlers
            // No dummy handlers needed here as they're registered with real handlers

            LOGGER.info("Client packet handlers registered");
        } else {

            // Server-side packet registration

            // On server, register dummy handlers (packets won't be received here anyway)
            registrar.playToClient(
                    ClaimOwnerSyncPacket.TYPE,
                    ClaimOwnerSyncPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            registrar.playToClient(
                    RealmSyncPacket.TYPE,
                    RealmSyncPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            registrar.playToClient(
                    HierarchySyncPacket.TYPE,
                    HierarchySyncPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            registrar.playToClient(
                    FatigueSyncPacket.TYPE,
                    FatigueSyncPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            registrar.playToClient(
                    StaminaSyncPacket.TYPE,
                    StaminaSyncPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            registrar.playToClient(
                    DietSyncPacket.TYPE,
                    DietSyncPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            registrar.playToClient(
                    FatigueWarningPacket.TYPE,
                    FatigueWarningPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            // RPG Gates sync from tharidiatweaks (dummy handler)
            registrar.playToClient(
                    SyncGateRestrictionsPacket.TYPE,
                    SyncGateRestrictionsPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            // Name request packet (dummy handler)
            registrar.playToClient(
                    RequestNamePacket.TYPE,
                    RequestNamePacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            // Race GUI packet (server-side dummy handler for handshake)
            registrar.playToClient(
                    OpenRaceGuiPacket.TYPE,
                    OpenRaceGuiPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            // Race selection packet (server-side handler)
            registrar.playToServer(
                    SelectRacePacket.TYPE,
                    SelectRacePacket.STREAM_CODEC,
                    SelectRacePacket::handle);
            // Zone music packet (dummy handler)
            registrar.playToClient(
                    ZoneMusicPacket.TYPE,
                    ZoneMusicPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            // Music file data packet (dummy handler)
            registrar.playToClient(
                    MusicFileDataPacket.TYPE,
                    MusicFileDataPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            // Trade packets (client-bound, dummy handlers)
            registrar.playToClient(
                    TradeRequestPacket.TYPE,
                    TradeRequestPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            registrar.playToClient(
                    TradeCompletePacket.TYPE,
                    TradeCompletePacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            registrar.playToClient(
                    TradeSyncPacket.TYPE,
                    TradeSyncPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });

            // Register bungeecord:main channel on server side (dummy handler for
            // consistency)
            registrar.playToClient(
                    BungeeCordPacket.TYPE,
                    BungeeCordPacket.STREAM_CODEC,
                    (packet, context) -> {
                    } // Dummy handler
            );

            // Video screen packets (dummy handlers for server)
            registrar.playToClient(
                    VideoScreenSyncPacket.TYPE,
                    VideoScreenSyncPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            registrar.playToClient(
                    VideoScreenDeletePacket.TYPE,
                    VideoScreenDeletePacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            registrar.playToClient(
                    VideoScreenSeekPacket.TYPE,
                    VideoScreenSeekPacket.STREAM_CODEC,
                    (packet, context) -> {
                    });
            registrar.playToClient(
                    VideoScreenVolumePacket.TYPE,
                    VideoScreenVolumePacket.STREAM_CODEC,
                    (packet, context) -> {
                    });

            // Ensure diet profile sync channel exists server-side so clients can connect
            registrar.playToClient(
                    DietProfileSyncPacket.TYPE,
                    DietProfileSyncPacket.STREAM_CODEC,
                    (packet, context) -> {});
            registrar.playToClient(
                    WeightConfigSyncPacket.TYPE,
                    WeightConfigSyncPacket.STREAM_CODEC,
                    (packet, context) -> {});
            LOGGER.info("Server-side packet registration completed (dummy handlers)");
        }

        // Register server-bound packets (works on both sides)
        registrar.playToServer(
                UpdateHierarchyPacket.TYPE,
                UpdateHierarchyPacket.STREAM_CODEC,
                UpdateHierarchyPacket::handle);
        registrar.playToServer(
                DungeonQueuePacket.TYPE,
                DungeonQueuePacket.STREAM_CODEC,
                DungeonQueuePacket::handle);
        registrar.playToServer(
                SelectComponentPacket.TYPE,
                SelectComponentPacket.STREAM_CODEC,
                SelectComponentPacket::handle);
        registrar.playToServer(
                SubmitNamePacket.TYPE,
                SubmitNamePacket.STREAM_CODEC,
                SubmitNamePacket::handle);
        registrar.playToServer(
                MeleeSwingPacket.TYPE,
                MeleeSwingPacket.STREAM_CODEC,
                MeleeSwingPacket::handle);
        // Trade packets (server-bound)
        registrar.playToServer(
                TradeResponsePacket.TYPE,
                TradeResponsePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> TradePacketHandler
                        .handleTradeResponse(packet, (ServerPlayer) context.player())));
        registrar.playToServer(
                TradeUpdatePacket.TYPE,
                TradeUpdatePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> TradePacketHandler
                        .handleTradeUpdate(packet, (ServerPlayer) context.player())));
        registrar.playToServer(
                TradeCancelPacket.TYPE,
                TradeCancelPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> TradePacketHandler
                        .handleTradeCancel(packet, (ServerPlayer) context.player())));
        registrar.playToServer(
                TradeFinalConfirmPacket.TYPE,
                TradeFinalConfirmPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> TradePacketHandler
                        .handleTradeFinalConfirm(packet, (ServerPlayer) context.player())));
        // Music file request packet (server-bound)
        registrar.playToServer(
                RequestMusicFilePacket.TYPE,
                RequestMusicFilePacket.STREAM_CODEC,
                (packet, context) -> context
                        .enqueueWork(() -> ServerMusicFileHandler
                                .handleMusicFileRequest(packet, (ServerPlayer) context.player())));
    }

    private void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(CLAIM_MENU.get(), ClaimScreen::new);
        event.register(PIETRO_MENU.get(), PietroScreen::new);
        event.register(COMPONENT_SELECTION_MENU.get(),
                ComponentSelectionScreen::new);
        event.register(TRADE_MENU.get(), TradeScreen::new);
        event.register(BATTLE_INVITE_MENU.get(), BattleInviteScreen::new);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // Send a full sync on login - this will clear and replace all client-side realm
            // data
            syncAllRealmsToPlayer((ServerPlayer) event.getEntity(), serverLevel);

            // Sync all video screens to the player
            syncAllVideoScreensToPlayer((ServerPlayer) event.getEntity(), serverLevel);

            PacketDistributor.sendToPlayer((ServerPlayer) event.getEntity(), WeightConfigSyncPacket.fromCurrentRegistry());
        }
    }

    @SubscribeEvent
    public void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        WeightConfigSyncPacket packet = WeightConfigSyncPacket.fromCurrentRegistry();
        if (event.getPlayer() != null) {
            PacketDistributor.sendToPlayer(event.getPlayer(), packet);
        } else {
            PacketDistributor.sendToAllPlayers(packet);
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
                    realm.getCenterChunk().z);
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
        VideoScreenRegistry registry = VideoScreenRegistry
                .get(serverLevel);

        java.util.Collection<VideoScreen> screens = registry
                .getScreensInDimension(dimension);

        for (VideoScreen screen : screens) {
            VideoScreenSyncPacket packet = new VideoScreenSyncPacket(
                    screen.getId(),
                    dimension,
                    screen.getCorner1(),
                    screen.getCorner2(),
                    screen.getFacing(),
                    screen.getVideoUrl(),
                    screen.getPlaybackState(),
                    screen.getVolume());
            PacketDistributor.sendToPlayer(player, packet);
        }

        LOGGER.info("Synced {} video screens to player {}", screens.size(), player.getName().getString());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Load claim registry from persistent storage
        net.minecraft.server.level.ServerLevel overworld = event.getServer()
                .getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld != null) {
            ClaimRegistry.loadFromPersistentStorage(overworld);
        } else {
            LOGGER.error("Could not load claim registry: overworld is null");
        }

        ItemCatalogueConfig.reload();
        ItemAttributeHandler.reload();
        PlayerStatsIncrementHandler.reload();

        // Register weight data loader
        event.getServer().getResourceManager();

        // Initialize database system
        initializeDatabaseSystem(event.getServer());

        // Initialize server transfer system
        initializeServerTransferSystem(event.getServer());
    }

    /**
     * Initializes the database system for cross-server communication
     */
    private void initializeDatabaseSystem(net.minecraft.server.MinecraftServer server) {
        try {
            LOGGER.info("Initializing database system...");

            // Create database manager
            databaseManager = new DatabaseManager(LOGGER);

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
     * Initializes the server transfer system
     */
    private void initializeServerTransferSystem(net.minecraft.server.MinecraftServer server) {
        try {
            LOGGER.info("Initializing server transfer system...");

            // Set database manager for transfer system
            ServerTransferManager.setDatabaseManager(databaseManager);
            TransferTokenManager.setDatabaseManager(databaseManager);
            DevWhitelistManager.setDatabaseManager(databaseManager);

            // Set server configuration from config file
            ServerTransferManager.setCurrentServerName(Config.SERVER_NAME.get());
            ServerTransferManager.setServerAddresses(
                    Config.MAIN_SERVER_IP.get(),
                    Config.DEV_SERVER_IP.get());

            LOGGER.info("Server transfer system initialized successfully");
            LOGGER.info("Current server: {}", Config.SERVER_NAME.get());
            LOGGER.info("Main server: {}", Config.MAIN_SERVER_IP.get());
            LOGGER.info("Dev server: {}", Config.DEV_SERVER_IP.get());

            // Initialize tick-based cleanup scheduler
            scheduleTokenCleanup(server);

        } catch (Exception e) {
            LOGGER.error("Failed to initialize server transfer system: {}", e.getMessage(), e);
        }
    }

    private void scheduleTokenCleanup(net.minecraft.server.MinecraftServer server) {
        // Store server reference for tick event
        currentServer = server;
        tickCounter = 0;
        LOGGER.info("Token cleanup scheduler initialized");
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // Only run cleanup on server thread and when server is available
        if (currentServer != null && currentServer.isSameThread()) {
            tickCounter++;

            if (tickCounter >= CLEANUP_INTERVAL_TICKS) {
                try {
                    TransferTokenManager.cleanupExpiredTokens();
                    LOGGER.debug("Token cleanup completed");
                } catch (Exception e) {
                    LOGGER.error("Error cleaning up expired tokens: {}", e.getMessage());
                }

                // Reset counter
                tickCounter = 0;
            }
        }
    }

    /**
     * Called when the server is stopping
     */
    public void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        LOGGER.info("Server stopping, cleaning up resources...");

        // Clear server reference
        currentServer = null;
        tickCounter = 0;

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
        event.addListener(new WeightDataLoader());
        event.addListener(new DietDataLoader());
        event.addListener(new CropProtectionConfig());
        event.addListener(new FatigueConfig());
        event.addListener(new StaminaConfig());
        event.addListener(new StaminaTagMappingsLoader());

        ItemCatalogueConfig.reload();
        ItemAttributeHandler.reload();
        PlayerStatsIncrementHandler.reload();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ClaimCommands.register(event.getDispatcher());
        ClaimAdminCommands.register(event.getDispatcher());
        DietCommands.register(event.getDispatcher());
        FatigueCommands.register(event.getDispatcher());
        CharacterCommands.register(event.getDispatcher());
        TradeCommands.register(event.getDispatcher());
        BattleCommands.register(event.getDispatcher());
        MarketCommands.register(event.getDispatcher());
        VideoScreenCommands.register(event.getDispatcher());
        ServerTransferCommands.register(event.getDispatcher());
        ItemCatalogueCommand.register(event.getDispatcher());
        StatsCommand.register(event.getDispatcher());
    }

    /**
     * Helper method to create a ResourceLocation for this mod
     */
    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
