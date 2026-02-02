package com.THproject.tharidia_things.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import com.THproject.tharidia_things.network.RightClickReleasePayload;
import com.THproject.tharidia_things.TharidiaThings;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class ClientMouseHandler {

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Post event) {
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_2 && event.getAction() == GLFW.GLFW_RELEASE) {
            // Invio del pacchetto al server
            PacketDistributor.sendToServer(new RightClickReleasePayload("Tasto rilasciato!"));
        }
    }
}