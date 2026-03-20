package com.THproject.tharidia_things.block.alchemist;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.THproject.tharidia_things.item.alchemist_potion.AlchemistPotions.BALL_POTION;

/**
 * Hard-coded mapping from result-jar value triplets to alchemist output ItemStacks.
 *
 * <p>Suppliers are evaluated lazily (after registries are frozen) to avoid
 * static-init ordering issues. Order of values matches result-jar fill order (0, 1, 2).
 */
public class AlchemistPotionRegistry {

    private record Recipe(int v0, int v1, int v2, int color,List<Holder<Potion>> effects) {
        boolean matches(int[] values) {
            return values[0] == v0 && values[1] == v1 && values[2] == v2;
        }

        ItemStack getPotion(){
            ItemStack stack = new ItemStack(BALL_POTION.get());
            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, false));
            PotionContents contents = PotionContents.EMPTY;
            for (Holder<Potion> effect : effects) {
                contents = contents.withPotion(effect);
            }
            stack.set(DataComponents.POTION_CONTENTS, contents);
            return stack;
        }
    }

    // TODO add all the recipes of the potion combination here
    private static final Recipe[] RECIPES = {
        new Recipe(8, 8, 8, 0xFF2222, List.of(Potions.HEALING)),
        new Recipe(6, 6, 6, 0x55AAFF, List.of(Potions.SWIFTNESS)),
        new Recipe(10, 10, 10, 0x22CC44, List.of(Potions.LEAPING)),
        new Recipe(12, 12, 12, 0x9900CC, List.of(Potions.HARMING)),
    };

    /** Returns a copy of the output ItemStack for the given triplet, or {@code null} if no match. */
    @Nullable
    public static ItemStack findPotion(int[] resultJarValues) {
        if (resultJarValues.length < 3) return null;
        for (Recipe r : RECIPES) {
            if (r.matches(resultJarValues)) return r.getPotion();
        }
        return null;
    }
}
