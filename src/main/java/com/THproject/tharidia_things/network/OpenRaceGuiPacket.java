package com.THproject.tharidia_things.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-to-Client packet to open the race selection GUI.
 *
 * <p>This packet is sent from the server when a player interacts with a
 * RacePointEntity. The actual handler that opens the GUI screen is in
 * {@link com.THproject.tharidia_things.client.ClientPacketHandler#handleOpenRaceGui}
 * to avoid loading client-side classes on the server.</p>
 *
 * @see com.THproject.tharidia_things.client.ClientPacketHandler#handleOpenRaceGui
 * @see com.THproject.tharidia_things.entity.RacePointEntity
 */
public record OpenRaceGuiPacket(String raceName) implements CustomPacketPayload {
    public static final Type<OpenRaceGuiPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("tharidiathings", "open_race_gui"));

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

    /**
     * Empty handler - actual handling is done in ClientPacketHandler.handleOpenRaceGui()
     * to prevent server from loading client GUI classes.
     */
    public static void handle(OpenRaceGuiPacket packet, IPayloadContext context) {
        // Intentionally empty - see ClientPacketHandler.handleOpenRaceGui()
    }
}
