package com.THproject.tharidia_things.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.util.TriState;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/**
 * Client-side handler for nametag visibility and custom name display
 * Only players with permission level 4 (op) can see nametags
 * Displays nametags in format: MinecraftName_CustomName
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = "tharidiathings")
public class NametagVisibilityHandler {
    
    /**
     * Client-side event that intercepts nametag rendering
     * Only allows nametags to be rendered if the viewing player has permission level 4
     * Modifies the nametag to show both Minecraft username and custom chosen name
     */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        // Get the client player (the one viewing)
        Minecraft mc = Minecraft.getInstance();
        Player viewer = mc.player;
        
        if (viewer == null) {
            // No viewer, hide nametag completely (no background)
            event.setCanRender(TriState.FALSE);
            return;
        }
        
        // Check if the viewer has permission level 4 (op)
        // In client-side, we check if the player has permission to use commands
        boolean isAdmin = viewer.hasPermissions(4);
        
        // Debug logging to check if event fires behind walls
        if (!isAdmin && event.getEntity() instanceof Player) {
            System.out.println("[NametagDebug] RenderNameTagEvent fired for player: " + 
                event.getEntity().getName().getString());
        }
        
        if (!isAdmin) {
            // Non-admin players cannot see ANY nametags - deny rendering completely
            event.setCanRender(TriState.FALSE);
        } else {
            // Admin can see nametags - modify the content to show MinecraftName_CustomName
            if (event.getEntity() instanceof Player targetPlayer) {
                String minecraftName = targetPlayer.getName().getString();
                Component customName = targetPlayer.getCustomName();
                
                if (customName != null) {
                    String chosenName = customName.getString();
                    // Format: MinecraftName_CustomName
                    Component newNameTag = Component.literal(minecraftName + "_" + chosenName);
                    event.setContent(newNameTag);
                }
            }
        }
    }
}
