package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import static com.THproject.tharidia_things.TharidiaThings.MODID;

/**
 * Packet sent from server to client during login to request name selection
 * This triggers the pre-login name selection screen
 */
public record RequestNamePacket(boolean needsName) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<RequestNamePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "request_name"));

    public static final StreamCodec<ByteBuf, RequestNamePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        RequestNamePacket::needsName,
        RequestNamePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    // Cached reflection — initialized once
    private static boolean reflectionInitialized = false;
    private static boolean reflectionAvailable = false;
    private static java.lang.reflect.Method needsToChooseNameMethod;

    private static boolean ensureReflection() {
        if (reflectionInitialized) return reflectionAvailable;
        reflectionInitialized = true;

        try {
            Class<?> nameServiceClass = Class.forName("com.THproject.tharidia_tweaks.name.NameService");
            needsToChooseNameMethod = nameServiceClass.getMethod("needsToChooseName", ServerPlayer.class);
            reflectionAvailable = true;
        } catch (ClassNotFoundException e) {
            TharidiaThings.LOGGER.warn("tharidia_tweaks mod not found! NameService is required.");
            reflectionAvailable = false;
        } catch (NoSuchMethodException e) {
            TharidiaThings.LOGGER.warn("NameService API changed! needsToChooseName method not found.", e);
            reflectionAvailable = false;
        }

        return reflectionAvailable;
    }

    /**
     * Server-side method to check if a player needs to choose a name
     * Uses reflection to call tharidia_tweaks NameService (cached)
     */
    public static boolean checkIfPlayerNeedsName(ServerPlayer serverPlayer) {
        if (!ensureReflection()) {
            return false;
        }

        try {
            return (boolean) needsToChooseNameMethod.invoke(null, serverPlayer);
        } catch (Exception e) {
            TharidiaThings.LOGGER.error("Error checking if player needs to choose name", e);
            return false;
        }
    }
}
