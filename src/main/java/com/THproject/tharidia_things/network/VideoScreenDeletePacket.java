package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Packet to notify clients about screen deletion
 */
public record VideoScreenDeletePacket(
    UUID screenId,
    String dimension
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<VideoScreenDeletePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "video_screen_delete"));
    
    public static final StreamCodec<ByteBuf, VideoScreenDeletePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        packet -> packet.screenId.toString(),
        ByteBufCodecs.STRING_UTF8,
        VideoScreenDeletePacket::dimension,
        (screenIdStr, dimension) -> new VideoScreenDeletePacket(
            UUID.fromString(screenIdStr),
            dimension
        )
    );
    
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
