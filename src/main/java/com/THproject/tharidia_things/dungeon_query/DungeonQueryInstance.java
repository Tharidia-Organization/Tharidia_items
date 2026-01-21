package com.THproject.tharidia_things.dungeon_query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber
public class DungeonQueryInstance {
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
    }

    public void callPlayers() {
        players_list.forEach(player -> {
            level.getPlayerByUUID(player).displayClientMessage(
                    Component.literal(String.format("Dungeon start in %d minutes", CALL_TIME / 60 / 20)), false);
        });
        call_countdown = CALL_TIME;
    }

    @SubscribeEvent
    public void onCallingPlayers(LevelTickEvent event) {
        if (call_countdown > 0) {
            float progress = call_countdown / CALL_TIME;
            progression_bar.setProgress(progress);
            call_countdown--;
        } else if (call_countdown == 0) {
            startDungeon();
            call_countdown--;
        }
    }

    @SubscribeEvent
    public void onStartingDungeon(LevelTickEvent event) {
        if (start_countdown > 0) {
            start_countdown--;
        } else if (start_countdown == 0) {
            teleportPlayers();
            start_countdown--;
        }
    }
}
