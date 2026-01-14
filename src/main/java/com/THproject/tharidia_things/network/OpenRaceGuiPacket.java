package com.THproject.tharidia_things.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet to open the race selection GUI on client
 */
public record OpenRaceGuiPacket(String raceName) implements CustomPacketPayload {
    public static final Type<OpenRaceGuiPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tharidiathings", "open_race_gui"));
    public static final StreamCodec<FriendlyByteBuf, OpenRaceGuiPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> buf.writeUtf(packet.raceName),
        buf -> new OpenRaceGuiPacket(buf.readUtf())
    );
    
    public OpenRaceGuiPacket(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }
    
    @Override
    public Type<OpenRaceGuiPacket> type() {
        return TYPE;
    }
    
    public static void handle(OpenRaceGuiPacket packet, IPayloadContext context) {
        // Handler is in ClientPacketHandler to avoid loading client classes on server
    }
}
