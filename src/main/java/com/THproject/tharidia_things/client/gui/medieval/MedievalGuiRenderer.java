package com.THproject.tharidia_things.client.gui.medieval;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

/**
 * Advanced Medieval GUI Renderer - Creates stunning, realistic medieval interfaces
 * Features: realistic parchment, embossed borders, metallic effects, wax seals, leather textures
 */
public class MedievalGuiRenderer {

    // ==================== REFINED MEDIEVAL COLOR PALETTE ====================

    // Parchment & Paper
    public static final int PARCHMENT_LIGHT = 0xFFE8D4B8;      // Light aged paper
    public static final int PARCHMENT_BASE = 0xFFD4BC94;       // Main parchment
    public static final int PARCHMENT_DARK = 0xFFC4A882;       // Darker parchment
    public static final int AGED_PARCHMENT = 0xFFB8956F;       // Heavily aged
    public static final int DARK_PARCHMENT = 0xFFA08060;       // Very dark parchment

    // Metals
    public static final int GOLD_BRIGHT = 0xFFFFD700;          // Bright gold highlight
    public static final int GOLD_MAIN = 0xFFDAA520;            // Main gold
    public static final int GOLD_DARK = 0xFFB8860B;            // Dark gold/shadow
    public static final int GOLD_LEAF = 0xFFF4C430;            // Gold leaf effect
    public static final int BRONZE_BRIGHT = 0xFFE6A855;        // Bronze highlight
    public static final int BRONZE = 0xFFCD7F32;               // Main bronze
    public static final int BRONZE_DARK = 0xFF8B5A2B;          // Dark bronze
    public static final int COPPER = 0xFFB87333;               // Copper accent
    public static final int SILVER = 0xFFC0C0C0;               // Silver
    public static final int IRON_DARK = 0xFF4A4A4A;            // Dark iron

    // Royal Colors
    public static final int DEEP_CRIMSON = 0xFF8B0000;         // Royal red
    public static final int CRIMSON_BRIGHT = 0xFFDC143C;       // Bright crimson
    public static final int ROYAL_GOLD = 0xFFDAA520;           // Royal gold
    public static final int PURPLE_REGAL = 0xFF4B0082;         // Indigo purple
    public static final int PURPLE_DARK = 0xFF2E0854;          // Dark purple
    public static final int BURGUNDY = 0xFF800020;             // Deep burgundy

    // Wood & Leather
    public static final int WOOD_DARK = 0xFF3E2723;            // Dark oak
    public static final int WOOD_MEDIUM = 0xFF5D4037;          // Medium wood
    public static final int WOOD_LIGHT = 0xFF8D6E63;           // Light wood
    public static final int LEATHER_DARK = 0xFF4A3728;         // Dark leather
    public static final int LEATHER_MAIN = 0xFF6B4423;         // Main leather
    public static final int LEATHER_LIGHT = 0xFF8B6914;        // Light leather

    // Inks & Text
    public static final int BLACK_INK = 0xFF1A1A1A;            // Near black
    public static final int BROWN_INK = 0xFF3E2723;            // Brown ink
    public static final int SEPIA = 0xFF704214;                // Sepia tone
    public static final int RED_INK = 0xFF8B0000;              // Red ink

    // Effects
    public static final int SHADOW_DARK = 0xCC000000;          // Strong shadow
    public static final int SHADOW_MEDIUM = 0x80000000;        // Medium shadow
    public static final int SHADOW_LIGHT = 0x40000000;         // Light shadow
    public static final int HIGHLIGHT = 0x40FFFFFF;            // White highlight
    public static final int GLOW_GOLD = 0x60FFD700;            // Gold glow

    // Wax Seal Colors
    public static final int WAX_RED = 0xFFB22222;              // Red wax
    public static final int WAX_RED_DARK = 0xFF8B0000;         // Dark red wax
    public static final int WAX_RED_LIGHT = 0xFFCD5C5C;        // Light red wax

    // ==================== MAIN BACKGROUND RENDERING ====================

    /**
     * Renders a parchment background with medieval styling (optimized for performance)
     */
    public static void renderParchmentBackground(GuiGraphics gui, int x, int y, int width, int height) {
        // Layer 1: Drop shadow (simple rectangles)
        gui.fill(x + 4, y + 4, x + width + 4, y + height + 4, SHADOW_MEDIUM);
        gui.fill(x + 2, y + 2, x + width + 2, y + height + 2, SHADOW_LIGHT);

        // Layer 2: Main parchment base
        gui.fill(x, y, x + width, y + height, PARCHMENT_BASE);

        // Layer 3: Center highlight (simple rectangles, not pixel-by-pixel)
        int highlightMargin = 40;
        gui.fill(x + highlightMargin, y + highlightMargin,
                x + width - highlightMargin, y + height - highlightMargin,
                (10 << 24) | 0xFFFFFF);

        // Layer 4: Simplified edge darkening (just a few gradient lines)
        renderSimpleEdgeWear(gui, x, y, width, height);

        // Layer 5: A few decorative fiber lines (very limited)
        renderSimpleFibers(gui, x, y, width, height);
    }

    /**
     * Renders simple edge darkening with minimal draw calls
     */
    private static void renderSimpleEdgeWear(GuiGraphics gui, int x, int y, int width, int height) {
        // Top edge gradient (3 lines)
        gui.fill(x, y, x + width, y + 3, (40 << 24) | (BROWN_INK & 0x00FFFFFF));
        gui.fill(x, y + 3, x + width, y + 6, (25 << 24) | (BROWN_INK & 0x00FFFFFF));
        gui.fill(x, y + 6, x + width, y + 10, (12 << 24) | (BROWN_INK & 0x00FFFFFF));

        // Bottom edge gradient
        gui.fill(x, y + height - 3, x + width, y + height, (40 << 24) | (BROWN_INK & 0x00FFFFFF));
        gui.fill(x, y + height - 6, x + width, y + height - 3, (25 << 24) | (BROWN_INK & 0x00FFFFFF));
        gui.fill(x, y + height - 10, x + width, y + height - 6, (12 << 24) | (BROWN_INK & 0x00FFFFFF));

        // Left edge gradient
        gui.fill(x, y, x + 3, y + height, (40 << 24) | (BROWN_INK & 0x00FFFFFF));
        gui.fill(x + 3, y, x + 6, y + height, (25 << 24) | (BROWN_INK & 0x00FFFFFF));
        gui.fill(x + 6, y, x + 10, y + height, (12 << 24) | (BROWN_INK & 0x00FFFFFF));

        // Right edge gradient
        gui.fill(x + width - 3, y, x + width, y + height, (40 << 24) | (BROWN_INK & 0x00FFFFFF));
        gui.fill(x + width - 6, y, x + width - 3, y + height, (25 << 24) | (BROWN_INK & 0x00FFFFFF));
        gui.fill(x + width - 10, y, x + width - 6, y + height, (12 << 24) | (BROWN_INK & 0x00FFFFFF));

        // Corner darkening (simple rectangles)
        int cs = 15;
        int cornerAlpha = 20;
        gui.fill(x, y, x + cs, y + cs, (cornerAlpha << 24) | (BROWN_INK & 0x00FFFFFF));
        gui.fill(x + width - cs, y, x + width, y + cs, (cornerAlpha << 24) | (BROWN_INK & 0x00FFFFFF));
        gui.fill(x, y + height - cs, x + cs, y + height, (cornerAlpha << 24) | (BROWN_INK & 0x00FFFFFF));
        gui.fill(x + width - cs, y + height - cs, x + width, y + height, (cornerAlpha << 24) | (BROWN_INK & 0x00FFFFFF));
    }

    /**
     * Renders simplified paper fiber texture (very few lines)
     */
    private static void renderSimpleFibers(GuiGraphics gui, int x, int y, int width, int height) {
        // Just a few horizontal fiber lines for texture
        int fiberAlpha = 10;
        int fiberColor = (fiberAlpha << 24) | (PARCHMENT_DARK & 0x00FFFFFF);

        // Fixed positions for deterministic rendering
        gui.fill(x + 20, y + 50, x + width - 30, y + 51, fiberColor);
        gui.fill(x + 15, y + 120, x + width - 25, y + 121, fiberColor);
        gui.fill(x + 25, y + 190, x + width - 35, y + 191, fiberColor);
        gui.fill(x + 18, y + 260, x + width - 28, y + 261, fiberColor);
    }

    // ==================== BORDER RENDERING ====================

    /**
     * Renders an ornate embossed border with metallic effect
     */
    public static void renderOrnateBorder(GuiGraphics gui, int x, int y, int width, int height, int primaryColor) {
        // Outer shadow line
        gui.fill(x + 2, y + height, x + width + 2, y + height + 2, SHADOW_MEDIUM);
        gui.fill(x + width, y + 2, x + width + 2, y + height + 2, SHADOW_MEDIUM);

        // Main border frame (embossed effect)
        int borderWidth = 4;

        // Outer dark edge (shadow)
        gui.fill(x, y, x + width, y + borderWidth, darker(primaryColor));
        gui.fill(x, y, x + borderWidth, y + height, darker(primaryColor));

        // Inner light edge (highlight)
        gui.fill(x, y + height - borderWidth, x + width, y + height, primaryColor);
        gui.fill(x + width - borderWidth, y, x + width, y + height, primaryColor);

        // Gold inlay line
        gui.fill(x + borderWidth, y + borderWidth, x + width - borderWidth, y + borderWidth + 1, GOLD_MAIN);
        gui.fill(x + borderWidth, y + borderWidth, x + borderWidth + 1, y + height - borderWidth, GOLD_MAIN);
        gui.fill(x + borderWidth, y + height - borderWidth - 1, x + width - borderWidth, y + height - borderWidth, GOLD_DARK);
        gui.fill(x + width - borderWidth - 1, y + borderWidth, x + width - borderWidth, y + height - borderWidth, GOLD_DARK);

        // Decorative corners
        renderDecorativeCorner(gui, x, y, primaryColor, 0);
        renderDecorativeCorner(gui, x + width - 24, y, primaryColor, 1);
        renderDecorativeCorner(gui, x, y + height - 24, primaryColor, 2);
        renderDecorativeCorner(gui, x + width - 24, y + height - 24, primaryColor, 3);
    }

    /**
     * Renders a decorative corner piece
     */
    private static void renderDecorativeCorner(GuiGraphics gui, int x, int y, int color, int corner) {
        int size = 24;

        // Main corner plate
        gui.fill(x, y, x + size, y + size, color);

        // Inner design - diamond pattern
        int centerX = x + size / 2;
        int centerY = y + size / 2;

        // Gold diamond
        for (int i = 0; i < 6; i++) {
            gui.fill(centerX - 6 + i, centerY - i, centerX + 6 - i, centerY - i + 1, GOLD_MAIN);
            gui.fill(centerX - 6 + i, centerY + i, centerX + 6 - i, centerY + i + 1, GOLD_DARK);
        }

        // Corner studs
        renderMetalStud(gui, x + 3, y + 3, 3, GOLD_MAIN);
        renderMetalStud(gui, x + size - 6, y + 3, 3, GOLD_MAIN);
        renderMetalStud(gui, x + 3, y + size - 6, 3, GOLD_MAIN);
        renderMetalStud(gui, x + size - 6, y + size - 6, 3, GOLD_MAIN);

        // Embossed border
        gui.fill(x, y, x + size, y + 1, lighter(color));
        gui.fill(x, y, x + 1, y + size, lighter(color));
        gui.fill(x, y + size - 1, x + size, y + size, darker(color));
        gui.fill(x + size - 1, y, x + size, y + size, darker(color));
    }

    /**
     * Renders a metallic stud/rivet
     */
    private static void renderMetalStud(GuiGraphics gui, int x, int y, int radius, int color) {
        // Shadow
        renderSpot(gui, x + 1, y + 1, radius, SHADOW_LIGHT);

        // Main body
        renderSpot(gui, x, y, radius, color);

        // Highlight
        gui.fill(x - 1, y - 1, x, y, lighter(color));
    }

    // ==================== BUTTON RENDERING ====================

    /**
     * Renders a beautifully crafted medieval button
     */
    public static void renderMedievalButton(GuiGraphics gui, int x, int y, int width, int height,
                                           String text, boolean hovered, boolean active) {
        // Determine colors based on state
        int baseColor, topColor, bottomColor, textColor;

        if (active) {
            baseColor = GOLD_MAIN;
            topColor = GOLD_BRIGHT;
            bottomColor = GOLD_DARK;
            textColor = BLACK_INK;
        } else if (hovered) {
            baseColor = LEATHER_MAIN;
            topColor = LEATHER_LIGHT;
            bottomColor = LEATHER_DARK;
            textColor = PARCHMENT_LIGHT;
        } else {
            baseColor = WOOD_MEDIUM;
            topColor = WOOD_LIGHT;
            bottomColor = WOOD_DARK;
            textColor = PARCHMENT_BASE;
        }

        // Drop shadow
        gui.fill(x + 2, y + 2, x + width + 2, y + height + 2, SHADOW_MEDIUM);

        // Main button body
        gui.fill(x, y, x + width, y + height, baseColor);

        // Top bevel (light)
        gui.fill(x, y, x + width, y + 2, topColor);
        gui.fill(x, y, x + 2, y + height, topColor);

        // Bottom bevel (dark)
        gui.fill(x, y + height - 2, x + width, y + height, bottomColor);
        gui.fill(x + width - 2, y, x + width, y + height, bottomColor);

        // Inner border
        gui.renderOutline(x + 3, y + 3, width - 6, height - 6, GOLD_DARK);

        // Corner accents
        gui.fill(x + 2, y + 2, x + 5, y + 5, GOLD_MAIN);
        gui.fill(x + width - 5, y + 2, x + width - 2, y + 5, GOLD_MAIN);
        gui.fill(x + 2, y + height - 5, x + 5, y + height - 2, GOLD_MAIN);
        gui.fill(x + width - 5, y + height - 5, x + width - 2, y + height - 2, GOLD_MAIN);

        // Pressed effect
        if (active) {
            gui.fill(x + 3, y + 3, x + width - 3, y + height - 3, SHADOW_LIGHT);
        }

        // Text (no shadow)
        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - mc.font.lineHeight) / 2;

        gui.drawString(mc.font, text, textX, textY, textColor, false);
    }

    // ==================== TAB RENDERING ====================

    /**
     * Renders a medieval tab with elegant styling
     */
    public static void renderMedievalTab(GuiGraphics gui, int x, int y, int width, int height,
                                        String text, boolean active, boolean hovered) {
        int baseColor = active ? PARCHMENT_LIGHT : (hovered ? PARCHMENT_BASE : PARCHMENT_DARK);
        int borderColor = active ? GOLD_MAIN : BRONZE;
        int textColor = active ? BROWN_INK : (hovered ? BROWN_INK : SEPIA);

        // Tab body with pointed bottom
        int bodyHeight = height - 6;
        gui.fill(x, y, x + width, y + bodyHeight, baseColor);

        // Pointed bottom extension
        for (int i = 0; i < 6; i++) {
            int indent = i;
            gui.fill(x + indent, y + bodyHeight + i, x + width - indent, y + bodyHeight + i + 1, baseColor);
        }

        // Border with emboss
        if (active) {
            gui.fill(x, y, x + width, y + 1, lighter(baseColor));
            gui.fill(x, y, x + 1, y + bodyHeight, lighter(baseColor));
            gui.fill(x + width - 1, y, x + width, y + bodyHeight, darker(baseColor));
        }

        // Gold trim for active
        if (active) {
            gui.fill(x + 2, y + 2, x + width - 2, y + 3, GOLD_MAIN);
            gui.fill(x + 2, y + 2, x + 3, y + bodyHeight - 2, GOLD_MAIN);
            gui.fill(x + width - 3, y + 2, x + width - 2, y + bodyHeight - 2, GOLD_DARK);
        }

        // Decorative top corners
        if (active) {
            renderMetalStud(gui, x + 4, y + 4, 2, GOLD_MAIN);
            renderMetalStud(gui, x + width - 6, y + 4, 2, GOLD_MAIN);
        }

        // Text
        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (bodyHeight - mc.font.lineHeight) / 2;

        gui.drawString(mc.font, text, textX, textY, textColor, false);
    }

    // ==================== PROGRESS BAR RENDERING ====================

    /**
     * Renders a beautiful medieval progress bar
     */
    public static void renderMedievalProgressBar(GuiGraphics gui, int x, int y, int width, int height,
                                                float progress, int fillColor) {
        // Outer frame (stone/metal look)
        gui.fill(x, y, x + width, y + height, IRON_DARK);

        // Inner bevel
        gui.fill(x + 1, y + 1, x + width - 1, y + 2, lighter(IRON_DARK));
        gui.fill(x + 1, y + 1, x + 2, y + height - 1, lighter(IRON_DARK));
        gui.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, darker(IRON_DARK));
        gui.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, darker(IRON_DARK));

        // Inner background (dark recess)
        gui.fill(x + 3, y + 3, x + width - 3, y + height - 3, BLACK_INK);

        // Progress fill
        progress = Math.max(0, Math.min(1, progress));
        int fillWidth = (int)((width - 6) * progress);

        if (fillWidth > 0) {
            // Main fill
            gui.fill(x + 3, y + 3, x + 3 + fillWidth, y + height - 3, fillColor);

            // Shine effect on top
            gui.fill(x + 3, y + 3, x + 3 + fillWidth, y + 5, lighter(fillColor));

            // Dark bottom edge
            gui.fill(x + 3, y + height - 5, x + 3 + fillWidth, y + height - 3, darker(fillColor));
        }

        // Gold corner studs
        renderMetalStud(gui, x + 1, y + 1, 2, BRONZE);
        renderMetalStud(gui, x + width - 3, y + 1, 2, BRONZE);
        renderMetalStud(gui, x + 1, y + height - 3, 2, BRONZE);
        renderMetalStud(gui, x + width - 3, y + height - 3, 2, BRONZE);
    }

    // ==================== DIVIDER & TITLE RENDERING ====================

    /**
     * Renders an elegant medieval divider
     */
    public static void renderMedievalDivider(GuiGraphics gui, int x, int y, int width) {
        // Main line with gradient
        gui.fill(x, y, x + width, y + 1, BROWN_INK);
        gui.fill(x, y + 1, x + width, y + 2, darker(BROWN_INK));

        // Center ornament
        int centerX = x + width / 2;
        renderDiamondOrnament(gui, centerX - 8, y - 4, 16, 10, GOLD_MAIN);

        // End ornaments
        renderMetalStud(gui, x, y - 1, 3, BRONZE);
        renderMetalStud(gui, x + width - 3, y - 1, 3, BRONZE);
    }

    /**
     * Renders a diamond-shaped ornament
     */
    private static void renderDiamondOrnament(GuiGraphics gui, int x, int y, int width, int height, int color) {
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        int halfW = width / 2;
        int halfH = height / 2;

        for (int dy = -halfH; dy <= halfH; dy++) {
            int rowWidth = (int)(halfW * (1 - Math.abs(dy) / (double)halfH));
            if (rowWidth > 0) {
                int py = centerY + dy;
                int rowColor = dy < 0 ? lighter(color) : darker(color);
                gui.fill(centerX - rowWidth, py, centerX + rowWidth, py + 1, rowColor);
            }
        }
        // Center gem
        gui.fill(centerX - 2, centerY - 1, centerX + 2, centerY + 2, CRIMSON_BRIGHT);
    }

    /**
     * Renders an elegant medieval title
     */
    public static void renderMedievalTitle(GuiGraphics gui, String title, int x, int y, int width) {
        Minecraft mc = Minecraft.getInstance();
        int titleWidth = mc.font.width(title);
        int titleX = x + (width - titleWidth) / 2;

        // Main title in gold (no shadow)
        gui.drawString(mc.font, "§6§l" + title, titleX, y, GOLD_MAIN, false);

        // Underline ornament
        int underlineY = y + mc.font.lineHeight + 4;
        renderMedievalDivider(gui, titleX - 15, underlineY, titleWidth + 30);
    }

    // ==================== SCROLL & LIST RENDERING ====================

    /**
     * Renders a scroll indicator arrow
     */
    public static void renderScrollIndicator(GuiGraphics gui, int x, int y, boolean up) {
        // Background circle
        renderSpot(gui, x + 5, y + 5, 6, LEATHER_DARK);
        renderSpot(gui, x + 5, y + 5, 5, LEATHER_MAIN);

        // Arrow
        Minecraft mc = Minecraft.getInstance();
        String arrow = up ? "\u25B2" : "\u25BC";
        gui.drawString(mc.font, arrow, x + 1, y + 1, GOLD_MAIN, false);
    }

    /**
     * Renders a medieval list item
     */
    public static void renderListItem(GuiGraphics gui, int x, int y, int width, int height,
                                     String text, boolean even, boolean selected) {
        int bgColor = selected ? GOLD_MAIN : (even ? PARCHMENT_BASE : PARCHMENT_DARK);
        int textColor = selected ? BLACK_INK : BROWN_INK;

        // Background with subtle gradient
        gui.fill(x, y, x + width, y + height, bgColor);

        if (!selected) {
            // Top highlight
            gui.fill(x, y, x + width, y + 1, (0x20 << 24) | 0xFFFFFF);
        }

        // Selection border
        if (selected) {
            gui.renderOutline(x, y, width, height, DEEP_CRIMSON);
            gui.renderOutline(x + 1, y + 1, width - 2, height - 2, GOLD_DARK);
        }

        // Left accent line
        gui.fill(x, y + 2, x + 2, y + height - 2, selected ? DEEP_CRIMSON : BRONZE);

        // Text
        Minecraft mc = Minecraft.getInstance();
        gui.drawString(mc.font, text, x + 8, y + (height - mc.font.lineHeight) / 2, textColor, false);
    }

    // ==================== WAX SEAL RENDERING ====================

    /**
     * Renders a decorative wax seal
     */
    public static void renderWaxSeal(GuiGraphics gui, int x, int y, int size) {
        // Wax body with irregular edge
        for (int angle = 0; angle < 360; angle += 10) {
            double rad = Math.toRadians(angle);
            int wobble = hash(angle) % 3;
            int edgeRadius = size / 2 + wobble;
            int ex = x + size / 2 + (int)(Math.cos(rad) * edgeRadius);
            int ey = y + size / 2 + (int)(Math.sin(rad) * edgeRadius);
            renderSpot(gui, ex, ey, 4, WAX_RED);
        }

        // Main seal body
        renderSpot(gui, x + size / 2, y + size / 2, size / 2 - 2, WAX_RED);

        // Highlight
        renderSpot(gui, x + size / 3, y + size / 3, size / 6, WAX_RED_LIGHT);

        // Shadow
        renderSpot(gui, x + size * 2 / 3, y + size * 2 / 3, size / 6, WAX_RED_DARK);

        // Embossed symbol (crown)
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        gui.fill(centerX - 4, centerY - 2, centerX + 4, centerY + 3, WAX_RED_DARK);
        gui.fill(centerX - 3, centerY - 4, centerX - 1, centerY - 1, WAX_RED_DARK);
        gui.fill(centerX - 1, centerY - 3, centerX + 1, centerY - 1, WAX_RED_DARK);
        gui.fill(centerX + 1, centerY - 4, centerX + 3, centerY - 1, WAX_RED_DARK);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Returns a lighter version of the color
     */
    private static int lighter(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 40);
        int b = Math.min(255, (color & 0xFF) + 40);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Returns a darker version of the color
     */
    private static int darker(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, ((color >> 16) & 0xFF) - 40);
        int g = Math.max(0, ((color >> 8) & 0xFF) - 40);
        int b = Math.max(0, (color & 0xFF) - 40);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Renders a simple spot/circle using a square approximation (efficient)
     */
    private static void renderSpot(GuiGraphics gui, int x, int y, int size, int color) {
        // Use a simple square instead of pixel-by-pixel circle for performance
        gui.fill(x - size, y - size, x + size, y + size, color);
    }

    /**
     * Simple hash function for deterministic randomness
     */
    private static int hash(int seed) {
        seed = ((seed >> 16) ^ seed) * 0x45d9f3b;
        seed = ((seed >> 16) ^ seed) * 0x45d9f3b;
        seed = (seed >> 16) ^ seed;
        return Math.abs(seed);
    }
}
