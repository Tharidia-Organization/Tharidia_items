package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.features.Equip;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EquipActionPacket(byte action, String name, String extraData) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
            "equip_action");
    public static final Type<EquipActionPacket> TYPE = new Type<>(ID);

    // Actions
    public static final byte ACTION_SAVE = 0;
    public static final byte ACTION_LOAD = 1;
    public static final byte ACTION_DELETE = 2;
    public static final byte ACTION_SHARE_REQUEST = 3; // Server asks client to share 'name' with 'extraData' (target
                                                       // player)
    public static final byte ACTION_ACCEPT = 4;
    public static final byte ACTION_DECLINE = 5;
    public static final byte ACTION_SYNC_REQUEST = 6; // Server asks client to sync list
    public static final byte ACTION_RENAME = 7;

    public static final StreamCodec<FriendlyByteBuf, EquipActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, EquipActionPacket::action,
            ByteBufCodecs.STRING_UTF8, EquipActionPacket::name,
            ByteBufCodecs.STRING_UTF8, EquipActionPacket::extraData,
            EquipActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final EquipActionPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // This packet runs on CLIENT side (triggered by server command)
            Equip.handleServerRequest(packet.action, packet.name, packet.extraData);
        });
    }
}
