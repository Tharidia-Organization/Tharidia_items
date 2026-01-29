package com.THproject.tharidia_things.event;

import com.THproject.tharidia_things.util.CurrencyHelper;
import com.THproject.tharidia_things.util.PlayerNameHelper;
import com.THproject.tharidia_things.network.TradeRequestPacket;
import com.THproject.tharidia_things.trade.TradeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles player right-click interactions to initiate trades
 */
public class TradeInteractionHandler {

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        // Only handle on server side
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Check if target is a player
        if (!(event.getTarget() instanceof ServerPlayer targetPlayer)) {
            return;
        }

        Player initiator = event.getEntity();
        if (!(initiator instanceof ServerPlayer serverInitiator)) {
            return;
        }

        // Don't allow self-trading
        if (initiator.getUUID().equals(targetPlayer.getUUID())) {
            return;
        }

        // Check if initiator is holding a currency item
        ItemStack heldItem = initiator.getItemInHand(event.getHand());
        if (!isCurrencyItem(heldItem)) {
            return;
        }

        // Check if either player is already in a trade
        if (TradeManager.isPlayerInTrade(serverInitiator.getUUID())) {
            serverInitiator.sendSystemMessage(Component.translatable("message.tharidiathings.trade.already_in_trade"));
            event.setCanceled(true);
            return;
        }

        if (TradeManager.isPlayerInTrade(targetPlayer.getUUID())) {
            String targetName = PlayerNameHelper.getChosenName(targetPlayer);
            serverInitiator.sendSystemMessage(Component.translatable("message.tharidiathings.trade.target_in_trade", targetName));
            event.setCanceled(true);
            return;
        }

        // Check if target already has a pending request
        if (TradeManager.hasPendingRequest(targetPlayer.getUUID())) {
            String targetName = PlayerNameHelper.getChosenName(targetPlayer);
            serverInitiator.sendSystemMessage(Component.translatable("message.tharidiathings.trade.target_has_pending", targetName));
            event.setCanceled(true);
            return;
        }

        // Create trade request (synchronized to prevent race conditions)
        String targetName = PlayerNameHelper.getChosenName(targetPlayer);
        if (!TradeManager.createTradeRequest(serverInitiator, targetPlayer)) {
            serverInitiator.sendSystemMessage(Component.translatable("message.tharidiathings.trade.request_failed"));
            event.setCanceled(true);
            return;
        }

        // Send packet to target player to show request screen
        String initiatorName = PlayerNameHelper.getChosenName(serverInitiator);

        PacketDistributor.sendToPlayer(targetPlayer, new TradeRequestPacket(
            serverInitiator.getUUID(),
            initiatorName
        ));

        // Notify initiator
        serverInitiator.sendSystemMessage(Component.translatable("message.tharidiathings.trade.request_sent", targetName));

        event.setCanceled(true);
    }

    private static boolean isCurrencyItem(ItemStack stack) {
        return CurrencyHelper.isCurrencyItem(stack);
    }
}
