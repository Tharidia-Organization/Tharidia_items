package com.THproject.tharidia_things.houseboundry;

/**
 * Calculated state based on animal wellness stats.
 * Determines production rate multipliers and visual feedback.
 */
public enum AnimalState {
    /**
     * Best state: comfort >= 70 AND stress <= 20 AND hygiene >= 60
     * Production interval x0.7 (30% faster)
     */
    GOLD,

    /**
     * Normal state: comfort 40-69 OR stress 21-49 (and not in other states)
     * Production interval x1.0 (normal)
     */
    OK,

    /**
     * Poor state: comfort 20-39 OR stress 50-69
     * Production interval x1.3 (30% slower)
     */
    LOW,

    /**
     * Worst state: comfort < 20 OR stress >= 70 OR diseased
     * No production at all
     */
    CRITICAL;

    /**
     * Calculates the animal state based on wellness data.
     *
     * @param comfort comfort level (0-100)
     * @param stress stress level (0-100, higher is worse)
     * @param hygiene hygiene level (0-100)
     * @param diseased whether the animal is diseased
     * @return the calculated AnimalState
     */
    public static AnimalState calculateState(int comfort, int stress, int hygiene, boolean diseased) {
        // CRITICAL: diseased OR comfort < 20 OR stress >= 70
        if (diseased || comfort < 20 || stress >= 70) {
            return CRITICAL;
        }

        // GOLD: comfort >= 70 AND stress <= 20 AND hygiene >= 60
        if (comfort >= 70 && stress <= 20 && hygiene >= 60) {
            return GOLD;
        }

        // LOW: comfort 20-39 OR stress 50-69
        if ((comfort >= 20 && comfort <= 39) || (stress >= 50 && stress <= 69)) {
            return LOW;
        }

        // OK: everything else (comfort 40-69 OR stress 21-49)
        return OK;
    }

    /**
     * Convenience method to calculate state from AnimalWellnessData.
     */
    public static AnimalState calculateState(AnimalWellnessData data) {
        return calculateState(
            data.getComfort(),
            data.getStress(),
            data.getHygiene(),
            data.isDiseased()
        );
    }

    /**
     * Gets the production interval multiplier for this state.
     *
     * @return multiplier (0.7 for GOLD, 1.0 for OK, 1.3 for LOW, -1 for CRITICAL meaning no production)
     */
    public double getProductionMultiplier() {
        return switch (this) {
            case GOLD -> 0.7;
            case OK -> 1.0;
            case LOW -> 1.3;
            case CRITICAL -> -1.0; // No production
        };
    }

    /**
     * Gets the growth speed multiplier for this state.
     *
     * @return multiplier (1.25 for GOLD, 1.0 for OK, 0.75 for LOW, 0 for CRITICAL)
     */
    public double getGrowthMultiplier() {
        return switch (this) {
            case GOLD -> 1.25;
            case OK -> 1.0;
            case LOW -> 0.75;
            case CRITICAL -> 0.0; // Growth stopped
        };
    }
}
