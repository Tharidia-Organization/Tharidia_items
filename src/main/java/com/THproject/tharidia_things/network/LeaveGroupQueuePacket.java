package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.dungeon_query.GroupDungeonQueueManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Packet sent from client to server when a player wants to leave the group queue.
 */
public record LeaveGroupQueuePacket(BlockPos pietroPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LeaveGroupQueuePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "leave_group_queue"));

    public static final StreamCodec<ByteBuf, LeaveGroupQueuePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LeaveGroupQueuePacket::pietroPos,
            LeaveGroupQueuePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LeaveGroupQueuePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BlockPos pos = packet.pietroPos();

                // Get queue before leaving (to check if it was disbanded)
                GroupDungeonQueueManager.GroupQueue queueBefore = GroupDungeonQueueManager.getQueue(pos);
                boolean wasLeader = queueBefore != null && queueBefore.isLeader(player.getUUID());

                boolean success = GroupDungeonQueueManager.leaveQueue(player.getUUID());

                if (success) {
                    player.sendSystemMessage(Component.translatable("gui.tharidiathings.realm.dungeon.left_queue"));

                    // Check if queue still exists
                    GroupDungeonQueueManager.GroupQueue queueAfter = GroupDungeonQueueManager.getQueue(pos);

                    if (wasLeader && queueAfter == null) {
                        // Queue was disbanded because leader left
                        // Notify all nearby players that queue is gone
                        syncQueueDisbanded(pos, player);
                    } else if (queueAfter != null) {
                        // Queue still exists, sync updated state
                        syncQueueToNearbyPlayers(pos, player);
                    } else {
                        // Queue empty and removed
                        syncQueueDisbanded(pos, player);
                    }
                }
            }
        });
    }

    private static void syncQueueToNearbyPlayers(BlockPos pos, ServerPlayer triggeringPlayer) {
        List<UUID> players = GroupDungeonQueueManager.getQueuePlayers(pos);
        GroupDungeonQueueManager.GroupQueue queue = GroupDungeonQueueManager.getQueue(pos);
        UUID leaderUUID = queue != null ? queue.getLeaderUUID() : null;

        SyncGroupQueuePacket syncPacket = new SyncGroupQueuePacket(pos, players, leaderUUID);

        for (ServerPlayer nearbyPlayer : triggeringPlayer.serverLevel().players()) {
            if (nearbyPlayer.blockPosition().distSqr(pos) <= 64 * 64) {
                PacketDistributor.sendToPlayer(nearbyPlayer, syncPacket);
            }
        }
    }

    private static void syncQueueDisbanded(BlockPos pos, ServerPlayer triggeringPlayer) {
        // Send empty queue to indicate disbandment
        SyncGroupQueuePacket syncPacket = new SyncGroupQueuePacket(pos, Collections.emptyList(), null);

        for (ServerPlayer nearbyPlayer : triggeringPlayer.serverLevel().players()) {
            if (nearbyPlayer.blockPosition().distSqr(pos) <= 64 * 64) {
                PacketDistributor.sendToPlayer(nearbyPlayer, syncPacket);

                // Also notify that leader left
                if (!nearbyPlayer.equals(triggeringPlayer)) {
                    nearbyPlayer.sendSystemMessage(Component.translatable("gui.tharidiathings.realm.dungeon.queue_disbanded"));
                }
            }
        }
    }
}
