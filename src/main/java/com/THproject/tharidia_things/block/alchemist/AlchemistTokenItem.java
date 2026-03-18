package com.THproject.tharidia_things.block.alchemist;

import com.THproject.tharidia_things.TharidiaThings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * A transient token item representing a numeric value being carried by the player
 * during the Alchemist Table crafting sequence.
 *
 * <p>The value is stored in {@link DataComponents#CUSTOM_DATA} under the key
 * {@code "AlchemistValue"} and is intentionally not shown in the item's tooltip.
 */
public class AlchemistTokenItem extends Item {

    public AlchemistTokenItem(Properties properties) {
        super(properties);
    }

    // ==================== Factory / Accessors ====================

    /**
     * Creates a result token carrying {@code value} using the AlchemistTokenItem as display.
     * Used for operation results that aren't tied to a specific jar's appearance.
     */
    public static ItemStack create(int value) {
        ItemStack stack = new ItemStack(TharidiaThings.ALCHEMIST_TOKEN.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt("AlchemistValue", value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /**
     * Creates a jar token that appears as the {@code alchemist_jar} item and
     * carries the AlchemistValue tag so it is recognised as a token by this class.
     */
    public static ItemStack createFromJar(int value) {
        ItemStack token = new ItemStack(TharidiaThings.ALCHEMIST_JAR.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt("AlchemistValue", value);
        token.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return token;
    }

    /** Reads the integer value stored in a token stack. Returns 0 if not a valid token. */
    public static int getValue(ItemStack stack) {
        if (!isToken(stack)) return 0;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag().getInt("AlchemistValue") : 0;
    }

    /**
     * Returns true if the stack is a token: either an AlchemistTokenItem instance
     * (result token) or any item carrying the "AlchemistValue" custom data tag (jar token).
     */
    public static boolean isToken(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof AlchemistTokenItem) return true;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains("AlchemistValue");
    }
}
