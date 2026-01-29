package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.trade.MarketBridge;
import com.THproject.tharidia_things.util.CurrencyHelper;
import com.THproject.tharidia_things.util.PlayerNameHelper;
import com.THproject.tharidia_things.Config;
import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.gui.TradeMenu;
import com.THproject.tharidia_things.trade.TradeManager;
import com.THproject.tharidia_things.trade.TradeSession;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles server-side processing of trade packets
 */
public class TradePacketHandler {

    public static void handleTradeResponse(TradeResponsePacket packet, ServerPlayer player) {
        if (packet.accepted()) {
            // Accept the trade request
            TradeSession session = TradeManager.acceptTradeRequest(player);

            if (session == null) {
                player.sendSystemMessage(Component.translatable("message.tharidiathings.trade.request_invalid"));
                return;
            }

            // Open trade menu for both players
            ServerPlayer requester = session.getPlayer1();
            ServerPlayer target = session.getPlayer2();

            openTradeMenu(requester, session);
            openTradeMenu(target, session);

            String targetName = PlayerNameHelper.getChosenName(target);
            String requesterName = PlayerNameHelper.getChosenName(requester);
            requester.sendSystemMessage(Component.translatable("message.tharidiathings.trade.accepted_by", targetName));
            target.sendSystemMessage(Component.translatable("message.tharidiathings.trade.started_with", requesterName));
        } else {
            // Decline the trade request
            TradeManager.declineTradeRequest(player);

            // Notify the requester
            ServerPlayer requester = player.getServer().getPlayerList().getPlayer(packet.requesterId());
            if (requester != null) {
                String playerName = PlayerNameHelper.getChosenName(player);
                requester.sendSystemMessage(Component.translatable("message.tharidiathings.trade.declined_by", playerName));
            }
        }
    }

    public static void handleTradeUpdate(TradeUpdatePacket packet, ServerPlayer player) {
        TradeSession session = TradeManager.getPlayerSession(player.getUUID());

        if (session == null) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.trade.no_active_session"));
            return;
        }

        // Validate items from client
        List<ItemStack> validatedItems = validateAndSanitizeItems(packet.items(), player);

        // Update player's items
        session.updatePlayerItems(player.getUUID(), validatedItems);
        session.setPlayerConfirmed(player.getUUID(), packet.confirmed());

        // Notify other player via packet
        ServerPlayer otherPlayer = session.getOtherPlayer(player.getUUID());
        if (otherPlayer != null) {
            // Calculate dynamic tax rate for the receiving player (needs to be done before menu update)
            List<ItemStack> itemsToSend = packet.confirmed() ? validatedItems : List.of();
            int currencyAmount = getTotalCurrency(itemsToSend);
            List<ItemStack> nonCurrencyItems = getNonCurrencyItems(session.getPlayerItems(player.getUUID()));
            double taxRate = getDynamicTaxRate(otherPlayer.getUUID(), currencyAmount, nonCurrencyItems);
            int taxAmount = calculateTaxAmount(itemsToSend, taxRate);

            // Update other player's menu server-side
            if (otherPlayer.containerMenu instanceof TradeMenu otherMenu) {
                otherMenu.setOtherPlayerConfirmed(packet.confirmed());

                // Only show items when confirmed
                if (packet.confirmed()) {
                    updateMenuItems(otherMenu.getOtherPlayerOffer(), validatedItems, taxRate);
                } else {
                    // Clear items if unconfirmed
                    clearMenuItems(otherMenu.getOtherPlayerOffer());
                }
            }

            PacketDistributor.sendToPlayer(otherPlayer, new TradeSyncPacket(
                itemsToSend,
                packet.confirmed(),
                session.isPlayerFinalConfirmed(player.getUUID()),
                taxRate,
                taxAmount
            ));
        }
    }

    public static void handleTradeFinalConfirm(TradeFinalConfirmPacket packet, ServerPlayer player) {
        TradeSession session = TradeManager.getPlayerSession(player.getUUID());
        
        if (session == null) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.trade.no_active_session"));
            return;
        }

        // Check if both players have confirmed their items first
        if (!session.isBothConfirmed()) {
            player.sendSystemMessage(Component.translatable("message.tharidiathings.trade.both_must_confirm"));
            return;
        }

        // Update final confirmation
        session.setPlayerFinalConfirmed(player.getUUID(), packet.confirmed());

        // Notify other player
        ServerPlayer otherPlayer = session.getOtherPlayer(player.getUUID());
        if (otherPlayer != null) {
            if (otherPlayer.containerMenu instanceof TradeMenu otherMenu) {
                otherMenu.setOtherPlayerFinalConfirmed(packet.confirmed());
            }
            
            // Send sync packet to update other player's client
            List<ItemStack> playerItems = session.getPlayerItems(player.getUUID());

            // Calculate dynamic tax rate for the receiving player
            int currencyAmount = getTotalCurrency(playerItems);
            List<ItemStack> nonCurrencyItems = getNonCurrencyItems(session.getPlayerItems(otherPlayer.getUUID()));
            double taxRate = getDynamicTaxRate(otherPlayer.getUUID(), currencyAmount, nonCurrencyItems);
            int taxAmount = calculateTaxAmount(playerItems, taxRate);

            PacketDistributor.sendToPlayer(otherPlayer, new TradeSyncPacket(
                playerItems,
                session.isPlayerConfirmed(player.getUUID()),
                packet.confirmed(),
                taxRate,
                taxAmount
            ));
        }

        // Check if both players final confirmed
        if (session.isBothFinalConfirmed()) {
            completeTrade(session);
        }
    }

    public static void handleTradeCancel(TradeCancelPacket packet, ServerPlayer player) {
        TradeSession session = TradeManager.getPlayerSession(player.getUUID());
        
        if (session == null) {
            return;
        }

        // Notify other player
        ServerPlayer otherPlayer = session.getOtherPlayer(player.getUUID());
        if (otherPlayer != null) {
            String playerName = PlayerNameHelper.getChosenName(player);
            otherPlayer.closeContainer();
            otherPlayer.sendSystemMessage(Component.translatable("message.tharidiathings.trade.cancelled_by", playerName));
        }

        // Close player's menu
        player.closeContainer();
        player.sendSystemMessage(Component.translatable("message.tharidiathings.trade.cancelled"));

        // Remove session
        TradeManager.cancelSession(session.getSessionId());
    }

    private static void openTradeMenu(ServerPlayer player, TradeSession session) {
        ServerPlayer otherPlayer = session.getOtherPlayer(player.getUUID());
        String otherPlayerName = PlayerNameHelper.getChosenName(otherPlayer);
        
        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, p) -> new TradeMenu(containerId, playerInventory, session, p),
            Component.translatable("gui.tharidiathings.trade.title")
        ), buf -> {
            buf.writeUUID(session.getSessionId());
            buf.writeUUID(otherPlayer.getUUID());
            buf.writeUtf(otherPlayerName);
        });
    }

    private static void completeTrade(TradeSession session) {
        ServerPlayer player1 = session.getPlayer1();
        ServerPlayer player2 = session.getPlayer2();

        // Null safety check - players might have disconnected
        if (player1 == null || player2 == null) {
            TharidiaThings.LOGGER.warn("Trade completion failed: one or both players disconnected");
            TradeManager.cancelSession(session.getSessionId());
            // Notify the player that is still connected
            if (player1 != null) {
                player1.sendSystemMessage(Component.translatable("message.tharidiathings.trade.other_disconnected"));
                player1.closeContainer();
            }
            if (player2 != null) {
                player2.sendSystemMessage(Component.translatable("message.tharidiathings.trade.other_disconnected"));
                player2.closeContainer();
            }
            return;
        }

        // Check if players are still online (connection might be stale)
        if (player1.hasDisconnected() || player2.hasDisconnected()) {
            TharidiaThings.LOGGER.warn("Trade completion failed: one or both players have disconnected");
            TradeManager.cancelSession(session.getSessionId());
            if (!player1.hasDisconnected()) {
                player1.sendSystemMessage(Component.translatable("message.tharidiathings.trade.other_disconnected"));
                player1.closeContainer();
            }
            if (!player2.hasDisconnected()) {
                player2.sendSystemMessage(Component.translatable("message.tharidiathings.trade.other_disconnected"));
                player2.closeContainer();
            }
            return;
        }

        // Get items from both players
        List<ItemStack> player1Items = session.getPlayer1Items();
        List<ItemStack> player2Items = session.getPlayer2Items();

        // Verify both players still have the items
        if (!verifyPlayerHasItems(player1, player1Items) || !verifyPlayerHasItems(player2, player2Items)) {
            player1.sendSystemMessage(Component.translatable("message.tharidiathings.trade.items_unavailable"));
            player2.sendSystemMessage(Component.translatable("message.tharidiathings.trade.items_unavailable"));
            TradeManager.cancelSession(session.getSessionId());
            player1.closeContainer();
            player2.closeContainer();
            return;
        }

        // Mark trade as completed FIRST to prevent item duplication from menu close
        if (player1.containerMenu instanceof TradeMenu tradeMenu1) {
            tradeMenu1.setTradeCompleted(true);
        }
        if (player2.containerMenu instanceof TradeMenu tradeMenu2) {
            tradeMenu2.setTradeCompleted(true);
        }

        // Create deep copies for rollback before any modification
        List<ItemStack> player1ItemsBackup = deepCopyItems(player1Items);
        List<ItemStack> player2ItemsBackup = deepCopyItems(player2Items);

        // Calculate currency amounts and dynamic tax rates
        // Player 1 receives player2Items, Player 2 receives player1Items
        int player1CurrencyReceiving = getTotalCurrency(player2Items);
        int player2CurrencyReceiving = getTotalCurrency(player1Items);

        // Get non-currency items for market value calculation
        List<ItemStack> player1TradedItems = getNonCurrencyItems(player1Items);
        List<ItemStack> player2TradedItems = getNonCurrencyItems(player2Items);

        // Calculate dynamic tax rates based on market value and player stats
        double player1TaxRate = getDynamicTaxRate(player1.getUUID(), player1CurrencyReceiving, player2TradedItems);
        double player2TaxRate = getDynamicTaxRate(player2.getUUID(), player2CurrencyReceiving, player1TradedItems);

        boolean transactionSuccess = false;
        try {
            // Phase 1: Remove items from both players
            removeItemsFromPlayer(player1, player1Items);
            removeItemsFromPlayer(player2, player2Items);

            // Phase 2: Apply tax to currency items with dynamic rates
            List<ItemStack> player1ItemsAfterTax = applyTax(player1Items, player2TaxRate); // P2 receives these
            List<ItemStack> player2ItemsAfterTax = applyTax(player2Items, player1TaxRate); // P1 receives these

            // Phase 3: Give items to opposite players (with tax applied)
            giveItemsToPlayer(player1, player2ItemsAfterTax);
            giveItemsToPlayer(player2, player1ItemsAfterTax);

            transactionSuccess = true;
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Trade transaction failed, attempting rollback: {}", e.getMessage(), e);

            // Rollback: give back original items to their owners
            try {
                giveItemsToPlayer(player1, player1ItemsBackup);
                giveItemsToPlayer(player2, player2ItemsBackup);
                TharidiaThings.LOGGER.info("Trade rollback successful");
            } catch (Exception rollbackEx) {
                TharidiaThings.LOGGER.error("Trade rollback FAILED - items may be lost! Players: {} and {}",
                    player1.getName().getString(), player2.getName().getString(), rollbackEx);
            }

            player1.sendSystemMessage(Component.translatable("message.tharidiathings.trade.internal_error"));
            player2.sendSystemMessage(Component.translatable("message.tharidiathings.trade.internal_error"));
            TradeManager.cancelSession(session.getSessionId());
            player1.closeContainer();
            player2.closeContainer();
            return;
        }

        // Send completion packets
        PacketDistributor.sendToPlayer(player1, new TradeCompletePacket(session.getSessionId(), true));
        PacketDistributor.sendToPlayer(player2, new TradeCompletePacket(session.getSessionId(), true));

        // Notify players
        player1.sendSystemMessage(Component.translatable("message.tharidiathings.trade.success"));
        player2.sendSystemMessage(Component.translatable("message.tharidiathings.trade.success"));

        // Count total items traded
        int totalItemsTraded = player1Items.size() + player2Items.size();

        // Record completed trade with Tharidia Features (treasury, stats, etc.)
        MarketBridge.recordCompletedTrade(
            session.getSessionId(),
            player1.getUUID(), player2.getUUID(),
            player1.getName().getString(), player2.getName().getString(),
            player1CurrencyReceiving, player2CurrencyReceiving,
            player1TaxRate, player2TaxRate,
            totalItemsTraded
        );

        // Send transaction data to Tharidia Features market system (for price tracking)
        MarketBridge.sendTransaction(
            player1.getServer(),
            player1.getUUID(), player2.getUUID(),
            player1.getName().getString(), player2.getName().getString(),
            player1Items, player2Items,
            false // Not fictional
        );

        // Close menus
        player1.closeContainer();
        player2.closeContainer();

        // Remove session
        TradeManager.cancelSession(session.getSessionId());

        TharidiaThings.LOGGER.info("Trade completed between {} and {}",
            player1.getName().getString(), player2.getName().getString());
    }

    private static List<ItemStack> deepCopyItems(List<ItemStack> items) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                copies.add(stack.copy());
            }
        }
        return copies;
    }

    /**
     * Validates and sanitizes items received from client.
     * Prevents exploits with invalid stack sizes or too many items.
     */
    private static List<ItemStack> validateAndSanitizeItems(List<ItemStack> items, ServerPlayer player) {
        List<ItemStack> validated = new ArrayList<>();
        int maxTradeSlots = 6;

        for (int i = 0; i < Math.min(items.size(), maxTradeSlots); i++) {
            ItemStack stack = items.get(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            // Validate stack size doesn't exceed max
            int maxStackSize = stack.getMaxStackSize();
            if (stack.getCount() > maxStackSize) {
                TharidiaThings.LOGGER.warn("Player {} tried to trade item with invalid stack size: {} (max: {})",
                    player.getName().getString(), stack.getCount(), maxStackSize);
                stack = stack.copyWithCount(maxStackSize);
            }

            // Validate stack size is positive
            if (stack.getCount() <= 0) {
                TharidiaThings.LOGGER.warn("Player {} tried to trade item with non-positive count: {}",
                    player.getName().getString(), stack.getCount());
                continue;
            }

            validated.add(stack.copy());
        }

        // Log if items were truncated
        if (items.size() > maxTradeSlots) {
            TharidiaThings.LOGGER.warn("Player {} tried to trade more than {} items, truncated",
                player.getName().getString(), maxTradeSlots);
        }

        return validated;
    }

    private static boolean verifyPlayerHasItems(ServerPlayer player, List<ItemStack> items) {
        // Check if player has items in their inventory
        // Items in trade slots should be in the player's inventory
        for (ItemStack requiredStack : items) {
            if (requiredStack.isEmpty()) continue;
            
            int remaining = requiredStack.getCount();
            
            // Check in main inventory and hotbar
            for (ItemStack invStack : player.getInventory().items) {
                if (ItemStack.isSameItemSameComponents(invStack, requiredStack)) {
                    remaining -= invStack.getCount();
                    if (remaining <= 0) break;
                }
            }
            
            // If still missing items, check if they're in the trade menu container
            if (remaining > 0 && player.containerMenu instanceof TradeMenu tradeMenu) {
                for (int i = 0; i < 6; i++) {
                    ItemStack tradeStack = tradeMenu.getPlayerOffer().getItem(i);
                    if (ItemStack.isSameItemSameComponents(tradeStack, requiredStack)) {
                        remaining -= tradeStack.getCount();
                        if (remaining <= 0) break;
                    }
                }
            }
            
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private static void removeItemsFromPlayer(ServerPlayer player, List<ItemStack> items) {
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            
            int toRemove = stack.getCount();
            for (int i = 0; i < player.getInventory().items.size() && toRemove > 0; i++) {
                ItemStack invStack = player.getInventory().items.get(i);
                if (ItemStack.isSameItemSameComponents(invStack, stack)) {
                    int removeCount = Math.min(toRemove, invStack.getCount());
                    invStack.shrink(removeCount);
                    toRemove -= removeCount;
                }
            }
        }
    }

    private static void giveItemsToPlayer(ServerPlayer player, List<ItemStack> items) {
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy)) {
                // If inventory is full, drop the item
                player.drop(copy, false);
            }
        }
    }

    private static void updateMenuItems(net.minecraft.world.Container container, List<ItemStack> items, double taxRate) {
        // Clear container
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        // Set new items (with tax preview for currency items)
        for (int i = 0; i < Math.min(items.size(), container.getContainerSize()); i++) {
            ItemStack displayStack = items.get(i).copy();

            // Apply tax preview to currency items
            if (isCurrencyItem(displayStack)) {
                int taxedAmount = applyTaxToAmount(displayStack.getCount(), taxRate);
                displayStack.setCount(taxedAmount);
            }

            container.setItem(i, displayStack);
        }
    }

    private static void clearMenuItems(net.minecraft.world.Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, ItemStack.EMPTY);
        }
    }

    private static boolean isCurrencyItem(ItemStack stack) {
        return CurrencyHelper.isCurrencyItem(stack);
    }

    private static int applyTaxToAmount(int amount, double taxRate) {
        if (amount <= 0) {
            return 0;
        }
        int taxedAmount = (int) Math.floor(amount * (1.0 - taxRate));
        // Ensure minimum of 1 coin when trading currency
        return Math.max(1, taxedAmount);
    }

    /**
     * Get total currency amount in a list of items
     */
    private static int getTotalCurrency(List<ItemStack> items) {
        int total = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && isCurrencyItem(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Get non-currency items for market value calculation
     */
    private static List<ItemStack> getNonCurrencyItems(List<ItemStack> items) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && !isCurrencyItem(stack)) {
                result.add(stack);
            }
        }
        return result;
    }

    /**
     * Apply tax to currency items with a specific tax rate
     */
    private static List<ItemStack> applyTax(List<ItemStack> items, double taxRate) {
        List<ItemStack> taxedItems = new ArrayList<>();

        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;

            ItemStack taxedStack = stack.copy();

            // Apply tax only to currency items
            if (isCurrencyItem(taxedStack)) {
                int taxedAmount = applyTaxToAmount(taxedStack.getCount(), taxRate);
                taxedStack.setCount(taxedAmount);

                if (taxedAmount > 0) {
                    taxedItems.add(taxedStack);
                }
            } else {
                taxedItems.add(taxedStack);
            }
        }

        return taxedItems;
    }

    private static int calculateTaxAmount(List<ItemStack> items, double taxRate) {
        int totalCurrency = getTotalCurrency(items);

        if (totalCurrency > 0) {
            int taxedAmount = (int) Math.floor(totalCurrency * (1.0 - taxRate));
            return totalCurrency - taxedAmount;
        }

        return 0;
    }

    /**
     * Get dynamic tax rate from Tharidia Features if available, otherwise use config default
     */
    private static double getDynamicTaxRate(UUID receiverUUID, int currencyAmount, List<ItemStack> tradedItems) {
        double defaultRate = Config.TRADE_TAX_RATE.get();
        return MarketBridge.getDynamicTaxRate(receiverUUID, currencyAmount, tradedItems, defaultRate);
    }
}
