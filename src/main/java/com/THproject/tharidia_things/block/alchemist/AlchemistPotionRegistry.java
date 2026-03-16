package com.THproject.tharidia_things.block.alchemist;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static com.THproject.tharidia_things.item.alchemist_potion.AlchemistPotions.BALL_POTION;

/**
 * Hard-coded mapping from result-jar value triplets to alchemist output ItemStacks.
 *
 * <p>Suppliers are evaluated lazily (after registries are frozen) to avoid
 * static-init ordering issues. Order of values matches result-jar fill order (0, 1, 2).
 */
public class AlchemistPotionRegistry {

    private record Recipe(int v0, int v1, int v2, Supplier<ItemStack> stack) {
        boolean matches(int[] values) {
            return values[0] == v0 && values[1] == v1 && values[2] == v2;
        }
    }

    // TODO add all the recipes of the potion combination here
    private static final Recipe[] RECIPES = {
        new Recipe(8, 8, 8, AlchemistPotionRegistry::instantHealthStack),
        new Recipe(6, 6, 6, AlchemistPotionRegistry::speedStack),
        new Recipe(10, 10, 10, AlchemistPotionRegistry::jumpBoostStack),
        new Recipe(12, 12, 12, AlchemistPotionRegistry::instantDamageStack),
    };

    /** Returns a copy of the output ItemStack for the given triplet, or {@code null} if no match. */
    @Nullable
    public static ItemStack findPotion(int[] resultJarValues) {
        if (resultJarValues.length < 3) return null;
        for (Recipe r : RECIPES) {
            if (r.matches(resultJarValues)) return r.stack().get().copy();
        }
        return null;
    }

    // ── Colour constants — edit these to change each potion's overlay tint ──────
    private static final int COLOR_HEALING        = 0xFF2222; // rosso
    private static final int COLOR_SPEED          = 0x55AAFF; // azzurro
    private static final int COLOR_JUMP_BOOST     = 0x22CC44; // verde
    private static final int COLOR_INSTANT_DAMAGE = 0x9900CC; // viola

    // ── Recipe builders ───────────────────────────────────────────────────────

    private static ItemStack instantHealthStack() {
        ItemStack stack = new ItemStack(BALL_POTION.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(COLOR_HEALING, false));
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Potions.HEALING).withEffectAdded(Potions.HEALING.value().getEffects().get(0)));
        return stack;
    }

    private static ItemStack speedStack() {
        ItemStack stack = new ItemStack(BALL_POTION.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(COLOR_SPEED, false));
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Potions.SWIFTNESS).withEffectAdded(Potions.SWIFTNESS.value().getEffects().get(0)));
        return stack;
    }

    private static ItemStack jumpBoostStack() {
        ItemStack stack = new ItemStack(BALL_POTION.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(COLOR_JUMP_BOOST, false));
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Potions.LEAPING).withEffectAdded(Potions.LEAPING.value().getEffects().get(0)));
        return stack;
    }

    private static ItemStack instantDamageStack() {
        ItemStack stack = new ItemStack(BALL_POTION.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(COLOR_INSTANT_DAMAGE, false));
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Potions.HARMING).withEffectAdded(Potions.HARMING.value().getEffects().get(0)));
        return stack;
    }
}
