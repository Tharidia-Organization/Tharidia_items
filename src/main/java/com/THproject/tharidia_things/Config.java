package com.THproject.tharidia_things;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Debug logging flags
    public static final ModConfigSpec.BooleanValue DEBUG_REALM_SYNC = BUILDER
            .comment("Enable detailed debug logging for realm synchronization (client/server)")
            .define("debugRealmSync", false);
    
    public static final ModConfigSpec.BooleanValue DEBUG_CLAIM_REGISTRY = BUILDER
            .comment("Enable detailed debug logging for claim registry operations")
            .define("debugClaimRegistry", false);
    
    public static final ModConfigSpec.BooleanValue DEBUG_BOUNDARY_RENDERING = BUILDER
            .comment("Enable detailed debug logging for boundary rendering")
            .define("debugBoundaryRendering", false);
    
    public static final ModConfigSpec.BooleanValue DEBUG_PROTECTION_CHECKS = BUILDER
            .comment("Enable detailed debug logging for protection/crop checks (WARNING: Very verbose!)")
            .define("debugProtectionChecks", false);

    // Smithing minigame settings
    public static final ModConfigSpec.DoubleValue SMITHING_MIN_CYCLE_TIME = BUILDER
            .comment("Minimum cycle time for the smithing minigame circle (in seconds)")
            .defineInRange("smithingMinCycleTime", 0.5, 0.1, 5.0);
    
    public static final ModConfigSpec.DoubleValue SMITHING_MAX_CYCLE_TIME = BUILDER
            .comment("Maximum cycle time for the smithing minigame circle (in seconds)")
            .defineInRange("smithingMaxCycleTime", 2.0, 0.5, 10.0);
    
    public static final ModConfigSpec.DoubleValue SMITHING_TOLERANCE = BUILDER
            .comment("Tolerance for hitting the target (0.0 = perfect only, 1.0 = very forgiving)")
            .defineInRange("smithingTolerance", 0.3, 0.0, 1.0);
    
    public static final ModConfigSpec.BooleanValue SMITHING_CAN_LOSE_PIECE = BUILDER
            .comment("Whether failed smithing attempts can destroy the piece")
            .define("smithingCanLosePiece", true);
    
    public static final ModConfigSpec.IntValue SMITHING_MAX_FAILURES = BUILDER
            .comment("Maximum number of failures before losing the piece (if enabled)")
            .defineInRange("smithingMaxFailures", 3, 1, 10);
    
    // Pinza (Tongs) settings
    public static final ModConfigSpec.IntValue PINZA_DURABILITY = BUILDER
            .comment("Maximum durability (uses) for the Pinza (Tongs)")
            .defineInRange("pinzaDurability", 480, 1, 10000);

    // Lobby server flag
    public static final ModConfigSpec.BooleanValue IS_LOBBY_SERVER = BUILDER
            .comment("If true, this server is treated as the Lobby: skip character name prompt here and only ask on main server")
            .define("isLobbyServer", false);

    // Database configuration for cross-server communication
    public static final ModConfigSpec.ConfigValue<String> DATABASE_HOST = BUILDER
            .comment("Database host address (e.g., 127.0.0.1 or localhost)")
            .define("databaseHost", "127.0.0.1");
    
    public static final ModConfigSpec.IntValue DATABASE_PORT = BUILDER
            .comment("Database port (default: 3306 for MySQL/MariaDB)")
            .defineInRange("databasePort", 3306, 1, 65535);
    
    public static final ModConfigSpec.ConfigValue<String> DATABASE_NAME = BUILDER
            .comment("Database name")
            .define("databaseName", "tharidia_queue");
    
    public static final ModConfigSpec.ConfigValue<String> DATABASE_USERNAME = BUILDER
            .comment("Database username")
            .define("databaseUsername", "tharidia_user");
    
    public static final ModConfigSpec.ConfigValue<String> DATABASE_PASSWORD = BUILDER
            .comment("Database password")
            .define("databasePassword", "changeme");
    
    public static final ModConfigSpec.BooleanValue DATABASE_ENABLED = BUILDER
            .comment("Enable database connection for cross-server communication")
            .define("databaseEnabled", false);

    // Market system configuration
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> TRADE_CURRENCY_ITEMS = BUILDER
            .comment("List of items that can be used as currency to initiate trades (e.g., minecraft:potato, minecraft:gold_nugget)")
            .defineList("tradeCurrencyItems", 
                java.util.Arrays.asList("minecraft:potato"), 
                obj -> obj instanceof String);
    
    public static final ModConfigSpec.DoubleValue TRADE_TAX_RATE = BUILDER
            .comment("Tax rate applied to currency items in trades (0.1 = 10% tax)")
            .defineInRange("tradeTaxRate", 0.1, 0.0, 1.0);

    // Server transfer configuration
    public static final ModConfigSpec.ConfigValue<String> SERVER_NAME = BUILDER
            .comment("Name of this server (main, dev, etc.)")
            .define("serverName", "main");
    
    public static final ModConfigSpec.ConfigValue<String> MAIN_SERVER_IP = BUILDER
            .comment("Main server IP:port for transfers")
            .define("mainServerIp", "172.18.0.10:25772");
    
    public static final ModConfigSpec.ConfigValue<String> DEV_SERVER_IP = BUILDER
            .comment("Development server IP:port for transfers")
            .define("devServerIp", "172.18.0.10:25566");
    
    public static final ModConfigSpec.BooleanValue DEV_DIRECT_ACCESS_ENABLED = BUILDER
            .comment("If true, players in the dev whitelist can connect directly to the dev server without using transfer tokens")
            .define("devDirectAccessEnabled", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
