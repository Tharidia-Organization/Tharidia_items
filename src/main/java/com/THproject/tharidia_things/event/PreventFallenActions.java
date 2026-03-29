package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import com.THproject.tharidia_things.features.Revive;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

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
    public static void onKeyMappingInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;
        ReviveAttachments revive = player.getData(ReviveAttachments.REVIVE_DATA.get());
        if (revive != null && revive.isFallen()) {
            if (event.isAttack() || event.isUseItem()) {
                event.setCanceled(true);
                event.setSwingHand(false); // For attack, prevent swing animation
            }
        }
    }

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
