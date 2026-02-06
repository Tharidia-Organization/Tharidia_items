package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.features.Equip;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EquipSharePacket(String targetOrSenderName, String equipName, String jsonContent)
        implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
            "equip_share");
    public static final Type<EquipSharePacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, EquipSharePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EquipSharePacket::targetOrSenderName,
            ByteBufCodecs.STRING_UTF8, EquipSharePacket::equipName,
            ByteBufCodecs.STRING_UTF8, EquipSharePacket::jsonContent,
            EquipSharePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final EquipSharePacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender) {
                // Server side: Forward to target player
                ServerPlayer target = sender.server.getPlayerList().getPlayerByName(packet.targetOrSenderName);
                if (target != null) {
                    if (target.hasPermissions(4)) {
                        // Send to target with sender's name
                        EquipSharePacket forwardPacket = new EquipSharePacket(sender.getGameProfile().getName(),
                                packet.equipName, packet.jsonContent);
                        PacketDistributor.sendToPlayer(target, forwardPacket);
                    } else {
                        sender.sendSystemMessage(Component.literal(
                                "Cannot share: Player " + target.getGameProfile().getName() + " is not an operator."));
                    }
                } else {
                    sender.sendSystemMessage(Component.literal("Player " + packet.targetOrSenderName + " not found"));
                }
            } else {
                // Client side: Receive share
                Equip.handleReceivedShare(packet.targetOrSenderName, packet.equipName, packet.jsonContent);
            }
        });
    }
}
