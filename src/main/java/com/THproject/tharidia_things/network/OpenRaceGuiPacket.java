package com.THproject.tharidia_things.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-to-Client packet to open the race selection GUI.
 * Sent when a player interacts with a RacePointEntity.
 * The actual handler that opens the GUI is in ClientPacketHandler.handleOpenRaceGui().
 */
public record OpenRaceGuiPacket() implements CustomPacketPayload {
    public static final Type<OpenRaceGuiPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("tharidiathings", "open_race_gui"));

    public static final StreamCodec<ByteBuf, OpenRaceGuiPacket> STREAM_CODEC = StreamCodec.unit(
            new OpenRaceGuiPacket()
    );

    @Override
    public Type<OpenRaceGuiPacket> type() {
        return TYPE;
    }

    /**
     * Empty handler — actual handling is done in ClientPacketHandler.handleOpenRaceGui()
     */
    public static void handle(OpenRaceGuiPacket packet, IPayloadContext context) {
        // Intentionally empty - see ClientPacketHandler.handleOpenRaceGui()
    }
}
