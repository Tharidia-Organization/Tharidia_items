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

    static final ModConfigSpec SPEC = BUILDER.build();
}
