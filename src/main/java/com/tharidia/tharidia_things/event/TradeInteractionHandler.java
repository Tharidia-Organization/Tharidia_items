package com.tharidia.tharidia_things.event;

import com.tharidia.tharidia_things.Config;
import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.network.TradeRequestPacket;
import com.tharidia.tharidia_things.trade.TradeManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

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
            serverInitiator.sendSystemMessage(Component.literal("§cSiete già impegnati in uno scambio!"));
            event.setCanceled(true);
            return;
        }

        if (TradeManager.isPlayerInTrade(targetPlayer.getUUID())) {
            serverInitiator.sendSystemMessage(Component.literal("§c" + targetPlayer.getName().getString() + " è già impegnato in uno scambio!"));
            event.setCanceled(true);
            return;
        }

        // Check if target already has a pending request
        if (TradeManager.hasPendingRequest(targetPlayer.getUUID())) {
            serverInitiator.sendSystemMessage(Component.literal("§c" + targetPlayer.getName().getString() + " ha già una richiesta di scambio in sospeso!"));
            event.setCanceled(true);
            return;
        }

        // Create trade request
        TradeManager.createTradeRequest(serverInitiator, targetPlayer);

        // Send packet to target player to show request screen
        PacketDistributor.sendToPlayer(targetPlayer, new TradeRequestPacket(
            serverInitiator.getUUID(),
            serverInitiator.getName().getString()
        ));

        // Notify initiator
        serverInitiator.sendSystemMessage(Component.literal("§6Richiesta di scambio inviata a " + targetPlayer.getName().getString()));

        event.setCanceled(true);
    }

    private static boolean isCurrencyItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        List<? extends String> currencyItems = Config.TRADE_CURRENCY_ITEMS.get();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        return currencyItems.stream()
                .anyMatch(currency -> {
                    try {
                        ResourceLocation currencyId = ResourceLocation.parse(currency);
                        return currencyId.equals(itemId);
                    } catch (Exception e) {
                        TharidiaThings.LOGGER.warn("Invalid currency item in config: {}", currency);
                        return false;
                    }
                });
    }
}
