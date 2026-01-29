package com.THproject.tharidia_things.trade;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active trade sessions on the server
 */
public class TradeManager {
    private static final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>(); // target -> requester
    private static final Object tradeLock = new Object(); // Lock for atomic trade operations

    /**
     * Create a pending trade request
     * @return true if request was created, false if player is busy
     */
    public static boolean createTradeRequest(ServerPlayer requester, ServerPlayer target) {
        UUID targetId = target.getUUID();
        UUID requesterId = requester.getUUID();

        // Synchronized to prevent race conditions
        synchronized (tradeLock) {
            // Check if target already has a pending request
            if (pendingRequests.containsKey(targetId)) {
                return false;
            }

            // Check if either player is already in a trade
            if (isPlayerInTrade(requesterId) || isPlayerInTrade(targetId)) {
                return false;
            }

            // Check if requester already has a pending request to someone else
            if (pendingRequests.containsValue(requesterId)) {
                return false;
            }

            pendingRequests.put(targetId, requesterId);
        }

        TharidiaThings.LOGGER.info("Trade request created: {} -> {}", requester.getName().getString(), target.getName().getString());
        return true;
    }

    /**
     * Accept a trade request and create a session
     */
    public static TradeSession acceptTradeRequest(ServerPlayer target) {
        UUID targetId = target.getUUID();

        synchronized (tradeLock) {
            UUID requesterId = pendingRequests.remove(targetId);

            if (requesterId == null) {
                return null;
            }

            ServerPlayer requester = target.getServer().getPlayerList().getPlayer(requesterId);
            if (requester == null) {
                return null;
            }

            // Double-check neither player is now in a trade (race condition prevention)
            if (isPlayerInTrade(requesterId) || isPlayerInTrade(targetId)) {
                return null;
            }

            // Create trade session
            TradeSession session = new TradeSession(requester, target);
            activeSessions.put(session.getSessionId(), session);

            TharidiaThings.LOGGER.info("Trade session created: {} <-> {}",
                requester.getName().getString(), target.getName().getString());

            return session;
        }
    }

    /**
     * Decline a trade request
     */
    public static void declineTradeRequest(ServerPlayer target) {
        UUID targetId = target.getUUID();
        UUID requesterId = pendingRequests.remove(targetId);
        
        if (requesterId != null) {
            TharidiaThings.LOGGER.info("Trade request declined by {}", target.getName().getString());
        }
    }

    /**
     * Get active session for a player
     */
    public static TradeSession getPlayerSession(UUID playerId) {
        return activeSessions.values().stream()
                .filter(session -> session.involves(playerId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get session by ID
     */
    public static TradeSession getSession(UUID sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * Check if player is in an active trade
     */
    public static boolean isPlayerInTrade(UUID playerId) {
        return activeSessions.values().stream()
                .anyMatch(session -> session.involves(playerId));
    }

    /**
     * Check if player has a pending request
     */
    public static boolean hasPendingRequest(UUID playerId) {
        return pendingRequests.containsKey(playerId);
    }

    /**
     * Cancel a trade session
     */
    public static void cancelSession(UUID sessionId) {
        TradeSession session = activeSessions.remove(sessionId);
        if (session != null) {
            TharidiaThings.LOGGER.info("Trade session cancelled: {}", sessionId);
        }
    }

    /**
     * Cancel any session involving a player and notify the other player
     */
    public static void cancelPlayerSession(UUID playerId) {
        TradeSession session = getPlayerSession(playerId);
        if (session != null) {
            // Notify the other player before cancelling
            ServerPlayer otherPlayer = session.getOtherPlayer(playerId);
            if (otherPlayer != null && !otherPlayer.hasDisconnected()) {
                otherPlayer.closeContainer();
                otherPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tharidiathings.trade.other_disconnected"));
            }
            cancelSession(session.getSessionId());
        }
        // Also remove any pending requests
        pendingRequests.remove(playerId);
        pendingRequests.values().removeIf(id -> id.equals(playerId));
    }

    /**
     * Clean up expired sessions
     */
    public static void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                TharidiaThings.LOGGER.info("Removing expired trade session: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Get all active sessions (for debugging)
     */
    public static Collection<TradeSession> getAllSessions() {
        return new ArrayList<>(activeSessions.values());
    }

    /**
     * Create a test trade session for admin testing
     */
    public static TradeSession createTestSession(ServerPlayer player) {
        // Check if player is already in a trade
        if (isPlayerInTrade(player.getUUID())) {
            return null;
        }

        // For testing purposes, create a session with the same player as both participants
        // This avoids the need to create a dummy ServerPlayer with ClientInformation
        TradeSession session = new TradeSession(player, player);
        activeSessions.put(session.getSessionId(), session);
        
        TharidiaThings.LOGGER.info("Test trade session created for: {}", player.getName().getString());
        
        return session;
    }
}
