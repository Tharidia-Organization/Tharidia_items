package com.THproject.tharidia_things.client.gui.medieval;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Medieval-styled progress bar with ornate frame and royal colors
 * Features embossed metal frame, shine effects, and elegant corner studs
 */
public class MedievalProgressBar {
    private int x, y, width, height;
    private float progress;
    private BarStyle style;
    private boolean showText;
    private String text;

    public enum BarStyle {
        // Gold bar - for XP, currency, etc.
        GOLD(MedievalGuiRenderer.GOLD_MAIN, MedievalGuiRenderer.GOLD_BRIGHT, MedievalGuiRenderer.GOLD_DARK),
        // Crimson bar - for health, danger
        CRIMSON(MedievalGuiRenderer.DEEP_CRIMSON, MedievalGuiRenderer.CRIMSON_BRIGHT, MedievalGuiRenderer.BURGUNDY),
        // Bronze bar - for stamina, standard progress
        BRONZE(MedievalGuiRenderer.BRONZE, MedievalGuiRenderer.BRONZE_BRIGHT, MedievalGuiRenderer.BRONZE_DARK),
        // Purple bar - for magic, mana
        PURPLE(MedievalGuiRenderer.PURPLE_REGAL, MedievalGuiRenderer.CRIMSON_BRIGHT, MedievalGuiRenderer.PURPLE_DARK),
        // Copper bar - for crafting, skills
        COPPER(MedievalGuiRenderer.COPPER, MedievalGuiRenderer.BRONZE_BRIGHT, MedievalGuiRenderer.LEATHER_DARK);

        public final int mainColor;
        public final int highlightColor;
        public final int shadowColor;

        BarStyle(int mainColor, int highlightColor, int shadowColor) {
            this.mainColor = mainColor;
            this.highlightColor = highlightColor;
            this.shadowColor = shadowColor;
        }
    }

    public MedievalProgressBar(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.progress = 0f;
        this.style = BarStyle.GOLD;
        this.showText = false;
        this.text = "";
    }

    public MedievalProgressBar setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        return this;
    }

    public MedievalProgressBar setStyle(BarStyle style) {
        this.style = style;
        return this;
    }

    public MedievalProgressBar setFillColor(int color) {
        // For backwards compatibility - creates a custom style based on color
        this.style = BarStyle.GOLD; // Default, the render will use this color directly
        return this;
    }

    public MedievalProgressBar showProgressText(String prefix, String suffix) {
        this.showText = true;
        this.text = prefix + " " + Math.round(progress * 100) + "% " + suffix;
        return this;
    }

    public MedievalProgressBar showValueText(int current, int max, String label) {
        this.showText = true;
        this.text = label + ": " + current + "/" + max;
        return this;
    }

    public MedievalProgressBar setText(String text) {
        this.showText = true;
        this.text = text;
        return this;
    }

    public MedievalProgressBar hideText() {
        this.showText = false;
        return this;
    }

    public MedievalProgressBar position(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public MedievalProgressBar size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public void render(GuiGraphics gui) {
        // Outer frame shadow
        gui.fill(x + 2, y + 2, x + width + 2, y + height + 2, MedievalGuiRenderer.SHADOW_MEDIUM);

        // Outer frame (iron/stone look)
        gui.fill(x, y, x + width, y + height, MedievalGuiRenderer.IRON_DARK);

        // Inner bevel - raised effect on top-left, recessed on bottom-right
        gui.fill(x + 1, y + 1, x + width - 1, y + 2, lighter(MedievalGuiRenderer.IRON_DARK));
        gui.fill(x + 1, y + 1, x + 2, y + height - 1, lighter(MedievalGuiRenderer.IRON_DARK));
        gui.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, darker(MedievalGuiRenderer.IRON_DARK));
        gui.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, darker(MedievalGuiRenderer.IRON_DARK));

        // Inner background (dark recess for depth)
        gui.fill(x + 3, y + 3, x + width - 3, y + height - 3, MedievalGuiRenderer.BLACK_INK);

        // Progress fill
        float clampedProgress = Math.max(0, Math.min(1, progress));
        int fillWidth = (int)((width - 6) * clampedProgress);

        if (fillWidth > 0) {
            int fillX = x + 3;
            int fillY = y + 3;
            int fillH = height - 6;

            // Main fill with style color
            gui.fill(fillX, fillY, fillX + fillWidth, fillY + fillH, style.mainColor);

            // Top shine effect (gradient)
            int shineHeight = Math.max(1, fillH / 4);
            gui.fill(fillX, fillY, fillX + fillWidth, fillY + shineHeight, style.highlightColor);

            // Middle shine line
            if (fillH > 4) {
                gui.fill(fillX, fillY + shineHeight, fillX + fillWidth, fillY + shineHeight + 1,
                        (0x40 << 24) | 0xFFFFFF);
            }

            // Bottom shadow edge
            gui.fill(fillX, fillY + fillH - shineHeight, fillX + fillWidth, fillY + fillH, style.shadowColor);

            // Leading edge highlight (progress front)
            if (fillWidth > 2) {
                gui.fill(fillX + fillWidth - 1, fillY + 1, fillX + fillWidth, fillY + fillH - 1, style.highlightColor);
            }
        }

        // Corner studs (bronze rivets)
        int studRadius = Math.min(2, Math.min(width, height) / 8);
        if (studRadius >= 1) {
            renderStud(gui, x + 1, y + 1, studRadius, MedievalGuiRenderer.BRONZE);
            renderStud(gui, x + width - 2 - studRadius, y + 1, studRadius, MedievalGuiRenderer.BRONZE);
            renderStud(gui, x + 1, y + height - 2 - studRadius, studRadius, MedievalGuiRenderer.BRONZE);
            renderStud(gui, x + width - 2 - studRadius, y + height - 2 - studRadius, studRadius, MedievalGuiRenderer.BRONZE);
        }

        // Render text if enabled (no shadow)
        if (showText && !text.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            int textWidth = mc.font.width(text);
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height - mc.font.lineHeight) / 2;

            gui.drawString(mc.font, text, textX, textY, MedievalGuiRenderer.GOLD_LEAF, false);
        }
    }

    /**
     * Renders a small metallic stud/rivet
     */
    private void renderStud(GuiGraphics gui, int x, int y, int radius, int color) {
        // Shadow
        gui.fill(x + 1, y + 1, x + radius * 2 + 1, y + radius * 2 + 1, MedievalGuiRenderer.SHADOW_LIGHT);
        // Main body
        gui.fill(x, y, x + radius * 2, y + radius * 2, color);
        // Highlight
        gui.fill(x, y, x + radius, y + radius, lighter(color));
        // Dark edge
        gui.fill(x + radius, y + radius, x + radius * 2, y + radius * 2, darker(color));
    }

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

    public float getProgress() {
        return progress;
    }

    public BarStyle getStyle() {
        return style;
    }
}
