package com.THproject.tharidia_things.gametest;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.config.StaminaConfig;
import com.THproject.tharidia_things.stamina.StaminaComputedStats;
import com.THproject.tharidia_things.stamina.StaminaModifier;
import com.THproject.tharidia_things.stamina.StaminaModifierEngine;
import com.THproject.tharidia_things.stamina.StaminaModifierType;
import com.THproject.tharidia_things.stamina.StaminaTagIntegration;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.List;

@GameTestHolder(TharidiaThings.MODID)
public class StaminaGameTests {
    @GameTest(template = "empty")
    public static void modifier_engine_applies_flat_and_percent(GameTestHelper helper) {
        List<StaminaModifier> modifiers = List.of(
                new StaminaModifier(StaminaModifierType.MAX_STAMINA_PERCENT, 10.0f, true, "p", 0),
                new StaminaModifier(StaminaModifierType.MAX_STAMINA_FLAT, 5.0f, false, "f", 1),
                new StaminaModifier(StaminaModifierType.REGEN_RATE_PERCENT, 50.0f, true, "r", 2)
        );

        StaminaComputedStats stats = StaminaModifierEngine.compute(
                100.0f,
                10.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                0.2f,
                0.8f,
                false,
                modifiers
        );

        if (Math.abs(stats.maxStamina() - 115.0f) > 0.001f) {
            throw new IllegalStateException("Unexpected maxStamina: " + stats.maxStamina());
        }
        if (Math.abs(stats.regenRatePerSecond() - 15.0f) > 0.001f) {
            throw new IllegalStateException("Unexpected regenRatePerSecond: " + stats.regenRatePerSecond());
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tag_integration_registers_and_resolves_modifier(GameTestHelper helper) {
        StaminaTagIntegration.registerTagMapping("skill:test_max", StaminaModifierType.MAX_STAMINA_FLAT, 12.0f, false, 0);
        StaminaModifier modifier = StaminaTagIntegration.getModifierForTag("skill:test_max");
        if (modifier == null) {
            throw new IllegalStateException("Modifier not resolved");
        }
        if (modifier.type() != StaminaModifierType.MAX_STAMINA_FLAT || Math.abs(modifier.value() - 12.0f) > 0.001f) {
            throw new IllegalStateException("Modifier mismatch: " + modifier);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bow_tension_cost_scales_with_delta_and_modifiers(GameTestHelper helper) {
        List<StaminaModifier> modifiers = List.of(
                new StaminaModifier(StaminaModifierType.CONSUMPTION_MULTIPLIER, 50.0f, true, "c", 0),
                new StaminaModifier(StaminaModifierType.BOW_TENSION_COST_PERCENT, 100.0f, true, "b", 1)
        );

        StaminaComputedStats stats = StaminaModifierEngine.compute(
                100.0f,
                10.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                0.2f,
                0.8f,
                false,
                modifiers
        );

        float tensionSeconds = StaminaConfig.getBowTensionThresholdSeconds() + (StaminaConfig.getBowMaxTensionTimeSeconds() * 0.5f);
        float bowWeight = 1.0f;

        float expectedCurve = StaminaConfig.isBowUseWeaponWeight() ? StaminaConfig.computeBowCurveMultiplier(bowWeight) : 1.0f;
        float expectedCurveFactor = 0.5f;
        float expected = (StaminaConfig.getBowConsumptionRatePerSecond() / 20.0f) * expectedCurveFactor * expectedCurve * stats.consumptionMultiplier() * stats.bowTensionCostMultiplier();

        float computed = StaminaConfig.computeBowHoldTickCost(tensionSeconds, bowWeight, stats);
        if (Math.abs(computed - expected) > 0.0001f) {
            throw new IllegalStateException("Unexpected bow tension cost: " + computed + " expected " + expected);
        }

        float holdComputed = StaminaConfig.computeBowHoldTickCost(0.0f, bowWeight, stats);
        if (Math.abs(holdComputed) > 0.0001f) {
            throw new IllegalStateException("Expected zero cost when deltaPower is zero");
        }

        float power = 0.75f;
        float expectedRelease = StaminaConfig.getBowBaseCost() * power * expectedCurve * stats.consumptionMultiplier() * stats.bowTensionCostMultiplier();
        float releaseCost = StaminaConfig.computeBowReleaseCost(power, bowWeight, stats);
        if (Math.abs(releaseCost - expectedRelease) > 0.0001f) {
            throw new IllegalStateException("Unexpected bow release cost: " + releaseCost + " expected " + expectedRelease);
        }

        helper.succeed();
    }
}
