package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Client-side input handler that blocks attack input BEFORE the swing animation.
 * This prevents the visual swing when attacking with restricted weapons.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class ClientInputHandler {
    
    /**
     * Intercepts attack input (left click) before swing animation
     */
    @SubscribeEvent
    public static void onAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
        // Only care about attack key
        if (event.isAttack()) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            
            var mainHand = player.getMainHandItem();
            
            // Check if the weapon is blocked in client cache
            if (ClientGateCache.isBlocked(mainHand)) {
                // Cancel the input -> NO swing animation
                event.setCanceled(true);
                event.setSwingHand(false);
                
                TharidiaThings.LOGGER.debug(
                    "Client blocked attack input with {} (cached as restricted)",
                    mainHand.getItem()
                );
            }
        }
    }
}
