package com.THproject.tharidia_things.config;

import com.THproject.tharidia_things.Config;
import com.THproject.tharidia_things.stamina.StaminaComputedStats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StaminaConfig extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static float baseMaxStamina;
    private static float baseRegenRatePerSecond;
    private static float baseSprintThresholdFraction;
    private static float combatTimeoutSeconds;

    private static float attackBaseCost;
    private static boolean attackUseWeaponWeight;
    private static String attackCurveType;
    private static List<Float> attackCurveCoefficients;

    private static float bowTensionThresholdSeconds;
    private static float bowBaseCost;
    private static float bowConsumptionRatePerSecond;
    private static float bowMaxTensionTimeSeconds;
    private static boolean bowUseWeaponWeight;
    private static String bowCurveType;
    private static List<Float> bowCurveCoefficients;

    private static float regenDelayAfterConsumptionSeconds;

    static {
        initializeDefaults();
    }

    public StaminaConfig() {
        super(GSON, "stamina_config");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> configs, ResourceManager resourceManager, ProfilerFiller profiler) {
        initializeDefaults();

        if (configs.isEmpty()) {
            return;
        }

        for (Map.Entry<ResourceLocation, JsonElement> entry : configs.entrySet()) {
            try {
                JsonObject root = entry.getValue().getAsJsonObject();

                if (root.has("baseValues") && root.get("baseValues").isJsonObject()) {
                    JsonObject base = root.getAsJsonObject("baseValues");
                    baseMaxStamina = getFloat(base, "maxStamina", baseMaxStamina);
                    baseRegenRatePerSecond = getFloat(base, "baseRegenRate", baseRegenRatePerSecond);
                    baseSprintThresholdFraction = getFloat(base, "sprintThreshold", baseSprintThresholdFraction);
                    combatTimeoutSeconds = getFloat(base, "combatTimeout", combatTimeoutSeconds);
                }

                if (root.has("consumption") && root.get("consumption").isJsonObject()) {
                    JsonObject consumption = root.getAsJsonObject("consumption");
                    if (consumption.has("attacks") && consumption.get("attacks").isJsonObject()) {
                        JsonObject attacks = consumption.getAsJsonObject("attacks");
                        attackBaseCost = getFloat(attacks, "baseCost", attackBaseCost);
                        attackUseWeaponWeight = getBoolean(attacks, "useWeaponWeight", attackUseWeaponWeight);
                        attackCurveType = getString(attacks, "curveType", attackCurveType);
                        attackCurveCoefficients = getFloatList(attacks, "coefficients", attackCurveCoefficients);
                    }
                    if (consumption.has("bows") && consumption.get("bows").isJsonObject()) {
                        JsonObject bows = consumption.getAsJsonObject("bows");
                        bowTensionThresholdSeconds = getFloat(bows, "tensionThreshold", bowTensionThresholdSeconds);
                        bowBaseCost = getFloat(bows, "baseCost", bowBaseCost);
                        bowConsumptionRatePerSecond = getFloat(bows, "consumptionRate", bowConsumptionRatePerSecond);
                        bowMaxTensionTimeSeconds = getFloat(bows, "maxTensionTime", bowMaxTensionTimeSeconds);
                        bowUseWeaponWeight = getBoolean(bows, "useWeaponWeight", bowUseWeaponWeight);
                        bowCurveType = getString(bows, "curveType", bowCurveType);
                        bowCurveCoefficients = getFloatList(bows, "coefficients", bowCurveCoefficients);

                        if (bows.has("fullDrawCost")
                                && !bows.has("baseCost")
                                && !bows.has("consumptionRate")
                                && !bows.has("tensionThreshold")
                                && !bows.has("maxTensionTime")) {
                            bowBaseCost = getFloat(bows, "fullDrawCost", bowBaseCost);
                            bowConsumptionRatePerSecond = 0.0f;
                            bowTensionThresholdSeconds = 0.0f;
                            bowMaxTensionTimeSeconds = 1.0f;
                        }
                    }
                }

                if (root.has("regeneration") && root.get("regeneration").isJsonObject()) {
                    JsonObject regen = root.getAsJsonObject("regeneration");
                    regenDelayAfterConsumptionSeconds = getFloat(regen, "delayAfterConsumption", regenDelayAfterConsumptionSeconds);
                }
            } catch (Exception ex) {
                LOGGER.error("Error loading stamina config from {}", entry.getKey(), ex);
            }
        }
    }

    private static void initializeDefaults() {
        baseMaxStamina = 100.0f;
        baseRegenRatePerSecond = 15.0f;
        baseSprintThresholdFraction = 0.2f;
        combatTimeoutSeconds = 7.0f;

        attackBaseCost = 15.0f;
        attackUseWeaponWeight = true;
        attackCurveType = "quadratic";
        attackCurveCoefficients = List.of(0.03f, 0.05f, 0.92f);

        bowTensionThresholdSeconds = 0.4f;
        bowBaseCost = 4.0f;
        bowConsumptionRatePerSecond = 8.0f;
        bowMaxTensionTimeSeconds = 1.0f;
        bowUseWeaponWeight = true;
        bowCurveType = "quadratic";
        bowCurveCoefficients = List.of(0.03f, 0.05f, 0.92f);

        regenDelayAfterConsumptionSeconds = 0.8f;
    }

    public static float getBaseMaxStamina() {
        return baseMaxStamina;
    }

    public static float getBaseRegenRatePerSecond() {
        return baseRegenRatePerSecond;
    }

    public static float getBaseSprintThresholdFraction() {
        return baseSprintThresholdFraction;
    }

    public static int getCombatTimeoutTicks() {
        return Math.max(0, Math.round(combatTimeoutSeconds * 20.0f));
    }

    public static float getAttackBaseCost() {
        return attackBaseCost;
    }

    public static boolean isAttackUseWeaponWeight() {
        return attackUseWeaponWeight;
    }

    public static float getBowTensionThresholdSeconds() {
        return bowTensionThresholdSeconds;
    }

    public static float getBowBaseCost() {
        return bowBaseCost;
    }

    public static float getBowConsumptionRatePerSecond() {
        return bowConsumptionRatePerSecond;
    }

    public static float getBowMaxTensionTimeSeconds() {
        return bowMaxTensionTimeSeconds;
    }

    public static boolean isBowUseWeaponWeight() {
        return bowUseWeaponWeight;
    }

    public static float getRegenDelayAfterConsumptionSeconds() {
        return regenDelayAfterConsumptionSeconds;
    }

    public static int getRegenDelayAfterConsumptionTicks() {
        return Math.max(0, Math.round(regenDelayAfterConsumptionSeconds * 20.0f));
    }

    public static boolean isCombatRegenReductionEnabled() {
        return Config.STAMINA_COMBAT_REGEN_REDUCTION_ENABLED.get();
    }

    public static int getCombatRegenReductionPercent() {
        return Mth.clamp(Config.STAMINA_COMBAT_REGEN_REDUCTION_PERCENT.get(), 0, 100);
    }

    public static float computeRegenMultiplier(boolean inCombat, boolean combatReductionEnabled, int combatRegenReductionPercent) {
        if (!inCombat) {
            return 1.0f;
        }
        if (!combatReductionEnabled) {
            return 0.0f;
        }
        int clamped = Mth.clamp(combatRegenReductionPercent, 0, 100);
        return 1.0f - (clamped / 100.0f);
    }

    public static float getRegenMultiplier(boolean inCombat) {
        return computeRegenMultiplier(inCombat, isCombatRegenReductionEnabled(), getCombatRegenReductionPercent());
    }

    public static float computeAttackCurveMultiplier(float weaponWeight) {
        return computeCurveMultiplier(attackCurveType, attackCurveCoefficients, weaponWeight);
    }

    public static float computeBowCurveMultiplier(float bowWeight) {
        return computeCurveMultiplier(bowCurveType, bowCurveCoefficients, bowWeight);
    }

    public static float computeBowHoldTickCost(float tensionSeconds, float bowWeight, StaminaComputedStats stats) {
        if (stats == null) {
            return 0.0f;
        }

        float threshold = Math.max(0.0f, bowTensionThresholdSeconds);
        if (tensionSeconds < threshold) {
            return 0.0f;
        }

        float beyond = tensionSeconds - threshold;
        float maxTension = Math.max(0.05f, bowMaxTensionTimeSeconds);
        float curve = Mth.clamp(beyond / maxTension, 0.0f, 1.0f);
        float perSecond = Math.max(0.0f, bowConsumptionRatePerSecond);

        float weightCurve = 1.0f;
        if (isBowUseWeaponWeight()) {
            weightCurve = computeBowCurveMultiplier(bowWeight);
        }

        float baseTick = (perSecond / 20.0f) * curve;
        return baseTick * weightCurve * stats.consumptionMultiplier() * stats.bowTensionCostMultiplier();
    }

    public static float computeBowReleaseCost(float power, float bowWeight, StaminaComputedStats stats) {
        if (stats == null) {
            return 0.0f;
        }

        float clamped = Mth.clamp(power, 0.0f, 1.0f);
        if (clamped <= 0.0f) {
            return 0.0f;
        }

        float weightCurve = 1.0f;
        if (isBowUseWeaponWeight()) {
            weightCurve = computeBowCurveMultiplier(bowWeight);
        }

        float base = Math.max(0.0f, bowBaseCost) * clamped;
        return base * weightCurve * stats.consumptionMultiplier() * stats.bowTensionCostMultiplier();
    }

    private static float computeCurveMultiplier(String curveTypeRaw, List<Float> coefficients, float weight) {
        String curveType = curveTypeRaw == null ? "" : curveTypeRaw.toLowerCase(Locale.ROOT);
        if (coefficients == null || coefficients.isEmpty()) {
            return 1.0f;
        }

        return switch (curveType) {
            case "linear" -> {
                float a = coefficients.size() >= 1 ? coefficients.get(0) : 1.0f;
                float b = coefficients.size() >= 2 ? coefficients.get(1) : 0.0f;
                yield Math.max(0.0f, a * weight + b);
            }
            case "quadratic" -> {
                float a = coefficients.size() >= 1 ? coefficients.get(0) : 0.0f;
                float b = coefficients.size() >= 2 ? coefficients.get(1) : 0.0f;
                float c = coefficients.size() >= 3 ? coefficients.get(2) : 1.0f;
                yield Math.max(0.0f, a * weight * weight + b * weight + c);
            }
            default -> 1.0f;
        };
    }

    private static float getFloat(JsonObject obj, String key, float fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsFloat();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String getString(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static List<Float> getFloatList(JsonObject obj, String key, List<Float> fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonArray()) {
            return fallback;
        }
        try {
            var list = obj.getAsJsonArray(key);
            var out = new java.util.ArrayList<Float>(list.size());
            for (JsonElement el : list) {
                try {
                    out.add(el.getAsFloat());
                } catch (Exception ignored) {
                }
            }
            return out.isEmpty() ? fallback : java.util.List.copyOf(out);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
