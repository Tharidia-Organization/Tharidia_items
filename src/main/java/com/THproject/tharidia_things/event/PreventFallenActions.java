package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.features.Revive;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

/**
 * Prevents mouse left/right click when player is fallen (ReviveAttachments)
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class PreventFallenActions {
    /**
     * Prevents attack (break block) and interact (place block) if player is fallen,
     * regardless of keybind.
     */
    @SubscribeEvent
    public static void preventMouseInput(InputEvent.InteractionKeyMappingTriggered event) {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;
        if (Revive.isPlayerFallen(player)) {
            if (event.isAttack() || event.isUseItem() || event.isPickBlock()) {
                event.setCanceled(true);
                event.setSwingHand(false); // For attack, prevent swing animation
            }
        }
    }

    @SubscribeEvent
    public static void onItemSwap(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        // keySwapOffhand is the 'F' key by default
        if (event.getKeyMapping() == mc.options.keySwapOffhand && Revive.isPlayerFallen(mc.player)) {
            event.setCanceled(true);
            event.setSwingHand(false); // Prevents the hand-swing animation
        }
    }

    /**
     * Prevents mouse wheel scrolling when player is fallen.
     */
    @SubscribeEvent
    public static void preventMouseScrolling(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;
        if (Revive.isPlayerFallen(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * Prevents opening inventory screen if player is fallen.
     */
    @SubscribeEvent
    public static void preventFallenOpenScreen(ScreenEvent.Opening event) {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;
        if (event.getScreen() instanceof InventoryScreen && Revive.isPlayerFallen(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * Prevent drop items when player is fallen.
     */
    @SubscribeEvent
    public static void preventFallenDropItem(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (Revive.isPlayerFallen(player)) {
            player.getInventory().add(event.getEntity().getItem());
            event.setCanceled(true);
        }
    }

    /**
     * Prevents movement input when player is fallen.
     */
    @SubscribeEvent
    public static void preventFallenMove(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            if (Revive.isPlayerFallen(player)) {
                event.getInput().forwardImpulse = 0;
                event.getInput().leftImpulse = 0;

                event.getInput().jumping = false;
                event.getInput().shiftKeyDown = false;
                event.getInput().up = false;
                event.getInput().down = false;
                event.getInput().left = false;
                event.getInput().right = false;
            }
        }
    }
}
