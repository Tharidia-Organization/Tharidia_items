package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.util.PlayerNameHelper;
import com.THproject.tharidia_things.gui.TradeMenu;
import com.THproject.tharidia_things.trade.TradeManager;
import com.THproject.tharidia_things.trade.TradeSession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Prevents players from opening their inventory or other containers while in a trade
 * Also prevents movement during trade (bypasses invmove mod)
 */
public class TradeInventoryBlocker {

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        
        // Only check on server side
        if (player.level().isClientSide()) {
            return;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        
        // Check if player is in a trade
        if (TradeManager.isPlayerInTrade(serverPlayer.getUUID())) {
            // Allow opening the trade menu itself
            if (event.getContainer() instanceof TradeMenu) {
                return;
            }
            
            // Block opening any other container (including inventory)
            serverPlayer.sendSystemMessage(Component.literal("§cNon puoi aprire l'inventario durante uno scambio!"));
            serverPlayer.closeContainer();
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Check if player is in a trade
            if (TradeManager.isPlayerInTrade(player.getUUID())) {
                // Cancel any horizontal movement (keep vertical for gravity)
                // This bypasses invmove mod by setting movement to zero every tick
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                
                // Also reset input flags to prevent movement
                player.setShiftKeyDown(false);
                player.setSprinting(false);
            }
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        Player player = event.getEntity();
        
        // Only check on server side
        if (player.level().isClientSide()) {
            return;
        }
        
        ServerPlayer serverPlayer = (ServerPlayer) player;
        
        // Check if player is still in a trade session
        TradeSession session = TradeManager.getPlayerSession(serverPlayer.getUUID());
        if (session != null) {
            // If the closed container was a trade menu, cancel the trade
            if (event.getContainer() instanceof TradeMenu) {
                // Trade was closed without completion - cancel it
                TradeManager.cancelSession(session.getSessionId());
                
                ServerPlayer otherPlayer = session.getOtherPlayer(serverPlayer.getUUID());
                if (otherPlayer != null) {
                    String playerName = PlayerNameHelper.getChosenName(serverPlayer);
                    otherPlayer.closeContainer();
                    otherPlayer.sendSystemMessage(Component.literal("§c" + playerName + " ha chiuso il menu di scambio."));
                }
                
                serverPlayer.sendSystemMessage(Component.literal("§7Scambio annullato."));
            }
        }
    }
}
