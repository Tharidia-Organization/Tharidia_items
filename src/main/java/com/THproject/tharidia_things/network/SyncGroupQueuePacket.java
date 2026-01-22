package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.client.ClientGroupQueueHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet sent from server to client to sync group queue state.
 */
public record SyncGroupQueuePacket(BlockPos pietroPos, List<UUID> playerUUIDs, UUID leaderUUID) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncGroupQueuePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "sync_group_queue"));

    public static final StreamCodec<ByteBuf, SyncGroupQueuePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncGroupQueuePacket decode(ByteBuf buf) {
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);

            int count = buf.readInt();
            List<UUID> uuids = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                long most = buf.readLong();
                long least = buf.readLong();
                uuids.add(new UUID(most, least));
            }

            boolean hasLeader = buf.readBoolean();
            UUID leader = null;
            if (hasLeader) {
                long most = buf.readLong();
                long least = buf.readLong();
                leader = new UUID(most, least);
            }

            return new SyncGroupQueuePacket(pos, uuids, leader);
        }

        @Override
        public void encode(ByteBuf buf, SyncGroupQueuePacket packet) {
            BlockPos.STREAM_CODEC.encode(buf, packet.pietroPos());

            buf.writeInt(packet.playerUUIDs().size());
            for (UUID uuid : packet.playerUUIDs()) {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            }

            boolean hasLeader = packet.leaderUUID() != null;
            buf.writeBoolean(hasLeader);
            if (hasLeader) {
                buf.writeLong(packet.leaderUUID().getMostSignificantBits());
                buf.writeLong(packet.leaderUUID().getLeastSignificantBits());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncGroupQueuePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Handle on client side
            ClientGroupQueueHandler.handleSync(packet.pietroPos(), packet.playerUUIDs(), packet.leaderUUID());
        });
    }
}
