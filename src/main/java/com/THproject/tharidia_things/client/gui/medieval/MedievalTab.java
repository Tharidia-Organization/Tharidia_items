package com.THproject.tharidia_things.client.gui.medieval;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Medieval-styled tab with pointed bottom and ornate appearance
 * Features parchment textures, gold trim, and elegant typography
 */
public class MedievalTab extends Button {
    private final TabStyle style;
    private boolean isSelected;

    public enum TabStyle {
        // Standard parchment tab - most common
        PARCHMENT(MedievalGuiRenderer.PARCHMENT_LIGHT, MedievalGuiRenderer.PARCHMENT_DARK, MedievalGuiRenderer.GOLD_MAIN, MedievalGuiRenderer.BRONZE),
        // Royal gold tab for important sections
        ROYAL(MedievalGuiRenderer.GOLD_MAIN, MedievalGuiRenderer.LEATHER_DARK, MedievalGuiRenderer.GOLD_BRIGHT, MedievalGuiRenderer.GOLD_DARK),
        // Wood tab for rustic look
        WOOD(MedievalGuiRenderer.WOOD_LIGHT, MedievalGuiRenderer.WOOD_DARK, MedievalGuiRenderer.BRONZE_BRIGHT, MedievalGuiRenderer.BRONZE_DARK),
        // Leather tab for inventory/equipment sections
        LEATHER(MedievalGuiRenderer.LEATHER_LIGHT, MedievalGuiRenderer.LEATHER_DARK, MedievalGuiRenderer.COPPER, MedievalGuiRenderer.BRONZE_DARK),
        // Purple tab for magical sections
        PURPLE(MedievalGuiRenderer.PURPLE_REGAL, MedievalGuiRenderer.PURPLE_DARK, MedievalGuiRenderer.GOLD_MAIN, MedievalGuiRenderer.BURGUNDY);

        public final int activeColor;
        public final int inactiveColor;
        public final int activeAccent;
        public final int inactiveAccent;

        TabStyle(int activeColor, int inactiveColor, int activeAccent, int inactiveAccent) {
            this.activeColor = activeColor;
            this.inactiveColor = inactiveColor;
            this.activeAccent = activeAccent;
            this.inactiveAccent = inactiveAccent;
        }
    }

    public MedievalTab(int x, int y, int width, int height, Component message, OnPress onPress, TabStyle style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
        this.isSelected = false;
    }

    public static MedievalTab builder(Component message, OnPress onPress, TabStyle style) {
        return new MedievalTab(0, 0, 80, 25, message, onPress, style);
    }

    public MedievalTab bounds(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.setWidth(width);
        this.setHeight(height);
        return this;
    }

    public MedievalTab setActive(boolean active) {
        this.isSelected = active;
        return this;
    }

    public boolean isActive() {
        return isSelected;
    }

    public MedievalTab build() {
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused() && !isSelected;

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // Determine colors based on state
        int baseColor = isSelected ? style.activeColor : (hovered ? lighter(style.inactiveColor) : style.inactiveColor);
        int accentColor = isSelected ? style.activeAccent : style.inactiveAccent;
        int textColor = isSelected ? MedievalGuiRenderer.BROWN_INK : (hovered ? MedievalGuiRenderer.BROWN_INK : MedievalGuiRenderer.SEPIA);
        int borderLight = lighter(baseColor);
        int borderDark = darker(baseColor);

        // Calculate tab shape - body and pointed extension
        int bodyHeight = h - 6;
        int pointHeight = 6;

        // Shadow for inactive tabs (active tab connects to content)
        if (!isSelected) {
            gui.fill(x + 2, y + 2, x + w + 2, y + bodyHeight + 2, MedievalGuiRenderer.SHADOW_LIGHT);
        }

        // Main tab body
        gui.fill(x, y, x + w, y + bodyHeight, baseColor);

        // Pointed bottom extension (creates bookmark effect)
        for (int i = 0; i < pointHeight; i++) {
            int indent = (i * w / 2) / pointHeight;
            int leftX = x + indent;
            int rightX = x + w - indent;
            int rowY = y + bodyHeight + i;

            // Only draw if there's space
            if (leftX < rightX) {
                gui.fill(leftX, rowY, rightX, rowY + 1, baseColor);

                // Border for the point
                if (isSelected) {
                    gui.fill(leftX, rowY, leftX + 1, rowY + 1, accentColor);
                    gui.fill(rightX - 1, rowY, rightX, rowY + 1, darker(accentColor));
                }
            }
        }

        // Embossed border effect for active tab
        if (isSelected) {
            // Top edge (light)
            gui.fill(x, y, x + w, y + 1, borderLight);
            // Left edge (light)
            gui.fill(x, y, x + 1, y + bodyHeight, borderLight);
            // Right edge (dark)
            gui.fill(x + w - 1, y, x + w, y + bodyHeight, borderDark);
        }

        // Gold/bronze trim for active tab
        if (isSelected) {
            // Inner gold line
            gui.fill(x + 2, y + 2, x + w - 2, y + 3, accentColor);
            gui.fill(x + 2, y + 2, x + 3, y + bodyHeight - 2, accentColor);
            gui.fill(x + w - 3, y + 2, x + w - 2, y + bodyHeight - 2, darker(accentColor));

            // Decorative corner studs
            renderTabStud(gui, x + 4, y + 4, 2, accentColor);
            renderTabStud(gui, x + w - 6, y + 4, 2, accentColor);
        }

        // Subtle texture pattern for active tab (paper fibers)
        if (isSelected) {
            for (int py = y + 4; py < y + bodyHeight - 4; py += 3) {
                int lineAlpha = 10 + (hash(py * 7) % 10);
                int lineColor = (lineAlpha << 24) | (MedievalGuiRenderer.AGED_PARCHMENT & 0x00FFFFFF);
                int startX = x + 4 + (hash(py * 13) % 6);
                int endX = x + w - 4 - (hash(py * 17) % 6);
                if (startX < endX) {
                    gui.fill(startX, py, endX, py + 1, lineColor);
                }
            }
        }

        // Hover effect - subtle glow
        if (hovered && !isSelected) {
            gui.fill(x + 1, y + 1, x + w - 1, y + 2, MedievalGuiRenderer.HIGHLIGHT);
        }

        // Text (no shadow)
        Minecraft mc = Minecraft.getInstance();
        String text = getMessage().getString();
        int textWidth = mc.font.width(text);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (bodyHeight - mc.font.lineHeight) / 2;

        gui.drawString(mc.font, text, textX, textY, textColor, false);
    }

    /**
     * Renders a small decorative stud on the tab
     */
    private void renderTabStud(GuiGraphics gui, int x, int y, int radius, int color) {
        // Main circle
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    int px = x + dx;
                    int py = y + dy;
                    int pixelColor = (dy < 0 && dx < 0) ? lighter(color) : (dy > 0 && dx > 0) ? darker(color) : color;
                    gui.fill(px, py, px + 1, py + 1, pixelColor);
                }
            }
        }
    }

    /**
     * Returns a lighter version of the color
     */
    private static int lighter(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + 35);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 35);
        int b = Math.min(255, (color & 0xFF) + 35);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Returns a darker version of the color
     */
    private static int darker(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, ((color >> 16) & 0xFF) - 35);
        int g = Math.max(0, ((color >> 8) & 0xFF) - 35);
        int b = Math.max(0, (color & 0xFF) - 35);
        return (a << 24) | (r << 16) | (g << 8) | b;
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
