package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.dungeon_query.DungeonInstanceManager;
import com.THproject.tharidia_things.dungeon_query.DungeonQueryInstance;
import com.THproject.tharidia_things.dungeon_query.GroupDungeonQueueManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.List;

/**
 * Packet sent from client to server when the leader wants to start the dungeon.
 */
public record StartGroupDungeonPacket(BlockPos pietroPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StartGroupDungeonPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "start_group_dungeon"));

    public static final StreamCodec<ByteBuf, StartGroupDungeonPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StartGroupDungeonPacket::pietroPos,
            StartGroupDungeonPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StartGroupDungeonPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                if (context.player() instanceof ServerPlayer player) {
                    // Check if tharidia_features is loaded - dungeon system requires it
                    if (!ModList.get().isLoaded("tharidia_features")) {
                        player.sendSystemMessage(Component.literal("Dungeon system requires tharidia_features mod."));
                        return;
                    }

                    BlockPos pos = packet.pietroPos();
                    if (pos == null) {
                        TharidiaThings.LOGGER.error("[GROUP DUNGEON] Pietro position is null");
                        return;
                    }

                    ServerLevel level = player.serverLevel();
                    if (level == null) {
                        TharidiaThings.LOGGER.error("[GROUP DUNGEON] Server level is null");
                        return;
                    }

                    // Check if player is the leader
                    GroupDungeonQueueManager.GroupQueue queue = GroupDungeonQueueManager.getQueue(pos);
                    if (queue == null) {
                        player.sendSystemMessage(Component.translatable("gui.tharidiathings.realm.dungeon.no_queue"));
                        return;
                    }

                    if (!queue.isLeader(player.getUUID())) {
                        player.sendSystemMessage(Component.translatable("gui.tharidiathings.realm.dungeon.not_leader"));
                        return;
                    }

                    // Get all players to teleport
                    List<ServerPlayer> playersToTeleport = GroupDungeonQueueManager.startDungeon(pos, player, level);

                    if (playersToTeleport == null || playersToTeleport.isEmpty()) {
                        player.sendSystemMessage(Component.translatable("gui.tharidiathings.realm.dungeon.start_failed"));
                        return;
                    }

                    // Create dungeon instance and teleport all players
                    DungeonQueryInstance dungeonInstance = new DungeonQueryInstance(level);
                    dungeonInstance.setRealmPos(pos);

                    for (ServerPlayer p : playersToTeleport) {
                        if (p != null) {
                            dungeonInstance.insertPlayer(p);
                        }
                    }

                    int maxInstances = getMaxInstancesFromFeatures();
                    if (DungeonInstanceManager.getActiveInstanceCount() >= maxInstances) {
                        DungeonInstanceManager.addToWaitingQueue(dungeonInstance);
                    } else {
                        // Register the instance to be ticked
                        DungeonInstanceManager.registerInstance(dungeonInstance);
                        // Start the dungeon
                        dungeonInstance.startDungeon();
                    }

                    // Notify all nearby players that queue is gone
                    try {
                        SyncGroupQueuePacket syncPacket = new SyncGroupQueuePacket(pos, Collections.emptyList(), null);
                        for (ServerPlayer nearbyPlayer : level.players()) {
                            if (nearbyPlayer != null && nearbyPlayer.blockPosition().distSqr(pos) <= 64 * 64) {
                                PacketDistributor.sendToPlayer(nearbyPlayer, syncPacket);
                            }
                        }
                    } catch (Exception e) {
                        TharidiaThings.LOGGER.warn("[GROUP DUNGEON] Failed to sync queue packet: {}", e.getMessage());
                    }

                    TharidiaThings.LOGGER.info("[GROUP DUNGEON] Started dungeon with {} players", playersToTeleport.size());
                }
            } catch (Exception e) {
                TharidiaThings.LOGGER.error("[GROUP DUNGEON] Error starting dungeon: {}", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Gets max instances from DungeonManager via reflection to avoid hard dependency.
     */
    private static int getMaxInstancesFromFeatures() {
        try {
            Class<?> dungeonManagerClass = Class.forName("com.THproject.tharidia_features.dungeon.DungeonManager");
            Object manager = dungeonManagerClass.getMethod("getInstance").invoke(null);
            return (int) dungeonManagerClass.getMethod("getMaxInstances").invoke(manager);
        } catch (Exception e) {
            TharidiaThings.LOGGER.warn("[GROUP DUNGEON] Could not get max instances: {}", e.getMessage());
            return 1; // Default to 1 instance if features not available
        }
    }
}
