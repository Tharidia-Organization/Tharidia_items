package com.THproject.tharidia_things.client.gui;

import com.THproject.tharidia_things.block.entity.PietroBlockEntity;
import com.THproject.tharidia_things.client.ClientPacketHandler;
import com.THproject.tharidia_things.client.gui.medieval.MedievalGuiRenderer;
import com.THproject.tharidia_things.client.gui.medieval.MedievalTab;
import com.THproject.tharidia_things.client.gui.medieval.MedievalButton;
import com.THproject.tharidia_things.client.gui.medieval.MedievalProgressBar;
import com.THproject.tharidia_things.gui.PietroMenu;
import com.THproject.tharidia_things.gui.inventory.PlayerInventoryPanelLayout;
import com.THproject.tharidia_things.client.gui.components.PlayerInventoryPanelRenderer;
import com.THproject.tharidia_things.network.DungeonQueuePacket;
import com.THproject.tharidia_things.realm.HierarchyRank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * Medieval-styled realm management screen with ornate tabs and royal aesthetics
 */
public class PietroScreen extends AbstractContainerScreen<PietroMenu> {
    // Tab identifiers
    private static final int TAB_EXPANSION = 0;
    private static final int TAB_CLAIMS = 1;
    private static final int TAB_DUNGEON = 2;

    // Medieval styling dimensions
    private static final int PARCHMENT_WIDTH = 300;
    private static final int PARCHMENT_HEIGHT = 350;
    private static final int BORDER_WIDTH = 15;

    private int currentTab = TAB_EXPANSION;
    private MedievalTab expansionTabButton;
    private MedievalTab claimsTabButton;
    private MedievalTab dungeonTabButton;
    private List<MedievalButton> hierarchyButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_PLAYERS = 6;
    private UUID selectedPlayerForRankChange = null;
    private boolean showRankSelectionMenu = false;
    private int rankMenuX = 0;
    private int rankMenuY = 0;
    private MedievalButton enterDungeonButton;
    private MedievalButton enterDungeonButtonGroup;
    private MedievalProgressBar expansionProgressBar;

    public PietroScreen(PietroMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PARCHMENT_WIDTH;
        this.imageHeight = PARCHMENT_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        int tabX = this.leftPos + BORDER_WIDTH;
        int tabY = this.topPos + 60;
        int tabWidth = 75;
        int tabHeight = 30;
        int tabSpacing = 10;

        // Create medieval-styled tabs
        expansionTabButton = MedievalTab.builder(
                Component.translatable("gui.tharidiathings.realm.tab.expansion"),
                button -> switchTab(TAB_EXPANSION),
                MedievalTab.TabStyle.ROYAL).bounds(tabX, tabY, tabWidth, tabHeight).setActive(true).build();

        claimsTabButton = MedievalTab.builder(
                Component.translatable("gui.tharidiathings.realm.tab.claims"),
                button -> switchTab(TAB_CLAIMS),
                MedievalTab.TabStyle.ROYAL).bounds(tabX + tabWidth + tabSpacing, tabY, tabWidth + 20, tabHeight)
                .build();

        dungeonTabButton = MedievalTab.builder(
                Component.translatable("gui.tharidiathings.realm.tab.dungeon"),
                button -> switchTab(TAB_DUNGEON),
                MedievalTab.TabStyle.ROYAL).bounds(tabX + 2 * (tabWidth + tabSpacing) + 20, tabY, tabWidth, tabHeight)
                .build();

        this.addRenderableWidget(expansionTabButton);
        this.addRenderableWidget(claimsTabButton);
        this.addRenderableWidget(dungeonTabButton);

        // Initialize expansion progress bar with relative coordinates
        expansionProgressBar = new MedievalProgressBar(
                BORDER_WIDTH + 20,
                180,
                this.imageWidth - BORDER_WIDTH * 2 - 40,
                15);

        // Create Entra button once, initially hidden
        enterDungeonButton = MedievalButton.builder(
                Component.translatable("gui.tharidiathings.realm.dungeon.enter_button"),
                button -> {
                    // Send packet to join dungeon queue
                    sendDungeonJoinRequest();
                },
                MedievalButton.ButtonStyle.ROYAL)
                .bounds(this.leftPos + PARCHMENT_WIDTH - 100, this.topPos + PARCHMENT_HEIGHT
                        - 40, 80, 25)
                .build();
        enterDungeonButton.visible = false;
        this.addRenderableWidget(enterDungeonButton);

        // Create Entra Gruppo button once, initially hidden
        enterDungeonButtonGroup = MedievalButton.builder(
                Component.translatable("gui.tharidiathings.realm.dungeon.enter_group_button"),
                button -> {
                    // Send packet to join dungeon queue
                    sendDungeonJoinRequest();
                },
                MedievalButton.ButtonStyle.ROYAL)
                .bounds(this.leftPos + PARCHMENT_WIDTH - 220, this.topPos + PARCHMENT_HEIGHT - 40, 120, 25).build();
        enterDungeonButtonGroup.visible = false;
        this.addRenderableWidget(enterDungeonButtonGroup);

        updateTabButtons();
    }

    private void updateTabButtons() {
        expansionTabButton.setActive(currentTab == TAB_EXPANSION);
        claimsTabButton.setActive(currentTab == TAB_CLAIMS);
        dungeonTabButton.setActive(currentTab == TAB_DUNGEON);

        // Fix button visibility - only show on dungeon tab
        if (enterDungeonButton != null) {
            enterDungeonButton.visible = (currentTab == TAB_DUNGEON);
        }
        if (enterDungeonButtonGroup != null) {
            enterDungeonButtonGroup.visible = (currentTab == TAB_DUNGEON);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Render medieval parchment background
        MedievalGuiRenderer.renderParchmentBackground(guiGraphics, x, y, this.imageWidth, this.imageHeight);

        // Render ornate border with royal crimson
        MedievalGuiRenderer.renderOrnateBorder(guiGraphics, x, y, this.imageWidth, this.imageHeight,
                MedievalGuiRenderer.DEEP_CRIMSON);

        // Render special frame around potato slot at top center
        renderPotatoSlotFrame(guiGraphics, x + 141, y + 35);

        // Render simple background for player inventory (left side outside main GUI)
        int inventoryBgX = x + PlayerInventoryPanelLayout.PANEL_OFFSET_X;
        int inventoryBgY = y + PlayerInventoryPanelLayout.PANEL_OFFSET_Y;
        int slotStartX = this.leftPos + PlayerInventoryPanelLayout.SLOT_OFFSET_X;
        int slotStartY = this.topPos + PlayerInventoryPanelLayout.SLOT_OFFSET_Y;
        PlayerInventoryPanelRenderer.renderPanel(guiGraphics, inventoryBgX, inventoryBgY, slotStartX, slotStartY);
    }

    /**
     * Renders a special medieval frame around the potato slot
     */
    private void renderPotatoSlotFrame(GuiGraphics gui, int x, int y) {
        // Outer frame with bronze color
        gui.fill(x - 3, y - 3, x + 21, y + 21, MedievalGuiRenderer.BRONZE);
        // Inner frame with gold
        gui.fill(x - 1, y - 1, x + 19, y + 19, MedievalGuiRenderer.ROYAL_GOLD);
        // Center with darker parchment
        gui.fill(x + 1, y + 1, x + 17, y + 17, MedievalGuiRenderer.DARK_PARCHMENT);

        // Add text label above slot
        gui.drawString(Minecraft.getInstance().font,
                Component.translatable("gui.tharidiathings.realm.currency_label").getString(), x - 5, y - 15,
                MedievalGuiRenderer.ROYAL_GOLD);
    }

    /**
     * Renders a decorative royal crest
     */
    private void renderRoyalCrest(GuiGraphics gui, int x, int y) {
        // Shield background
        gui.fill(x - 2, y - 2, x + 42, y + 42, MedievalGuiRenderer.BRONZE);
        gui.fill(x, y, x + 40, y + 40, MedievalGuiRenderer.ROYAL_GOLD);

        // Inner shield
        gui.fill(x + 5, y + 5, x + 35, y + 35, MedievalGuiRenderer.DEEP_CRIMSON);
        gui.fill(x + 8, y + 8, x + 32, y + 32, MedievalGuiRenderer.PURPLE_REGAL);

        // Crown symbol
        gui.drawString(Minecraft.getInstance().font, "♔", x + 12, y + 12, MedievalGuiRenderer.GOLD_LEAF);

        // Decorative cross
        gui.fill(x + 18, y + 2, x + 22, y + 38, MedievalGuiRenderer.BRONZE);
        gui.fill(x + 2, y + 18, x + 38, y + 22, MedievalGuiRenderer.BRONZE);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // renderLabels uses relative coordinates (0,0 is top-left of GUI container)
        int x = BORDER_WIDTH;
        int y = 100;

        // Render medieval title
        MedievalGuiRenderer.renderMedievalTitle(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.title").getString(),
                x, y, this.imageWidth - BORDER_WIDTH * 2);

        // Render content based on current tab
        if (currentTab == TAB_EXPANSION) {
            renderExpansionTab(guiGraphics);
        } else if (currentTab == TAB_CLAIMS) {
            renderClaimsTab(guiGraphics);
        } else if (currentTab == TAB_DUNGEON) {
            renderDungeonTab(guiGraphics);
        }

        // Render decorative elements
        renderSideDecorations(guiGraphics, 0, 0);
    }

    /**
     * Renders side decorative elements
     */
    private void renderSideDecorations(GuiGraphics gui, int x, int y) {
        // Left side decorations
        for (int i = 0; i < 4; i++) {
            int decorY = y + 120 + (i * 50);
            gui.drawString(Minecraft.getInstance().font, "✦", x + 8, decorY, MedievalGuiRenderer.BRONZE);
        }

        // Right side decorations
        for (int i = 0; i < 4; i++) {
            int decorY = y + 120 + (i * 50);
            gui.drawString(Minecraft.getInstance().font, "✦", x + this.imageWidth - 20, decorY,
                    MedievalGuiRenderer.BRONZE);
        }
    }

    private void renderExpansionTab(GuiGraphics guiGraphics) {
        PietroBlockEntity pietroEntity = this.menu.getBlockEntity();
        if (pietroEntity != null) {
            // Use relative coordinates (0,0 = top-left of GUI)
            int yPos = 130;
            int textX = BORDER_WIDTH + 20;

            // Lord information with medieval styling
            renderMedievalTextLine(guiGraphics,
                    Component.translatable("gui.tharidiathings.realm.lord_label").getString(), textX, yPos,
                    MedievalGuiRenderer.BROWN_INK);
            yPos += 20;

            String owner = pietroEntity.getOwnerName();
            if (owner == null || owner.isEmpty()) {
                owner = Component.translatable("gui.tharidiathings.common.unknown").getString();
            }
            renderMedievalTextLine(guiGraphics, "§f" + owner, textX + 20, yPos, MedievalGuiRenderer.BLACK_INK);
            yPos += 30;

            // Render divider
            MedievalGuiRenderer.renderMedievalDivider(guiGraphics, textX, yPos,
                    this.imageWidth - BORDER_WIDTH * 2 - 40);
            yPos += 25;

            // Realm size information
            int size = this.menu.getRealmSize();
            renderMedievalTextLine(guiGraphics,
                    Component.translatable("gui.tharidiathings.realm.size_label").getString(), textX, yPos,
                    MedievalGuiRenderer.BROWN_INK);
            yPos += 20;
            renderMedievalTextLine(guiGraphics,
                    Component.translatable("gui.tharidiathings.realm.size_value", size).getString(), textX + 20, yPos,
                    MedievalGuiRenderer.ROYAL_GOLD);
            yPos += 30;

            // Expansion progress with medieval progress bar
            if (size >= 15) {
                renderMedievalTextLine(guiGraphics,
                        Component.translatable("gui.tharidiathings.realm.max_level").getString(), textX, yPos,
                        MedievalGuiRenderer.PURPLE_REGAL);
            } else {
                renderMedievalTextLine(guiGraphics,
                        Component.translatable("gui.tharidiathings.realm.expansion_cost_label").getString(), textX,
                        yPos, MedievalGuiRenderer.BROWN_INK);
                yPos += 25;

                int stored = this.menu.getStoredPotatoes();
                int required = pietroEntity.getPotatoCostForNextLevel();
                float progress = required > 0 ? (float) stored / required : 0f;

                // Update and render medieval progress bar with relative coordinates
                expansionProgressBar.position(BORDER_WIDTH + 20, yPos).setProgress(progress).showValueText(stored,
                        required, "");
                expansionProgressBar.render(guiGraphics);

                yPos += 25;
                int remaining = required - stored;
                renderMedievalTextLine(guiGraphics,
                        Component.translatable("gui.tharidiathings.realm.coins_needed", remaining).getString(), textX,
                        yPos, MedievalGuiRenderer.BROWN_INK);
            }

            yPos += 30;
            MedievalGuiRenderer.renderMedievalDivider(guiGraphics, textX, yPos,
                    this.imageWidth - BORDER_WIDTH * 2 - 40);
            yPos += 25;

            // Instructions
            renderMedievalTextLine(guiGraphics,
                    Component.translatable("gui.tharidiathings.realm.deposit_hint_1").getString(), textX, yPos,
                    MedievalGuiRenderer.BROWN_INK);
            yPos += 15;
            renderMedievalTextLine(guiGraphics,
                    Component.translatable("gui.tharidiathings.realm.deposit_hint_2").getString(), textX, yPos,
                    MedievalGuiRenderer.BROWN_INK);
        }
    }

    /**
     * Renders text without shadow effect for cleaner medieval appearance
     */
    private void renderMedievalTextLine(GuiGraphics gui, String text, int x, int y, int color) {
        // Main text only - no shadow
        gui.drawString(Minecraft.getInstance().font, text, x, y, color);
    }

    private void renderClaimsTab(GuiGraphics guiGraphics) {
        // Use relative coordinates (0,0 = top-left of GUI)
        int yPos = 130;
        int textX = BORDER_WIDTH + 20;

        // Title with medieval styling
        renderMedievalTextLine(guiGraphics, Component.translatable("gui.tharidiathings.realm.claims_title").getString(),
                textX, yPos, MedievalGuiRenderer.BROWN_INK);
        yPos += 20;

        // Total coins with royal styling
        int totalPotatoes = this.menu.getTotalClaimPotatoes();
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.treasury", totalPotatoes).getString(), textX, yPos,
                MedievalGuiRenderer.ROYAL_GOLD);
        yPos += 30;

        // Render divider
        MedievalGuiRenderer.renderMedievalDivider(guiGraphics, textX, yPos, this.imageWidth - BORDER_WIDTH * 2 - 40);
        yPos += 25;

        // Hierarchy section with medieval styling
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.hierarchy_title").getString(), textX, yPos,
                MedievalGuiRenderer.BROWN_INK);
        yPos += 25;

        // Get hierarchy data from client cache
        Map<UUID, Integer> hierarchyData = ClientPacketHandler.getCachedHierarchyData();
        UUID ownerUUID = ClientPacketHandler.getCachedOwnerUUID();
        String ownerName = ClientPacketHandler.getCachedOwnerName();

        // Check if current player is the lord
        UUID currentPlayerUUID = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID()
                : null;
        boolean isLord = currentPlayerUUID != null && currentPlayerUUID.equals(ownerUUID);

        // Add lord to the list if available
        List<PlayerHierarchyEntry> entries = new ArrayList<>();
        if (ownerUUID != null) {
            entries.add(new PlayerHierarchyEntry(ownerUUID, ownerName, HierarchyRank.LORD.getLevel()));
        }

        // Add other vassals from hierarchy data
        for (Map.Entry<UUID, Integer> entry : hierarchyData.entrySet()) {
            String playerName = getPlayerNameFromUUID(entry.getKey());
            entries.add(new PlayerHierarchyEntry(entry.getKey(), playerName, entry.getValue()));
        }

        // Sort by rank level (descending)
        entries.sort((a, b) -> Integer.compare(b.rankLevel, a.rankLevel));

        // Display vassal list with medieval styling
        int displayStartIndex = scrollOffset;
        int displayEndIndex = Math.min(scrollOffset + MAX_VISIBLE_PLAYERS, entries.size());

        for (int i = displayStartIndex; i < displayEndIndex; i++) {
            PlayerHierarchyEntry entry = entries.get(i);
            HierarchyRank rank = HierarchyRank.fromLevel(entry.rankLevel);

            // Alternate background for medieval list effect
            boolean even = (i % 2 == 0);
            MedievalGuiRenderer.renderListItem(
                    guiGraphics,
                    textX - 5,
                    yPos - 2,
                    this.imageWidth - BORDER_WIDTH * 2 - 30,
                    18,
                    "",
                    even,
                    false);

            // Player name with medieval styling
            String displayName = entry.playerName.length() > 15 ? entry.playerName.substring(0, 15) : entry.playerName;
            renderMedievalTextLine(guiGraphics, "§f" + displayName, textX + 5, yPos + 4, MedievalGuiRenderer.BLACK_INK);

            // Rank title with royal colors
            String rankColor = getRankColor(rank);
            renderMedievalTextLine(guiGraphics,
                    rankColor + Component.translatable(getRankTranslationKey(rank)).getString(), textX + 120, yPos + 4,
                    MedievalGuiRenderer.BROWN_INK);

            // Change rank button (only for lord and not for themselves)
            if (isLord && !entry.playerUUID.equals(ownerUUID)) {
                renderMedievalRankButton(guiGraphics, textX + 200, yPos, entry.playerUUID);
            }

            yPos += 20;
        }

        // Medieval scroll indicators
        if (scrollOffset > 0) {
            MedievalGuiRenderer.renderScrollIndicator(guiGraphics, textX + this.imageWidth - BORDER_WIDTH * 2 - 50, 160,
                    true);
        }
        if (displayEndIndex < entries.size()) {
            MedievalGuiRenderer.renderScrollIndicator(guiGraphics, textX + this.imageWidth - BORDER_WIDTH * 2 - 50, 250,
                    false);
        }

        // Render rank selection menu if open
        if (showRankSelectionMenu && selectedPlayerForRankChange != null) {
            renderRankSelectionMenu(guiGraphics);
        }
    }

    /**
     * Renders a medieval-styled rank change button
     */
    private void renderMedievalRankButton(GuiGraphics gui, int x, int y, UUID playerUUID) {
        MedievalGuiRenderer.renderMedievalButton(gui, x, y, 45, 16,
                Component.translatable("gui.tharidiathings.realm.rank_change").getString(), false, true);
    }

    private void renderDungeonTab(GuiGraphics guiGraphics) {
        // Use relative coordinates (0,0 = top-left of GUI)
        int yPos = 130;
        int textX = BORDER_WIDTH + 20;

        // Title with medieval styling
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.dungeon_title").getString(), textX, yPos,
                MedievalGuiRenderer.BROWN_INK);
        yPos += 30;

        // Render divider
        MedievalGuiRenderer.renderMedievalDivider(guiGraphics, textX, yPos, this.imageWidth - BORDER_WIDTH * 2 - 40);
        yPos += 25;

        // Dungeon information
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.dungeon_enter_label").getString(), textX, yPos,
                MedievalGuiRenderer.BROWN_INK);
        yPos += 25;

        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.dungeon_desc_1").getString(), textX, yPos,
                MedievalGuiRenderer.BROWN_INK);
        yPos += 15;
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.dungeon_desc_2").getString(), textX, yPos,
                MedievalGuiRenderer.BROWN_INK);
        yPos += 30;

        // Render divider
        MedievalGuiRenderer.renderMedievalDivider(guiGraphics, textX, yPos, this.imageWidth - BORDER_WIDTH * 2 - 40);
        yPos += 25;

        // Status information
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.queue_status_label").getString(), textX, yPos,
                MedievalGuiRenderer.BROWN_INK);
        yPos += 20;
        renderMedievalTextLine(guiGraphics, Component.translatable("gui.tharidiathings.realm.queue_ready").getString(),
                textX + 20, yPos, MedievalGuiRenderer.PURPLE_REGAL);
    }

    @Override
    protected void rebuildWidgets() {
        super.rebuildWidgets();

        // Clear existing dungeon button if it exists
        if (enterDungeonButton != null) {
            this.removeWidget(enterDungeonButton);
            enterDungeonButton = null;
        }
        if (enterDungeonButtonGroup != null) {
            this.removeWidget(enterDungeonButtonGroup);
            enterDungeonButtonGroup = null;
        }

        // Add medieval-styled dungeon button only on dungeon tab
        if (currentTab == TAB_DUNGEON) {
            enterDungeonButton = MedievalButton.builder(
                    Component.translatable("gui.tharidiathings.realm.dungeon.enter_button"),
                    button -> {
                        // Send packet to join dungeon queue
                        sendDungeonJoinRequest();
                    },
                    MedievalButton.ButtonStyle.ROYAL)
                    .bounds(this.leftPos + PARCHMENT_WIDTH - 100, this.topPos + PARCHMENT_HEIGHT - 40, 80, 25).build();
            this.addRenderableWidget(enterDungeonButton);

            // Create Entra Gruppo button once, initially hidden
            enterDungeonButtonGroup = MedievalButton.builder(
                    Component.translatable("gui.tharidiathings.realm.dungeon.enter_group_button"),
                    button -> {
                        // Send packet to join dungeon queue
                        sendDungeonJoinRequest();
                    },
                    MedievalButton.ButtonStyle.ROYAL)
                    .bounds(this.leftPos + PARCHMENT_WIDTH - 220, this.topPos + PARCHMENT_HEIGHT - 40, 120, 25).build();
            enterDungeonButtonGroup.visible = false;
            this.addRenderableWidget(enterDungeonButtonGroup);
        }
    }

    private void renderRankSelectionMenu(GuiGraphics guiGraphics) {
        int menuWidth = 120;
        int menuHeight = 110;

        // Use stored menu position (set when button is clicked)
        int menuX = rankMenuX;
        int menuY = rankMenuY;

        // Medieval-styled popup menu
        MedievalGuiRenderer.renderParchmentBackground(guiGraphics, menuX, menuY, menuWidth, menuHeight);
        MedievalGuiRenderer.renderOrnateBorder(guiGraphics, menuX, menuY, menuWidth, menuHeight,
                MedievalGuiRenderer.BRONZE);

        // Title with medieval styling
        renderMedievalTextLine(guiGraphics,
                Component.translatable("gui.tharidiathings.realm.rank_select_title").getString(), menuX + 10,
                menuY + 10, MedievalGuiRenderer.BROWN_INK);

        // Rank options (except LORD)
        HierarchyRank[] selectableRanks = { HierarchyRank.CONSIGLIERE, HierarchyRank.GUARDIA, HierarchyRank.MILIZIANO,
                HierarchyRank.COLONO };
        int optionY = menuY + 30;

        for (HierarchyRank rank : selectableRanks) {
            String rankColor = getRankColor(rank);
            // Highlight on hover
            int relMouseX = (int) (Minecraft.getInstance().mouseHandler.xpos()
                    * Minecraft.getInstance().getWindow().getGuiScaledWidth()
                    / Minecraft.getInstance().getWindow().getScreenWidth());
            int relMouseY = (int) (Minecraft.getInstance().mouseHandler.ypos()
                    * Minecraft.getInstance().getWindow().getGuiScaledHeight()
                    / Minecraft.getInstance().getWindow().getScreenHeight());

            if (relMouseX >= menuX && relMouseX <= menuX + menuWidth && relMouseY >= optionY
                    && relMouseY <= optionY + 18) {
                MedievalGuiRenderer.renderListItem(guiGraphics, menuX + 5, optionY - 2, menuWidth - 10, 18, "", false,
                        true);
            }

            renderMedievalTextLine(guiGraphics,
                    rankColor + Component.translatable(getRankTranslationKey(rank)).getString(), menuX + 15,
                    optionY + 4, MedievalGuiRenderer.BLACK_INK);
            optionY += 20;
        }
    }

    private void switchTab(int tab) {
        currentTab = tab;
        scrollOffset = 0;
        updateTabButtons();
    }

    private void sendDungeonJoinRequest() {
        // Send packet to server to join dungeon queue
        DungeonQueuePacket packet = new DungeonQueuePacket();
        PacketDistributor.sendToServer(packet);
    }

    private String getRankColor(HierarchyRank rank) {
        switch (rank) {
            case LORD:
                return "§6§l";
            case CONSIGLIERE:
                return "§5§l";
            case GUARDIA:
                return "§b§l";
            case MILIZIANO:
                return "§7§l";
            case COLONO:
                return "§8§l";
            default:
                return "§f";
        }
    }

    private String getPlayerNameFromUUID(UUID uuid) {
        // Try to get player name from various sources
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(uuid)) {
            return Minecraft.getInstance().player.getName().getString();
        }

        // Check hierarchy cache
        Map<UUID, Integer> hierarchyData = ClientPacketHandler.getCachedHierarchyData();
        if (hierarchyData.containsKey(uuid)) {
            // This is a simplified approach - in a real implementation you'd want a proper
            // UUID->name mapping
            return Component.translatable("gui.tharidiathings.common.player_unknown", uuid.toString().substring(0, 8))
                    .getString();
        }

        return Component.translatable("gui.tharidiathings.common.unknown").getString();
    }

    private static String getRankTranslationKey(HierarchyRank rank) {
        return "gui.tharidiathings.realm.rank." + rank.name().toLowerCase(Locale.ROOT);
    }

    private static class PlayerHierarchyEntry {
        final UUID playerUUID;
        final String playerName;
        final int rankLevel;

        PlayerHierarchyEntry(UUID uuid, String name, int level) {
            this.playerUUID = uuid;
            this.playerName = name != null ? name
                    : Component.translatable("gui.tharidiathings.common.unknown").getString();
            this.rankLevel = level;
        }
    }
}
