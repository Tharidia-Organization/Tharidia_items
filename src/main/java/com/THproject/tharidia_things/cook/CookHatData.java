package com.THproject.tharidia_things.cook;

import java.util.UUID;
import java.util.function.UnaryOperator;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CookHatData {
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> PLAYER_UUID = register(
        "player_uuid", builder -> builder.persistent(UUIDUtil.CODEC)
    );

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return TharidiaThings.DATA_COMPONENT_TYPES.register(name,
                () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register() {}
}
