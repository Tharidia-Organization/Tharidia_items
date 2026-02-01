package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.client.video.VideoScreenRenderHandler;
import com.THproject.tharidia_things.client.video.VideoToolsManager;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.stamina.StaminaAttachments;
import com.THproject.tharidia_things.stamina.StaminaData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
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
        
        // Update video players
        VideoScreenRenderHandler.onClientTick();
        
        applyStaminaToExperienceBar(mc);
    }
    
    private static void applyStaminaToExperienceBar(Minecraft mc) {
        if (mc.options.hideGui) {
            return;
        }
        
        var player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        
        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);
        float max = data.getMaxStamina();
        if (max <= 0.0f) {
            return;
        }
        
        float ratio = Mth.clamp(data.getCurrentStamina() / max, 0.0f, 1.0f);
        player.experienceProgress = ratio;
    }
}
