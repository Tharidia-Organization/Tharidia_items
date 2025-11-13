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
            String targetName = com.tharidia.tharidia_things.util.PlayerNameHelper.getChosenName(targetPlayer);
            serverInitiator.sendSystemMessage(Component.literal("§c" + targetName + " è già impegnato in uno scambio!"));
            event.setCanceled(true);
            return;
        }

        // Check if target already has a pending request
        if (TradeManager.hasPendingRequest(targetPlayer.getUUID())) {
            String targetName = com.tharidia.tharidia_things.util.PlayerNameHelper.getChosenName(targetPlayer);
            serverInitiator.sendSystemMessage(Component.literal("§c" + targetName + " ha già una richiesta di scambio in sospeso!"));
            event.setCanceled(true);
            return;
        }

        // Create trade request
        TradeManager.createTradeRequest(serverInitiator, targetPlayer);

        // Send packet to target player to show request screen
        String initiatorName = com.tharidia.tharidia_things.util.PlayerNameHelper.getChosenName(serverInitiator);
        String targetName = com.tharidia.tharidia_things.util.PlayerNameHelper.getChosenName(targetPlayer);
        
        PacketDistributor.sendToPlayer(targetPlayer, new TradeRequestPacket(
            serverInitiator.getUUID(),
            initiatorName
        ));

        // Notify initiator
        serverInitiator.sendSystemMessage(Component.literal("§6Richiesta di scambio inviata a " + targetName));

        event.setCanceled(true);
    }

    private static boolean isCurrencyItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemIdString = itemId.toString();
        
        // Check if it's a Numismatic Overhaul coin or money bag
        if (itemIdString.equals("numismaticoverhaul:bronze_coin") ||
            itemIdString.equals("numismaticoverhaul:silver_coin") ||
            itemIdString.equals("numismaticoverhaul:gold_coin") ||
            itemIdString.equals("numismaticoverhaul:money_bag")) {
            return true;
        }
        
        // Check config list
        List<? extends String> currencyItems = Config.TRADE_CURRENCY_ITEMS.get();
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
