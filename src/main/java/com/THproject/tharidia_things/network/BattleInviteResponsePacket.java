package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.event.BattleLogic;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record BattleInviteResponsePacket(UUID inviterUuid, boolean accepted) implements CustomPacketPayload {
    // 1. A unique ID for this packet
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID,
            "battle_invite_response");

    public static final Type<BattleInviteResponsePacket> TYPE = new Type<>(ID);

    // 2. The "StreamCodec" to write/read the packet data
    public static final StreamCodec<FriendlyByteBuf, BattleInviteResponsePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), // How to read/write the UUID
            BattleInviteResponsePacket::inviterUuid,
            ByteBufCodecs.BOOL, // How to read/write the boolean
            BattleInviteResponsePacket::accepted,
            BattleInviteResponsePacket::new // How to create the packet
    );

    // 3. This tells the packet what its ID is
    @Override
    public Type<BattleInviteResponsePacket> type() {
        return TYPE;
    }

    // 4. This is the code that runs ON THE SERVER when the packet is received
    public static void handle(BattleInviteResponsePacket packet, IPayloadContext context) {
        // 'context.player()' is the player who sent the packet (the target)
        ServerPlayer targetPlayer = (ServerPlayer) context.player();

        // Find the inviter using the UUID we received
        ServerPlayer inviterPlayer = targetPlayer.getServer().getPlayerList().getPlayer(packet.inviterUuid());

        // Make sure the inviter is still online
        if (inviterPlayer == null) {
            targetPlayer.sendSystemMessage(Component.literal("The inviter is no longer online."));
            return;
        }

        if (packet.accepted()) {
            // --- ACCEPTED LOGIC---
            BattleLogic.startBattle(inviterPlayer, targetPlayer);
        } else {
            // --- DECLINED LOGIC ---
            inviterPlayer.sendSystemMessage(
                    Component.literal(targetPlayer.getName().getString() + " declined your battle."));
            targetPlayer.sendSystemMessage(Component.literal("You declined the battle."));
        }
    }
}
