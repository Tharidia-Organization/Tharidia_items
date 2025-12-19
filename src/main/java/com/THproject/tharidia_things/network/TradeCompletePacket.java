package com.THproject.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Sent when both players have confirmed and trade is complete
 */
public record TradeCompletePacket(UUID tradeSessionId, boolean success) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<TradeCompletePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "trade_complete"));

    public static final StreamCodec<ByteBuf, TradeCompletePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
        TradeCompletePacket::tradeSessionId,
        ByteBufCodecs.BOOL,
        TradeCompletePacket::success,
        TradeCompletePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
