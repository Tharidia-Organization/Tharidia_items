package com.THproject.tharidia_things.client.gui.medieval;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Medieval GUI renderer for creating rich, gorgeous feudal system interfaces
 * Features parchment backgrounds, ornate borders, royal colors, and decorative elements
 */
public class MedievalGuiRenderer {
    
    // Medieval color palette - royal and authentic
    public static final int DEEP_CRIMSON = 0xFF8B0000;
    public static final int ROYAL_GOLD = 0xFFDAA520;
    public static final int PURPLE_REGAL = 0xFF4B0082;
    public static final int DARK_PURPLE = 0xFF301934;
    public static final int BRONZE = 0xFFCD7F32;
    public static final int AGED_PARCHMENT = 0xFFB8956F;  // Much darker aged paper
    public static final int DARK_PARCHMENT = 0xFFA08060;  // Very dark background
    public static final int BROWN_INK = 0xFF3E2723;
    public static final int BLACK_INK = 0xFF1A1A1A;
    public static final int GOLD_LEAF = 0xFFFFD700;
    public static final int SILVER = 0xFFC0C0C0;
    public static final int WOOD_DARK = 0xFF5D4037;
    public static final int WOOD_LIGHT = 0xFF8D6E63;
    public static final int STONE_GRAY = 0xFF616161;
    public static final int SHADOW = 0x80000000;
    
    /**
     * Renders a realistic parchment background with texture and aging effects
     */
    public static void renderParchmentBackground(GuiGraphics gui, int x, int y, int width, int height) {
        // Base parchment color
        gui.fill(x, y, x + width, y + height, AGED_PARCHMENT);
        
        // Add realistic texture effects
        renderParchmentTexture(gui, x, y, width, height);
        
        // Add edge darkening for aged effect
        renderEdgeDarkening(gui, x, y, width, height);
    }
    
    /**
     * Renders realistic parchment texture with fibers and stains
     */
    private static void renderParchmentTexture(GuiGraphics gui, int x, int y, int width, int height) {
        // Add horizontal fiber lines (paper texture)
        for (int i = 0; i < height; i += 2) {
            if ((hash(x + i) % 5) == 0) {
                int fiberY = y + i;
                int alpha = 10 + (hash(x + i * 2) % 20);
                gui.fill(x, fiberY, x + width, fiberY + 1, 
                        (alpha << 24) | (DARK_PARCHMENT & 0x00FFFFFF));
            }
        }
        
        // Add subtle vertical fibers
        for (int i = 0; i < width; i += 3) {
            if ((hash(y + i) % 7) == 0) {
                int fiberX = x + i;
                int alpha = 5 + (hash(y + i * 3) % 15);
                gui.fill(fiberX, y, fiberX + 1, y + height, 
                        (alpha << 24) | (DARK_PARCHMENT & 0x00FFFFFF));
            }
        }
        
        // Add age spots and stains
        for (int i = 0; i < 15; i++) {
            int spotX = x + (hash(i * 37) % width);
            int spotY = y + (hash(i * 73) % height);
            int spotSize = 3 + (hash(i * 13) % 8);
            int alpha = 15 + (hash(i * 23) % 25);
            
            // Create circular stain effect
            for (int dx = -spotSize; dx <= spotSize; dx++) {
                for (int dy = -spotSize; dy <= spotSize; dy++) {
                    if (dx * dx + dy * dy <= spotSize * spotSize) {
                        int stainAlpha = alpha - (Math.abs(dx) + Math.abs(dy)) * 2;
                        if (stainAlpha > 0 && spotX + dx >= x && spotX + dx < x + width &&
                            spotY + dy >= y && spotY + dy < y + height) {
                            gui.fill(spotX + dx, spotY + dy, spotX + dx + 1, spotY + dy + 1,
                                    (stainAlpha << 24) | (BROWN_INK & 0x00FFFFFF));
                        }
                    }
                }
            }
        }
        
        // Add burn marks on edges
        renderBurnMarks(gui, x, y, width, height);
    }
    
    /**
     * Renders burn marks on parchment edges
     */
    private static void renderBurnMarks(GuiGraphics gui, int x, int y, int width, int height) {
        // Top edge burn marks
        for (int i = 0; i < 5; i++) {
            int burnX = x + (hash(i * 47) % width);
            int burnWidth = 2 + (hash(i * 29) % 4);
            int alpha = 30 + (hash(i * 17) % 40);
            gui.fill(burnX, y, burnX + burnWidth, y + 3, 
                    (alpha << 24) | (BLACK_INK & 0x00FFFFFF));
        }
        
        // Bottom edge burn marks
        for (int i = 0; i < 5; i++) {
            int burnX = x + (hash(i * 59) % width);
            int burnWidth = 2 + (hash(i * 31) % 4);
            int alpha = 30 + (hash(i * 19) % 40);
            gui.fill(burnX, y + height - 3, burnX + burnWidth, y + height, 
                    (alpha << 24) | (BLACK_INK & 0x00FFFFFF));
        }
    }
    
    /**
     * Simple hash function for deterministic random values based on coordinates
     */
    private static int hash(int seed) {
        seed = ((seed >> 16) ^ seed) * 0x45d9f3b;
        seed = ((seed >> 16) ^ seed) * 0x45d9f3b;
        seed = (seed >> 16) ^ seed;
        return Math.abs(seed);
    }
    
    /**
     * Adds darkened edges to simulate aged parchment
     */
    private static void renderEdgeDarkening(GuiGraphics gui, int x, int y, int width, int height) {
        int edgeWidth = 8;
        
        // Top edge
        for (int i = 0; i < edgeWidth; i++) {
            int alpha = 60 - (i * 7);
            gui.fill(x, y + i, x + width, y + i + 1, 
                    (alpha << 24) | (BROWN_INK & 0x00FFFFFF));
        }
        
        // Bottom edge
        for (int i = 0; i < edgeWidth; i++) {
            int alpha = 60 - (i * 7);
            gui.fill(x, y + height - i - 1, x + width, y + height - i, 
                    (alpha << 24) | (BROWN_INK & 0x00FFFFFF));
        }
        
        // Left edge
        for (int i = 0; i < edgeWidth; i++) {
            int alpha = 60 - (i * 7);
            gui.fill(x + i, y, x + i + 1, y + height, 
                    (alpha << 24) | (BROWN_INK & 0x00FFFFFF));
        }
        
        // Right edge
        for (int i = 0; i < edgeWidth; i++) {
            int alpha = 60 - (i * 7);
            gui.fill(x + width - i - 1, y, x + width - i, y + height, 
                    (alpha << 24) | (BROWN_INK & 0x00FFFFFF));
        }
    }
    
    /**
     * Renders an ornate medieval border with corner flourishes
     */
    public static void renderOrnateBorder(GuiGraphics gui, int x, int y, int width, int height, int color) {
        // Main border
        gui.renderOutline(x, y, width, height, color);
        
        // Inner border for depth
        gui.renderOutline(x + 2, y + 2, width - 4, height - 4, 
                ((color & 0x00FFFFFF) | 0x40000000));
        
        // Corner flourishes
        renderCornerFlourish(gui, x, y, 0, color); // Top-left
        renderCornerFlourish(gui, x + width - 20, y, 1, color); // Top-right
        renderCornerFlourish(gui, x, y + height - 20, 2, color); // Bottom-left
        renderCornerFlourish(gui, x + width - 20, y + height - 20, 3, color); // Bottom-right
    }
    
    /**
     * Renders decorative corner flourish
     * corner: 0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right
     */
    private static void renderCornerFlourish(GuiGraphics gui, int x, int y, int corner, int color) {
        // Simple corner decoration with clean lines
        // Outer border
        gui.fill(x, y, x + 20, y + 3, color);
        gui.fill(x, y, x + 3, y + 20, color);
        gui.fill(x + 17, y, x + 20, y + 20, color);
        gui.fill(x, y + 17, x + 20, y + 20, color);
        
        // Inner accent
        gui.fill(x + 3, y + 3, x + 17, y + 4, GOLD_LEAF);
        gui.fill(x + 3, y + 3, x + 4, y + 17, GOLD_LEAF);
        gui.fill(x + 16, y + 3, x + 17, y + 17, GOLD_LEAF);
        gui.fill(x + 3, y + 16, x + 17, y + 17, GOLD_LEAF);
    }
    
    /**
     * Renders a medieval-style divider line with decorative pattern
     */
    public static void renderMedievalDivider(GuiGraphics gui, int x, int y, int width) {
        // Main line
        gui.fill(x, y, x + width, y + 2, BROWN_INK);
        
        // Decorative dots
        for (int i = 10; i < width - 10; i += 20) {
            gui.fill(x + i, y - 2, x + i + 4, y + 4, ROYAL_GOLD);
            gui.fill(x + i + 1, y - 1, x + i + 3, y + 3, DEEP_CRIMSON);
        }
    }
    
    /**
     * Renders a medieval-style title with decorative underline (no shadow)
     */
    public static void renderMedievalTitle(GuiGraphics gui, String title, int x, int y, int width) {
        Minecraft mc = Minecraft.getInstance();
        
        // Calculate centered position
        int titleWidth = mc.font.width(title);
        int titleX = x + (width - titleWidth) / 2;
        
        // Main title in royal colors (no shadow)
        gui.drawString(mc.font, "§6§l" + title, titleX, y, ROYAL_GOLD);
        
        // Decorative underline
        int underlineY = y + mc.font.lineHeight + 3;
        renderMedievalDivider(gui, titleX - 10, underlineY, titleWidth + 20);
    }
    
    /**
     * Renders a medieval-style button with ornate styling
     */
    public static void renderMedievalButton(GuiGraphics gui, int x, int y, int width, int height, 
                                           String text, boolean hovered, boolean active) {
        // Choose colors based on state
        int bgColor = active ? ROYAL_GOLD : (hovered ? BRONZE : WOOD_DARK);
        int textColor = active ? BLACK_INK : AGED_PARCHMENT;
        int borderColor = active ? DEEP_CRIMSON : BRONZE;
        
        // Button background with gradient effect
        gui.fill(x, y, x + width, y + height, bgColor);
        
        // Highlight on top if hovered
        if (hovered) {
            gui.fill(x, y, x + width, y + height / 3, 
                    ((bgColor & 0x00FFFFFF) | 0x40000000));
        }
        
        // Ornate border
        renderOrnateBorder(gui, x, y, width, height, borderColor);
        
        // Button text
        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - mc.font.lineHeight) / 2;
        
        gui.drawString(mc.font, text, textX, textY, textColor);
    }
    
    /**
     * Renders a medieval-style progress bar with ornate frame
     */
    public static void renderMedievalProgressBar(GuiGraphics gui, int x, int y, int width, int height, 
                                                float progress, int fillColor) {
        // Frame background
        gui.fill(x, y, x + width, y + height, STONE_GRAY);
        
        // Inner background
        gui.fill(x + 2, y + 2, x + width - 2, y + height - 2, BLACK_INK);
        
        // Progress fill with gradient effect
        int fillWidth = (int)((width - 4) * Math.min(1.0f, Math.max(0.0f, progress)));
        if (fillWidth > 0) {
            gui.fill(x + 2, y + 2, x + 2 + fillWidth, y + height - 2, fillColor);
            
            // Highlight on top of progress
            gui.fill(x + 2, y + 2, x + 2 + fillWidth, y + 2 + height / 3, 
                    ((fillColor & 0x00FFFFFF) | 0x40000000));
        }
        
        // Ornate border
        renderOrnateBorder(gui, x, y, width, height, BRONZE);
    }
    
    /**
     * Renders a medieval-style tab
     */
    public static void renderMedievalTab(GuiGraphics gui, int x, int y, int width, int height, 
                                       String text, boolean active, boolean hovered) {
        int bgColor = active ? ROYAL_GOLD : (hovered ? WOOD_LIGHT : WOOD_DARK);
        int textColor = active ? BLACK_INK : AGED_PARCHMENT;
        
        // Tab background with pointed bottom
        gui.fill(x, y, x + width, y + height - 5, bgColor);
        
        // Pointed bottom
        for (int i = 0; i < 5; i++) {
            int pointWidth = width - (i * 2);
            int pointX = x + i;
            gui.fill(pointX, y + height - 5 + i, pointX + pointWidth, y + height - 4 + i, bgColor);
        }
        
        // Border
        gui.renderOutline(x, y, width, height - 5, BRONZE);
        
        // Tab text
        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 5 - mc.font.lineHeight) / 2;
        
        gui.drawString(mc.font, text, textX, textY, textColor);
    }
    
    /**
     * Renders a scroll indicator for lists
     */
    public static void renderScrollIndicator(GuiGraphics gui, int x, int y, boolean up) {
        String symbol = up ? "§6▲" : "§6▼";
        Minecraft mc = Minecraft.getInstance();
        
        // Background circle
        gui.fill(x - 1, y - 1, x + 9, y + 9, BRONZE);
        gui.fill(x, y, x + 8, y + 8, WOOD_DARK);
        
        // Arrow symbol
        gui.drawString(mc.font, symbol, x, y, ROYAL_GOLD);
    }
    
    /**
     * Renders a medieval-style list item with alternating background
     */
    public static void renderListItem(GuiGraphics gui, int x, int y, int width, int height, 
                                    String text, boolean even, boolean selected) {
        int bgColor = selected ? ROYAL_GOLD : (even ? AGED_PARCHMENT : DARK_PARCHMENT);
        int textColor = selected ? BLACK_INK : BROWN_INK;
        
        // Background
        gui.fill(x, y, x + width, y + height, bgColor);
        
        // Text
        Minecraft mc = Minecraft.getInstance();
        gui.drawString(mc.font, text, x + 5, y + (height - mc.font.lineHeight) / 2, textColor);
        
        // Selection indicator
        if (selected) {
            gui.renderOutline(x, y, width, height, DEEP_CRIMSON);
        }
    }
}
