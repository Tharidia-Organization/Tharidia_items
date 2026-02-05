package com.THproject.tharidia_things.client.gui;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.gui.ArmorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ArmorScreen extends AbstractContainerScreen<ArmorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TharidiaThings.MODID, "textures/gui/armor_gui.png");
    private float xMouse;
    private float yMouse;

    public ArmorScreen(ArmorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 172;
        this.imageHeight = 177;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Do not render "Inventory" or title labels
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Example position for player render
        int playerX = x + 123;
        int playerY = y + 74;
        int scale = 30;

        renderEntityInInventoryFollowsMouse(guiGraphics, playerX, playerY, scale, (float) playerX - this.xMouse,
                (float) (playerY - 50) - this.yMouse, this.minecraft.player);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.xMouse = (float) mouseX;
        this.yMouse = (float) mouseY;
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * Helper method to render the entity following the mouse cursor,
     * similar to standard InventoryScreen logic.
     */
    private void renderEntityInInventoryFollowsMouse(GuiGraphics guiGraphics, int x, int y, int scale, float mouseX,
            float mouseY, net.minecraft.world.entity.LivingEntity entity) {
        float f = (float) Math.atan((double) (mouseX / 40.0F));
        float f1 = (float) Math.atan((double) (mouseY / 40.0F));
        Quaternionf quaternionf = (new Quaternionf()).rotateZ((float) Math.PI);
        Quaternionf quaternionf1 = (new Quaternionf()).rotateX(f1 * 20.0F * ((float) Math.PI / 180F));
        quaternionf.mul(quaternionf1);
        float f2 = entity.yBodyRot;
        float f3 = entity.getYRot();
        float f4 = entity.getXRot();
        float f5 = entity.yHeadRotO;
        float f6 = entity.yHeadRot;
        entity.yBodyRot = 180.0F + f * 20.0F;
        entity.setYRot(180.0F + f * 40.0F);
        entity.setXRot(-f1 * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        InventoryScreen.renderEntityInInventory(guiGraphics, (float) x, (float) y, (float) scale, new Vector3f(),
                quaternionf, null, entity);

        entity.yBodyRot = f2;
        entity.setYRot(f3);
        entity.setXRot(f4);
        entity.yHeadRotO = f5;
        entity.yHeadRot = f6;
    }
}
