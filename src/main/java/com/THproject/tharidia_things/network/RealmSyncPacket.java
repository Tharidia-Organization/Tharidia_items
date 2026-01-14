package com.THproject.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

public record RealmSyncPacket(List<RealmData> realms, boolean fullSync) implements CustomPacketPayload {

    // Convenience constructor for backward compatibility (defaults to incremental update)
    public RealmSyncPacket(List<RealmData> realms) {
        this(realms, false);
    }

    public static final CustomPacketPayload.Type<RealmSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "realm_sync"));

    public static final StreamCodec<ByteBuf, RealmSyncPacket> STREAM_CODEC = StreamCodec.composite(
        RealmData.STREAM_CODEC.apply(ByteBufCodecs.list()),
        RealmSyncPacket::realms,
        ByteBufCodecs.BOOL,
        RealmSyncPacket::fullSync,
        RealmSyncPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record RealmData(BlockPos pos, int realmSize, String ownerName, int centerChunkX, int centerChunkZ) {
        public static final StreamCodec<ByteBuf, RealmData> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            RealmData::pos,
            ByteBufCodecs.VAR_INT,
            RealmData::realmSize,
            ByteBufCodecs.STRING_UTF8,
            RealmData::ownerName,
            ByteBufCodecs.VAR_INT,
            RealmData::centerChunkX,
            ByteBufCodecs.VAR_INT,
            RealmData::centerChunkZ,
            RealmData::new
        );
    }
}
