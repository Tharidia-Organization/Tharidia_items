package com.tharidia.tharidia_things.client;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.block.entity.PietroBlockEntity;
import com.tharidia.tharidia_things.realm.RealmManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class RealmClientHandler {
    private static boolean wasInRealm = false;
    private static boolean isInRealm = false;
    private static String currentRealmOwner = "";

    /**
     * Gets the current realm status for rendering
     */
    public static boolean isPlayerInRealm() {
        return isInRealm;
    }

    /**
     * Gets the current realm owner name
     */
    public static String getCurrentRealmOwner() {
        return currentRealmOwner;
    }
    
    @SubscribeEvent
    public static void onClientPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            Minecraft mc = Minecraft.getInstance();
            
            // Check if we're in single player or on a server
            if (mc.level != null && !mc.level.isClientSide) {
                return; // Skip if somehow called on server side
            }
            
            // We need to check realm status
            // Note: On client side, we need to track this differently
            // For now, we'll use a simple position check
            // In a full implementation, you'd want to sync this data from server
            
            checkRealmStatus(player);
        }
    }
    
    private static void checkRealmStatus(LocalPlayer player) {
        // Update the realm status
        wasInRealm = isInRealm;

        // For client-side, we need to check against server data
        // This is a simplified check - in production you'd sync this from server
        Minecraft mc = Minecraft.getInstance();
        PietroBlockEntity currentRealm = null;

        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            // Single player - we can access server data
            ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(player.level().dimension());
            if (serverLevel != null) {
                currentRealm = RealmManager.getRealmAt(serverLevel, player.blockPosition());
                isInRealm = currentRealm != null;

                if (isInRealm && currentRealm != null) {
                    currentRealmOwner = currentRealm.getOwnerName();
                }
            } else {
                isInRealm = false;
            }
        } else {
            // Multiplayer - would need packet sync (to be implemented)
            // For now, just maintain current state
        }

        // Check if status changed
        if (isInRealm && !wasInRealm) {
            // Just entered realm
            if (!currentRealmOwner.isEmpty()) {
                player.displayClientMessage(Component.literal("§6Sei nel regno di " + currentRealmOwner), false);
            } else {
                player.displayClientMessage(Component.literal("§6Sei nel regno"), false);
            }
        } else if (!isInRealm && wasInRealm) {
            // Just left realm
            player.displayClientMessage(Component.literal("§7Hai lasciato il regno."), false);
            currentRealmOwner = "";
        }
    }
    
    /**
     * Resets the realm status (useful for dimension changes)
     */
    public static void reset() {
        wasInRealm = false;
        isInRealm = false;
    }
}
