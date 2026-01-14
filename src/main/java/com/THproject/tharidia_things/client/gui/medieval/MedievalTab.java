package com.THproject.tharidia_things.client.gui.medieval;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Medieval-styled tab with pointed bottom and ornate appearance
 */
public class MedievalTab extends Button {
    private final TabStyle style;
    private boolean active;
    
    public enum TabStyle {
        ROYAL(MedievalGuiRenderer.ROYAL_GOLD, MedievalGuiRenderer.DEEP_CRIMSON),
        WOOD(MedievalGuiRenderer.WOOD_DARK, MedievalGuiRenderer.BRONZE),
        PURPLE(MedievalGuiRenderer.PURPLE_REGAL, MedievalGuiRenderer.DARK_PURPLE),
        BRONZE(MedievalGuiRenderer.BRONZE, MedievalGuiRenderer.WOOD_DARK);
        
        public final int activeColor;
        public final int inactiveColor;
        
        TabStyle(int activeColor, int inactiveColor) {
            this.activeColor = activeColor;
            this.inactiveColor = inactiveColor;
        }
    }
    
    public MedievalTab(int x, int y, int width, int height, Component message, OnPress onPress, TabStyle style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
        this.active = false;
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
        this.active = active;
        return this;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public MedievalTab build() {
        return this;
    }
    
    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused() && !active;
        
        MedievalGuiRenderer.renderMedievalTab(
            gui,
            getX(),
            getY(),
            getWidth(),
            getHeight(),
            getMessage().getString(),
            active,
            hovered
        );
    }
}
