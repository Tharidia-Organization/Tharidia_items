package com.tharidia.tharidia_things.network;

import com.tharidia.tharidia_things.TharidiaThings;
import com.tharidia.tharidia_things.gui.TradeMenu;
import com.tharidia.tharidia_things.trade.TradeManager;
import com.tharidia.tharidia_things.trade.TradeSession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

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

        // Notify other player
        ServerPlayer otherPlayer = session.getOtherPlayer(player.getUUID());
        if (otherPlayer != null && otherPlayer.containerMenu instanceof TradeMenu otherMenu) {
            // Update other player's view
            otherMenu.setOtherPlayerConfirmed(packet.confirmed());
            
            // Update items in the menu
            updateMenuItems(otherMenu.getOtherPlayerOffer(), packet.items());
        }

        // Check if both players confirmed
        if (session.isBothConfirmed()) {
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

        // Give items to opposite players
        giveItemsToPlayer(player1, player2Items);
        giveItemsToPlayer(player2, player1Items);

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
        
        // Set new items
        for (int i = 0; i < Math.min(items.size(), container.getContainerSize()); i++) {
            container.setItem(i, items.get(i).copy());
        }
    }
}
