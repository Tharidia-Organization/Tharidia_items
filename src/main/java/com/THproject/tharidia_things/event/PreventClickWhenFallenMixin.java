package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Prevents mouse left/right click when player is fallen (ReviveAttachments)
 */
@EventBusSubscriber(modid = com.THproject.tharidia_things.TharidiaThings.MODID, value = Dist.CLIENT)
public class PreventClickWhenFallenMixin {
    /**
     * Prevents attack (break block) and interact (place block) if player is fallen, regardless of keybind.
     */
    @SubscribeEvent
    public static void onKeyMappingInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        ReviveAttachments revive = player.getData(ReviveAttachments.REVIVE_DATA.get());
        if (revive != null && revive.isFallen()) {
            if (event.isAttack() || event.isUseItem()) {
                event.setCanceled(true);
                event.setSwingHand(false); // For attack, prevent swing animation
            }
        }
    }
}
