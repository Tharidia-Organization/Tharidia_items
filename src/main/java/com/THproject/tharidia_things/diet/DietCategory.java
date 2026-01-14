package com.THproject.tharidia_things.diet;

/**
 * Represents the six diet categories tracked for every player.
 */
public enum DietCategory {
    GRAIN,
    PROTEIN,
    VEGETABLE,
    FRUIT,
    SUGAR,
    WATER;

    public static final DietCategory[] VALUES = values();
    public static final int COUNT = VALUES.length;
}
