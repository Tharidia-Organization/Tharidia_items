package com.THproject.tharidia_things.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A button that renders a PNG texture image.
 * Used for the medieval GUI tab buttons.
 * Supports a separate pressed/active texture.
 */
public class ImageTabButton extends Button {

    private final ResourceLocation texture;
    private final ResourceLocation pressedTexture;
    private final int texWidth;
    private final int texHeight;
    private boolean active = false;

    public ImageTabButton(int x, int y, int width, int height, ResourceLocation texture,
                          ResourceLocation pressedTexture, int texWidth, int texHeight, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.pressedTexture = pressedTexture;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isTabActive() {
        return this.active;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render the appropriate texture based on active state
        ResourceLocation texToRender = (active && pressedTexture != null) ? pressedTexture : texture;
        guiGraphics.blit(texToRender, getX(), getY(), 0, 0, width, height, width, height);

        // Hover effect (only when not active)
        if (isHovered && !active) {
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x20FFFFFF);
        }
    }

    public static Builder builder(ResourceLocation texture, int texWidth, int texHeight, OnPress onPress) {
        return new Builder(texture, null, texWidth, texHeight, onPress);
    }

    public static Builder builder(ResourceLocation texture, ResourceLocation pressedTexture, int texWidth, int texHeight, OnPress onPress) {
        return new Builder(texture, pressedTexture, texWidth, texHeight, onPress);
    }

    public static class Builder {
        private final ResourceLocation texture;
        private final ResourceLocation pressedTexture;
        private final int texWidth;
        private final int texHeight;
        private final OnPress onPress;
        private int x, y, width, height;
        private boolean active = false;

        public Builder(ResourceLocation texture, ResourceLocation pressedTexture, int texWidth, int texHeight, OnPress onPress) {
            this.texture = texture;
            this.pressedTexture = pressedTexture;
            this.texWidth = texWidth;
            this.texHeight = texHeight;
            this.onPress = onPress;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder setActive(boolean active) {
            this.active = active;
            return this;
        }

        public ImageTabButton build() {
            ImageTabButton button = new ImageTabButton(x, y, width, height, texture, pressedTexture, texWidth, texHeight, onPress);
            button.setActive(active);
            return button;
        }
    }
}
