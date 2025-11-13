package com.tharidia.tharidia_things.network;

import com.tharidia.tharidia_things.Config;
import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.gui.TradeMenu;
import com.tharidia.tharidia_things.trade.TradeManager;
import com.tharidia.tharidia_things.trade.TradeSession;
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
                player.sendSystemMessage(Component.literal("§cLa richiesta di scambio non è più valida."));
                return;
            }

            // Open trade menu for both players
            ServerPlayer requester = session.getPlayer1();
            ServerPlayer target = session.getPlayer2();

            openTradeMenu(requester, session);
            openTradeMenu(target, session);

            requester.sendSystemMessage(Component.literal("§6" + target.getName().getString() + " ha accettato lo scambio!"));
            target.sendSystemMessage(Component.literal("§6Scambio iniziato con " + requester.getName().getString()));
        } else {
            // Decline the trade request
            TradeManager.declineTradeRequest(player);
            
            // Notify the requester
            ServerPlayer requester = player.getServer().getPlayerList().getPlayer(packet.requesterId());
            if (requester != null) {
                requester.sendSystemMessage(Component.literal("§c" + player.getName().getString() + " ha rifiutato lo scambio."));
            }
        }
    }

    public static void handleTradeUpdate(TradeUpdatePacket packet, ServerPlayer player) {
        TradeSession session = TradeManager.getPlayerSession(player.getUUID());
        
        if (session == null) {
            player.sendSystemMessage(Component.literal("§cNessuna sessione di scambio attiva."));
            return;
        }

        // Update player's items
        session.updatePlayerItems(player.getUUID(), packet.items());
        session.setPlayerConfirmed(player.getUUID(), packet.confirmed());

        // Notify other player via packet
        ServerPlayer otherPlayer = session.getOtherPlayer(player.getUUID());
        if (otherPlayer != null) {
            // Update other player's menu server-side
            if (otherPlayer.containerMenu instanceof TradeMenu otherMenu) {
                otherMenu.setOtherPlayerConfirmed(packet.confirmed());
                
                // Only show items when confirmed
                if (packet.confirmed()) {
                    updateMenuItems(otherMenu.getOtherPlayerOffer(), packet.items());
                } else {
                    // Clear items if unconfirmed
                    clearMenuItems(otherMenu.getOtherPlayerOffer());
                }
            }
            
            // Send sync packet to other player's client
            List<ItemStack> itemsToSend = packet.confirmed() ? packet.items() : List.of();
            
            // Calculate tax info for the items being sent
            double taxRate = Config.TRADE_TAX_RATE.get();
            int taxAmount = calculateTaxAmount(itemsToSend, taxRate);
            
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
            player.sendSystemMessage(Component.literal("§cNessuna sessione di scambio attiva."));
            return;
        }

        // Check if both players have confirmed their items first
        if (!session.isBothConfirmed()) {
            player.sendSystemMessage(Component.literal("§cEntrambi i giocatori devono prima confermare i loro oggetti."));
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
            
            // Calculate tax info for the items being sent
            double taxRate = Config.TRADE_TAX_RATE.get();
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
            otherPlayer.closeContainer();
            otherPlayer.sendSystemMessage(Component.literal("§c" + player.getName().getString() + " ha annullato lo scambio."));
        }

        // Close player's menu
        player.closeContainer();
        player.sendSystemMessage(Component.literal("§7Scambio annullato."));

        // Remove session
        TradeManager.cancelSession(session.getSessionId());
    }

    private static void openTradeMenu(ServerPlayer player, TradeSession session) {
        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, p) -> new TradeMenu(containerId, playerInventory, session, p),
            Component.literal("Scambio")
        ), buf -> {
            buf.writeUUID(session.getSessionId());
            buf.writeUUID(session.getOtherPlayer(player.getUUID()).getUUID());
            buf.writeUtf(session.getOtherPlayer(player.getUUID()).getName().getString());
        });
    }

    private static void completeTrade(TradeSession session) {
        ServerPlayer player1 = session.getPlayer1();
        ServerPlayer player2 = session.getPlayer2();

        // Get items from both players
        List<ItemStack> player1Items = session.getPlayer1Items();
        List<ItemStack> player2Items = session.getPlayer2Items();

        // Verify both players still have the items
        if (!verifyPlayerHasItems(player1, player1Items) || !verifyPlayerHasItems(player2, player2Items)) {
            player1.sendSystemMessage(Component.literal("§cScambio fallito: alcuni oggetti non sono più disponibili."));
            player2.sendSystemMessage(Component.literal("§cScambio fallito: alcuni oggetti non sono più disponibili."));
            TradeManager.cancelSession(session.getSessionId());
            player1.closeContainer();
            player2.closeContainer();
            return;
        }

        // Remove items from both players
        removeItemsFromPlayer(player1, player1Items);
        removeItemsFromPlayer(player2, player2Items);

        // Apply tax to currency items
        List<ItemStack> player1ItemsAfterTax = applyTax(player1Items);
        List<ItemStack> player2ItemsAfterTax = applyTax(player2Items);

        // Give items to opposite players (with tax applied)
        giveItemsToPlayer(player1, player2ItemsAfterTax);
        giveItemsToPlayer(player2, player1ItemsAfterTax);

        // Send completion packets
        PacketDistributor.sendToPlayer(player1, new TradeCompletePacket(session.getSessionId(), true));
        PacketDistributor.sendToPlayer(player2, new TradeCompletePacket(session.getSessionId(), true));

        // Notify players
        player1.sendSystemMessage(Component.literal("§6§l⚜ Scambio completato con successo! ⚜"));
        player2.sendSystemMessage(Component.literal("§6§l⚜ Scambio completato con successo! ⚜"));

        // Send transaction data to Tharidia Features market system
        com.tharidia.tharidia_things.trade.MarketBridge.sendTransaction(
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

    private static boolean verifyPlayerHasItems(ServerPlayer player, List<ItemStack> items) {
        for (ItemStack requiredStack : items) {
            if (requiredStack.isEmpty()) continue;
            
            int remaining = requiredStack.getCount();
            for (ItemStack invStack : player.getInventory().items) {
                if (ItemStack.isSameItemSameComponents(invStack, requiredStack)) {
                    remaining -= invStack.getCount();
                    if (remaining <= 0) break;
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

    private static void updateMenuItems(net.minecraft.world.Container container, List<ItemStack> items) {
        // Clear container
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, ItemStack.EMPTY);
        }
        
        // Set new items (with tax preview for currency items)
        for (int i = 0; i < Math.min(items.size(), container.getContainerSize()); i++) {
            ItemStack displayStack = items.get(i).copy();
            
            // Apply tax preview to currency items
            if (isCurrencyItem(displayStack)) {
                int taxedAmount = applyTaxToAmount(displayStack.getCount());
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
                        return false;
                    }
                });
    }

    private static int applyTaxToAmount(int amount) {
        double taxRate = Config.TRADE_TAX_RATE.get();
        return (int) Math.floor(amount * (1.0 - taxRate));
    }

    private static List<ItemStack> applyTax(List<ItemStack> items) {
        List<ItemStack> taxedItems = new ArrayList<>();
        
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            
            ItemStack taxedStack = stack.copy();
            
            // Apply tax only to currency items
            if (isCurrencyItem(taxedStack)) {
                int taxedAmount = applyTaxToAmount(taxedStack.getCount());
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
        int totalCurrency = 0;
        
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && isCurrencyItem(stack)) {
                totalCurrency += stack.getCount();
            }
        }
        
        if (totalCurrency > 0) {
            int taxedAmount = (int) Math.floor(totalCurrency * (1.0 - taxRate));
            return totalCurrency - taxedAmount;
        }
        
        return 0;
    }
}
