package com.THproject.tharidia_things.diet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

import java.util.EnumMap;

/**
 * Immutable per-item diet contribution profile.
 */
public record DietProfile(float[] values) {
    public static final DietProfile EMPTY = new DietProfile(new float[DietCategory.COUNT]);

    public static final Codec<DietProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("grain", 0.0f).forGetter(profile -> profile.get(DietCategory.GRAIN)),
            Codec.FLOAT.optionalFieldOf("protein", 0.0f).forGetter(profile -> profile.get(DietCategory.PROTEIN)),
            Codec.FLOAT.optionalFieldOf("vegetable", 0.0f).forGetter(profile -> profile.get(DietCategory.VEGETABLE)),
            Codec.FLOAT.optionalFieldOf("fruit", 0.0f).forGetter(profile -> profile.get(DietCategory.FRUIT)),
            Codec.FLOAT.optionalFieldOf("sugar", 0.0f).forGetter(profile -> profile.get(DietCategory.SUGAR)),
            Codec.FLOAT.optionalFieldOf("water", 0.0f).forGetter(profile -> profile.get(DietCategory.WATER))
    ).apply(instance, DietProfile::of));

    public static DietProfile of(float grain, float protein, float vegetable, float fruit, float sugar, float water) {
        float[] array = new float[DietCategory.COUNT];
        array[DietCategory.GRAIN.ordinal()] = clamp(grain);
        array[DietCategory.PROTEIN.ordinal()] = clamp(protein);
        array[DietCategory.VEGETABLE.ordinal()] = clamp(vegetable);
        array[DietCategory.FRUIT.ordinal()] = clamp(fruit);
        array[DietCategory.SUGAR.ordinal()] = clamp(sugar);
        array[DietCategory.WATER.ordinal()] = clamp(water);
        return new DietProfile(array);
    }

    private static float clamp(float value) {
        return Mth.clamp(value, 0.0f, 1000.0f);
    }

    public float get(DietCategory category) {
        return values[category.ordinal()];
    }

    public float[] copyValues() {
        float[] copy = new float[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        return copy;
    }

    public boolean isEmpty() {
        for (float value : values) {
            if (value > 0.0f) {
                return false;
            }
        }
        return true;
    }

    public static DietProfile fromEnumMap(EnumMap<DietCategory, Float> map) {
        float[] array = new float[DietCategory.COUNT];
        for (DietCategory category : DietCategory.VALUES) {
            array[category.ordinal()] = clamp(map.getOrDefault(category, 0.0f));
        }
        return new DietProfile(array);
    }
}
