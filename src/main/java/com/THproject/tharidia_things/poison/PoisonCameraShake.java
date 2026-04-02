package com.THproject.tharidia_things.poison;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class PoisonCameraShake {
    @SubscribeEvent
    public static void shakeCamera(ViewportEvent.ComputeCameraAngles event) {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;

        PoisonAttachments attachment = PoisonHelper.getAttachment(player);
        if (attachment == null)
            return;

        float progress = attachment.getProgress();
        player.displayClientMessage(Component.literal(String.format("%.2f", progress)), true);
        if (progress >= 0.6) {
            float partialTicks = (float) event.getPartialTick();
            float ticks = player.tickCount + partialTicks;
            progress -= 0.6f;
            float intensity = 10 * progress;

            float shakePitch = (float) Math.sin(ticks * 0.3f) * intensity;
            float shakeRoll = (float) Math.cos(ticks * 0.2f) * intensity;

            event.setPitch(event.getPitch() + shakePitch);
            event.setRoll(event.getRoll() + shakeRoll);
        }
    }
}
