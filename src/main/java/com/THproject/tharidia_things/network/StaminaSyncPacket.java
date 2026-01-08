package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.stamina.StaminaAttachments;
import com.THproject.tharidia_things.stamina.StaminaData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record StaminaSyncPacket(float currentStamina, float maxStamina, boolean inCombat) implements CustomPacketPayload {
    public static final Type<StaminaSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "stamina_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StaminaSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.FLOAT,
                    StaminaSyncPacket::currentStamina,
                    net.minecraft.network.codec.ByteBufCodecs.FLOAT,
                    StaminaSyncPacket::maxStamina,
                    net.minecraft.network.codec.ByteBufCodecs.BOOL,
                    StaminaSyncPacket::inCombat,
                    StaminaSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StaminaSyncPacket packet, Player player) {
        StaminaData data = player.getData(StaminaAttachments.STAMINA_DATA);
        data.setMaxStamina(packet.maxStamina());
        data.setCurrentStamina(packet.currentStamina());
        data.setInCombat(packet.inCombat());
    }
}

