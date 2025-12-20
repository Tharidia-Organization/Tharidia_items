package com.THproject.tharidia_things.diet;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Stores per-player diet state (six categories plus decay tracking).
 */
public class DietData implements INBTSerializable<CompoundTag> {
    public static final float DEFAULT_START_PERCENT = 0.8f;
    private final float[] values = new float[DietCategory.COUNT];
    private long lastDecayTimeMs = System.currentTimeMillis();
    private boolean dirty = false;
    private boolean initialized = false;

    public DietData() {}

    public float get(DietCategory category) {
        return values[category.ordinal()];
    }

    public void set(DietCategory category, float value, float max) {
        int idx = category.ordinal();
        float clamped = clamp(value, max);
        if (values[idx] != clamped) {
            values[idx] = clamped;
            markDirty();
        }
    }

    public void add(DietProfile profile, DietProfile maxValues) {
        for (DietCategory category : DietCategory.VALUES) {
            float delta = profile.get(category);
            if (delta <= 0.0f) continue;
            int idx = category.ordinal();
            float max = maxValues.get(category);
            float newValue = clamp(values[idx] + delta, max);
            if (newValue != values[idx]) {
                values[idx] = newValue;
                markDirty();
            }
        }
    }

    public boolean applyDecay(DietProfile decayRates, float deltaSeconds) {
        if (deltaSeconds <= 0.0f) {
            return false;
        }

        boolean changed = false;
        for (DietCategory category : DietCategory.VALUES) {
            int idx = category.ordinal();
            float value = values[idx];
            if (value <= 0.0f) continue;

            float decayPerSecond = decayRates.get(category);
            if (decayPerSecond <= 0.0f) continue;

            float newValue = Math.max(0.0f, value - (decayPerSecond * deltaSeconds));
            if (newValue != value) {
                changed = true;
                values[idx] = newValue;
            }
        }
        if (changed) {
            markDirty();
        }
        return changed;
    }

    public void ensureInitialized(DietProfile maxValues, float percent) {
        if (initialized) {
            return;
        }
        preload(maxValues, percent);
        initialized = true;
        dirty = true;
    }
    
    public void reset(DietProfile maxValues, float percent) {
        preload(maxValues, percent);
        initialized = true;
        markDirty();
    }

    public float[] copyValues() {
        float[] copy = new float[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        return copy;
    }

    public void setAll(float[] newValues) {
        if (newValues.length != values.length) {
            throw new IllegalArgumentException("Expected " + values.length + " values, got " + newValues.length);
        }
        boolean changed = false;
        for (int i = 0; i < values.length; i++) {
            float clamped = Math.max(0.0f, newValues[i]);
            if (values[i] != clamped) {
                values[i] = clamped;
                changed = true;
            }
        }
        if (changed) {
            markDirty();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public long getLastDecayTimeMs() {
        return lastDecayTimeMs;
    }

    public void setLastDecayTimeMs(long timestamp) {
        this.lastDecayTimeMs = timestamp;
    }

    private float clamp(float value, float max) {
        if (max <= 0.0f) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(value, max));
    }

    public void markDirty() {
        this.dirty = true;
        this.initialized = true;
    }

    private void preloadDefaults(float percent) {
        preload(DietRegistry.getMaxValues(), percent);
        this.initialized = false;
        this.dirty = false;
    }

    private void preload(DietProfile maxValues, float percent) {
        float clampedPercent = Math.max(0.0f, Math.min(1.0f, percent));
        for (DietCategory category : DietCategory.VALUES) {
            int idx = category.ordinal();
            float max = maxValues.get(category);
            values[idx] = clamp(max * clampedPercent, max);
        }
    }

    public boolean consumeDirty() {
        if (dirty) {
            dirty = false;
            return true;
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        for (DietCategory category : DietCategory.VALUES) {
            tag.putFloat(category.name().toLowerCase(), values[category.ordinal()]);
        }
        tag.putLong("lastDecayTimeMs", lastDecayTimeMs);
        tag.putBoolean("initialized", initialized);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        for (DietCategory category : DietCategory.VALUES) {
            String key = category.name().toLowerCase();
            if (tag.contains(key)) {
                values[category.ordinal()] = tag.getFloat(key);
            }
        }
        lastDecayTimeMs = tag.contains("lastDecayTimeMs")
                ? tag.getLong("lastDecayTimeMs")
                : System.currentTimeMillis();
        initialized = tag.getBoolean("initialized");
    }
}
