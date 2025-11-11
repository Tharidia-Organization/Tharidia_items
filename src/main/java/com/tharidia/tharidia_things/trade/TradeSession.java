package com.tharidia.tharidia_things.trade;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Represents an active trade session between two players
 */
public class TradeSession {
    private final UUID sessionId;
    private final ServerPlayer player1;
    private final ServerPlayer player2;
    private final List<ItemStack> player1Items;
    private final List<ItemStack> player2Items;
    private boolean player1Confirmed;
    private boolean player2Confirmed;
    private final long creationTime;

    public TradeSession(ServerPlayer player1, ServerPlayer player2) {
        this.sessionId = UUID.randomUUID();
        this.player1 = player1;
        this.player2 = player2;
        this.player1Items = new ArrayList<>();
        this.player2Items = new ArrayList<>();
        this.player1Confirmed = false;
        this.player2Confirmed = false;
        this.creationTime = System.currentTimeMillis();
    }

    public void updatePlayerItems(UUID playerId, List<ItemStack> items) {
        if (player1.getUUID().equals(playerId)) {
            player1Items.clear();
            player1Items.addAll(items);
            player1Confirmed = false; // Reset confirmation when items change
        } else if (player2.getUUID().equals(playerId)) {
            player2Items.clear();
            player2Items.addAll(items);
            player2Confirmed = false; // Reset confirmation when items change
        }
    }

    public void setPlayerConfirmed(UUID playerId, boolean confirmed) {
        if (player1.getUUID().equals(playerId)) {
            player1Confirmed = confirmed;
        } else if (player2.getUUID().equals(playerId)) {
            player2Confirmed = confirmed;
        }
    }

    public boolean isBothConfirmed() {
        return player1Confirmed && player2Confirmed;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > 300000; // 5 minutes
    }

    public UUID getSessionId() { return sessionId; }
    public ServerPlayer getPlayer1() { return player1; }
    public ServerPlayer getPlayer2() { return player2; }
    public List<ItemStack> getPlayer1Items() { return new ArrayList<>(player1Items); }
    public List<ItemStack> getPlayer2Items() { return new ArrayList<>(player2Items); }
    public boolean isPlayer1Confirmed() { return player1Confirmed; }
    public boolean isPlayer2Confirmed() { return player2Confirmed; }

    public ServerPlayer getOtherPlayer(UUID playerId) {
        if (player1.getUUID().equals(playerId)) {
            return player2;
        } else if (player2.getUUID().equals(playerId)) {
            return player1;
        }
        return null;
    }

    public boolean involves(UUID playerId) {
        return player1.getUUID().equals(playerId) || player2.getUUID().equals(playerId);
    }
}
