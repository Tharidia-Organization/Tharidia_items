package com.THproject.tharidia_things.network;

import java.util.List;
import java.util.UUID;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.command.EquipCommand;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EquipListSyncPacket(List<String> equips, boolean pending) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
            "equip_list_sync");
    public static final Type<EquipListSyncPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, EquipListSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), EquipListSyncPacket::equips,
            ByteBufCodecs.BOOL, EquipListSyncPacket::pending,
            EquipListSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final EquipListSyncPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // Server side: update cache
            UUID playerId = context.player().getUUID();
            if (packet.pending) {
                EquipCommand.updatePendingCache(playerId, packet.equips);
            } else {
                EquipCommand.updateEquipCache(playerId, packet.equips);
            }
        });
    }
}
