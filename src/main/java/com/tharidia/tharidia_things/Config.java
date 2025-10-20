package com.tharidia.tharidia_things;

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

    static final ModConfigSpec SPEC = BUILDER.build();
}
