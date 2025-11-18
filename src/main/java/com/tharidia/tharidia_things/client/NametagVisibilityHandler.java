package com.tharidia.tharidia_things.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/**
 * Client-side handler for nametag visibility
 * Only players with permission level 4 (op) can see nametags
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = "tharidiathings")
public class NametagVisibilityHandler {
    
    /**
     * Client-side event that intercepts nametag rendering
     * Only allows nametags to be rendered if the viewing player has permission level 4
     */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        // Get the client player (the one viewing)
        Minecraft mc = Minecraft.getInstance();
        Player viewer = mc.player;
        
        if (viewer == null) {
            // No viewer, hide nametag to be safe
            event.setContent(null);
            return;
        }
        
        // Check if the viewer has permission level 4 (op)
        // In client-side, we check if the player has permission to use commands
        boolean isAdmin = viewer.hasPermissions(4);
        
        if (!isAdmin) {
            // Non-admin players cannot see ANY nametags - set content to null to hide
            event.setContent(null);
        }
        // If isAdmin is true, the nametag renders normally with its original content
    }
}
