package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GiveUpPacket() implements CustomPacketPayload {
    public static final Type<GiveUpPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "give_up"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GiveUpPacket> STREAM_CODEC = StreamCodec
            .unit(new GiveUpPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GiveUpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                serverPlayer.kill();
            }
        });
    }
}
