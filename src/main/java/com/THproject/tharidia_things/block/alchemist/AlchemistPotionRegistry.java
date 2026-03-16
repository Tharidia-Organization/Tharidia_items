package com.THproject.tharidia_things.block.alchemist;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

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

    // ── Recipe builders ───────────────────────────────────────────────────────

    private static ItemStack instantHealthStack() {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.of(Potions.HEALING), Optional.empty(), java.util.List.of()));
        return stack;
    }
}
