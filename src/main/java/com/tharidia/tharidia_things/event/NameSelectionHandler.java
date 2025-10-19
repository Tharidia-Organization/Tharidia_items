package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.gui.NameSelectionMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles name selection on player login (SERVER SIDE ONLY)
 * Requires tharidia_tweaks mod to be present on the server
 */
public class NameSelectionHandler {
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Only run on server side
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Check if player needs to choose a name
            try {
                // Use reflection to check if NameService is available (server-side only dependency)
                Class<?> nameServiceClass = Class.forName("com.tharidia.tharidia_tweaks.name.NameService");
                java.lang.reflect.Method needsToChooseNameMethod = nameServiceClass.getMethod("needsToChooseName", ServerPlayer.class);
                
                boolean needsName = (boolean) needsToChooseNameMethod.invoke(null, serverPlayer);
                
                if (needsName) {
                    TharidiaThings.LOGGER.info("Player {} needs to choose a name, opening GUI", 
                        serverPlayer.getName().getString());
                    
                    // Open name selection GUI after a short delay to ensure player is fully loaded
                    serverPlayer.getServer().execute(() -> {
                        try {
                            Thread.sleep(500); // Wait 500ms for player to be fully loaded
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        
                        serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inv, player) -> new NameSelectionMenu(id, inv),
                            Component.translatable("gui.tharidiathings.name_selection")
                        ));
                        
                        // Send a message to the player
                        serverPlayer.sendSystemMessage(
                            Component.literal("§eWelcome! Please choose your display name before continuing.")
                        );
                    });
                }
            } catch (ClassNotFoundException e) {
                TharidiaThings.LOGGER.error("tharidia_tweaks mod not found! NameService is required for name selection functionality.", e);
            } catch (Exception e) {
                TharidiaThings.LOGGER.error("Error checking if player needs to choose name", e);
            }
        }
    }
}
