package com.THproject.tharidia_things.dungeon_query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.THproject.tharidia_features.dungeon.DungeonInstance;
import com.THproject.tharidia_features.dungeon.DungeonManager;
import com.THproject.tharidia_things.sounds.ModSounds;
import com.mojang.logging.LogUtils;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class DungeonQueryInstance {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final int START_TIME = 100; // 5 seconds
    private final int CALL_TIME = 200; // 5 minutes
    private final int RANGE = 5;

    private ServerLevel level;
    private List<UUID> players_list;
    private List<ServerPlayer> playersToTeleport;
    private ServerBossEvent progression_bar;
    private DungeonStatus status;
    private int instanceId;
    private BlockPos realmPos;

    private int start_countdown;
    private int call_countdown;
    private boolean show_particle;

    public DungeonQueryInstance(ServerLevel level) {
        this.level = level;
        players_list = new ArrayList<>();
        this.progression_bar = new ServerBossEvent(
                Component.literal("You are in queue for dungeon"),
                BossBarColor.RED,
                BossBarOverlay.PROGRESS);

        this.start_countdown = -1;
        this.call_countdown = -1;

        this.status = DungeonStatus.IDLE;
    }

    public void setRealmPos(BlockPos pos) {
        realmPos = pos;
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
        show_particle = true;

        DungeonManager manager = DungeonManager.getInstance();
        playersToTeleport = new ArrayList<>();
        for (UUID playerUUID : players_list) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                playersToTeleport.add(player);
                progression_bar.removePlayer(player);
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("You are entering in the dungeon")
                                .withColor(0x00FF00)));
                player.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal("Remain inside the circle")
                                .withColor(0x610ABF)));
            }
        }

        if (!playersToTeleport.isEmpty()) {
            // Play crimson forest mood sound to all players
            for (ServerPlayer p : playersToTeleport) {
                p.playNotifySound(ModSounds.DUNGEON_START.get(), SoundSource.MASTER, 1.0f, 1.0f);
            }

            boolean success = manager.joinGroupQueue(playersToTeleport);
            if (success) {
                LOGGER.info("[GROUP DUNGEON] Teleported {} players to dungeon", playersToTeleport.size());
            } else {
                LOGGER.warn(
                        "[GROUP DUNGEON] Failed to teleport group - no instance available or players already in dungeon");
            }
        }
    }

    public void teleportPlayers() {
        status = DungeonStatus.RUNNING;

        show_particle = false;

        DungeonManager manager = DungeonManager.getInstance();

        // Get instance ID from first player if available - all players in queue share
        // same instance
        if (!playersToTeleport.isEmpty()) {
            int pInstance = manager.getPlayerInstance(playersToTeleport.get(0).getUUID());
            if (pInstance != -1) {
                this.instanceId = pInstance;
            }
        }

        // Remove players if are not in range (RANGE)
        if (realmPos != null) {
            List<UUID> toRemove = new ArrayList<>();
            for (UUID uuid : players_list) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(uuid);
                if (p == null || p.distanceToSqr(realmPos.getX() + 0.5, realmPos.getY() + 0.5,
                        realmPos.getZ() + 0.5) > RANGE * RANGE) {
                    toRemove.add(uuid);
                }
            }

            for (UUID uuid : toRemove) {
                // IMPORTANT: Tell DungeonManager to remove player!
                // This ensures if instance becomes empty, it is cleaned up correctly
                manager.removePlayer(uuid, level.getServer());

                players_list.remove(uuid);

                if (playersToTeleport != null) {
                    playersToTeleport.removeIf(player -> player.getUUID().equals(uuid));
                }

                ServerPlayer p = level.getServer().getPlayerList().getPlayer(uuid);
                if (p != null) {
                    progression_bar.removePlayer(p);
                    p.displayClientMessage(Component.literal(
                            "You are too far from the realm, you have been removed from the queue!")
                            .withColor(0xFF0000), true);
                }
            }
        }

        if (playersToTeleport.isEmpty()) {
            endDungeon();
            cleanup();
            return;
        }

        for (ServerPlayer player : playersToTeleport) {
            // Re-fetch or use stored instanceId. Stored is safer if we just got it.
            // But manager is the source of truth
            int pInstance = manager.getPlayerInstance(player.getUUID());
            if (pInstance != -1) {
                instanceId = pInstance; // Update local tracker
                manager.teleportPlayerToDungeon(player, instanceId);
            }
        }

        // Cleanup: remove boss bar from all players
        cleanup();
    }

    public void endDungeon() {
        status = DungeonStatus.IDLE;
    }

    /**
     * Cleans up this instance: removes boss bar from all players
     */
    public void cleanup() {
        // Remove boss bar from all players
        for (UUID playerUUID : players_list) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
            if (player != null) {
                progression_bar.removePlayer(player);
            }
        }
        LOGGER.debug("[GROUP DUNGEON] Instance cleaned up");
    }

    public void callPlayers() {
        players_list.forEach(playerUUID -> {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
            // Show title to players instead of chat message
            if (player != null) {
                player.displayClientMessage(
                        Component.literal(String.format("Dungeon start in %d minutes", CALL_TIME / 60 / 20)), false);
            }
        });
        show_particle = true;
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
            progression_bar.setName(Component.literal(String.format("Dungeon start in %d", call_countdown / 20)));
            progression_bar.setColor(BossBarColor.WHITE);
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

        if (show_particle) {
            if (realmPos != null) {
                for (int i = 0; i < 360; i += 10) {
                    double angle = Math.toRadians(i);
                    double x = realmPos.getX() + 0.5 + Math.cos(angle) * RANGE;
                    double z = realmPos.getZ() + 0.5 + Math.sin(angle) * RANGE;
                    level.sendParticles(ParticleTypes.WITCH, x, realmPos.getY() + 0.2, z, 1, 0, 0, 0, 0);
                }
            }
        }

        if (status == DungeonStatus.RUNNING) {
            // 1. Ottieni il Manager direttamente
            DungeonManager manager = DungeonManager.getInstance();

            // 2. Prendi la mappa delle istanze
            Map<Integer, DungeonInstance> activeInstances = manager.getActiveInstances();

            // 3. Recupera LA TUA specifica istanza usando l'ID salvato
            DungeonInstance myInstance = activeInstances.get(this.instanceId);

            // 4. Controlla se l'istanza esiste ancora ed è vuota
            if (myInstance != null && myInstance.getPlayerCount() == 0) {
                endDungeon();
            }
            // Opzionale: se myInstance è null, significa che il manager l'ha già cancellata
            else if (myInstance == null) {
                endDungeon();
            }
        }
    }
}
