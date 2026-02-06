package com.THproject.tharidia_things.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import com.THproject.tharidia_things.network.RightClickReleasePayload;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.ReviveAttachments;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class ClientMouseHandler {

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Post event) {
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_2 && event.getAction() == GLFW.GLFW_RELEASE) {
            // Client side reset logic for Revive
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.getConnection() != null) {
                ReviveAttachments playerAttachments = mc.player
                        .getData(com.THproject.tharidia_things.compoundTag.ReviveAttachments.REVIVE_DATA.get());

                java.util.UUID revivingUUID = playerAttachments.getRevivingPlayer();
                if (revivingUUID != null) {
                    net.minecraft.world.entity.Entity targetEntity = mc.player.level().getPlayerByUUID(revivingUUID);
                    if (targetEntity instanceof net.minecraft.world.entity.player.Player targetPlayer) {
                        ReviveAttachments targetAttachments = targetPlayer
                                .getData(com.THproject.tharidia_things.compoundTag.ReviveAttachments.REVIVE_DATA.get());
                        targetAttachments.resetResTime();
                    }
                    playerAttachments.setRevivingPlayer(null);
                }

                // Invio del pacchetto al server
                PacketDistributor.sendToServer(new RightClickReleasePayload("Tasto rilasciato!"));
            }
        }
    }
}