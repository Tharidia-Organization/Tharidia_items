package com.THproject.tharidia_things.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Network packet sent from server to client containing music file data
 * Files are sent in chunks to avoid packet size limits
 */
public record MusicFileDataPacket(
    String musicFile,
    byte[] data,
    int chunkIndex,
    int totalChunks,
    boolean isLastChunk
) implements CustomPacketPayload {
    
    public static final Type<MusicFileDataPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("tharidiathings", "music_file_data")
    );
    
    public static final StreamCodec<FriendlyByteBuf, MusicFileDataPacket> STREAM_CODEC = StreamCodec.of(
        MusicFileDataPacket::write,
        MusicFileDataPacket::read
    );
    
    // Maximum chunk size (1MB to be safe with network limits)
    public static final int MAX_CHUNK_SIZE = 1024 * 1024;
    
    public static void write(FriendlyByteBuf buf, MusicFileDataPacket packet) {
        buf.writeUtf(packet.musicFile);
        buf.writeInt(packet.data.length);
        buf.writeBytes(packet.data);
        buf.writeInt(packet.chunkIndex);
        buf.writeInt(packet.totalChunks);
        buf.writeBoolean(packet.isLastChunk);
    }
    
    public static MusicFileDataPacket read(FriendlyByteBuf buf) {
        String musicFile = buf.readUtf();
        int dataLength = buf.readInt();
        byte[] data = new byte[dataLength];
        buf.readBytes(data);
        int chunkIndex = buf.readInt();
        int totalChunks = buf.readInt();
        boolean isLastChunk = buf.readBoolean();
        return new MusicFileDataPacket(musicFile, data, chunkIndex, totalChunks, isLastChunk);
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
