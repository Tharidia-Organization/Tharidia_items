package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.TharidiaThingsClient;
import com.THproject.tharidia_things.diet.ClientDietProfileCache;
import com.THproject.tharidia_things.diet.DietCategory;
import com.THproject.tharidia_things.diet.DietProfile;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public record DietProfileSyncPacket(Map<ResourceLocation, DietProfile> profiles) implements CustomPacketPayload {
    public static final Type<DietProfileSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "diet_profile_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DietProfileSyncPacket> STREAM_CODEC =
            StreamCodec.of(
                    DietProfileSyncPacket::encode,
                    DietProfileSyncPacket::decode
            );

    private static void encode(RegistryFriendlyByteBuf buf, DietProfileSyncPacket packet) {
        buf.writeVarInt(packet.profiles.size());
        for (Map.Entry<ResourceLocation, DietProfile> entry : packet.profiles.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            float[] values = entry.getValue().values();
            for (float value : values) {
                buf.writeFloat(value);
            }
        }
    }

    private static DietProfileSyncPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, DietProfile> profiles = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            float[] values = new float[DietCategory.COUNT];
            for (int j = 0; j < DietCategory.COUNT; j++) {
                values[j] = buf.readFloat();
            }
            profiles.put(id, new DietProfile(values));
        }
        return new DietProfileSyncPacket(profiles);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DietProfileSyncPacket packet, Player player) {
        ClientDietProfileCache clientCache = TharidiaThingsClient.getClientDietCache();
        if (clientCache != null) {
            clientCache.updateFromServer(packet.profiles());
        }
    }
}
