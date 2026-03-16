package com.THproject.tharidia_things.block.alchemist;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent server→client to start or stop the looping stir sound on the Alchemist Table.
 * Using a packet allows the client to stop the sound mid-playback.
 */
public record AlchemistStirSoundPayload(boolean start, double x, double y, double z)
        implements CustomPacketPayload {

    public static final Type<AlchemistStirSoundPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "alchemist_stir_sound"));

    public static final StreamCodec<FriendlyByteBuf, AlchemistStirSoundPayload> CODEC =
            CustomPacketPayload.codec(AlchemistStirSoundPayload::write, AlchemistStirSoundPayload::new);

    public AlchemistStirSoundPayload(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(start);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
