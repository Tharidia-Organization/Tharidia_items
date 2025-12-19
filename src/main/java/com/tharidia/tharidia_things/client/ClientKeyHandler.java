package com.tharidia.tharidia_things.client;

import com.tharidia.tharidia_things.TharidiaThings;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "tharidiathings", value = Dist.CLIENT)
public class ClientKeyHandler {
    private static long lastPress = 0;
    
    static {
        TharidiaThings.LOGGER.info("[VIDEO TOOLS] ClientKeyHandler loaded - F10 will show installer GUI");
    }
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        
        if (mc.player == null) {
            return;
        }
        
        // Check if toggle claim boundaries key was pressed
        if (KeyBindings.TOGGLE_CLAIM_BOUNDARIES.consumeClick()) {
            ClaimBoundaryRenderer.toggleBoundaries();
            RealmBoundaryRenderer.toggleBoundaries();
        }
        
        // Debug: Press F10 to force-show video tools installation GUI
        if (org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getWindow(), GLFW.GLFW_KEY_F10) == GLFW.GLFW_PRESS) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPress > 500) { // 500ms debounce
                lastPress = currentTime;
                TharidiaThings.LOGGER.info("[VIDEO TOOLS] F10 key pressed - triggering GUI");
                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e[DEBUG] Showing video tools installation GUI"));
                com.tharidia.tharidia_things.client.video.VideoToolsManager.getInstance().forceShowInstallationGUI();
            }
        }
        
        // Update video players
        com.tharidia.tharidia_things.client.video.VideoScreenRenderHandler.onClientTick();
    }
}
