package com.THproject.tharidia_things.block.alchemist;

import com.google.gson.JsonObject;

/**
 * All numeric parameters for the Alchemist Table temperature mini-game.
 * Loaded from {@code data/tharidiathings/alchemist_table/temperature_config.json}
 * and reloaded on {@code /reload}.
 *
 * <p>Change values in a datapack overlay and run {@code /reload} — no restart needed.
 */
public class AlchemistTemperatureConfig {

    /** Singleton updated by {@link AlchemistTemperatureConfigLoader} on every reload. */
    public static volatile AlchemistTemperatureConfig INSTANCE = new AlchemistTemperatureConfig();

    // ── Range thresholds (0-100 scale) ────────────────────────────────────────
    /** Below this → dark smoke + penalty accumulates. */
    public float tempCriticalLow  = 20f;
    /** Lowest value of the "green" zone — no penalty. */
    public float tempOptimalMin   = 35f;
    /** Highest value of the "green" zone — no penalty. */
    public float tempOptimalMax   = 65f;
    /** Above this → red smoke + penalty accumulates. */
    public float tempCriticalHigh = 80f;

    // ── Dynamics ──────────────────────────────────────────────────────────────
    /** Degrees lost per server tick (natural cooling). */
    public float tempDecayPerTick         = 0.08f;
    /** Degrees gained per tick while the bellows is active. */
    public float tempGainPerBellowsTick   = 0.35f;
    /** Starting temperature when the fire is first lit. */
    public float tempInitial              = 50f;

    // ── Yield penalty ────────────────────────────────────────────────────────
    /**
     * Ticks outside critical range before one penalty point is added.
     * e.g. 60 = 3 s at 20 tps.
     */
    public int penaltyIntervalTicks = 60;
    /** Maximum penalty points (each reduces final dose count by 1; min doses = 1). */
    public int maxYieldPenalty      = 3;

    // ── Particles ────────────────────────────────────────────────────────────
    /** Server ticks between smoke particle bursts when the fire is lit. */
    public int smokeParticleIntervalTicks = 4;

    // ── JSON loader ──────────────────────────────────────────────────────────

    public static AlchemistTemperatureConfig fromJson(JsonObject j) {
        AlchemistTemperatureConfig c = new AlchemistTemperatureConfig();
        if (j.has("temp_critical_low"))            c.tempCriticalLow            = j.get("temp_critical_low").getAsFloat();
        if (j.has("temp_optimal_min"))             c.tempOptimalMin             = j.get("temp_optimal_min").getAsFloat();
        if (j.has("temp_optimal_max"))             c.tempOptimalMax             = j.get("temp_optimal_max").getAsFloat();
        if (j.has("temp_critical_high"))           c.tempCriticalHigh           = j.get("temp_critical_high").getAsFloat();
        if (j.has("temp_decay_per_tick"))          c.tempDecayPerTick           = j.get("temp_decay_per_tick").getAsFloat();
        if (j.has("temp_gain_per_bellows_tick"))   c.tempGainPerBellowsTick     = j.get("temp_gain_per_bellows_tick").getAsFloat();
        if (j.has("temp_initial"))                 c.tempInitial                = j.get("temp_initial").getAsFloat();
        if (j.has("penalty_interval_ticks"))       c.penaltyIntervalTicks       = j.get("penalty_interval_ticks").getAsInt();
        if (j.has("max_yield_penalty"))            c.maxYieldPenalty            = j.get("max_yield_penalty").getAsInt();
        if (j.has("smoke_particle_interval_ticks"))c.smokeParticleIntervalTicks = j.get("smoke_particle_interval_ticks").getAsInt();
        return c;
    }
}
