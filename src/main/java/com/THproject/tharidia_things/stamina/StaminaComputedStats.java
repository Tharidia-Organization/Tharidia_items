package com.THproject.tharidia_things.stamina;

public record StaminaComputedStats(
        float maxStamina,
        float regenRatePerSecond,
        float consumptionMultiplier,
        float attackCostMultiplier,
        float rollCostMultiplier,
        float bowTensionCostMultiplier,
        float sprintThresholdFraction,
        float regenDelayAfterConsumptionSeconds,
        boolean blockRegenOverride
) {
}
