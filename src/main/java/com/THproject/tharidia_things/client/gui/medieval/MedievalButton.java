package com.THproject.tharidia_things.client.gui.medieval;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Medieval-styled button with ornate appearance and royal colors
 */
public class MedievalButton extends Button {
    private final ButtonStyle style;
    
    public enum ButtonStyle {
        ROYAL(MedievalGuiRenderer.ROYAL_GOLD, MedievalGuiRenderer.DEEP_CRIMSON),
        ROYAL_GOLD(MedievalGuiRenderer.ROYAL_GOLD, MedievalGuiRenderer.DEEP_CRIMSON),
        BRONZE(MedievalGuiRenderer.BRONZE, MedievalGuiRenderer.WOOD_DARK),
        WOOD(MedievalGuiRenderer.WOOD_DARK, MedievalGuiRenderer.BRONZE),
        PURPLE(MedievalGuiRenderer.PURPLE_REGAL, MedievalGuiRenderer.DARK_PURPLE);
        
        public final int bgColor;
        public final int borderColor;
        
        ButtonStyle(int bgColor, int borderColor) {
            this.bgColor = bgColor;
            this.borderColor = borderColor;
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
        boolean active = this.active && visible;
        
        MedievalGuiRenderer.renderMedievalButton(
            gui, 
            getX(), 
            getY(), 
            getWidth(), 
            getHeight(),
            getMessage().getString(),
            hovered,
            active
        );
    }
}
