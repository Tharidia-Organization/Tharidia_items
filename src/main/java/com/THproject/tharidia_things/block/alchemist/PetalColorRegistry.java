package com.THproject.tharidia_things.block.alchemist;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Registry of named petal colors used by the Alchemist Table crafting system.
 *
 * <h3>Color → Integer mapping</h3>
 * Three primary colors are assigned single-digit values (1, 2, 3).
 * Three secondary (mix) colors are assigned two-digit values whose digits
 * are the values of the two primaries they blend:
 * <pre>
 *   Rosso    (Red)    = 1
 *   Giallo   (Yellow) = 2
 *   Blu      (Blue)   = 3
 *   Arancione (R+Y)   = 12  ← digits 1 and 2
 *   Verde    (Y+B)    = 23  ← digits 2 and 3
 *   Viola    (R+B)    = 13  ← digits 1 and 3
 * </pre>
 *
 * <h3>Classification</h3>
 * Any RGB color (e.g. from the herbalist minigame) is classified by
 * converting to HSL and bucketing the hue angle.  Achromatic petals
 * (saturation < 15 %) return {@code null}.
 */
public final class PetalColorRegistry {

    private PetalColorRegistry() {}

    // ==================== Color Entries ====================

    public enum PetalColor {
        ROSSO    ("rosso",     1,  0xDD2222),
        GIALLO   ("giallo",    2,  0xEEDD00),
        BLU      ("blu",       3,  0x2244DD),
        ARANCIONE("arancione", 12, 0xFF8800),
        VERDE    ("verde",     23, 0x22CC44),
        VIOLA    ("viola",     13, 0x8800CC);

        /** Command/display name (lower-case Italian). */
        public final String name;
        /** Crafting integer value used by {@link AlchemistJarRegistry}. */
        public final int value;
        /** Canonical RGB used when creating petal stacks via command. */
        public final int rgb;

        PetalColor(String name, int value, int rgb) {
            this.name = name;
            this.value = value;
            this.rgb   = rgb;
        }
    }

    // ==================== Public API ====================

    /** All defined color names, for command suggestion. */
    public static List<String> allNames() {
        return Arrays.stream(PetalColor.values()).map(c -> c.name).toList();
    }

    /** Returns the {@link PetalColor} matching {@code name} (case-insensitive), or empty. */
    public static Optional<PetalColor> fromName(String name) {
        for (PetalColor c : PetalColor.values()) {
            if (c.name.equalsIgnoreCase(name)) return Optional.of(c);
        }
        return Optional.empty();
    }

    /**
     * Classifies an RGB integer (0xRRGGBB) into one of the six named colors
     * using HSL hue bucketing.
     *
     * @return the matching {@link PetalColor}, or {@code null} if the color is
     *         achromatic (saturation < 15 %) and cannot be classified.
     */
    @Nullable
    public static PetalColor classifyRgb(int rgb) {
        float[] hsl = rgbToHsl(rgb);
        float h = hsl[0];
        float s = hsl[1];

        if (s < 0.15f) return null; // achromatic — no classification

        // Hue buckets (degrees 0-360):
        //   [330, 360) ∪ [0, 20)  → Rosso     (1)
        //   [20,  45)             → Arancione  (12)
        //   [45,  70)             → Giallo     (2)
        //   [70,  165)            → Verde      (23)
        //   [165, 260)            → Blu        (3)
        //   [260, 330)            → Viola      (13)
        if (h >= 330 || h < 20)  return PetalColor.ROSSO;
        if (h < 45)              return PetalColor.ARANCIONE;
        if (h < 70)              return PetalColor.GIALLO;
        if (h < 165)             return PetalColor.VERDE;
        if (h < 260)             return PetalColor.BLU;
        return PetalColor.VIOLA;
    }

    /**
     * Returns the crafting integer value of the petal in {@code stack},
     * or 0 if the stack is not a petal or has an unclassifiable color.
     */
    public static int getPetalValue(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof com.THproject.tharidia_things.item.PetalItem)) {
            return 0;
        }
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        if (dyed == null) return 0;
        PetalColor color = classifyRgb(dyed.rgb());
        return color != null ? color.value : 0;
    }

    /**
     * Creates a petal {@link ItemStack} with the given color and amount.
     */
    public static ItemStack createPetalStack(PetalColor color, int amount) {
        ItemStack stack = new ItemStack(TharidiaThings.PETAL.get(), amount);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color.rgb, false));
        return stack;
    }

    // ==================== Internal helpers ====================

    /**
     * Converts an 0xRRGGBB integer to HSL.
     *
     * @return float[3] = { hue [0,360), saturation [0,1], lightness [0,1] }
     */
    private static float[] rgbToHsl(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >>  8) & 0xFF) / 255.0f;
        float b = ( rgb        & 0xFF) / 255.0f;

        float max   = Math.max(r, Math.max(g, b));
        float min   = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float l = (max + min) / 2.0f;
        float s = 0.0f;
        float h = 0.0f;

        if (delta > 1e-6f) {
            s = delta / (1.0f - Math.abs(2.0f * l - 1.0f));

            if      (max == r) h = 60.0f * (((g - b) / delta) % 6);
            else if (max == g) h = 60.0f * (((b - r) / delta) + 2);
            else               h = 60.0f * (((r - g) / delta) + 4);

            if (h < 0) h += 360.0f;
        }

        return new float[]{h, s, l};
    }
}
