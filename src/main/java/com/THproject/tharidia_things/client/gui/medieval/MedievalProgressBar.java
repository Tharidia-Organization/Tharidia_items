package com.THproject.tharidia_things.client.gui.medieval;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Medieval-styled progress bar with ornate frame and royal colors
 */
public class MedievalProgressBar {
    private int x, y, width, height;
    private float progress;
    private int fillColor;
    private boolean showText;
    private String text;
    
    public MedievalProgressBar(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.progress = 0f;
        this.fillColor = MedievalGuiRenderer.ROYAL_GOLD;
        this.showText = false;
        this.text = "";
    }
    
    public MedievalProgressBar setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        return this;
    }
    
    public MedievalProgressBar setFillColor(int color) {
        this.fillColor = color;
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
        // Render the progress bar with medieval styling
        MedievalGuiRenderer.renderMedievalProgressBar(gui, x, y, width, height, progress, fillColor);
        
        // Render text if enabled
        if (showText && !text.isEmpty()) {
            int textWidth = Minecraft.getInstance().font.width(text);
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height - Minecraft.getInstance().font.lineHeight) / 2;
            
            // Shadow effect
            gui.drawString(Minecraft.getInstance().font, text, textX + 1, textY + 1, MedievalGuiRenderer.SHADOW);
            // Main text
            gui.drawString(Minecraft.getInstance().font, "§6" + text, textX, textY, MedievalGuiRenderer.GOLD_LEAF);
        }
    }
    
    public float getProgress() {
        return progress;
    }
    
    public int getFillColor() {
        return fillColor;
    }
}
