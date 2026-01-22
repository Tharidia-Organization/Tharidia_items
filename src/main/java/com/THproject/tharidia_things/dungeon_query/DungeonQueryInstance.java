package com.THproject.tharidia_things.dungeon_query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class DungeonQueryInstance {
    private static final Logger LOGGER = LogUtils.getLogger();

    private int START_TIME = 100; // 5 seconds
    private int CALL_TIME = 6000; // 5 minutes

    private ServerLevel level;
    private List<UUID> players_list;
    private ServerBossEvent progression_bar;
    private DungeonStatus status;

    private int start_countdown;
    private int call_countdown;

    public DungeonQueryInstance(ServerLevel level) {
        this.level = level;
        players_list = new ArrayList<>();
        this.progression_bar = new ServerBossEvent(
                Component.literal("Test"),
                BossBarColor.GREEN,
                BossBarOverlay.PROGRESS);

        this.start_countdown = -1;
        this.call_countdown = -1;

        this.status = DungeonStatus.IDLE;
    }

    public void insertPlayer(Player player) {
        if (!players_list.contains(player.getUUID())) {
            players_list.add(player.getUUID());
            progression_bar.addPlayer((ServerPlayer) player);
            status = DungeonStatus.QUEUE;
        }
    }

    public void removePlayer(Player player) {
        if (players_list.contains(player.getUUID())) {
            players_list.remove(player.getUUID());
            progression_bar.removePlayer((ServerPlayer) player);
        }
        if (players_list.size() == 0)
            status = DungeonStatus.IDLE;
    }

    public List<UUID> getPlayers() {
        return players_list;
    }

    public DungeonStatus getStatus() {
        return status;
    }

    public void startDungeon() {
        status = DungeonStatus.STARTING;
        start_countdown = START_TIME;
        call_countdown = -1;
    }

    public void endDungeon() {
        status = DungeonStatus.IDLE;
    }

    public void teleportPlayers() {
        status = DungeonStatus.RUNNING;

        try {
            // Use reflection to access DungeonManager from Tharidia Features
            Class<?> dungeonManagerClass = Class.forName("com.THproject.tharidia_features.dungeon.DungeonManager");
            java.lang.reflect.Method getInstanceMethod = dungeonManagerClass.getMethod("getInstance");
            Object dungeonManager = getInstanceMethod.invoke(null);

            if (dungeonManager != null) {
                // Collect all online players from the group
                List<ServerPlayer> playersToTeleport = new ArrayList<>();
                for (UUID playerUUID : players_list) {
                    ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
                    if (player != null) {
                        playersToTeleport.add(player);
                    }
                }

                if (!playersToTeleport.isEmpty()) {
                    // Use joinGroupQueue to teleport all players to the SAME instance
                    java.lang.reflect.Method joinGroupQueueMethod = dungeonManagerClass.getMethod("joinGroupQueue", List.class);
                    boolean success = (Boolean) joinGroupQueueMethod.invoke(dungeonManager, playersToTeleport);

                    if (success) {
                        LOGGER.info("[GROUP DUNGEON] Teleported {} players to dungeon", playersToTeleport.size());
                    } else {
                        LOGGER.warn("[GROUP DUNGEON] Failed to teleport group - no instance available or players already in dungeon");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to teleport group to dungeon", e);
            // Notify players of the error
            for (UUID playerUUID : players_list) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
                if (player != null) {
                    player.sendSystemMessage(
                            Component.literal("§cErrore durante l'accesso al dungeon di gruppo."));
                }
            }
        }

        // Cleanup: remove boss bar from all players and mark as complete
        cleanup();
    }

    /**
     * Cleans up this instance: removes boss bar from all players and sets status to IDLE.
     */
    public void cleanup() {
        // Remove boss bar from all players
        for (UUID playerUUID : players_list) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                progression_bar.removePlayer(player);
            }
        }
        players_list.clear();
        status = DungeonStatus.IDLE;
        LOGGER.debug("[GROUP DUNGEON] Instance cleaned up");
    }

    public void callPlayers() {
        players_list.forEach(playerUUID -> {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                player.displayClientMessage(
                        Component.literal(String.format("Dungeon start in %d minutes", CALL_TIME / 60 / 20)), false);
            }
        });
        call_countdown = CALL_TIME;
    }

    /**
     * Ticks this dungeon instance. Should be called once per server tick.
     */
    public void tick() {
        // Handle call countdown
        if (call_countdown > 0) {
            float progress = (float) call_countdown / CALL_TIME;
            progression_bar.setProgress(progress);
            call_countdown--;
        } else if (call_countdown == 0) {
            startDungeon();
            call_countdown--;
        }

        // Handle start countdown
        if (start_countdown > 0) {
            start_countdown--;
        } else if (start_countdown == 0) {
            teleportPlayers();
            start_countdown--;
        }
    }
}
