package com.tharidia.tharidia_things.client.gui;

import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import com.tharidia.tharidia_things.client.ClientPacketHandler;
import com.tharidia.tharidia_things.gui.PietroMenu;
import com.tharidia.tharidia_things.network.UpdateHierarchyPacket;
import com.tharidia.tharidia_things.realm.HierarchyRank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class PietroScreen extends AbstractContainerScreen<PietroMenu> {
    private static final ResourceLocation TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "textures/gui/pietro_gui.png");
    
    private static final int TAB_EXPANSION = 0;
    private static final int TAB_CLAIMS = 1;
    
    private int currentTab = TAB_EXPANSION;
    private Button expansionTabButton;
    private Button claimsTabButton;
    private List<Button> hierarchyButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_PLAYERS = 6;
    private UUID selectedPlayerForRankChange = null;
    private boolean showRankSelectionMenu = false;

    public PietroScreen(PietroMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 250;
        this.imageHeight = 300;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int tabX = this.leftPos + 10;
        int tabY = this.topPos + 15;
        int tabWidth = 70;
        int tabHeight = 20;
        
        // Expansion tab button
        expansionTabButton = Button.builder(
            Component.literal("Espansione"),
            button -> switchTab(TAB_EXPANSION)
        ).bounds(tabX, tabY, tabWidth, tabHeight).build();
        
        // Claims tab button
        claimsTabButton = Button.builder(
            Component.literal("Rivendicazioni"),
            button -> switchTab(TAB_CLAIMS)
        ).bounds(tabX + tabWidth + 5, tabY, tabWidth + 30, tabHeight).build();
        
        this.addRenderableWidget(expansionTabButton);
        this.addRenderableWidget(claimsTabButton);
        
        updateTabButtons();
    }
    
    private void switchTab(int tab) {
        currentTab = tab;
        scrollOffset = 0;
        updateTabButtons();
        rebuildWidgets();
    }
    
    private void updateTabButtons() {
        expansionTabButton.active = currentTab != TAB_EXPANSION;
        claimsTabButton.active = currentTab != TAB_CLAIMS;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 250, 300);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render title centered at the top
        int titleX = (this.imageWidth - this.font.width(this.title)) / 2;
        guiGraphics.drawString(this.font, this.title, titleX, 6, 4210752, false);
        
        // Render content based on current tab
        if (currentTab == TAB_EXPANSION) {
            renderExpansionTab(guiGraphics);
        } else if (currentTab == TAB_CLAIMS) {
            renderClaimsTab(guiGraphics);
        }
    }
    
    private void renderExpansionTab(GuiGraphics guiGraphics) {
        PietroBlockEntity pietroEntity = this.menu.getBlockEntity();
        if (pietroEntity != null) {
            int yPos = 45; // Start below tabs
            int color = 0x404040; // Dark gray
            
            // Owner
            String owner = pietroEntity.getOwnerName();
            if (owner == null || owner.isEmpty()) {
                owner = "Unknown";
            }
            guiGraphics.drawString(this.font, "§6Proprietario: §f" + owner, 10, yPos, color, false);
            yPos += 12;
            
            // Realm size - use synced data from menu
            int size = this.menu.getRealmSize();
            guiGraphics.drawString(this.font, "§6Dimensione Regno: §f" + size + "x" + size + " chunks", 
                10, yPos, color, false);
            yPos += 12;
            
            // Potato progress - use synced data from menu
            if (size >= 15) {
                guiGraphics.drawString(this.font, "§aRegno al massimo livello!", 10, yPos, color, false);
            } else {
                int stored = this.menu.getStoredPotatoes();
                int required = pietroEntity.getPotatoCostForNextLevel();
                int remaining = required - stored;
                
                guiGraphics.drawString(this.font, "§6Monete per espansione:", 10, yPos, color, false);
                yPos += 12;
                
                guiGraphics.drawString(this.font, "§e" + stored + "§7/§e" + required + 
                    " §7(§6" + remaining + " §7necessarie)", 10, yPos, color, false);
                yPos += 12;
                
                // Progress bar
                int barWidth = 180;
                int barHeight = 8;
                int barX = 10;
                int barY = yPos;
                
                // Background (dark gray)
                guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF555555);
                
                // Progress (gold)
                if (required > 0) {
                    int fillWidth = (int)((stored / (float)required) * barWidth);
                    guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, 0xFFFFAA00);
                }
                
                // Border (black)
                guiGraphics.renderOutline(barX, barY, barWidth, barHeight, 0xFF000000);
            }
            
            yPos += 20;
            
            // Instructions
            guiGraphics.drawString(this.font, "§7Metti le patate nello slot →", 10, yPos, color, false);
            yPos += 10;
            guiGraphics.drawString(this.font, "§7per espandere il regno", 10, yPos, color, false);
        }
    }
    
    private void renderClaimsTab(GuiGraphics guiGraphics) {
        int yPos = 45; // Start below tabs
        int color = 0x404040; // Dark gray
        
        // Title - compacted
        guiGraphics.drawString(this.font, "§6§lRivendicazioni", 10, yPos, color, false);
        yPos += 15;
        
        // Total potatoes - compacted
        int totalPotatoes = this.menu.getTotalClaimPotatoes();
        guiGraphics.drawString(this.font, "§6Monete: §e§l" + totalPotatoes, 10, yPos, color, false);
        yPos += 20;
        
        // Divider line
        guiGraphics.fill(10, yPos, 240, yPos + 1, 0xFF404040);
        yPos += 5;
        
        // Hierarchy section
        guiGraphics.drawString(this.font, "§6Gerarchia del Regno:", 10, yPos, color, false);
        yPos += 12;
        
        // Get hierarchy data from client cache
        Map<UUID, Integer> hierarchyData = ClientPacketHandler.getCachedHierarchyData();
        UUID ownerUUID = ClientPacketHandler.getCachedOwnerUUID();
        String ownerName = ClientPacketHandler.getCachedOwnerName();
        
        // Check if current player is the owner
        UUID currentPlayerUUID = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        boolean isOwner = currentPlayerUUID != null && currentPlayerUUID.equals(ownerUUID);
        
        // Add owner to the list if available
        List<PlayerHierarchyEntry> entries = new ArrayList<>();
        if (ownerUUID != null) {
            entries.add(new PlayerHierarchyEntry(ownerUUID, ownerName, HierarchyRank.LORD.getLevel()));
        }
        
        // Add other players from hierarchy data
        for (Map.Entry<UUID, Integer> entry : hierarchyData.entrySet()) {
            String playerName = getPlayerNameFromUUID(entry.getKey());
            entries.add(new PlayerHierarchyEntry(entry.getKey(), playerName, entry.getValue()));
        }
        
        // Sort by rank level (descending)
        entries.sort((a, b) -> Integer.compare(b.rankLevel, a.rankLevel));
        
        // Display player list with scrolling
        int displayStartIndex = scrollOffset;
        int displayEndIndex = Math.min(scrollOffset + MAX_VISIBLE_PLAYERS, entries.size());
        
        for (int i = displayStartIndex; i < displayEndIndex; i++) {
            PlayerHierarchyEntry entry = entries.get(i);
            HierarchyRank rank = HierarchyRank.fromLevel(entry.rankLevel);
            
            // Player name
            String displayName = entry.playerName.length() > 12 ? entry.playerName.substring(0, 12) : entry.playerName;
            guiGraphics.drawString(this.font, "§f" + displayName, 12, yPos, color, false);
            
            // Rank name
            String rankColor = getRankColor(rank);
            guiGraphics.drawString(this.font, rankColor + rank.getDisplayName(), 110, yPos, color, false);
            
            // Change rank button (only for owner and not for themselves)
            if (isOwner && !entry.playerUUID.equals(ownerUUID)) {
                // Draw button background
                int buttonX = 185;
                int buttonY = yPos - 2;
                int buttonWidth = 40;
                int buttonHeight = 12;
                
                guiGraphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0xFF555555);
                guiGraphics.renderOutline(buttonX, buttonY, buttonWidth, buttonHeight, 0xFF888888);
                
                // Draw button text
                guiGraphics.drawString(this.font, "§fCambia", buttonX + 3, buttonY + 2, color, false);
            }
            
            yPos += 18;
        }
        
        // Scroll indicators
        if (scrollOffset > 0) {
            guiGraphics.drawString(this.font, "§7▲", 230, 85, color, false);
        }
        if (displayEndIndex < entries.size()) {
            guiGraphics.drawString(this.font, "§7▼", 230, 165, color, false);
        }
        
        // Render rank selection menu if open
        if (showRankSelectionMenu && selectedPlayerForRankChange != null) {
            renderRankSelectionMenu(guiGraphics);
        }
    }
    
    private void renderRankSelectionMenu(GuiGraphics guiGraphics) {
        int menuX = this.leftPos + 90;
        int menuY = this.topPos + 90;
        int menuWidth = 80;
        int menuHeight = 90;
        
        // Background
        guiGraphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xDD000000);
        guiGraphics.renderOutline(menuX, menuY, menuWidth, menuHeight, 0xFFFFFFFF);
        
        // Title
        guiGraphics.drawString(this.font, "§6Scegli Rango:", menuX + 5, menuY + 5, 0x404040, false);
        
        // Rank options (except LORD)
        HierarchyRank[] selectableRanks = {HierarchyRank.CONSIGLIERE, HierarchyRank.GUARDIA, HierarchyRank.MILIZIANO, HierarchyRank.COLONO};
        int optionY = menuY + 18;
        
        for (HierarchyRank rank : selectableRanks) {
            String rankColor = getRankColor(rank);
            guiGraphics.drawString(this.font, rankColor + rank.getDisplayName(), menuX + 8, optionY, 0x404040, false);
            optionY += 15;
        }
    }
    
    private String getRankColor(HierarchyRank rank) {
        return switch (rank) {
            case LORD -> "§6"; // Gold
            case CONSIGLIERE -> "§5"; // Purple
            case GUARDIA -> "§9"; // Blue
            case MILIZIANO -> "§a"; // Green
            case COLONO -> "§7"; // Gray
        };
    }
    
    private String getPlayerNameFromUUID(UUID uuid) {
        if (Minecraft.getInstance().level != null) {
            var player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
            if (player != null) {
                return player.getName().getString();
            }
        }
        return "Unknown";
    }
    
    private static class PlayerHierarchyEntry {
        UUID playerUUID;
        String playerName;
        int rankLevel;
        
        PlayerHierarchyEntry(UUID playerUUID, String playerName, int rankLevel) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.rankLevel = rankLevel;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && currentTab == TAB_CLAIMS) { // Left click on claims tab
            // Check if clicking on rank selection menu
            if (showRankSelectionMenu && selectedPlayerForRankChange != null) {
                int menuX = this.leftPos + 5; //TODO fix the values to put the menu in the correct position
                int menuY = this.topPos + 5;
                int menuWidth = 80;
                int menuHeight = 90;
                
                if (mouseX >= menuX && mouseX <= menuX + menuWidth && mouseY >= menuY && mouseY <= menuY + menuHeight) {
                    // Check which rank was clicked
                    HierarchyRank[] selectableRanks = {HierarchyRank.CONSIGLIERE, HierarchyRank.GUARDIA, HierarchyRank.MILIZIANO, HierarchyRank.COLONO};
                    int optionY = menuY + 18;
                    
                    for (HierarchyRank rank : selectableRanks) {
                        if (mouseY >= optionY && mouseY <= optionY + 15) {
                            // Send packet to server to update hierarchy
                            UpdateHierarchyPacket packet = new UpdateHierarchyPacket(
                                this.menu.getBlockPos(),
                                selectedPlayerForRankChange,
                                rank.getLevel()
                            );
                            PacketDistributor.sendToServer(packet);
                            
                            // Close menu
                            showRankSelectionMenu = false;
                            selectedPlayerForRankChange = null;
                            return true;
                        }
                        optionY += 15;
                    }
                } else {
                    // Clicked outside menu, close it
                    showRankSelectionMenu = false;
                    selectedPlayerForRankChange = null;
                    return true;
                }
            }
            
            // Check if clicking on "Cambia" buttons
            Map<UUID, Integer> hierarchyData = ClientPacketHandler.getCachedHierarchyData();
            UUID ownerUUID = ClientPacketHandler.getCachedOwnerUUID();
            String ownerName = ClientPacketHandler.getCachedOwnerName();
            UUID currentPlayerUUID = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
            boolean isOwner = currentPlayerUUID != null && currentPlayerUUID.equals(ownerUUID);
            
            if (isOwner) {
                // Build entry list
                List<PlayerHierarchyEntry> entries = new ArrayList<>();
                if (ownerUUID != null) {
                    entries.add(new PlayerHierarchyEntry(ownerUUID, ownerName, HierarchyRank.LORD.getLevel()));
                }
                for (Map.Entry<UUID, Integer> entry : hierarchyData.entrySet()) {
                    String playerName = getPlayerNameFromUUID(entry.getKey());
                    entries.add(new PlayerHierarchyEntry(entry.getKey(), playerName, entry.getValue()));
                }
                entries.sort((a, b) -> Integer.compare(b.rankLevel, a.rankLevel));
                
                // Check button positions
                int displayStartIndex = scrollOffset;
                int displayEndIndex = Math.min(scrollOffset + MAX_VISIBLE_PLAYERS, entries.size());
                int yPos = this.topPos + 97; // Starting Y position for player list
                
                for (int i = displayStartIndex; i < displayEndIndex; i++) {
                    PlayerHierarchyEntry entry = entries.get(i);
                    
                    if (!entry.playerUUID.equals(ownerUUID)) {
                        int buttonX = this.leftPos + 185;
                        int buttonY = yPos - 2;
                        int buttonWidth = 40;
                        int buttonHeight = 12;
                        
                        if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth && 
                            mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                            // Open rank selection menu
                            selectedPlayerForRankChange = entry.playerUUID;
                            showRankSelectionMenu = true;
                            return true;
                        }
                    }
                    
                    yPos += 18;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentTab == TAB_CLAIMS) {
            Map<UUID, Integer> hierarchyData = ClientPacketHandler.getCachedHierarchyData();
            UUID ownerUUID = ClientPacketHandler.getCachedOwnerUUID();
            
            int totalPlayers = hierarchyData.size() + (ownerUUID != null ? 1 : 0);
            int maxScroll = Math.max(0, totalPlayers - MAX_VISIBLE_PLAYERS);
            
            if (scrollY > 0) {
                // Scroll up
                scrollOffset = Math.max(0, scrollOffset - 1);
                return true;
            } else if (scrollY < 0) {
                // Scroll down
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
                return true;
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Override
    public void removed() {
        super.removed();
        // Clear hierarchy cache when screen is closed
        ClientPacketHandler.clearHierarchyCache();
    }
    
    // Removed containerTick - processing moved to server side
}
