package com.THproject.tharidia_things.gui.inventory;

/**
 * Shared layout constants for the medieval-styled player inventory panel rendered next to GUIs.
 */
public final class PlayerInventoryPanelLayout {
    public static final int PANEL_WIDTH = 170;
    public static final int PANEL_HEIGHT = 140;
    public static final int PANEL_GAP = 15;

    /** Horizontal offset (relative to GUI left) that positions the panel to the left of the parchment. */
    public static final int PANEL_OFFSET_X = -(PANEL_WIDTH + PANEL_GAP);
    /** Vertical offset (relative to GUI top) aligning the panel mid-height with the parchment. */
    public static final int PANEL_OFFSET_Y = 95;

    /** Starting X for the first inventory slot (relative to GUI left). */
    public static final int SLOT_OFFSET_X = PANEL_OFFSET_X + 5;
    /** Starting Y for the first inventory slot (relative to GUI top). */
    public static final int SLOT_OFFSET_Y = 118;

    private PlayerInventoryPanelLayout() {
        // Utility class
    }
}
