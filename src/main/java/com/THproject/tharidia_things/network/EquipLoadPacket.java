package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.features.EquipServerHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EquipLoadPacket(String jsonContent) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "equip_load");
    public static final Type<EquipLoadPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, EquipLoadPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            EquipLoadPacket::jsonContent,
            EquipLoadPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final EquipLoadPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {

                if (serverPlayer.hasPermissions(2)) {
                    try {
                        System.out.println("Processing EquipLoadPacket for " + serverPlayer.getName().getString());
                        System.out.println("Received JSON length: " + packet.jsonContent.length());
                        JsonObject json = JsonParser.parseString(packet.jsonContent).getAsJsonObject();
                        EquipServerHandler.apply(serverPlayer, json);
                    } catch (Exception e) {
                        System.err.println("Error handling equip load packet: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Player " + serverPlayer.getName().getString()
                            + " tried to load equip but lacks permissions (level 2 required)");
                    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component
                            .literal("Error: You do not have permission to load equips."));
                }
            }
        });
    }
}
