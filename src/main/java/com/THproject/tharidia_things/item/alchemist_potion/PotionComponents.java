package com.THproject.tharidia_things.item.alchemist_potion;

import java.util.function.UnaryOperator;

import com.THproject.tharidia_things.TharidiaThings;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class PotionComponents {
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> CRAFTED_TIME = register(
            "potion_crafted_time", builder -> builder.persistent(Codec.LONG));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return TharidiaThings.DATA_COMPONENT_TYPES.register(name,
                () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register() {
    }
}
