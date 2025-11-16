package com.tharidia.tharidia_things.client.gui;

import com.tharidia.tharidia_things.gui.BattleInviteMenu;
import com.tharidia.tharidia_things.network.BattleInviteResponsePacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class BattleInviteScreen extends AbstractContainerScreen<BattleInviteMenu> {

    public BattleInviteScreen(BattleInviteMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        // Set the size of our small GUI window
        this.imageWidth = 176;
        this.imageHeight = 80;
    }

    @Override
    protected void init() {
        super.init();

        // This is where you add buttons!
        // 'leftPos' and 'topPos' are the top-left corner of your GUI window

        // Accept Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Accept"),
                (button) -> {
                    // Send a packet to the server with "accepted = true"
                    var packet = new BattleInviteResponsePacket(this.menu.inviterUUID, true);
                    PacketDistributor.sendToServer(packet);
                    this.onClose(); // Close the GUI
                })
                .bounds(this.leftPos + 30, this.topPos + 40, 50, 20) // x, y, width, height
                .build());

        // Decline Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Decline"),
                (button) -> {
                    // Send a packet to the server with "accepted = false"
                    var packet = new BattleInviteResponsePacket(this.menu.inviterUUID, false);
                    PacketDistributor.sendToServer(packet);
                    this.onClose(); // Close the GUI
                })
                .bounds(this.leftPos + 96, this.topPos + 40, 50, 20) // x, y, width, height
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Renders the default dark transparent background
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Renders tooltips if you hover over anything (not needed, but good)
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // We don't have a background texture, so this can be empty.
        // The 'renderBackground' in the render() method handles the darkening.
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw the text on the screen

        // Draw the title ("Battle Invitation")
        graphics.drawString(this.font,
                this.title,
                (this.imageWidth / 2 - this.font.width(this.title) / 2),
                10,
                0x404040, // Dark gray color
                false); // No shadow

        // Get the inviter's name from the menu
        Component inviteText = Component.literal("Invite from: ").append(this.menu.getInviterName());

        // Draw the "Invite from: [Player]" text
        graphics.drawString(this.font,
                inviteText,
                (this.imageWidth / 2 - this.font.width(inviteText) / 2),
                25,
                0x404040,
                false);
    }
}