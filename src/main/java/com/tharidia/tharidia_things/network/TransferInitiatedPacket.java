package com.tharidia.tharidia_things.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.tharidia.tharidia_things.TharidiaThings.MODID;

public record TransferInitiatedPacket(String fromServer, String toServer) implements CustomPacketPayload {
    public static final Type<TransferInitiatedPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "transfer_initiated"));
    
    public static final StreamCodec<FriendlyByteBuf, TransferInitiatedPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeUtf(packet.fromServer);
            buf.writeUtf(packet.toServer);
        },
        buf -> new TransferInitiatedPacket(buf.readUtf(), buf.readUtf())
    );
    
    public TransferInitiatedPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readUtf());
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(TransferInitiatedPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Handler is in ClientPacketHandler to avoid loading client classes on server
        });
    }
}
