package com.THproject.tharidia_things.gui.inventory;

/**
 * Shared layout constants for the medieval-styled player inventory panel rendered next to GUIs.
 * Panel fits: 9 slots x 18px = 162px width, 3 rows + hotbar (58 gap + 18) = 76px slot area
 */
public final class PlayerInventoryPanelLayout {
    public static final int PANEL_WIDTH = 176;   // 162 slots + 14 padding (7 each side)
    public static final int PANEL_HEIGHT = 96;   // 76 slot area + 20 padding (10 top, 10 bottom)
    public static final int PANEL_GAP = 10;

    /** Horizontal offset (relative to GUI left) that positions the panel to the left of the parchment. */
    public static final int PANEL_OFFSET_X = -(PANEL_WIDTH + PANEL_GAP);
    /** Vertical offset (relative to GUI top) aligning the panel mid-height with the parchment. */
    public static final int PANEL_OFFSET_Y = 144;

    /** Starting X for the first inventory slot (relative to GUI left). */
    public static final int SLOT_OFFSET_X = PANEL_OFFSET_X + 7;
    /** Starting Y for the first inventory slot (relative to GUI top). */
    public static final int SLOT_OFFSET_Y = PANEL_OFFSET_Y + 10;

    private PlayerInventoryPanelLayout() {
        // Utility class
    }
}
