package com.THproject.tharidia_things.spice;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Per-player attachment storing spice levels on a 0–100 scale.
 * Default value for each spice is 50. Eating spiced food adds +10 per spice
 * present.
 * Persisted via NBT across login/logout.
 */
public class PlayerSpiceData implements INBTSerializable<CompoundTag> {
    public static final float DEFAULT_VALUE = 50.0f;
    public static final float MIN_VALUE = 0.0f;
    public static final float MAX_VALUE = 100.0f;
    public static final float GAIN_PER_FOOD = 10.0f;

    private final float[] values = new float[SpiceType.VALUES.length];
    private boolean initialized = false;

    public PlayerSpiceData() {
        for (int i = 0; i < values.length; i++) {
            values[i] = DEFAULT_VALUE;
        }
    }

    public float[] getAllValue() {
        return values;
    }

    public float get(SpiceType type) {
        return values[type.ordinal()];
    }

    public void set(SpiceType type, float value) {
        values[type.ordinal()] = clamp(value);
    }

    public void add(SpiceType type, float amount) {
        int idx = type.ordinal();
        values[idx] = clamp(values[idx] + amount);
    }

    /**
     * Called when the player eats food with spices.
     * Adds GAIN_PER_FOOD for each spice present in the food.
     */
    public void onFoodConsumed(SpiceData spiceData) {
        for (SpiceType type : spiceData.spices()) {
            add(type, GAIN_PER_FOOD);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void ensureInitialized() {
        if (!initialized) {
            for (int i = 0; i < values.length; i++) {
                values[i] = DEFAULT_VALUE;
            }
            initialized = true;
        }
    }

    private static float clamp(float value) {
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        for (SpiceType type : SpiceType.VALUES) {
            tag.putFloat(type.getName(), values[type.ordinal()]);
        }
        tag.putBoolean("initialized", initialized);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        for (SpiceType type : SpiceType.VALUES) {
            String key = type.getName();
            if (tag.contains(key)) {
                values[type.ordinal()] = clamp(tag.getFloat(key));
            }
        }
        initialized = tag.getBoolean("initialized");
    }
}
