package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet to show fatigue warning on client
 */
public record FatigueWarningPacket(int minutesLeft) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FatigueWarningPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "fatigue_warning"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, FatigueWarningPacket> STREAM_CODEC = 
        StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT,
            FatigueWarningPacket::minutesLeft,
            FatigueWarningPacket::new
        );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
