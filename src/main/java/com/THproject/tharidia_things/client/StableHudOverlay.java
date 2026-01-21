package com.THproject.tharidia_things.client;

import com.THproject.tharidia_things.TharidiaThings;
import com.THproject.tharidia_things.block.StableBlock;
import com.THproject.tharidia_things.block.StableDummyBlock;
import com.THproject.tharidia_things.block.entity.StableBlockEntity;
import com.THproject.tharidia_things.client.gui.medieval.MedievalGuiRenderer;
import com.THproject.tharidia_things.stable.AnimalTypeHelper;
import com.THproject.tharidia_things.stable.StableConfigLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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

    // Colors
    private static final int COLOR_TITLE = MedievalGuiRenderer.GOLD_LEAF;
    private static final int COLOR_HEADER = MedievalGuiRenderer.ROYAL_GOLD;
    private static final int COLOR_NORMAL = MedievalGuiRenderer.BROWN_INK;
    private static final int COLOR_GOOD = MedievalGuiRenderer.BRONZE;
    private static final int COLOR_WARNING = MedievalGuiRenderer.PURPLE_REGAL;
    private static final int COLOR_DANGER = MedievalGuiRenderer.DEEP_CRIMSON;
    private static final int COLOR_GRAY = MedievalGuiRenderer.STONE_GRAY;

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
            // Get master position from dummy
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
            lines.add(new LineData(LineType.GRAY, Component.translatable("gui.tharidiathings.stable.no_animals")));
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

            // Individual animal info
            int idx = 0;
            for (StableBlockEntity.AnimalData animal : stable.getAnimals()) {
                idx++;
                lines.addAll(buildAnimalLines(animal, idx, config));
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
            lines.add(new LineData(LineType.GRAY, Component.translatable("gui.tharidiathings.stable.shelter")
                .append(": ").append(Component.translatable("gui.tharidiathings.stable.shelter.not_installed"))));
        }

        // Production summary
        if (animalCount > 0) {
            lines.add(new LineData(LineType.EMPTY, Component.empty()));
            lines.add(new LineData(LineType.HEADER, Component.translatable("gui.tharidiathings.stable.production")));

            boolean canProduce = stable.hasWater() && stable.getFoodAmount() > 0;
            if (!canProduce) {
                lines.add(new LineData(LineType.DANGER, Component.translatable("gui.tharidiathings.stable.production.blocked")));
            } else {
                // Check day/night
                if (stable.getLevel() != null) {
                    long dayTime = stable.getLevel().getDayTime();
                    boolean isDaytime = config.isDaytime(dayTime);
                    if (!isDaytime) {
                        lines.add(new LineData(LineType.GRAY, Component.translatable("gui.tharidiathings.stable.production.resting")));
                    } else {
                        lines.add(new LineData(LineType.GOOD, Component.translatable("gui.tharidiathings.stable.production.active")));
                    }
                }
            }

            // Milk available
            if (stable.canCollectMilk()) {
                lines.add(new LineData(LineType.GOOD, Component.translatable("gui.tharidiathings.stable.production.milk_available")));
            }

            // Eggs available
            if (stable.canCollectEggs()) {
                int eggCount = stable.getTotalEggCount();
                lines.add(new LineData(LineType.GOOD, Component.translatable("gui.tharidiathings.stable.production.eggs_available", eggCount)));
            }
        }

        return lines;
    }

    /**
     * Builds info lines for a single animal.
     */
    private static List<LineData> buildAnimalLines(StableBlockEntity.AnimalData animal, int index,
                                                   com.THproject.tharidia_things.stable.StableConfig config) {
        List<LineData> lines = new ArrayList<>();

        // Phase
        Component phase;
        LineType phaseType;
        if (animal.isBaby) {
            phase = Component.translatable("gui.tharidiathings.stable.animal.baby");
            phaseType = LineType.NORMAL;
        } else if (AnimalTypeHelper.isEggProducingType(animal.entityType) &&
                   animal.totalEggsProduced >= config.maxEggsPerChicken()) {
            phase = Component.translatable("gui.tharidiathings.stable.animal.barren");
            phaseType = LineType.GRAY;
        } else {
            phase = Component.translatable("gui.tharidiathings.stable.animal.adult");
            phaseType = LineType.NORMAL;
        }

        // State
        StableBlockEntity.AnimalState state = animal.calculateState();
        String stateIcon;
        LineType stateType;
        switch (state) {
            case GOLD -> {
                stateIcon = "★";
                stateType = LineType.TITLE;
            }
            case OK -> {
                stateIcon = "●";
                stateType = LineType.GOOD;
            }
            case LOW -> {
                stateIcon = "▼";
                stateType = LineType.WARNING;
            }
            case CRITICAL -> {
                stateIcon = "✖";
                stateType = LineType.DANGER;
            }
            default -> {
                stateIcon = "?";
                stateType = LineType.NORMAL;
            }
        }

        lines.add(new LineData(phaseType, Component.literal("  " + index + ". ")
            .append(phase)
            .append(" " + stateIcon)));

        // Growth progress for babies
        if (animal.isBaby) {
            float growthPercent = (float) animal.growthTicks / config.growthTimeTicks() * 100;
            lines.add(new LineData(LineType.GRAY, Component.literal("     ")
                .append(Component.translatable("gui.tharidiathings.stable.animal.growth", (int) growthPercent))));
        }

        // Disease warning
        if (animal.diseased) {
            long now = System.currentTimeMillis();
            long diseaseStart = animal.diseaseStartTimestamp > 0 ? animal.diseaseStartTimestamp : now;
            animal.diseaseStartTimestamp = diseaseStart;
            long diseaseMinutes = (now - diseaseStart) / 60000;
            long timeRemaining = Math.max(0, 120 - diseaseMinutes);
            lines.add(new LineData(LineType.DANGER, Component.literal("     ")
                .append(Component.translatable("gui.tharidiathings.stable.animal.sick", (int) timeRemaining))));
        }

        // Milk ready
        if (!animal.isBaby && AnimalTypeHelper.isMilkProducingType(animal.entityType) && animal.milkReady) {
            lines.add(new LineData(LineType.GOOD, Component.literal("     ")
                .append(Component.translatable("gui.tharidiathings.stable.animal.milk_ready"))));
        }

        // Eggs available
        if (!animal.isBaby && AnimalTypeHelper.isEggProducingType(animal.entityType) && animal.eggCount > 0) {
            lines.add(new LineData(LineType.GOOD, Component.literal("     ")
                .append(Component.translatable("gui.tharidiathings.stable.animal.eggs", animal.eggCount))));
        }

        return lines;
    }

    /**
     * Formats an entity name to be more readable.
     */
    private static String formatEntityName(String path) {
        // Capitalize first letter and replace underscores
        if (path == null || path.isEmpty()) return "Unknown";
        String formatted = path.replace("_", " ");
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }

    /**
     * Gets a translatable component for a resource level.
     */
    private static Component getResourceStatusComponent(float level) {
        if (level >= 0.7f) return Component.translatable("gui.tharidiathings.stable.resource.level.high");
        if (level >= 0.3f) return Component.translatable("gui.tharidiathings.stable.resource.level.medium");
        if (level > 0) return Component.translatable("gui.tharidiathings.stable.resource.level.low");
        return Component.translatable("gui.tharidiathings.stable.resource.level.empty");
    }

    /**
     * Gets the line type for a resource level.
     */
    private static LineType getResourceLineType(float level) {
        if (level >= 0.7f) return LineType.GOOD;
        if (level >= 0.3f) return LineType.NORMAL;
        if (level > 0) return LineType.WARNING;
        return LineType.DANGER;
    }

    /**
     * Renders the overlay on screen.
     */
    private static void renderOverlay(GuiGraphics guiGraphics, Minecraft mc, List<LineData> lines) {
        int padding = 8;
        int lineHeight = 10;
        int maxWidth = 0;

        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY) continue;
            int width = mc.font.width(line.text());
            if (line.type() == LineType.TITLE) {
                width += 20;
            }
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        int boxWidth = maxWidth + padding * 2;
        int boxHeight = measureHeight(mc, lines, lineHeight) + padding * 2;

        int startX = 10;
        int startY = 10;

        guiGraphics.fill(startX + 2, startY + 2, startX + boxWidth + 2, startY + boxHeight + 2, MedievalGuiRenderer.SHADOW);
        MedievalGuiRenderer.renderParchmentBackground(guiGraphics, startX, startY, boxWidth, boxHeight);
        guiGraphics.renderOutline(startX, startY, boxWidth, boxHeight, MedievalGuiRenderer.BRONZE);
        guiGraphics.renderOutline(startX + 2, startY + 2, boxWidth - 4, boxHeight - 4,
            ((MedievalGuiRenderer.BRONZE & 0x00FFFFFF) | 0x40000000));

        int y = startY + padding;
        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY) {
                y += lineHeight / 2;
                continue;
            }

            int color = getColorForType(line.type());

            int x = startX + padding;
            if (line.type() == LineType.TITLE) {
                MedievalGuiRenderer.renderMedievalTitle(guiGraphics, line.text().getString(), startX, y, boxWidth);
                y += mc.font.lineHeight + 8;
                continue;
            }

            guiGraphics.drawString(mc.font, line.text(), x, y, color, false);
            if (line.type() == LineType.HEADER) {
                int dividerWidth = Math.max(60, mc.font.width(line.text()));
                MedievalGuiRenderer.renderMedievalDivider(guiGraphics, x, y + lineHeight, dividerWidth);
                y += lineHeight + 4;
            } else {
                y += lineHeight;
            }
        }
    }

    private static int measureHeight(Minecraft mc, List<LineData> lines, int lineHeight) {
        int height = 0;
        for (LineData line : lines) {
            if (line.type() == LineType.EMPTY) {
                height += lineHeight / 2;
            } else if (line.type() == LineType.TITLE) {
                height += mc.font.lineHeight + 8;
            } else if (line.type() == LineType.HEADER) {
                height += lineHeight + 4;
            } else {
                height += lineHeight;
            }
        }
        return height;
    }

    /**
     * Gets the color for a line type.
     */
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
