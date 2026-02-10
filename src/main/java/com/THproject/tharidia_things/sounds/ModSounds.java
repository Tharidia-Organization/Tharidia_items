package com.THproject.tharidia_things.sounds;

import java.util.function.Supplier;

import com.THproject.tharidia_things.TharidiaThings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister
            .create(BuiltInRegistries.SOUND_EVENT, TharidiaThings.MODID);

    public static final Supplier<SoundEvent> CRUSHER_HAMMER_USE = registerSoundEvent("crusher_hammer_use");
    public static final Supplier<SoundEvent> CHUNK_BREAK = registerSoundEvent("chunk_break");
    public static final Supplier<SoundEvent> DUNGEON_START = registerSoundEvent("dungeon_start");
    public static final Supplier<SoundEvent> PULVERIZER_WORKING = registerSoundEvent("pulverizer_working");

    public static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TharidiaThings.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
