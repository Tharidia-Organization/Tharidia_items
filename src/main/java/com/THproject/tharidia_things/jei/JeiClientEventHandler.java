package com.THproject.tharidia_things.jei;

import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Client-side NeoForge event listener that drives periodic JEI tag filter checks.
 *
 * Registered manually from {@code TharidiaThings} only when JEI is loaded,
 * so this class is never instantiated in environments without JEI.
 */
public class JeiClientEventHandler {

    private static int tickCounter = 0;

    /** Checks for tag changes every 60 ticks (~3 s). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer)) return;
        if (++tickCounter < 60) return;
        tickCounter = 0;
        JeiTagFilterManager.checkAndApplyFilter();
    }

    /** Restores full JEI visibility when the local player disconnects. */
    @SubscribeEvent
    public static void onPlayerLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        tickCounter = 0;
        JeiTagFilterManager.onPlayerLeave();
    }
}
