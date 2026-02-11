package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.StableBlock;
import com.THproject.tharidia_things.block.StableDummyBlock;
import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import com.THproject.tharidia_things.client.gui.medieval.MedievalGuiRenderer;
import com.THproject.tharidia_things.stable.AnimalTypeHelper;
import com.THproject.tharidia_things.stable.StableConfig;
import com.THproject.tharidia_things.stable.StableConfigLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD overlay that displays stable information when the player looks at a stable.
 * Updates every 2 seconds (40 ticks) for performance.
 */
@EventBusSubscriber(modid = TharidiaThings.MODID, value = Dist.CLIENT)
public class StableHudOverlay {

    // Update interval in milliseconds (2 seconds)
    private static final long UPDATE_INTERVAL_MS = 2000;

    // Raycast distance
    private static final double RAYCAST_DISTANCE = 8.0;

    // Cached data
    private static StableBlockEntity cachedStable = null;
    private static long lastUpdateTime = 0;
    private static List<LineData> cachedLines = new ArrayList<>();

    // Line types for coloring
    private enum LineType {
        TITLE, HEADER, NORMAL, GOOD, WARNING, DANGER, GRAY, EMPTY
    }

    // Line data record
    private record LineData(LineType type, Component text) {}

    // Colors (ARGB for drawString)
    private static final int COLOR_TITLE = MedievalGuiRenderer.GOLD_LEAF;
    private static final int COLOR_HEADER = MedievalGuiRenderer.ROYAL_GOLD;
    private static final int COLOR_NORMAL = MedievalGuiRenderer.BROWN_INK;
    private static final int COLOR_GOOD = MedievalGuiRenderer.BRONZE;
    private static final int COLOR_WARNING = MedievalGuiRenderer.PURPLE_REGAL;
    private static final int COLOR_DANGER = MedievalGuiRenderer.DEEP_CRIMSON;
    private static final int COLOR_GRAY = MedievalGuiRenderer.BROWN_INK;

    // Scale for body text (non-title, non-header lines)
    private static final float BODY_SCALE = 0.75f;
    private static final int BODY_LINE_SPACING = 2;

    // Indicator RGB colors (no alpha, for TextColor.fromRgb)
    private static final int IND_GOOD = 0x55AA55;
    private static final int IND_OK = 0x8B6914;
    private static final int IND_WARNING = 0xCC8800;
    private static final int IND_DANGER = 0xCC2222;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null) {
            return;
        }

        // Don't render if in a menu or paused
        if (mc.screen != null) {
            return;
        }

        // Check if player is looking at a stable
        StableBlockEntity stable = getTargetedStable(mc, player);

        if (stable == null) {
            cachedStable = null;
            cachedLines.clear();
            return;
        }

        // Update cached data every 2 seconds or if stable changed
        long now = System.currentTimeMillis();
        if (stable != cachedStable || now - lastUpdateTime >= UPDATE_INTERVAL_MS) {
            cachedStable = stable;
            lastUpdateTime = now;
            cachedLines = buildInfoLines(stable);
        }

        // Render the overlay
        if (!cachedLines.isEmpty()) {
            renderOverlay(event.getGuiGraphics(), mc, cachedLines);
        }
    }

    /**
     * Gets the stable the player is looking at, if any.
     */
    private static StableBlockEntity getTargetedStable(Minecraft mc, LocalPlayer player) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(RAYCAST_DISTANCE));

        BlockHitResult hitResult = mc.level.clip(new ClipContext(
            eyePos, endPos,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos hitPos = hitResult.getBlockPos();
        BlockState state = mc.level.getBlockState(hitPos);

        // Check if it's a stable or dummy block
        if (state.getBlock() instanceof StableBlock) {
            BlockEntity be = mc.level.getBlockEntity(hitPos);
            if (be instanceof StableBlockEntity stable) {
                return stable;
            }
        } else if (state.getBlock() instanceof StableDummyBlock) {
            BlockPos masterPos = StableDummyBlock.getMasterPos(mc.level, hitPos);
            if (masterPos != null) {
                BlockEntity be = mc.level.getBlockEntity(masterPos);
                if (be instanceof StableBlockEntity stable) {
                    return stable;
                }
            }
        }

        return null;
    }

    /**
     * Builds the info lines to display.
     */
    private static List<LineData> buildInfoLines(StableBlockEntity stable) {
        List<LineData> lines = new ArrayList<>();
        var config = StableConfigLoader.getConfig();

        // Title
        lines.add(new LineData(LineType.TITLE, Component.translatable("gui.tharidiathings.stable.title")));

        // Animals section
        int animalCount = stable.getAnimals().size();
        int maxAnimals = config.maxAnimals();

        if (animalCount == 0) {
            lines.add(new LineData(LineType.NORMAL, Component.translatable("gui.tharidiathings.stable.no_animals")));
        } else {
            EntityType<?> animalType = stable.getAnimalType();
            Component typeName = animalType != null ?
                EntityType.getKey(animalType).getPath().contains(":") ?
                    Component.literal(formatEntityName(BuiltInRegistries.ENTITY_TYPE.getKey(animalType).getPath())) :
                    Component.translatable(animalType.getDescriptionId()) :
                Component.translatable("gui.tharidiathings.stable.unknown");

            lines.add(new LineData(LineType.HEADER, Component.translatable("gui.tharidiathings.stable.animals")));
            lines.add(new LineData(LineType.NORMAL, Component.literal("")
                .append(typeName)
                .append(" " + animalCount + "/" + maxAnimals)));

            int idx = 0;
            for (StableBlockEntity.AnimalData animal : stable.getAnimals()) {
                idx++;
                lines.addAll(buildAnimalLines(animal, idx, config, animalCount));
            }
        }

        // Resources section
        lines.add(new LineData(LineType.EMPTY, Component.empty()));
        lines.add(new LineData(LineType.HEADER, Component.translatable("gui.tharidiathings.stable.resources")));

        // Water
        float waterLevel = stable.getWaterLevel();
        Component waterStatus = getResourceStatusComponent(waterLevel);
        LineType waterType = getResourceLineType(waterLevel);
        lines.add(new LineData(waterType, Component.translatable("gui.tharidiathings.stable.resource.water")
            .append(": ").append(waterStatus)));

        // Food
        float foodLevel = stable.getFoodLevel();
        Component foodStatus = getResourceStatusComponent(foodLevel);
        LineType foodType = getResourceLineType(foodLevel);
        lines.add(new LineData(foodType, Component.translatable("gui.tharidiathings.stable.resource.food")
            .append(": ").append(foodStatus)));

        // Manure
        int manureAmount = stable.getManureAmount();
        int maxManure = config.maxManure();
        Component manureStatus;
        LineType manureType;
        if (manureAmount >= config.manureCollectAmount()) {
            manureStatus = Component.translatable("gui.tharidiathings.stable.resource.manure.ready");
            manureType = LineType.GOOD;
        } else if (manureAmount > maxManure * 0.7) {
            manureStatus = Component.translatable("gui.tharidiathings.stable.resource.level.high");
            manureType = LineType.WARNING;
        } else {
            manureStatus = Component.translatable("gui.tharidiathings.stable.resource.level.low");
            manureType = LineType.NORMAL;
        }
        lines.add(new LineData(manureType, Component.translatable("gui.tharidiathings.stable.resource.manure")
            .append(": ").append(manureStatus)));

        // Conditions section
        lines.add(new LineData(LineType.EMPTY, Component.empty()));
        lines.add(new LineData(LineType.HEADER, Component.translatable("gui.tharidiathings.stable.conditions")));

        // Bedding
        int beddingFresh = stable.getBeddingFreshness();
        Component beddingStatus;
        LineType beddingType;
        if (beddingFresh >= 70) {
            beddingStatus = Component.translatable("gui.tharidiathings.stable.bedding.fresh");
            beddingType = LineType.GOOD;
        } else if (beddingFresh >= 40) {
            beddingStatus = Component.translatable("gui.tharidiathings.stable.bedding.ok");
            beddingType = LineType.NORMAL;
        } else if (beddingFresh > 0) {
            beddingStatus = Component.translatable("gui.tharidiathings.stable.bedding.dirty");
            beddingType = LineType.WARNING;
        } else {
            beddingStatus = Component.translatable("gui.tharidiathings.stable.bedding.none");
            beddingType = LineType.DANGER;
        }
        lines.add(new LineData(beddingType, Component.translatable("gui.tharidiathings.stable.bedding")
            .append(": ").append(beddingStatus)));

        // Shelter
        if (stable.hasShelterUpgrade()) {
            lines.add(new LineData(LineType.GOOD, Component.translatable("gui.tharidiathings.stable.shelter")
                .append(": ").append(Component.translatable("gui.tharidiathings.stable.shelter.installed"))));
        } else {
            lines.add(new LineData(LineType.NORMAL, Component.translatable("gui.tharidiathings.stable.shelter")
                .append(": ").append(Component.translatable("gui.tharidiathings.stable.shelter.not_installed"))));
        }

        // Breeding section
        if (animalCount >= 2) {
            lines.addAll(buildBreedingSection(stable, config));
        }

        // Production summary
        if (animalCount > 0) {
            lines.add(new LineData(LineType.EMPTY, Component.empty()));
            lines.add(new LineData(LineType.HEADER, Component.translatable("gui.tharidiathings.stable.production")));

            boolean canProduce = stable.hasWater() && stable.getFoodAmount() > 0;
            if (!canProduce) {
                lines.add(new LineData(LineType.DANGER, Component.translatable("gui.tharidiathings.stable.production.blocked")));
            } else {
                if (stable.getLevel() != null) {
                    long dayTime = stable.getLevel().getDayTime();
                    boolean isDaytime = config.isDaytime(dayTime);
                    if (!isDaytime) {
                        lines.add(new LineData(LineType.NORMAL, Component.translatable("gui.tharidiathings.stable.production.resting")));
                    } else {
                        lines.add(new LineData(LineType.GOOD, Component.translatable("gui.tharidiathings.stable.production.active")));
                    }
                }
            }

            if (stable.canCollectMilk()) {
                lines.add(new LineData(LineType.GOOD, Component.translatable("gui.tharidiathings.stable.production.milk_available")));
            }

            if (stable.canCollectEggs()) {
                int eggCount = stable.getTotalEggCount();
                lines.add(new LineData(LineType.GOOD, Component.translatable("gui.tharidiathings.stable.production.eggs_available", eggCount)));
            }
        }

        // Hints section
        lines.addAll(buildHintsSection(stable, config));

        return lines;
    }

    /**
     * Builds info lines for a single animal.
     */
    private static List<LineData> buildAnimalLines(StableBlockEntity.AnimalData animal, int index,
                                                   StableConfig config, int totalAnimals) {
        List<LineData> lines = new ArrayList<>();

        // Phase
        Component phase;
        if (animal.isBaby) {
            phase = Component.translatable("gui.tharidiathings.stable.animal.baby");
        } else if (AnimalTypeHelper.isEggProducingType(animal.entityType) &&
                   animal.totalEggsProduced >= config.maxEggsPerChicken()) {
            phase = Component.translatable("gui.tharidiathings.stable.animal.barren");
        } else {
            phase = Component.translatable("gui.tharidiathings.stable.animal.adult");
        }

        // State with readable name
        StableBlockEntity.AnimalState state = animal.calculateState();
        String stateIcon;
        LineType stateType;
        Component stateName;
        switch (state) {
            case GOLD -> {
                stateIcon = "\u2605";
                stateType = LineType.TITLE;
                stateName = Component.translatable("gui.tharidiathings.stable.animal.state.gold");
            }
            case OK -> {
                stateIcon = "\u25CF";
                stateType = LineType.GOOD;
                stateName = Component.translatable("gui.tharidiathings.stable.animal.state.ok");
            }
            case LOW -> {
                stateIcon = "\u25BC";
                stateType = LineType.WARNING;
                stateName = Component.translatable("gui.tharidiathings.stable.animal.state.low");
            }
            case CRITICAL -> {
                stateIcon = "\u2716";
                stateType = LineType.DANGER;
                stateName = Component.translatable("gui.tharidiathings.stable.animal.state.critical");
            }
            default -> {
                stateIcon = "?";
                stateType = LineType.NORMAL;
                stateName = Component.translatable("gui.tharidiathings.stable.unknown");
            }
        }

        // Main line: index + phase + icon + state name
        lines.add(new LineData(stateType, Component.literal(" " + index + ". ")
            .append(phase)
            .append(" " + stateIcon + " ")
            .append(stateName)));

        // Wellness indicators (colored dots instead of numbers)
        int comfortRgb = getComfortIndicatorRgb(animal.comfort);
        int stressRgb = getStressIndicatorRgb(animal.stress);
        int hygieneRgb = getHygieneIndicatorRgb(animal.hygiene);

        Component wellnessLine = Component.literal("    ")
            .append(Component.translatable("gui.tharidiathings.stable.animal.comfort_label"))
            .append(Component.literal(" \u25CF ").withStyle(s -> s.withColor(TextColor.fromRgb(comfortRgb))))
            .append(Component.translatable("gui.tharidiathings.stable.animal.stress_label"))
            .append(Component.literal(" \u25CF ").withStyle(s -> s.withColor(TextColor.fromRgb(stressRgb))))
            .append(Component.translatable("gui.tharidiathings.stable.animal.hygiene_label"))
            .append(Component.literal(" \u25CF").withStyle(s -> s.withColor(TextColor.fromRgb(hygieneRgb))));
        lines.add(new LineData(LineType.NORMAL, wellnessLine));

        // Growth progress for babies (colored indicator)
        if (animal.isBaby) {
            float growthPercent = (float) animal.growthTicks / config.growthTimeTicks() * 100;
            int growthRgb = getGrowthIndicatorRgb(growthPercent);
            Component growthLine = Component.literal("    ")
                .append(Component.translatable("gui.tharidiathings.stable.animal.growing"))
                .append(Component.literal(" \u25CF").withStyle(s -> s.withColor(TextColor.fromRgb(growthRgb))));
            lines.add(new LineData(LineType.NORMAL, growthLine));
        }

        // Disease warning
        if (animal.diseased) {
            long now = System.currentTimeMillis();
            long diseaseStart = animal.diseaseStartTimestamp > 0 ? animal.diseaseStartTimestamp : now;
            long diseaseMinutes = (now - diseaseStart) / 60000;
            long timeRemaining = Math.max(0, 120 - diseaseMinutes);
            lines.add(new LineData(LineType.DANGER, Component.literal("    ")
                .append(Component.translatable("gui.tharidiathings.stable.animal.sick", (int) timeRemaining))));
        }

        // Breeding status
        if (!animal.isBaby && totalAnimals == 2) {
            if (animal.hasBred) {
                lines.add(new LineData(LineType.NORMAL, Component.literal("    ")
                    .append(Component.translatable("gui.tharidiathings.stable.animal.bred"))));
            } else if (animal.feedCount > 0) {
                lines.add(new LineData(LineType.NORMAL, Component.literal("    ")
                    .append(Component.translatable("gui.tharidiathings.stable.animal.feed_progress",
                        animal.feedCount, config.feedRequiredForBreeding()))));
            }
        }

        // Milk ready
        if (!animal.isBaby && AnimalTypeHelper.isMilkProducingType(animal.entityType) && animal.milkReady) {
            lines.add(new LineData(LineType.GOOD, Component.literal("    ")
                .append(Component.translatable("gui.tharidiathings.stable.animal.milk_ready"))));
        }

        // Eggs available
        if (!animal.isBaby && AnimalTypeHelper.isEggProducingType(animal.entityType) && animal.eggCount > 0) {
            lines.add(new LineData(LineType.GOOD, Component.literal("    ")
                .append(Component.translatable("gui.tharidiathings.stable.animal.eggs", animal.eggCount))));
        }

        return lines;
    }

    // ==================== WELLNESS INDICATOR COLORS ====================

    private static int getComfortIndicatorRgb(int comfort) {
        if (comfort >= 70) return IND_GOOD;
        if (comfort >= 40) return IND_OK;
        if (comfort >= 20) return IND_WARNING;
        return IND_DANGER;
    }

    private static int getStressIndicatorRgb(int stress) {
        // Inverted: lower stress is better
        if (stress <= 20) return IND_GOOD;
        if (stress < 50) return IND_OK;
        if (stress < 70) return IND_WARNING;
        return IND_DANGER;
    }

    private static int getHygieneIndicatorRgb(int hygiene) {
        if (hygiene >= 60) return IND_GOOD;
        if (hygiene >= 40) return IND_OK;
        if (hygiene >= 20) return IND_WARNING;
        return IND_DANGER;
    }

    private static int getGrowthIndicatorRgb(float percent) {
        if (percent >= 75) return IND_GOOD;
        if (percent >= 50) return IND_OK;
        if (percent >= 25) return IND_WARNING;
        return IND_DANGER;
    }

    // ==================== BREEDING SECTION ====================

    private static List<LineData> buildBreedingSection(StableBlockEntity stable, StableConfig config) {
        List<LineData> lines = new ArrayList<>();
        List<StableBlockEntity.AnimalData> animals = stable.getAnimals();
        int animalCount = animals.size();

        if (animalCount < 2) return lines;

        lines.add(new LineData(LineType.EMPTY, Component.empty()));
        lines.add(new LineData(LineType.HEADER, Component.translatable("gui.tharidiathings.stable.breeding")));

        if (animalCount >= config.maxAnimals()) {
            lines.add(new LineData(LineType.NORMAL, Component.translatable("gui.tharidiathings.stable.breeding.stable_full")));
            return lines;
        }

        boolean hasBabies = false;
        boolean anyBred = false;
        boolean anyBadState = false;
        for (StableBlockEntity.AnimalData animal : animals) {
            if (animal.isBaby) hasBabies = true;
            if (animal.hasBred) anyBred = true;
            StableBlockEntity.AnimalState state = animal.calculateState();
            if (state == StableBlockEntity.AnimalState.LOW || state == StableBlockEntity.AnimalState.CRITICAL) {
                anyBadState = true;
            }
        }

        if (hasBabies) {
            lines.add(new LineData(LineType.NORMAL, Component.translatable("gui.tharidiathings.stable.breeding.need_adults")));
        } else if (anyBred) {
            lines.add(new LineData(LineType.NORMAL, Component.translatable("gui.tharidiathings.stable.breeding.already_bred")));
        } else if (anyBadState) {
            lines.add(new LineData(LineType.WARNING, Component.translatable("gui.tharidiathings.stable.breeding.poor_condition")));
        } else {
            Component foodName = getBreedingFoodName(stable.getAnimalType());
            lines.add(new LineData(LineType.NORMAL, Component.translatable("gui.tharidiathings.stable.breeding.feed_hint", foodName)));
        }

        return lines;
    }

    // ==================== HINTS SECTION ====================

    private static List<LineData> buildHintsSection(StableBlockEntity stable, StableConfig config) {
        List<LineData> lines = new ArrayList<>();
        List<Component> hints = new ArrayList<>();

        int animalCount = stable.getAnimals().size();

        if (stable.hasDiseasedAnimal()) {
            hints.add(Component.translatable("gui.tharidiathings.stable.hint.cure"));
        }
        if (animalCount > 0 && !stable.hasWater()) {
            hints.add(Component.translatable("gui.tharidiathings.stable.hint.water"));
        }
        if (animalCount > 0 && stable.getFoodAmount() == 0) {
            hints.add(Component.translatable("gui.tharidiathings.stable.hint.food"));
        }

        if (stable.canCollectEggs()) {
            hints.add(Component.translatable("gui.tharidiathings.stable.hint.eggs"));
        }
        if (stable.canCollectMilk()) {
            hints.add(Component.translatable("gui.tharidiathings.stable.hint.milk"));
        }
        if (stable.canCollectManure()) {
            hints.add(Component.translatable("gui.tharidiathings.stable.hint.manure"));
        }

        if (animalCount > 0 && stable.getBeddingFreshness() == 0) {
            hints.add(Component.translatable("gui.tharidiathings.stable.hint.bedding"));
        } else if (animalCount > 0 && stable.getBeddingFreshness() > 0 && stable.getBeddingFreshness() < 40) {
            hints.add(Component.translatable("gui.tharidiathings.stable.hint.bedding_dirty"));
        }

        if (animalCount > 0 && !stable.hasShelterUpgrade()) {
            hints.add(Component.translatable("gui.tharidiathings.stable.hint.shelter"));
        }

        if (animalCount == 0) {
            hints.add(Component.translatable("gui.tharidiathings.stable.hint.add_animal"));
        }

        if (!hints.isEmpty()) {
            lines.add(new LineData(LineType.EMPTY, Component.empty()));
            lines.add(new LineData(LineType.HEADER, Component.translatable("gui.tharidiathings.stable.hints")));

            int maxHints = Math.min(3, hints.size());
            for (int i = 0; i < maxHints; i++) {
                lines.add(new LineData(LineType.NORMAL, Component.literal("\u25B8 ").append(hints.get(i))));
            }
        }

        return lines;
    }

    // ==================== BREEDING FOOD HELPER ====================

    private static Component getBreedingFoodName(EntityType<?> entityType) {
        if (entityType == null) return Component.translatable("item.minecraft.wheat");

        if (entityType == EntityType.COW || entityType == EntityType.SHEEP ||
            entityType == EntityType.MOOSHROOM || entityType == EntityType.GOAT) {
            return Component.translatable("item.minecraft.wheat");
        }
        if (entityType == EntityType.CHICKEN) {
            return Component.translatable("gui.tharidiathings.stable.breeding.food.seeds");
        }
        if (entityType == EntityType.PIG) {
            return Component.translatable("gui.tharidiathings.stable.breeding.food.pig");
        }
        if (entityType == EntityType.RABBIT) {
            return Component.translatable("gui.tharidiathings.stable.breeding.food.rabbit");
        }
        if (entityType == EntityType.HORSE || entityType == EntityType.DONKEY || entityType == EntityType.MULE) {
            return Component.translatable("gui.tharidiathings.stable.breeding.food.horse");
        }
        if (entityType == EntityType.LLAMA || entityType == EntityType.TRADER_LLAMA) {
            return Component.translatable("item.minecraft.hay_block");
        }
        if (entityType == EntityType.TURTLE) {
            return Component.translatable("item.minecraft.seagrass");
        }
        if (entityType == EntityType.CAMEL) {
            return Component.translatable("item.minecraft.cactus");
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId != null) {
            String path = entityId.getPath().toLowerCase();
            if (path.contains("chicken") || path.contains("hen") || path.contains("duck") ||
                path.contains("turkey") || path.contains("goose")) {
                return Component.translatable("gui.tharidiathings.stable.breeding.food.seeds");
            }
            if (path.contains("pig") || path.contains("boar") || path.contains("hog")) {
                return Component.translatable("gui.tharidiathings.stable.breeding.food.pig");
            }
            if (path.contains("rabbit") || path.contains("bunny")) {
                return Component.translatable("gui.tharidiathings.stable.breeding.food.rabbit");
            }
            if (path.contains("horse") || path.contains("donkey") || path.contains("mule")) {
                return Component.translatable("gui.tharidiathings.stable.breeding.food.horse");
            }
        }

        return Component.translatable("item.minecraft.wheat");
    }

    // ==================== UTILITY ====================

    private static String formatEntityName(String path) {
        if (path == null || path.isEmpty()) return "Unknown";
        String formatted = path.replace("_", " ");
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }

    private static Component getResourceStatusComponent(float level) {
        if (level >= 0.7f) return Component.translatable("gui.tharidiathings.stable.resource.level.high");
        if (level >= 0.3f) return Component.translatable("gui.tharidiathings.stable.resource.level.medium");
        if (level > 0) return Component.translatable("gui.tharidiathings.stable.resource.level.low");
        return Component.translatable("gui.tharidiathings.stable.resource.level.empty");
    }

    private static LineType getResourceLineType(float level) {
        if (level >= 0.7f) return LineType.GOOD;
        if (level >= 0.3f) return LineType.NORMAL;
        if (level > 0) return LineType.WARNING;
        return LineType.DANGER;
    }

    // ==================== RENDERING (compact layout) ====================

    private static void renderOverlay(GuiGraphics guiGraphics, Minecraft mc, List<LineData> lines) {
        int padding = 6;
        int lineHeight = 9;
        int maxWidth = 0;

        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY) continue;
            Component styledLine = line.text().copy().withStyle(style -> style.withFont(MedievalGuiRenderer.MEDIEVAL_FONT));
            int width = mc.font.width(styledLine);
            // Body text is rendered smaller, so its screen width is scaled down
            if (line.type() != LineType.TITLE && line.type() != LineType.HEADER) {
                width = Math.round(width * BODY_SCALE);
            }
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        int boxWidth = maxWidth + padding * 2;
        int boxHeight = measureHeight(mc, lines, lineHeight) + padding * 2;

        int startX = 10;
        int startY = 10;

        guiGraphics.fill(startX + 2, startY + 2, startX + boxWidth + 2, startY + boxHeight + 2, MedievalGuiRenderer.SHADOW_DARK);
        MedievalGuiRenderer.renderParchmentBackground(guiGraphics, startX, startY, boxWidth, boxHeight);
        guiGraphics.renderOutline(startX, startY, boxWidth, boxHeight, MedievalGuiRenderer.BRONZE);
        guiGraphics.renderOutline(startX + 2, startY + 2, boxWidth - 4, boxHeight - 4,
            ((MedievalGuiRenderer.BRONZE & 0x00FFFFFF) | 0x40000000));

        int scaledLineHeight = Math.round(lineHeight * BODY_SCALE) + BODY_LINE_SPACING;
        int y = startY + padding;
        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY) {
                y += 3;
                continue;
            }

            int color = getColorForType(line.type());

            int x = startX + padding;
            Component styledText = line.text().copy().withStyle(style -> style.withFont(MedievalGuiRenderer.MEDIEVAL_FONT));

            if (line.type() == LineType.TITLE) {
                int titleWidth = mc.font.width(styledText);
                int titleX = startX + (boxWidth - titleWidth) / 2;
                guiGraphics.drawString(mc.font, styledText, titleX, y, color, false);
                y += mc.font.lineHeight + 2;
                continue;
            }

            if (line.type() == LineType.HEADER) {
                guiGraphics.drawString(mc.font, styledText, x, y, color, false);
                y += lineHeight;
            } else {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(BODY_SCALE, BODY_SCALE, 1.0f);
                guiGraphics.drawString(mc.font, styledText,
                    Math.round(x / BODY_SCALE), Math.round(y / BODY_SCALE), color, false);
                guiGraphics.pose().popPose();
                y += scaledLineHeight;
            }
        }
    }

    private static int measureHeight(Minecraft mc, List<LineData> lines, int lineHeight) {
        int scaledLineHeight = Math.round(lineHeight * BODY_SCALE) + BODY_LINE_SPACING;
        int height = 0;
        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY) {
                height += 3;
            } else if (line.type() == LineType.TITLE) {
                height += mc.font.lineHeight + 2;
            } else if (line.type() == LineType.HEADER) {
                height += lineHeight;
            } else {
                height += scaledLineHeight;
            }
        }
        return height;
    }

    private static int getColorForType(LineType type) {
        return switch (type) {
            case TITLE -> COLOR_TITLE;
            case HEADER -> COLOR_HEADER;
            case GOOD -> COLOR_GOOD;
            case WARNING -> COLOR_WARNING;
            case DANGER -> COLOR_DANGER;
            case GRAY -> COLOR_GRAY;
            default -> COLOR_NORMAL;
        };
    }
}
