package com.THproject.tharidia_things.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.THproject.tharidia_things.TharidiaThings;

public record RightClickReleasePayload(String message) implements CustomPacketPayload {

    public static final Type<RightClickReleasePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "right_click_release"));

    public static final StreamCodec<FriendlyByteBuf, RightClickReleasePayload> CODEC = CustomPacketPayload.codec(
            RightClickReleasePayload::write,
            RightClickReleasePayload::new);

    public RightClickReleasePayload(FriendlyByteBuf buffer) {
        this(buffer.readUtf());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.message);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}