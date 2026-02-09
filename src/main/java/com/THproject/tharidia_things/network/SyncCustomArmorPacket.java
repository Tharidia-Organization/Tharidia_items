package com.THproject.tharidia_things.network;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.compoundTag.CustomArmorAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record SyncCustomArmorPacket(int entityId, List<ItemStack> items) implements CustomPacketPayload {
    public static final Type<SyncCustomArmorPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, "sync_custom_armor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCustomArmorPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entityId);
                buf.writeVarInt(packet.items.size());
                for (ItemStack stack : packet.items) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                }
            },
            buf -> {
                int id = buf.readVarInt();
                int size = buf.readVarInt();
                List<ItemStack> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                }
                return new SyncCustomArmorPacket(id, list);
            });

    @Override
    public Type<SyncCustomArmorPacket> type() {
        return TYPE;
    }

    public static void handle(SyncCustomArmorPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player localPlayer = context.player();
            if (localPlayer == null || localPlayer.level() == null)
                return;

            Entity entity = localPlayer.level().getEntity(packet.entityId);
            if (entity instanceof Player savedPlayer) {
                Container container = savedPlayer.getData(CustomArmorAttachments.CUSTOM_ARMOR_DATA.get());
                for (int i = 0; i < packet.items.size() && i < container.getContainerSize(); i++) {
                    container.setItem(i, packet.items.get(i));
                }
            }
        });
    }
}
