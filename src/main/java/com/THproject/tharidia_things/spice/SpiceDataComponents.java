package com.THproject.tharidia_things.spice;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SpiceDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TharidiaThings.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpiceData>> SPICE_DATA =
            DATA_COMPONENT_TYPES.register("spice_data", () ->
                    DataComponentType.<SpiceData>builder()
                            .persistent(SpiceData.CODEC)
                            .networkSynchronized(SpiceData.STREAM_CODEC)
                            .build());
}
