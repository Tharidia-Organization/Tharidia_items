package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent server→client with the list of item IDs this specific player is allowed to see in JEI.
 * An empty list means "no filter active – show everything".
 * The server computes this from the player's entity tags and the loaded jei_filters.json.
 */
public record JeiFilterSyncPacket(List<String> allowedItemIds) implements CustomPacketPayload {

    public static final Type<JeiFilterSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "jei_filter_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JeiFilterSyncPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public JeiFilterSyncPacket decode(RegistryFriendlyByteBuf buf) {
                    int size = buf.readVarInt();
                    List<String> ids = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        ids.add(buf.readUtf());
                    }
                    return new JeiFilterSyncPacket(ids);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, JeiFilterSyncPacket packet) {
                    buf.writeVarInt(packet.allowedItemIds().size());
                    for (String id : packet.allowedItemIds()) {
                        buf.writeUtf(id);
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
