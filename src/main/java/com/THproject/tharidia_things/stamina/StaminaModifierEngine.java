package com.THproject.tharidia_things.stamina;

import java.util.Comparator;
import java.util.List;

public final class StaminaModifierEngine {
    private StaminaModifierEngine() {
    }

    public static StaminaComputedStats compute(
            float baseMaxStamina,
            float baseRegenRatePerSecond,
            float baseConsumptionMultiplier,
            float baseAttackCostMultiplier,
            float baseRollCostMultiplier,
            float baseBowTensionCostMultiplier,
            float baseSprintThresholdFraction,
            float baseRegenDelaySeconds,
            boolean baseBlockRegenOverride,
            List<StaminaModifier> modifiers
    ) {
        float maxStamina = baseMaxStamina;
        float regenRatePerSecond = baseRegenRatePerSecond;
        float consumptionMultiplier = baseConsumptionMultiplier;
        float attackCostMultiplier = baseAttackCostMultiplier;
        float rollCostMultiplier = baseRollCostMultiplier;
        float bowTensionCostMultiplier = baseBowTensionCostMultiplier;
        float sprintThresholdFraction = baseSprintThresholdFraction;
        float regenDelayAfterConsumptionSeconds = baseRegenDelaySeconds;
        boolean blockRegenOverride = baseBlockRegenOverride;

        if (modifiers != null && !modifiers.isEmpty()) {
            for (StaminaModifier modifier : modifiers.stream().sorted(Comparator.comparingInt(StaminaModifier::priority)).toList()) {
                if (modifier == null || modifier.source() == null) {
                    continue;
                }

                switch (modifier.type()) {
                    case MAX_STAMINA_FLAT -> maxStamina += modifier.value();
                    case MAX_STAMINA_PERCENT -> maxStamina *= (1.0f + (modifier.value() / 100.0f));
                    case REGEN_RATE_FLAT -> regenRatePerSecond += modifier.value();
                    case REGEN_RATE_PERCENT -> regenRatePerSecond *= (1.0f + (modifier.value() / 100.0f));
                    case CONSUMPTION_MULTIPLIER -> consumptionMultiplier *= (1.0f + (modifier.value() / 100.0f));
                    case ATTACK_COST_PERCENT -> attackCostMultiplier *= (1.0f + (modifier.value() / 100.0f));
                    case ROLL_COST_PERCENT -> rollCostMultiplier *= (1.0f + (modifier.value() / 100.0f));
                    case BOW_TENSION_COST_PERCENT -> bowTensionCostMultiplier *= (1.0f + (modifier.value() / 100.0f));
                    case SPRINT_THRESHOLD_PERCENT -> sprintThresholdFraction *= (1.0f + (modifier.value() / 100.0f));
                    case REGEN_DELAY_FLAT -> regenDelayAfterConsumptionSeconds += modifier.value();
                    case BLOCK_REGEN_OVERRIDE -> blockRegenOverride = modifier.value() != 0.0f;
                    default -> {
                    }
                }
            }
        }

        if (maxStamina < 1.0f) {
            maxStamina = 1.0f;
        }
        if (regenRatePerSecond < 0.0f) {
            regenRatePerSecond = 0.0f;
        }
        if (consumptionMultiplier < 0.0f) {
            consumptionMultiplier = 0.0f;
        }
        if (attackCostMultiplier < 0.0f) {
            attackCostMultiplier = 0.0f;
        }
        if (rollCostMultiplier < 0.0f) {
            rollCostMultiplier = 0.0f;
        }
        if (bowTensionCostMultiplier < 0.0f) {
            bowTensionCostMultiplier = 0.0f;
        }
        if (sprintThresholdFraction < 0.0f) {
            sprintThresholdFraction = 0.0f;
        }
        if (sprintThresholdFraction > 1.0f) {
            sprintThresholdFraction = 1.0f;
        }
        if (regenDelayAfterConsumptionSeconds < 0.0f) {
            regenDelayAfterConsumptionSeconds = 0.0f;
        }

        return new StaminaComputedStats(
                maxStamina,
                regenRatePerSecond,
                consumptionMultiplier,
                attackCostMultiplier,
                rollCostMultiplier,
                bowTensionCostMultiplier,
                sprintThresholdFraction,
                regenDelayAfterConsumptionSeconds,
                blockRegenOverride
        );
    }
}
