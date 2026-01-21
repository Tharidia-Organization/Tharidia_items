package com.THproject.tharidia_things.client.gui.medieval;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Medieval-styled button with ornate appearance and royal colors
 * Features embossed effects, gold trim, and elegant typography
 */
public class MedievalButton extends Button {
    private final ButtonStyle style;

    public enum ButtonStyle {
        // Primary styles - most commonly used
        ROYAL(MedievalGuiRenderer.GOLD_MAIN, MedievalGuiRenderer.DEEP_CRIMSON, MedievalGuiRenderer.GOLD_BRIGHT, MedievalGuiRenderer.GOLD_DARK),
        WOOD(MedievalGuiRenderer.WOOD_MEDIUM, MedievalGuiRenderer.BRONZE, MedievalGuiRenderer.WOOD_LIGHT, MedievalGuiRenderer.WOOD_DARK),
        BRONZE(MedievalGuiRenderer.BRONZE, MedievalGuiRenderer.BRONZE_DARK, MedievalGuiRenderer.BRONZE_BRIGHT, MedievalGuiRenderer.LEATHER_DARK),
        LEATHER(MedievalGuiRenderer.LEATHER_MAIN, MedievalGuiRenderer.LEATHER_DARK, MedievalGuiRenderer.LEATHER_LIGHT, MedievalGuiRenderer.WOOD_DARK),
        PURPLE(MedievalGuiRenderer.PURPLE_REGAL, MedievalGuiRenderer.PURPLE_DARK, MedievalGuiRenderer.CRIMSON_BRIGHT, MedievalGuiRenderer.BLACK_INK),
        // Action styles
        SUCCESS(MedievalGuiRenderer.BRONZE, MedievalGuiRenderer.WOOD_DARK, MedievalGuiRenderer.BRONZE_BRIGHT, MedievalGuiRenderer.BRONZE_DARK),
        DANGER(MedievalGuiRenderer.DEEP_CRIMSON, MedievalGuiRenderer.BURGUNDY, MedievalGuiRenderer.CRIMSON_BRIGHT, MedievalGuiRenderer.BLACK_INK),
        PARCHMENT(MedievalGuiRenderer.PARCHMENT_BASE, MedievalGuiRenderer.BROWN_INK, MedievalGuiRenderer.PARCHMENT_LIGHT, MedievalGuiRenderer.PARCHMENT_DARK);

        public final int baseColor;
        public final int borderColor;
        public final int highlightColor;
        public final int shadowColor;

        ButtonStyle(int baseColor, int borderColor, int highlightColor, int shadowColor) {
            this.baseColor = baseColor;
            this.borderColor = borderColor;
            this.highlightColor = highlightColor;
            this.shadowColor = shadowColor;
        }
    }

    public MedievalButton(int x, int y, int width, int height, Component message, OnPress onPress, ButtonStyle style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
    }

    public static MedievalButton builder(Component message, OnPress onPress, ButtonStyle style) {
        return new MedievalButton(0, 0, 200, 20, message, onPress, style);
    }

    public MedievalButton bounds(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.setWidth(width);
        this.setHeight(height);
        return this;
    }

    public MedievalButton build() {
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();
        boolean isActive = this.active && visible;

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // Determine colors based on state
        int baseColor, topColor, bottomColor, textColor, accentColor;

        if (!isActive) {
            // Disabled state - greyed out
            baseColor = MedievalGuiRenderer.IRON_DARK;
            topColor = MedievalGuiRenderer.SILVER;
            bottomColor = MedievalGuiRenderer.BLACK_INK;
            textColor = MedievalGuiRenderer.SILVER;
            accentColor = MedievalGuiRenderer.IRON_DARK;
        } else if (hovered) {
            // Hovered - use style's highlight colors
            baseColor = style.highlightColor;
            topColor = lighter(style.highlightColor);
            bottomColor = style.baseColor;
            textColor = getContrastText(style.highlightColor);
            accentColor = MedievalGuiRenderer.GOLD_BRIGHT;
        } else {
            // Normal state - use style colors
            baseColor = style.baseColor;
            topColor = style.highlightColor;
            bottomColor = style.shadowColor;
            textColor = getContrastText(style.baseColor);
            accentColor = MedievalGuiRenderer.GOLD_MAIN;
        }

        // Drop shadow (deeper when hovered for lift effect)
        int shadowOffset = hovered ? 3 : 2;
        gui.fill(x + shadowOffset, y + shadowOffset, x + w + shadowOffset, y + h + shadowOffset, MedievalGuiRenderer.SHADOW_MEDIUM);

        // Main button body
        gui.fill(x, y, x + w, y + h, baseColor);

        // Top bevel (light - raised effect)
        gui.fill(x, y, x + w, y + 2, topColor);
        gui.fill(x, y, x + 2, y + h, topColor);

        // Bottom bevel (dark - recessed effect)
        gui.fill(x, y + h - 2, x + w, y + h, bottomColor);
        gui.fill(x + w - 2, y, x + w, y + h, bottomColor);

        // Inner border with gold/bronze trim
        gui.renderOutline(x + 3, y + 3, w - 6, h - 6, style.borderColor);

        // Corner accents - small metallic studs
        int studSize = 3;
        renderButtonStud(gui, x + 2, y + 2, studSize, accentColor);
        renderButtonStud(gui, x + w - 2 - studSize, y + 2, studSize, accentColor);
        renderButtonStud(gui, x + 2, y + h - 2 - studSize, studSize, accentColor);
        renderButtonStud(gui, x + w - 2 - studSize, y + h - 2 - studSize, studSize, accentColor);

        // Pressed effect (subtle inner shadow when hovered)
        if (hovered && isActive) {
            gui.fill(x + 4, y + 4, x + w - 4, y + 6, MedievalGuiRenderer.SHADOW_LIGHT);
            gui.fill(x + 4, y + 4, x + 6, y + h - 4, MedievalGuiRenderer.SHADOW_LIGHT);
        }

        // Text (no shadow)
        Minecraft mc = Minecraft.getInstance();
        String text = getMessage().getString();
        int textWidth = mc.font.width(text);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (h - mc.font.lineHeight) / 2;

        gui.drawString(mc.font, text, textX, textY, textColor, false);
    }

    /**
     * Renders a small decorative stud at button corners
     */
    private void renderButtonStud(GuiGraphics gui, int x, int y, int size, int color) {
        // Main body
        gui.fill(x, y, x + size, y + size, color);
        // Highlight (top-left)
        gui.fill(x, y, x + 1, y + size - 1, lighter(color));
        gui.fill(x, y, x + size - 1, y + 1, lighter(color));
        // Shadow (bottom-right)
        gui.fill(x + size - 1, y + 1, x + size, y + size, darker(color));
        gui.fill(x + 1, y + size - 1, x + size, y + size, darker(color));
    }

    /**
     * Returns a lighter version of the color
     */
    private static int lighter(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + 45);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 45);
        int b = Math.min(255, (color & 0xFF) + 45);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Returns a darker version of the color
     */
    private static int darker(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, ((color >> 16) & 0xFF) - 45);
        int g = Math.max(0, ((color >> 8) & 0xFF) - 45);
        int b = Math.max(0, (color & 0xFF) - 45);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Returns appropriate text color (light or dark) for good contrast
     */
    private static int getContrastText(int bgColor) {
        int r = (bgColor >> 16) & 0xFF;
        int g = (bgColor >> 8) & 0xFF;
        int b = bgColor & 0xFF;
        // Calculate luminance
        double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
        return luminance > 0.5 ? MedievalGuiRenderer.BLACK_INK : MedievalGuiRenderer.PARCHMENT_LIGHT;
    }
}
