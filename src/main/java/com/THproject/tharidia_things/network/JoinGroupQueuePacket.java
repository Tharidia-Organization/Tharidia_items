package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.dungeon_query.GroupDungeonQueueManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * Packet sent from client to server when a player wants to join or create a group queue.
 */
public record JoinGroupQueuePacket(BlockPos pietroPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JoinGroupQueuePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "join_group_queue"));

    public static final StreamCodec<ByteBuf, JoinGroupQueuePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, JoinGroupQueuePacket::pietroPos,
            JoinGroupQueuePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JoinGroupQueuePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BlockPos pos = packet.pietroPos();

                // Check if player is already in a queue
                GroupDungeonQueueManager.GroupQueue existingPlayerQueue =
                        GroupDungeonQueueManager.getPlayerQueue(player.getUUID());

                if (existingPlayerQueue != null) {
                    player.sendSystemMessage(Component.translatable("gui.tharidiathings.realm.dungeon.already_in_queue"));
                    return;
                }

                // Try to create or join queue
                boolean success = GroupDungeonQueueManager.createOrJoinQueue(pos, player);

                if (success) {
                    GroupDungeonQueueManager.GroupQueue queue = GroupDungeonQueueManager.getQueue(pos);
                    if (queue != null) {
                        boolean isLeader = queue.isLeader(player.getUUID());
                        if (isLeader) {
                            player.sendSystemMessage(Component.translatable("gui.tharidiathings.realm.dungeon.queue_created"));
                        } else {
                            player.sendSystemMessage(Component.translatable("gui.tharidiathings.realm.dungeon.joined_queue"));
                        }

                        // Sync to all nearby players
                        syncQueueToNearbyPlayers(pos, player);
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("gui.tharidiathings.realm.dungeon.queue_full"));
                }
            }
        });
    }

    /**
     * Syncs the queue state to all players within range of the Pietro block.
     */
    private static void syncQueueToNearbyPlayers(BlockPos pos, ServerPlayer triggeringPlayer) {
        List<UUID> players = GroupDungeonQueueManager.getQueuePlayers(pos);
        GroupDungeonQueueManager.GroupQueue queue = GroupDungeonQueueManager.getQueue(pos);
        UUID leaderUUID = queue != null ? queue.getLeaderUUID() : null;

        SyncGroupQueuePacket syncPacket = new SyncGroupQueuePacket(pos, players, leaderUUID);

        // Send to all players within 64 blocks
        for (ServerPlayer nearbyPlayer : triggeringPlayer.serverLevel().players()) {
            if (nearbyPlayer.blockPosition().distSqr(pos) <= 64 * 64) {
                PacketDistributor.sendToPlayer(nearbyPlayer, syncPacket);
            }
        }
    }
}
