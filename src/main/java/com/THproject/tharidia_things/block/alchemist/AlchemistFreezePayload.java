package com.THproject.tharidia_things.block.alchemist;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent server→client to freeze or unfreeze the local player during an
 * Alchemist Table animation (Pestel, Distillation, Centrifuge, Ritual).
 */
public record AlchemistFreezePayload(boolean freeze)
        implements CustomPacketPayload {

    public static final Type<AlchemistFreezePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "alchemist_freeze"));

    public static final StreamCodec<FriendlyByteBuf, AlchemistFreezePayload> CODEC =
            CustomPacketPayload.codec(AlchemistFreezePayload::write, AlchemistFreezePayload::new);

    public AlchemistFreezePayload(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(freeze);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
