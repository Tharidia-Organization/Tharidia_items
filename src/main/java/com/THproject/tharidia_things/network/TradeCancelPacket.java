package com.THproject.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Sent when a player cancels the trade
 */
public record TradeCancelPacket(UUID traderId) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<TradeCancelPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "trade_cancel"));

    public static final StreamCodec<ByteBuf, TradeCancelPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
        TradeCancelPacket::traderId,
        TradeCancelPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
