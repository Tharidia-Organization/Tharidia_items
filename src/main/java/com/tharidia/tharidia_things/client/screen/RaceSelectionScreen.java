package com.tharidia.tharidia_things.client.screen;

import com.tharidia.tharidia_things.character.RaceData;
import com.tharidia.tharidia_things.network.SelectRacePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Full-screen GUI for displaying race information
 */
public class RaceSelectionScreen extends Screen {
    private final String raceName;
    private RaceData.RaceInfo raceInfo;
    private List<FormattedCharSequence> wrappedDescription;
    private int scrollOffset = 0;
    private int maxScroll = 0; // Will be calculated dynamically
    private static final ResourceLocation MAP_TEXTURE = ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/gui/spawn_map.png");
    private int mapWidth = 1200; // Your PNG dimensions
    private int mapHeight = 900; // Your PNG dimensions
    
    // Map click points for race selection (temporary horizontal layout)
    private static final MapPoint[] MAP_POINTS = {
        new MapPoint("umano", 300, 450),
        new MapPoint("elfo", 450, 450),
        new MapPoint("nano", 600, 450),
        new MapPoint("dragonide", 750, 450),
        new MapPoint("orcho", 900, 450)
    };
    
    private String selectedRace = null;
    private RaceData.RaceInfo selectedRaceInfo = null;
    private Button confirmButton = null;
    
    public RaceSelectionScreen(String raceName) {
        super(Component.literal("Race Selection"));
        this.raceName = raceName;
        // Don't set raceInfo initially - wait for map click
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Load PNG dimensions
        loadMapDimensions();
        
        // Wrap description text for left column layout
        if (selectedRaceInfo != null) {
            this.wrappedDescription = new ArrayList<>();
            // Use narrower width for left column (40% of screen)
            int leftColumnWidth = (this.width * 2) / 5 - 40;
            this.wrappedDescription = font.split(Component.literal(selectedRaceInfo.description), leftColumnWidth);
            
            // Calculate maxScroll based on content
            int statsStartY = 50 + 40 + (wrappedDescription.size() * (font.lineHeight + 3)) + 50; // Title + desc + space
            int availableHeight = this.height - 120 - statsStartY; // Available space for stats
            int totalStatsHeight = selectedRaceInfo.characteristics.size() * (font.lineHeight + 2);
            
            if (totalStatsHeight > availableHeight) {
                this.maxScroll = totalStatsHeight - availableHeight; // Positive value for how far we can scroll
            } else {
                this.maxScroll = 0; // No scrolling needed
            }
        }
        
        // Add confirm button (disabled until race is selected)
        int buttonWidth = 200;
        int buttonHeight = 20;
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = this.height - 60;
        
        // Store reference to the button
        confirmButton = this.addRenderableWidget(Button.builder(
            Component.literal("Conferma"),
            button -> {
                if (selectedRace != null) {
                    // Send packet to server with selected race
                    PacketDistributor.sendToServer(new SelectRacePacket(selectedRace));
                    this.onClose();
                }
            }
        ).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build());
        
        // Disable button initially
        confirmButton.active = false;
    }
    
    private void loadMapDimensions() {
        try {
            // Get the resource manager
            var resourceManager = this.minecraft.getResourceManager();
            
            // Get the resource for the PNG
            Resource resource = resourceManager.getResource(MAP_TEXTURE).orElse(null);
            if (resource != null) {
                // Read the PNG dimensions
                try (InputStream is = resource.open()) {
                    BufferedImage image = ImageIO.read(is);
                    if (image != null) {
                        this.mapWidth = image.getWidth();
                        this.mapHeight = image.getHeight();
                    }
                }
            }
        } catch (Exception e) {
            // Keep default dimensions if loading fails
            this.mapWidth = 256;
            this.mapHeight = 256;
        }
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Don't call super to avoid blur effect
        // Don't render transparent background as it darkens the map
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Don't call super.render() to avoid unwanted overlays
        
        // Dark background only for left side
        int leftColumnWidth = (this.width * 2) / 5;
        guiGraphics.fill(0, 0, leftColumnWidth, this.height, 0xDD000000);
        
        // Left column content
        int leftColumnX = 20;
        int y = 50;
        
        if (selectedRaceInfo == null) {
            // Show title and instruction when no race selected
            guiGraphics.drawString(this.font, "§6§lScegli la tua destinazione", leftColumnX, y, 0xFFFFFF);
            y += 30;
            guiGraphics.drawString(this.font, "§7Clicca sulla mappa per selezionare una razza", leftColumnX, y, 0xAAAAAA);
            y += 40;
            
            // Kingdom description
            guiGraphics.drawString(this.font, "§e§lIl Regno di Tharidia", leftColumnX, y, 0xFFFF00);
            y += 25;
            
            String[] kingdomDesc = {
                "§7Nato dalle ceneri della grande guerra dei cinque popoli,",
                "§7il Regno di Tharidia sorge maestoso tra montagne sacre",
                "§7e foreste antiche. Qui, cinque razze uniscono le loro sorti",
                "§7sotto la guida del Gran Concilio, mantenendo un precario",
                "§7equilibrio tra potere e saggezza, tradizione e progresso.",
                "§7",
                "§7Ogni razza porta con sé doni unici: la resilienza umana,",
                "§7la grazia elfica, l'abilità nanica, il potere draconico,",
                "§7e la forza bruta degli orchi. Insieme, formano un regno",
                "§7dove l'onore e il corone illuminano il sentiero di ogni",
                "§7avventuriero abbastanza audace da scrivere il proprio destino."
            };
            
            for (String line : kingdomDesc) {
                guiGraphics.drawString(this.font, line, leftColumnX, y, 0xFFFFFF);
                y += font.lineHeight + 2;
            }
        } else {
            // Show race information when race is selected
            y = 50;
            
            // Race title
            guiGraphics.drawString(this.font, selectedRaceInfo.name, leftColumnX, y, 0xFFFFFF);
            y += 40;
            
            // Description
            if (wrappedDescription != null) {
                for (FormattedCharSequence line : wrappedDescription) {
                    guiGraphics.drawString(this.font, line, leftColumnX, Math.round(y), 0xFFFFFF);
                    y += font.lineHeight + 3;
                }
            }
            
            y += 20; // Space before stats
            
            // Stats section title
            guiGraphics.drawString(this.font, "Caratteristiche:", leftColumnX, y, 0xFFFF00);
            y += 30;
            
            // Calculate scroll area for characteristics
            int clipTop = y;
            int clipBottom = this.height - 120;
            
            // Enable clipping for characteristics only
            guiGraphics.enableScissor(leftColumnX, clipTop, leftColumnX + leftColumnWidth - 20, clipBottom);
            
            // Draw stats in single column (scrollable)
            int statsY = y - scrollOffset;
            for (Map.Entry<String, Integer> entry : selectedRaceInfo.characteristics.entrySet()) {
                String statName = entry.getKey();
                int value = entry.getValue();
                
                // Color code based on value
                int color = 0xFFFFFF;
                if (value > 110) color = 0x55FF55; // Green for high
                else if (value < 90) color = 0xFF5555; // Red for low
                else color = 0xFFFF55; // Yellow for average
                
                String text = "• " + statName + ": " + value;
                if (statsY + font.lineHeight > clipTop && statsY < clipBottom) {
                    guiGraphics.drawString(this.font, text, leftColumnX, Math.round(statsY), color);
                }
                statsY += font.lineHeight + 2;
            }
            
            // Disable clipping
            guiGraphics.disableScissor();
        }
        
        // Map area coordinates (no dark background here)
        int mapX = leftColumnWidth + 20;
        int mapY = 50;
        int mapAreaWidth = (this.width * 3) / 5 - 40;
        int mapAreaHeight = this.height - 150;
        
        // Draw map first (always visible)
        // Right column - Map image or placeholder
        // Map variables already defined above
        
        // Try to draw the map texture, or show placeholder if missing
        try {
            // Calculate aspect ratio using actual PNG dimensions
            float imageRatio = (float) this.mapWidth / this.mapHeight;
            float areaRatio = (float)mapAreaWidth / mapAreaHeight;
            
            int renderWidth, renderHeight, renderX, renderY;
            
            if (imageRatio > areaRatio) {
                // Image is wider than the area - fit to width
                renderWidth = mapAreaWidth;
                renderHeight = (int)(mapAreaWidth / imageRatio);
                renderX = mapX;
                renderY = mapY + (mapAreaHeight - renderHeight) / 2;
            } else {
                // Image is taller than the area - fit to height
                renderHeight = mapAreaHeight;
                renderWidth = (int)(mapAreaHeight * imageRatio);
                renderX = mapX + (mapAreaWidth - renderWidth) / 2;
                renderY = mapY;
            }
            
            // Draw the texture with fixed screen dimensions
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            
            // Save current pose state
            guiGraphics.pose().pushPose();
            
            // Get GUI scale and apply inverse scaling to maintain fixed size
            float guiScale = (float)Minecraft.getInstance().getWindow().getGuiScale();
            guiGraphics.pose().scale(1.0f / guiScale, 1.0f / guiScale, 1.0f);
            
            // Calculate fixed screen size (match your PNG dimensions exactly)
            int fixedWidth = 1200;  // Exact PNG width
            int fixedHeight = 900;  // Exact PNG height
            
            // Center the fixed-size image
            int fixedX = (int)(renderX * guiScale) + (int)((renderWidth * guiScale - fixedWidth) / 2);
            int fixedY = (int)(renderY * guiScale) + (int)((renderHeight * guiScale - fixedHeight) / 2);
            
            // Draw at fixed screen coordinates
            guiGraphics.blit(MAP_TEXTURE, fixedX, fixedY, 0, 0, fixedWidth, fixedHeight, this.mapWidth, this.mapHeight);
            
            // Draw clickable points on the map
            for (MapPoint point : MAP_POINTS) {
                // Calculate scaled position
                int scaledX = fixedX + (int)((point.x * fixedWidth) / mapWidth);
                int scaledY = fixedY + (int)((point.y * fixedHeight) / mapHeight);
                
                // Draw point (circle with border)
                int pointSize = 15;
                int color = point.race.equals(selectedRace) ? 0xFF00FF00 : 0xFFFFFF00;
                int borderColor = point.race.equals(selectedRace) ? 0xFF00AA00 : 0xFFFFFFFF;
                
                // Outer circle (border)
                guiGraphics.fill(scaledX - pointSize, scaledY - pointSize, 
                                scaledX + pointSize, scaledY + pointSize, borderColor);
                // Inner circle
                guiGraphics.fill(scaledX - pointSize + 2, scaledY - pointSize + 2, 
                                scaledX + pointSize - 2, scaledY + pointSize - 2, color);
            }
            
            // Restore pose state
            guiGraphics.pose().popPose();
            
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        } catch (Exception e) {
            // Draw placeholder text when texture is missing
            String placeholder = "Mappa di Spawn\n(Coming Soon)";
            String[] lines = placeholder.split("\n");
            int textY = mapY + mapAreaHeight / 2 - 10;
            for (String line : lines) {
                guiGraphics.drawCenteredString(this.font, line, mapX + mapAreaWidth / 2, textY, 0xAAAAAA);
                textY += font.lineHeight + 5;
            }
        }
        
        // If no race selected, show instructions
        if (selectedRaceInfo == null) {
            // Draw instructions and widgets even when no race is selected
                        int instructionY = this.height - 100;
            guiGraphics.drawCenteredString(this.font, "Premi ESC per chiudere", this.width / 2, instructionY, 0xAAAAAA);

            // Render widgets manually since we removed super.render()
            this.renderables.forEach(widget -> widget.render(guiGraphics, mouseX, mouseY, partialTick));
            return;
        }
        
        
        // Try to draw the map texture, or show placeholder if missing
        try {
            // Calculate aspect ratio using actual PNG dimensions
            float imageRatio = (float) this.mapWidth / this.mapHeight;
            float areaRatio = (float)mapAreaWidth / mapAreaHeight;
            
            int renderWidth, renderHeight, renderX, renderY;
            
            if (imageRatio > areaRatio) {
                // Image is wider than the area - fit to width
                renderWidth = mapAreaWidth;
                renderHeight = (int)(mapAreaWidth / imageRatio);
                renderX = mapX;
                renderY = mapY + (mapAreaHeight - renderHeight) / 2;
            } else {
                // Image is taller than the area - fit to height
                renderHeight = mapAreaHeight;
                renderWidth = (int)(mapAreaHeight * imageRatio);
                renderX = mapX + (mapAreaWidth - renderWidth) / 2;
                renderY = mapY;
            }
            
            // Draw the texture with fixed screen dimensions
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            
            // Save current pose state
            guiGraphics.pose().pushPose();
            
            // Get GUI scale and apply inverse scaling to maintain fixed size
            float guiScale = (float)Minecraft.getInstance().getWindow().getGuiScale();
            guiGraphics.pose().scale(1.0f / guiScale, 1.0f / guiScale, 1.0f);
            
            // Calculate fixed screen size (match your PNG dimensions exactly)
            int fixedWidth = 1200;  // Exact PNG width
            int fixedHeight = 900;  // Exact PNG height
            
            // Center the fixed-size image
            int fixedX = (int)(renderX * guiScale) + (int)((renderWidth * guiScale - fixedWidth) / 2);
            int fixedY = (int)(renderY * guiScale) + (int)((renderHeight * guiScale - fixedHeight) / 2);
            
            // Draw at fixed screen coordinates
            guiGraphics.blit(MAP_TEXTURE, fixedX, fixedY, 0, 0, fixedWidth, fixedHeight, this.mapWidth, this.mapHeight);
            
            // Draw clickable points on the map
            for (MapPoint point : MAP_POINTS) {
                // Calculate scaled position
                int scaledX = fixedX + (int)((point.x * fixedWidth) / mapWidth);
                int scaledY = fixedY + (int)((point.y * fixedHeight) / mapHeight);
                
                // Draw point (circle with border)
                int pointSize = 15;
                int color = point.race.equals(selectedRace) ? 0xFF00FF00 : 0xFFFFFF00;
                int borderColor = point.race.equals(selectedRace) ? 0xFF00AA00 : 0xFFFFFFFF;
                
                // Outer circle (border)
                guiGraphics.fill(scaledX - pointSize, scaledY - pointSize, 
                                scaledX + pointSize, scaledY + pointSize, borderColor);
                // Inner circle
                guiGraphics.fill(scaledX - pointSize + 2, scaledY - pointSize + 2, 
                                scaledX + pointSize - 2, scaledY + pointSize - 2, color);
            }
            
            // Restore pose state
            guiGraphics.pose().popPose();
            
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        } catch (Exception e) {
            // Draw placeholder text when texture is missing
            String placeholder = "Mappa di Spawn\n(Coming Soon)";
            String[] lines = placeholder.split("\n");
            int textY = mapY + mapAreaHeight / 2 - 10;
            for (String line : lines) {
                guiGraphics.drawCenteredString(this.font, line, mapX + mapAreaWidth / 2, textY, 0xAAAAAA);
                textY += font.lineHeight + 5;
            }
        }
        
        // Instructions
        int instructionY = this.height - 100;
        guiGraphics.drawCenteredString(this.font, "Premi ESC per chiudere", this.width / 2, instructionY, 0xAAAAAA);

        // Render widgets manually since we removed super.render()
        this.renderables.forEach(widget -> widget.render(guiGraphics, mouseX, mouseY, partialTick));
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        // Handle scrolling - only when mouse is over left column
        int leftColumnWidth = (this.width * 2) / 5;
        if (mouseX <= leftColumnWidth + 20) {
            // Invert scroll direction - scroll down should show more content
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - deltaY * 15));
            return true;
        }
        return false;
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if click is on the map area
        int leftColumnWidth = (this.width * 2) / 5;
        int mapX = leftColumnWidth + 20;
        int mapY = 50;
        int mapAreaWidth = (this.width * 3) / 5 - 40;
        int mapAreaHeight = this.height - 150;
        
        if (mouseX >= mapX && mouseX <= mapX + mapAreaWidth &&
            mouseY >= mapY && mouseY <= mapY + mapAreaHeight) {
            
            // Calculate which point was clicked
            for (MapPoint point : MAP_POINTS) {
                // Calculate point position on screen
                double pointX = mapX + (point.x * mapAreaWidth) / mapWidth;
                double pointY = mapY + (point.y * mapAreaHeight) / mapHeight;
                
                // Check if click is within point radius (30 pixels)
                double distance = Math.sqrt(Math.pow(mouseX - pointX, 2) + Math.pow(mouseY - pointY, 2));
                if (distance <= 30) {
                    // Select this race
                    selectRace(point.race);
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void selectRace(String race) {
        selectedRace = race;
        selectedRaceInfo = RaceData.getRaceInfo(race);
        
        // Update wrapped description
        if (selectedRaceInfo != null) {
            int leftColumnWidth = (this.width * 2) / 5 - 40;
            this.wrappedDescription = font.split(Component.literal(selectedRaceInfo.description), leftColumnWidth);
            
            // Recalculate scroll
            int statsStartY = 50 + 40 + (wrappedDescription.size() * (font.lineHeight + 3)) + 50;
            int availableHeight = this.height - 120 - statsStartY;
            int totalStatsHeight = selectedRaceInfo.characteristics.size() * (font.lineHeight + 2);
            
            if (totalStatsHeight > availableHeight) {
                this.maxScroll = totalStatsHeight - availableHeight;
            } else {
                this.maxScroll = 0;
            }
            
            scrollOffset = 0; // Reset scroll
        }
        
        // Enable confirm button
        if (confirmButton != null) {
            confirmButton.active = true;
        }
    }
    
    private static class MapPoint {
        final String race;
        final int x;
        final int y;
        
        MapPoint(String race, int x, int y) {
            this.race = race;
            this.x = x;
            this.y = y;
        }
    }
}
