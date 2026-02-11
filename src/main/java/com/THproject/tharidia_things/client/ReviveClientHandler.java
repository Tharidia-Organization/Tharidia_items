package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.client.screen.FallenScreen;
import com.THproject.tharidia_things.compoundTag.ReviveAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ReviveClientHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) {
            return;
        }

        var reviveData = player.getData(ReviveAttachments.REVIVE_DATA);
        if (reviveData == null) {
            return;
        }

        Screen currentScreen = mc.screen;

        if (reviveData.isFallen() && reviveData.canRevive()) {
            if (!(currentScreen instanceof FallenScreen) && !(currentScreen instanceof ChatScreen)) {
                mc.setScreen(new FallenScreen());
            }
        } else {
            // If player is NOT fallen but in ReviveScreen, close it
            if (currentScreen instanceof FallenScreen) {
                mc.setScreen(null);
            }
        }
    }
}
