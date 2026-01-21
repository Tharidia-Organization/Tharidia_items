package com.THproject.tharidia_things.houseboundry;

/**
 * Lifecycle phases for animals in the Houseboundry system.
 * Based on real-time, not Minecraft ticks.
 */
public enum LifecyclePhase {
    /**
     * Baby phase - cannot produce, consumes more food.
     * Duration: configurable per animal type (default 1 hour real time)
     */
    BABY,

    /**
     * Productive phase - can produce secondary products.
     * Duration: configurable per animal type (default 10 days real time)
     */
    PRODUCTIVE,

    /**
     * Barren phase - cannot produce anymore, only slaughter value.
     * Permanent state after productive phase ends.
     */
    BARREN;

    /**
     * Gets the phase from its name string.
     * Returns BABY if name is invalid.
     */
    public static LifecyclePhase fromName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BABY;
        }
    }
}
