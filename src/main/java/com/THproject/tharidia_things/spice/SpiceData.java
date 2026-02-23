package com.THproject.tharidia_things.spice;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Data component stored on food ItemStacks to track which spices have been applied.
 */
public record SpiceData(Set<SpiceType> spices) {

    public static final SpiceData EMPTY = new SpiceData(EnumSet.noneOf(SpiceType.class));

    // Codec: serialize as list of spice names
    public static final Codec<SpiceData> CODEC = Codec.STRING.listOf().xmap(
            names -> {
                EnumSet<SpiceType> set = EnumSet.noneOf(SpiceType.class);
                for (String name : names) {
                    SpiceType type = SpiceType.byName(name);
                    if (type != null) {
                        set.add(type);
                    }
                }
                return new SpiceData(set);
            },
            data -> data.spices().stream().map(SpiceType::getName).toList()
    );

    // StreamCodec: serialize as list of ordinals
    public static final StreamCodec<ByteBuf, SpiceData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SpiceData decode(ByteBuf buf) {
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            EnumSet<SpiceType> set = EnumSet.noneOf(SpiceType.class);
            for (int i = 0; i < size; i++) {
                int ordinal = ByteBufCodecs.VAR_INT.decode(buf);
                if (ordinal >= 0 && ordinal < SpiceType.VALUES.length) {
                    set.add(SpiceType.VALUES[ordinal]);
                }
            }
            return new SpiceData(set);
        }

        @Override
        public void encode(ByteBuf buf, SpiceData data) {
            List<SpiceType> list = data.spices().stream().toList();
            ByteBufCodecs.VAR_INT.encode(buf, list.size());
            for (SpiceType type : list) {
                ByteBufCodecs.VAR_INT.encode(buf, type.ordinal());
            }
        }
    };

    public boolean hasSpice(SpiceType type) {
        return spices.contains(type);
    }

    public SpiceData withSpice(SpiceType type) {
        if (spices.contains(type)) {
            return this;
        }
        EnumSet<SpiceType> newSet = EnumSet.copyOf(spices);
        newSet.add(type);
        return new SpiceData(newSet);
    }

    public SpiceData merge(SpiceData other) {
        EnumSet<SpiceType> newSet = EnumSet.noneOf(SpiceType.class);
        newSet.addAll(spices);
        newSet.addAll(other.spices());
        return new SpiceData(newSet);
    }

    public boolean isEmpty() {
        return spices.isEmpty();
    }

    // EnumSet doesn't guarantee consistent equals for records, so wrap in immutable view
    public SpiceData {
        spices = spices.isEmpty() ? EnumSet.noneOf(SpiceType.class) : EnumSet.copyOf(spices);
    }
}
