package com.THproject.tharidia_things.stable;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug logger that periodically logs information about the nearest stable to the player.
 * Logs every 5 seconds (100 ticks).
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class StableDebugLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(StableDebugLogger.class);

    // Log interval in ticks (5 seconds = 100 ticks)
    private static final int LOG_INTERVAL_TICKS = 100;

    // Search radius for finding stables
    private static final int SEARCH_RADIUS = 32;

    // Enable/disable debug logging
    private static boolean enabled = true;

    private static int tickCounter = 0;

    public static void setEnabled(boolean enabled) {
        StableDebugLogger.enabled = enabled;
        LOGGER.info("[STABLE DEBUG] Debug logging {}", enabled ? "ENABLED" : "DISABLED");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!enabled) return;

        Player player = event.getEntity();

        // Only run on client side and for the local player
        if (!player.level().isClientSide) return;
        if (Minecraft.getInstance().player != player) return;

        tickCounter++;
        if (tickCounter < LOG_INTERVAL_TICKS) return;
        tickCounter = 0;

        // Find nearest stable
        StableBlockEntity nearestStable = findNearestStable(player);

        if (nearestStable == null) {
            LOGGER.info("\n" +
                "╔══════════════════════════════════════════════════════════════╗\n" +
                "║                    STABLE DEBUG INFO                          ║\n" +
                "╠══════════════════════════════════════════════════════════════╣\n" +
                "║  No stable found within {} blocks                            ║\n" +
                "╚══════════════════════════════════════════════════════════════╝",
                SEARCH_RADIUS);
            return;
        }

        logStableInfo(nearestStable, player);
    }

    private static StableBlockEntity findNearestStable(Player player) {
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();

        StableBlockEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS / 2; y <= SEARCH_RADIUS / 2; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockEntity be = level.getBlockEntity(checkPos);
                    if (be instanceof StableBlockEntity stable) {
                        double distSq = checkPos.distSqr(playerPos);
                        if (distSq < nearestDistSq) {
                            nearestDistSq = distSq;
                            nearest = stable;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    private static void logStableInfo(StableBlockEntity stable, Player player) {
        StableConfig cfg = StableConfigLoader.getConfig();
        BlockPos stablePos = stable.getBlockPos();
        double distance = Math.sqrt(stablePos.distSqr(player.blockPosition()));

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                           STABLE DEBUG INFO                                   ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Position: %s (%.1f blocks away)%s║\n",
            stablePos.toShortString(), distance, spaces(50 - stablePos.toShortString().length() - String.format("%.1f", distance).length())));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");

        // Animals section
        sb.append("║  📦 ANIMALS                                                                   ║\n");
        sb.append(String.format("║    Count: %d / %d (max)%s║\n",
            stable.getAnimals().size(), cfg.maxAnimals(), spaces(55 - digitCount(stable.getAnimals().size()) - digitCount(cfg.maxAnimals()))));

        if (stable.hasAnimal()) {
            String animalType = stable.getAnimalType() != null ?
                BuiltInRegistries.ENTITY_TYPE.getKey(stable.getAnimalType()).toString() : "Unknown";
            sb.append(String.format("║    Type: %s%s║\n", animalType, spaces(67 - animalType.length())));

            int idx = 0;
            for (StableBlockEntity.AnimalData animal : stable.getAnimals()) {
                idx++;
                String entityName = BuiltInRegistries.ENTITY_TYPE.getKey(animal.entityType).getPath();

                // Determine lifecycle phase
                String phase;
                if (animal.isBaby) {
                    phase = "BABY";
                } else if (animal.entityType == EntityType.CHICKEN && animal.totalEggsProduced >= cfg.maxEggsPerChicken()) {
                    phase = "BARREN";
                } else {
                    phase = "PRODUCTIVE";
                }

                sb.append("║  ──────────────────────────────────────────────────────────────────────────  ║\n");
                sb.append(String.format("║    [%d] %s - %s%s║\n",
                    idx, entityName.toUpperCase(), phase, spaces(60 - entityName.length() - phase.length())));

                // Growth progress (babies only)
                if (animal.isBaby) {
                    float growthProgress = (float) animal.growthTicks / cfg.growthTimeTicks() * 100;
                    String growthBar = createProgressBar(growthProgress / 100f, 15);
                    sb.append(String.format("║        Growth: %s %.1f%% (%d/%d ticks)%s║\n",
                        growthBar, growthProgress, animal.growthTicks, cfg.growthTimeTicks(),
                        spaces(23 - String.format("%.1f", growthProgress).length() - digitCount(animal.growthTicks) - digitCount(cfg.growthTimeTicks()))));
                }

                // Feed count (for breeding)
                sb.append(String.format("║        Feed Count: %d / %d%s║\n",
                    animal.feedCount, cfg.feedRequiredForBreeding(),
                    spaces(51 - digitCount(animal.feedCount) - digitCount(cfg.feedRequiredForBreeding()))));

                // Breeding status (Houseboundry: one-time breeding)
                String breedStatus = animal.hasBred ? "✗ Already bred (cannot breed again)" : "✓ Can breed";
                sb.append(String.format("║        Breeding: %s%s║\n",
                    breedStatus, spaces(animal.hasBred ? 40 : 63)));

                // Egg production (chickens only)
                if (animal.entityType == EntityType.CHICKEN) {
                    boolean isBarren = animal.totalEggsProduced >= cfg.maxEggsPerChicken();
                    String eggStatus = isBarren ? "BARREN - no more eggs" : "producing";
                    sb.append(String.format("║        Eggs Available: %d%s║\n",
                        animal.eggCount, spaces(52 - digitCount(animal.eggCount))));
                    sb.append(String.format("║        Lifetime Eggs: %d / %d (%s)%s║\n",
                        animal.totalEggsProduced, cfg.maxEggsPerChicken(), eggStatus,
                        spaces(40 - digitCount(animal.totalEggsProduced) - digitCount(cfg.maxEggsPerChicken()) - eggStatus.length())));
                    if (!animal.isBaby && !isBarren) {
                        float eggProgress = (float) animal.eggProductionTicks / cfg.eggProductionTimeTicks() * 100;
                        String eggBar = createProgressBar(eggProgress / 100f, 15);
                        sb.append(String.format("║        Next Egg: %s %.1f%% (%d/%d ticks)%s║\n",
                            eggBar, eggProgress, animal.eggProductionTicks, cfg.eggProductionTimeTicks(),
                            spaces(21 - String.format("%.1f", eggProgress).length() - digitCount(animal.eggProductionTicks) - digitCount(cfg.eggProductionTimeTicks()))));
                    }
                }

                // Milk production (cows, goats, mooshrooms)
                if (isMilkProducingType(animal.entityType)) {
                    String milkStatus = animal.milkReady ? "§aREADY" : "producing...";
                    sb.append(String.format("║        Milk Status: %s%s║\n",
                        milkStatus, spaces(animal.milkReady ? 54 : 46)));
                    if (!animal.isBaby && !animal.milkReady) {
                        float milkProgress = (float) animal.milkProductionTicks / cfg.milkProductionTimeTicks() * 100;
                        String milkBar = createProgressBar(milkProgress / 100f, 15);
                        sb.append(String.format("║        Next Milk: %s %.1f%% (%d/%d ticks)%s║\n",
                            milkBar, milkProgress, animal.milkProductionTicks, cfg.milkProductionTimeTicks(),
                            spaces(20 - String.format("%.1f", milkProgress).length() - digitCount(animal.milkProductionTicks) - digitCount(cfg.milkProductionTimeTicks()))));
                    }
                }

                // ===== HOUSEBOUNDRY WELLNESS STATS =====
                sb.append("║    ─── WELLNESS ───────────────────────────────────────────────────────────  ║\n");

                // Comfort
                String comfortBar = createProgressBar(animal.comfort / 100f, 15);
                sb.append(String.format("║        Comfort: %s %d%%                                            ║\n",
                    comfortBar, animal.comfort));

                // Stress (higher = worse, so invert color logic)
                String stressBar = createProgressBar(animal.stress / 100f, 15);
                String stressIndicator = animal.stress >= 70 ? "⚠ HIGH" : animal.stress >= 50 ? "↑ ELEVATED" : "OK";
                sb.append(String.format("║        Stress:  %s %d%% %s%s║\n",
                    stressBar, animal.stress, stressIndicator,
                    spaces(35 - digitCount(animal.stress) - stressIndicator.length())));

                // Hygiene
                String hygieneBar = createProgressBar(animal.hygiene / 100f, 15);
                sb.append(String.format("║        Hygiene: %s %d%%                                            ║\n",
                    hygieneBar, animal.hygiene));

                // Animal State
                StableBlockEntity.AnimalState state = animal.calculateState();
                String stateIcon = switch (state) {
                    case GOLD -> "★ GOLD";
                    case OK -> "● OK";
                    case LOW -> "▼ LOW";
                    case CRITICAL -> "✖ CRITICAL";
                };
                String stateEffect = switch (state) {
                    case GOLD -> "(+30% production)";
                    case OK -> "(normal)";
                    case LOW -> "(-30% production)";
                    case CRITICAL -> "(NO production!)";
                };
                sb.append(String.format("║        State: %s %s%s║\n",
                    stateIcon, stateEffect, spaces(47 - stateIcon.length() - stateEffect.length())));

                // Disease status
                if (animal.diseased) {
                    long diseaseMinutes = animal.diseaseStartTimestamp > 0 ?
                        (System.currentTimeMillis() - animal.diseaseStartTimestamp) / 60000 : 0;
                    long timeRemaining = 120 - diseaseMinutes;
                    String diseasePhase;
                    if (diseaseMinutes < 60) {
                        diseasePhase = "Early (curable)";
                    } else if (diseaseMinutes < 100) {
                        diseasePhase = "Advanced";
                    } else {
                        diseasePhase = "TERMINAL";
                    }
                    sb.append(String.format("║        Disease: ⚠ YES - %s (%d min remaining)%s║\n",
                        diseasePhase, timeRemaining,
                        spaces(24 - diseasePhase.length() - digitCount((int)timeRemaining))));
                } else {
                    sb.append("║        Disease: ✓ Healthy                                                     ║\n");
                }

                // Brush cooldown
                long brushElapsed = System.currentTimeMillis() - animal.lastBrushTimestamp;
                int brushCooldown = Math.max(0, (int)((60000 - brushElapsed) / 1000));
                if (brushCooldown > 0) {
                    sb.append(String.format("║        Last Brush: %ds cooldown remaining%s║\n",
                        brushCooldown, spaces(35 - digitCount(brushCooldown))));
                } else {
                    sb.append("║        Last Brush: Ready for brushing                                         ║\n");
                }
            }
        } else {
            sb.append("║    (No animals)                                                               ║\n");
        }

        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");

        // Resources section
        sb.append("║  💧 RESOURCES                                                                 ║\n");

        // Water
        float waterPercent = stable.getWaterLevel() * 100;
        String waterBar = createProgressBar(stable.getWaterLevel(), 20);
        sb.append(String.format("║    Water: %s %.1f%% (has water: %s)%s║\n",
            waterBar, waterPercent, stable.hasWater() ? "YES" : "NO",
            spaces(26 - String.format("%.1f", waterPercent).length() - (stable.hasWater() ? 3 : 2))));

        // Food
        float foodPercent = stable.getFoodLevel() * 100;
        String foodBar = createProgressBar(stable.getFoodLevel(), 20);
        sb.append(String.format("║    Food:  %s %.1f%% (%d/%d items)%s║\n",
            foodBar, foodPercent, stable.getFoodAmount(), cfg.maxFoodItems(),
            spaces(25 - String.format("%.1f", foodPercent).length() - digitCount(stable.getFoodAmount()) - digitCount(cfg.maxFoodItems()))));

        // Manure
        float manurePercent = (float) stable.getManureAmount() / cfg.maxManure() * 100;
        String manureBar = createProgressBar((float) stable.getManureAmount() / cfg.maxManure(), 20);
        sb.append(String.format("║    Manure: %s %.1f%% (%d/%d, collect: %s)%s║\n",
            manureBar, manurePercent, stable.getManureAmount(), cfg.maxManure(),
            stable.canCollectManure() ? "YES" : "NO",
            spaces(17 - String.format("%.1f", manurePercent).length() - digitCount(stable.getManureAmount()) - digitCount(cfg.maxManure()) - (stable.canCollectManure() ? 3 : 2))));

        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");

        // Houseboundry section
        sb.append("║  🏠 HOUSEBOUNDRY                                                              ║\n");

        // Weather effects
        Level level = player.level();
        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();
        String weatherStatus = isThundering ? "⛈ THUNDERSTORM" :
                              isRaining ? "🌧 RAINING" : "☀ CLEAR";
        String weatherEffect = isThundering ? "(stress +15!)" :
                              (isRaining && !stable.hasShelterUpgrade()) ? "(comfort -2/h)" : "";
        sb.append(String.format("║    Weather: %s %s%s║\n",
            weatherStatus, weatherEffect,
            spaces(56 - weatherStatus.length() - weatherEffect.length())));

        // Bedding
        float beddingPercent = (float) stable.getBeddingFreshness() / 100 * 100;
        String beddingBar = createProgressBar((float) stable.getBeddingFreshness() / 100, 20);
        String beddingStatus = stable.getBeddingFreshness() >= 70 ? "FRESH" :
                               stable.getBeddingFreshness() >= 40 ? "OK" :
                               stable.getBeddingFreshness() >= 10 ? "STALE" :
                               stable.getBeddingFreshness() > 0 ? "DIRTY" : "NONE";
        sb.append(String.format("║    Bedding: %s %d%% [%s]%s║\n",
            beddingBar, stable.getBeddingFreshness(), beddingStatus,
            spaces(40 - digitCount(stable.getBeddingFreshness()) - beddingStatus.length())));

        // Shelter
        sb.append(String.format("║    Shelter Upgrade: %s%s║\n",
            stable.hasShelterUpgrade() ? "✓ INSTALLED" : "✗ NOT INSTALLED",
            spaces(stable.hasShelterUpgrade() ? 45 : 41)));

        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");

        // Production status
        sb.append("║  📊 PRODUCTION STATUS                                                         ║\n");
        boolean canProduce = stable.hasWater() && stable.getFoodAmount() > 0;
        sb.append(String.format("║    Can Produce: %s (Water: %s, Food: %s)%s║\n",
            canProduce ? "YES" : "NO",
            stable.hasWater() ? "✓" : "✗",
            stable.getFoodAmount() > 0 ? "✓" : "✗",
            spaces(canProduce ? 35 : 36)));

        sb.append(String.format("║    Can Collect Milk: %s%s║\n",
            stable.canCollectMilk() ? "YES" : "NO",
            spaces(stable.canCollectMilk() ? 53 : 54)));

        sb.append(String.format("║    Can Collect Eggs: %s (Total eggs available: %d)%s║\n",
            stable.canCollectEggs() ? "YES" : "NO",
            stable.getTotalEggCount(),
            spaces(stable.canCollectEggs() ? 25 - digitCount(stable.getTotalEggCount()) : 26 - digitCount(stable.getTotalEggCount()))));

        // Summary counts
        int milkReadyCount = 0;
        int milkProducerCount = 0;
        int eggProducerCount = 0;
        int barrenCount = 0;
        int babyCount = 0;
        int adultCount = 0;
        // Wellness counts
        int goldCount = 0;
        int okCount = 0;
        int lowCount = 0;
        int criticalCount = 0;
        int diseasedCount = 0;
        int avgComfort = 0;
        int avgStress = 0;
        int avgHygiene = 0;

        for (StableBlockEntity.AnimalData animal : stable.getAnimals()) {
            // Wellness averages
            avgComfort += animal.comfort;
            avgStress += animal.stress;
            avgHygiene += animal.hygiene;

            // State counts
            switch (animal.calculateState()) {
                case GOLD -> goldCount++;
                case OK -> okCount++;
                case LOW -> lowCount++;
                case CRITICAL -> criticalCount++;
            }
            if (animal.diseased) diseasedCount++;

            if (animal.isBaby) {
                babyCount++;
            } else {
                adultCount++;
                if (isMilkProducingType(animal.entityType)) {
                    milkProducerCount++;
                    if (animal.milkReady) {
                        milkReadyCount++;
                    }
                }
                if (animal.entityType == EntityType.CHICKEN) {
                    if (animal.totalEggsProduced >= cfg.maxEggsPerChicken()) {
                        barrenCount++;
                    } else {
                        eggProducerCount++;
                    }
                }
            }
        }

        int totalAnimals = stable.getAnimals().size();
        if (totalAnimals > 0) {
            avgComfort /= totalAnimals;
            avgStress /= totalAnimals;
            avgHygiene /= totalAnimals;
        }

        sb.append(String.format("║    Summary: %d babies, %d adults (%d barren)%s║\n",
            babyCount, adultCount, barrenCount,
            spaces(32 - digitCount(babyCount) - digitCount(adultCount) - digitCount(barrenCount))));

        // Wellness summary
        sb.append(String.format("║    Wellness: ★%d ●%d ▼%d ✖%d (Diseased: %d)%s║\n",
            goldCount, okCount, lowCount, criticalCount, diseasedCount,
            spaces(33 - digitCount(goldCount) - digitCount(okCount) - digitCount(lowCount) - digitCount(criticalCount) - digitCount(diseasedCount))));

        sb.append(String.format("║    Avg Stats: Comfort %d%%, Stress %d%%, Hygiene %d%%%s║\n",
            avgComfort, avgStress, avgHygiene,
            spaces(31 - digitCount(avgComfort) - digitCount(avgStress) - digitCount(avgHygiene))));

        if (milkProducerCount > 0) {
            sb.append(String.format("║    Milk: %d producers, %d ready to collect%s║\n",
                milkProducerCount, milkReadyCount,
                spaces(35 - digitCount(milkProducerCount) - digitCount(milkReadyCount))));
        }
        if (eggProducerCount > 0 || barrenCount > 0) {
            sb.append(String.format("║    Eggs: %d producing, %d barren (no more eggs)%s║\n",
                eggProducerCount, barrenCount,
                spaces(29 - digitCount(eggProducerCount) - digitCount(barrenCount))));
        }

        // Brush cooldown
        int brushCooldown = stable.getBrushCooldownSeconds();
        sb.append(String.format("║    Brush Status: %s%s║\n",
            brushCooldown > 0 ? brushCooldown + "s cooldown" : "READY",
            spaces(brushCooldown > 0 ? 49 - digitCount(brushCooldown) : 53)));

        // Disease warning
        if (diseasedCount > 0) {
            sb.append("║    ⚠ WARNING: Diseased animals need honey cure!                              ║\n");
        }

        sb.append(String.format("║    Can Slaughter: %s%s║\n",
            stable.canSlaughter() ? "YES" : "NO",
            spaces(stable.canSlaughter() ? 56 : 57)));

        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝");

        LOGGER.info(sb.toString());
    }

    private static String createProgressBar(float percent, int length) {
        int filled = (int) (percent * length);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");
        return bar.toString();
    }

    private static String spaces(int count) {
        if (count <= 0) return "";
        return " ".repeat(count);
    }

    private static int digitCount(int number) {
        if (number == 0) return 1;
        return (int) Math.log10(Math.abs(number)) + 1 + (number < 0 ? 1 : 0);
    }

    /**
     * Checks if an entity type can produce milk.
     * Supports vanilla animals (cow, goat, mooshroom) and modded animals.
     */
    private static boolean isMilkProducingType(EntityType<?> entityType) {
        if (entityType == EntityType.COW ||
            entityType == EntityType.GOAT ||
            entityType == EntityType.MOOSHROOM) {
            return true;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId != null) {
            String path = entityId.getPath().toLowerCase();
            return path.contains("cow") || path.contains("goat") || path.contains("milk");
        }
        return false;
    }
}
