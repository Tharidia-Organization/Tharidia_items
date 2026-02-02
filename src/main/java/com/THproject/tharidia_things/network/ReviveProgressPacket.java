package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReviveProgressPacket(int currentResTime, int maxResTime, String text) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ReviveProgressPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "revive_progress"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReviveProgressPacket> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT,
            ReviveProgressPacket::currentResTime,
            net.minecraft.network.codec.ByteBufCodecs.INT,
            ReviveProgressPacket::maxResTime,
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8,
            ReviveProgressPacket::text,
            ReviveProgressPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
