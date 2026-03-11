package com.THproject.tharidia_things.block.alchemist;

/**
 * Phases of the Alchemist Table crafting sequence.
 */
public enum AlchemistCraftingPhase {
    /** No active crafting. Table accepts input and player interaction. */
    IDLE,

    /** Main crafting timer running. */
    PROCESSING,

    /** Crafting succeeded; output animation plays, then resets to IDLE. */
    FINISHING
}
