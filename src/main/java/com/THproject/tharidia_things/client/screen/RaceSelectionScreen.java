package com.THproject.tharidia_things.client.screen;

import com.THproject.tharidia_things.character.RaceData;
import com.THproject.tharidia_things.network.SelectRacePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Full-screen GUI for displaying race information and selection
 */
public class RaceSelectionScreen extends Screen {
    private final String raceName;
    private List<FormattedCharSequence> wrappedDescription;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private static final ResourceLocation MAP_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/gui/spawn_map.png");

    // Map dimensions (loaded from PNG or defaults)
    private int mapWidth = 1200;
    private int mapHeight = 900;

    // Cached render coordinates for click detection
    private int cachedRenderX = 0;
    private int cachedRenderY = 0;
    private int cachedRenderWidth = 0;
    private int cachedRenderHeight = 0;

    // Map click points for race selection - FIXED: "orcho" -> "orco"
    private static final MapPoint[] MAP_POINTS = {
            new MapPoint("umano", 300, 450),
            new MapPoint("elfo", 450, 450),
            new MapPoint("nano", 600, 450),
            new MapPoint("dragonide", 750, 450),
            new MapPoint("orco", 900, 450)  // FIXED: was "orcho"
    };

    private String selectedRace = null;
    private RaceData.RaceInfo selectedRaceInfo = null;
    private Button confirmButton = null;

    public RaceSelectionScreen(String raceName) {
        super(Component.literal("Race Selection"));
        this.raceName = raceName;
    }

    @Override
    protected void init() {
        super.init();

        // Load PNG dimensions efficiently (without loading full image)
        loadMapDimensions();

        // Update description if race already selected
        updateDescriptionLayout();

        // Add confirm button (disabled until race is selected)
        int buttonWidth = 200;
        int buttonHeight = 20;
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = this.height - 60;

        confirmButton = this.addRenderableWidget(Button.builder(
                Component.literal("Conferma"),
                button -> {
                    if (selectedRace != null && RaceData.isValidRace(selectedRace)) {
                        PacketDistributor.sendToServer(new SelectRacePacket(selectedRace));
                        this.onClose();
                    }
                }
        ).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build());

        confirmButton.active = (selectedRace != null);
    }

    /**
     * Load map dimensions efficiently using ImageInputStream
     * This avoids loading the entire image into memory just to get dimensions
     */
    private void loadMapDimensions() {
        try {
            var resourceManager = this.minecraft.getResourceManager();
            Resource resource = resourceManager.getResource(MAP_TEXTURE).orElse(null);

            if (resource != null) {
                try (InputStream is = resource.open();
                     ImageInputStream iis = ImageIO.createImageInputStream(is)) {

                    Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                    if (readers.hasNext()) {
                        ImageReader reader = readers.next();
                        try {
                            reader.setInput(iis);
                            this.mapWidth = reader.getWidth(0);
                            this.mapHeight = reader.getHeight(0);
                        } finally {
                            reader.dispose();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Keep default dimensions if loading fails
            this.mapWidth = 1200;
            this.mapHeight = 900;
        }
    }

    private void updateDescriptionLayout() {
        if (selectedRaceInfo != null) {
            int leftColumnWidth = (this.width * 2) / 5 - 40;
            this.wrappedDescription = font.split(Component.literal(selectedRaceInfo.description), leftColumnWidth);

            // Calculate maxScroll based on content
            int statsStartY = 50 + 40 + (wrappedDescription.size() * (font.lineHeight + 3)) + 50;
            int availableHeight = this.height - 120 - statsStartY;
            int totalStatsHeight = selectedRaceInfo.characteristics.size() * (font.lineHeight + 2);

            this.maxScroll = Math.max(0, totalStatsHeight - availableHeight);
        } else {
            this.wrappedDescription = new ArrayList<>();
            this.maxScroll = 0;
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Don't render default background to keep map visible
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render left panel with dark background
        int leftColumnWidth = (this.width * 2) / 5;
        guiGraphics.fill(0, 0, leftColumnWidth, this.height, 0xDD000000);

        // Render left column content
        renderLeftColumn(guiGraphics, leftColumnWidth);

        // Render map on right side
        renderMap(guiGraphics, leftColumnWidth);

        // Instructions
        int instructionY = this.height - 100;
        guiGraphics.drawCenteredString(this.font, "Premi ESC per chiudere", this.width / 2, instructionY, 0xAAAAAA);

        // Render widgets
        this.renderables.forEach(widget -> widget.render(guiGraphics, mouseX, mouseY, partialTick));
    }

    private void renderLeftColumn(GuiGraphics guiGraphics, int leftColumnWidth) {
        int leftColumnX = 20;
        int y = 50;

        if (selectedRaceInfo == null) {
            // Show title and instructions when no race selected
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
            // Show selected race information
            guiGraphics.drawString(this.font, "§6§l" + selectedRaceInfo.name, leftColumnX, y, 0xFFFFFF);
            y += 40;

            // Description
            if (wrappedDescription != null) {
                for (FormattedCharSequence line : wrappedDescription) {
                    guiGraphics.drawString(this.font, line, leftColumnX, y, 0xFFFFFF);
                    y += font.lineHeight + 3;
                }
            }

            y += 20;

            // Stats section
            guiGraphics.drawString(this.font, "§e§lCaratteristiche:", leftColumnX, y, 0xFFFF00);
            y += 30;

            // Scrollable stats area
            int clipTop = y;
            int clipBottom = this.height - 120;

            guiGraphics.enableScissor(leftColumnX, clipTop, leftColumnX + leftColumnWidth - 20, clipBottom);

            int statsY = y - scrollOffset;
            for (Map.Entry<String, Integer> entry : selectedRaceInfo.characteristics.entrySet()) {
                String statName = entry.getKey();
                int value = entry.getValue();

                // Color based on value
                int color;
                if (value > 110) color = 0x55FF55;      // Green for high
                else if (value < 90) color = 0xFF5555;  // Red for low
                else color = 0xFFFF55;                   // Yellow for average

                String text = "• " + statName + ": " + value;
                if (statsY + font.lineHeight > clipTop && statsY < clipBottom) {
                    guiGraphics.drawString(this.font, text, leftColumnX, statsY, color);
                }
                statsY += font.lineHeight + 2;
            }

            guiGraphics.disableScissor();
        }
    }

    private void renderMap(GuiGraphics guiGraphics, int leftColumnWidth) {
        int mapX = leftColumnWidth + 20;
        int mapY = 50;
        int mapAreaWidth = (this.width * 3) / 5 - 40;
        int mapAreaHeight = this.height - 150;

        try {
            // Calculate aspect ratio
            float imageRatio = (float) this.mapWidth / this.mapHeight;
            float areaRatio = (float) mapAreaWidth / mapAreaHeight;

            int renderWidth, renderHeight, renderX, renderY;

            if (imageRatio > areaRatio) {
                renderWidth = mapAreaWidth;
                renderHeight = (int) (mapAreaWidth / imageRatio);
                renderX = mapX;
                renderY = mapY + (mapAreaHeight - renderHeight) / 2;
            } else {
                renderHeight = mapAreaHeight;
                renderWidth = (int) (mapAreaHeight * imageRatio);
                renderX = mapX + (mapAreaWidth - renderWidth) / 2;
                renderY = mapY;
            }

            // Cache render coordinates for click detection
            this.cachedRenderX = renderX;
            this.cachedRenderY = renderY;
            this.cachedRenderWidth = renderWidth;
            this.cachedRenderHeight = renderHeight;

            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            guiGraphics.pose().pushPose();

            float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
            guiGraphics.pose().scale(1.0f / guiScale, 1.0f / guiScale, 1.0f);

            int fixedWidth = this.mapWidth;
            int fixedHeight = this.mapHeight;

            int fixedX = (int) (renderX * guiScale) + (int) ((renderWidth * guiScale - fixedWidth) / 2);
            int fixedY = (int) (renderY * guiScale) + (int) ((renderHeight * guiScale - fixedHeight) / 2);

            guiGraphics.blit(MAP_TEXTURE, fixedX, fixedY, 0, 0, fixedWidth, fixedHeight, this.mapWidth, this.mapHeight);

            // Draw clickable points on the map
            for (MapPoint point : MAP_POINTS) {
                int scaledX = fixedX + (int) ((point.x * fixedWidth) / mapWidth);
                int scaledY = fixedY + (int) ((point.y * fixedHeight) / mapHeight);

                int pointSize = 15;
                boolean isSelected = point.race.equals(selectedRace);
                int color = isSelected ? 0xFF00FF00 : 0xFFFFFF00;
                int borderColor = isSelected ? 0xFF00AA00 : 0xFFFFFFFF;

                // Draw outer border
                guiGraphics.fill(scaledX - pointSize, scaledY - pointSize,
                        scaledX + pointSize, scaledY + pointSize, borderColor);
                // Draw inner fill
                guiGraphics.fill(scaledX - pointSize + 2, scaledY - pointSize + 2,
                        scaledX + pointSize - 2, scaledY + pointSize - 2, color);
            }

            guiGraphics.pose().popPose();
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        } catch (Exception e) {
            // Fallback placeholder
            guiGraphics.drawCenteredString(this.font, "Mappa di Spawn",
                    mapX + mapAreaWidth / 2, mapY + mapAreaHeight / 2 - 10, 0xAAAAAA);
            guiGraphics.drawCenteredString(this.font, "(Coming Soon)",
                    mapX + mapAreaWidth / 2, mapY + mapAreaHeight / 2 + 5, 0xAAAAAA);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int leftColumnWidth = (this.width * 2) / 5;
        if (mouseX <= leftColumnWidth + 20 && selectedRaceInfo != null) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - deltaY * 15));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Use cached render coordinates for accurate click detection
        if (cachedRenderWidth > 0 && cachedRenderHeight > 0) {
            if (mouseX >= cachedRenderX && mouseX <= cachedRenderX + cachedRenderWidth &&
                    mouseY >= cachedRenderY && mouseY <= cachedRenderY + cachedRenderHeight) {

                // Convert screen coordinates to map coordinates
                double relativeX = (mouseX - cachedRenderX) / cachedRenderWidth;
                double relativeY = (mouseY - cachedRenderY) / cachedRenderHeight;

                int mapClickX = (int) (relativeX * mapWidth);
                int mapClickY = (int) (relativeY * mapHeight);

                // Check distance to each point
                for (MapPoint point : MAP_POINTS) {
                    double distance = Math.sqrt(
                            Math.pow(mapClickX - point.x, 2) +
                                    Math.pow(mapClickY - point.y, 2)
                    );

                    // Click radius in map coordinates (adjust as needed)
                    if (distance <= 50) {
                        selectRace(point.race);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void selectRace(String race) {
        // Validate race exists
        RaceData.RaceInfo info = RaceData.getRaceInfo(race);
        if (info == null) {
            // Race not found - show error feedback
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§cRazza non trovata: " + race), true);
            }
            return;
        }

        selectedRace = race;
        selectedRaceInfo = info;
        scrollOffset = 0;

        // Update description layout
        updateDescriptionLayout();

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
